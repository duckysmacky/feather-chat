package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.listeners.ConsoleInputListener;
import io.github.duckysmacky.featherchat.common.request.Message;
import io.github.duckysmacky.featherchat.common.listeners.MessageListener;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private final UUID id;
    private final BlockingQueue<Message> incomingMessagePool;
    private final BlockingQueue<Message> outgoingMessagePool;
    private final AtomicBoolean isConnected;
    private final Thread consoleInputListener;
    private final Thread incomingMessageListener;
    private final Thread outgoingMessageListener;
    private ServerConnection server;

    public Client() {
        this.id = UUID.randomUUID();
        this.incomingMessagePool = new LinkedBlockingQueue<>();
        this.outgoingMessagePool = new LinkedBlockingQueue<>();
        this.isConnected = new AtomicBoolean();

        this.consoleInputListener = new Thread(new ConsoleInputListener(
            isConnected::get,
            input -> outgoingMessagePool.add(Message.textMessage(id, input)),
            this::disconnect
        ), "Console input Listener");

        this.incomingMessageListener = new Thread(new MessageListener(isConnected::get, incomingMessagePool, this::handleIncomingMessage), "Incoming message Listener");
        this.outgoingMessageListener = new Thread(new MessageListener(isConnected::get, outgoingMessagePool, this::handleOutgoingMessage), "Outgoing message Listener");
    }

    private void handleIncomingMessage(Message message) {
        switch (message.getType()) {
            case TEXT -> System.out.println(message);
            case DISCONNECT -> disconnect();
        }
    }

    private void handleOutgoingMessage(Message message) {
        if (!isConnected.get()) return;

        try {
            server.sendMessage(message);
        } catch (IOException e) {
            System.err.printf("Unable to send a message to server: %s%n", e.getMessage());
        }

        System.out.println(message);
    }

    public void connect(String host, int port) throws IOException {
        System.out.printf("Connecting to %s:%s...%n", host, port);

        this.server = new ServerConnection(host, port, incomingMessagePool);

        Message connectionMessage = Message.connectMessage(id);
        server.sendMessage(connectionMessage);

        this.isConnected.set(true);
        this.consoleInputListener.start();
        this.incomingMessageListener.start();
        this.outgoingMessageListener.start();

        System.out.printf("Successfully connected to %s:%s with ID '%s'%n", host, port, id);
        System.out.println("Press CTRL + D to disconnect");
    }

    public void disconnect() {
        new Thread(() -> {
            System.out.println("Disconnecting from server...");

            try {
                server.sendMessage(Message.disconnectMessage(id));
            } catch (IOException _) {}

            this.server.close();

            this.isConnected.set(false);
            this.consoleInputListener.interrupt();
            this.incomingMessageListener.interrupt();
            this.outgoingMessageListener.interrupt();

            try {
                this.consoleInputListener.join();
                this.incomingMessageListener.join();
                this.outgoingMessageListener.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Successfully disconnected from server");
        }, "Server disconnector").start();
    }
}
