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
                        application.displayError(String.format("Unable to read a message from server: %s%n", msg));
                        break;
                    }
                }
            }
        }, "Server message receiver");
        this.serverMessageReceiver.start();
    }

    public void handleIncomingMessage(Message message) {
        switch (message.getType()) {
            case TEXT -> application.displayMessage(message);
            case DISCONNECT -> {
                application.displayInfo("Server has disconnected you.");
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
                application.displayMessage(message);
            } catch (IOException e) {
                application.displayError(String.format("Unable to send message to server: %s%n", e.getMessage()));
            }
        });
    }

    public void stop() {
        isReceiving.set(false);
        serverMessageReceiver.interrupt();

        try {
            serverMessageReceiver.join();
        } catch (InterruptedException e) {
            application.displayError(String.format("Error while stopping message handler: %s%n", e.getMessage()));
        }

        messageSender.shutdown();
    }
}
