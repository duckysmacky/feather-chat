package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.ConsoleInputListener;
import io.github.duckysmacky.featherchat.common.Message;
import io.github.duckysmacky.featherchat.common.MessageListener;
import io.github.duckysmacky.featherchat.common.MessageType;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private final UUID id;
    private final BlockingQueue<Message> incomingMessagePool;
    private final BlockingQueue<Message> outgoingMessagePool;
    private final ExecutorService messageHandler;
    private final AtomicBoolean isConnected;
    private final Thread consoleInputListener;
    private final Thread incomingMessageListener;
    private final Thread outgoingMessageListener;
    private ServerConnection server;

    public Client() {
        this.id = UUID.randomUUID();
        this.incomingMessagePool = new LinkedBlockingQueue<>();
        this.outgoingMessagePool = new LinkedBlockingQueue<>();
        this.messageHandler = Executors.newSingleThreadExecutor();
        this.isConnected = new AtomicBoolean();

        this.consoleInputListener = new Thread(new ConsoleInputListener(
            isConnected::get,
            input -> outgoingMessagePool.add(Message.textMessage(id, input)),
            () -> new Thread(this::disconnect).start()
        ));

        this.incomingMessageListener = new Thread(new MessageListener(isConnected::get, incomingMessagePool, this::handleIncomingMessage));
        this.outgoingMessageListener = new Thread(new MessageListener(isConnected::get, outgoingMessagePool, this::handleOutgoingMessage));
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Error: not enough arguments provided");
            System.err.println("Usage: client <host> <port>");
            return;
        }

        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);

        Client client = new Client();

        try {
            client.connect(serverAddress, serverPort);
        } catch (IOException e) {
            System.err.printf("Unable to connect to the server: %s%n", e.getMessage());
        }
    }

    private void handleIncomingMessage(Message message) {
        messageHandler.submit(() -> {
            if (message.getType() == MessageType.TEXT)
                System.out.println(message);
        });
    }

    private void handleOutgoingMessage(Message message) {
        messageHandler.submit(() -> {
            if (!isConnected.get()) return;

            try {
                server.sendMessage(message);
            } catch (IOException e) {
                System.err.printf("Unable to send a message to server: %s%n", e.getMessage());
            }

            System.out.println(message);
        });
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
        System.out.println("Disconnecting from server...");

        try {
            Message disconnectionMessage = Message.disconnectMessage(id);
            server.sendMessage(disconnectionMessage);
        } catch (IOException e) {
            System.err.printf("Unable to send DISCONNECT message to server: %s%n", e.getMessage());
        }

        this.server.close();

        this.isConnected.set(false);
        this.incomingMessageListener.interrupt();
        this.outgoingMessageListener.interrupt();

        try {
            this.consoleInputListener.join();
            this.incomingMessageListener.join();
            this.outgoingMessageListener.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.messageHandler.shutdown();

        System.out.println("Successfully disconnected from server");
    }
}
