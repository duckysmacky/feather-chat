package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.request.Message;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageHandler {
    private final ServerConnection serverConnection;
    private final ChatApplication application;
    private final AtomicBoolean isReceiving;
    private final Thread serverMessageReceiver;
    private final ExecutorService messageSender;

    public MessageHandler(ServerConnection serverConnection, ChatApplication application) {
        this.serverConnection = serverConnection;
        this.application = application;
        this.isReceiving = new AtomicBoolean(true);
        this.messageSender = Executors.newSingleThreadExecutor();

        this.serverMessageReceiver = new Thread(() -> {
            while (isReceiving.get()) {
                try {
                    Message message = serverConnection.readMessage();
                    CompletableFuture.runAsync(() -> handleIncomingMessage(message));
                } catch (IOException e) {
                    String msg = e.getMessage();
                    if (msg != null) {
                        if (msg.strip().equalsIgnoreCase("socket closed")) break;

                        System.err.printf("Unable to read a message from server: %s%n", msg);
                    }
                }
            }
        }, "Server message receiver");
        this.serverMessageReceiver.start();
    }

    public void handleIncomingMessage(Message message) {
        switch (message.getType()) {
            case TEXT -> application.appendMessageBox(message.toString());
            case DISCONNECT -> {
                application.appendMessageBox("Server has disconnected you.");
                serverConnection.close();
                stop();
            }
        }
    }

    public void handleOutgoingMessage(Message message) {
        if (!isReceiving.get()) return;

        messageSender.submit(() -> {
            try {
                serverConnection.writeMessage(message);
                application.appendMessageBox(message.toString());
            } catch (IOException e) {
                System.err.printf("Unable to send message to server: %s%n", e.getMessage());
            }
        });
    }

    public void stop() {
        isReceiving.set(false);
        serverMessageReceiver.interrupt();

        try {
            serverMessageReceiver.join();
        } catch (InterruptedException e) {
            System.err.printf("Error while waiting for server message listener to stop: %s%n", e.getMessage());
        }

        messageSender.shutdown();
    }
}
