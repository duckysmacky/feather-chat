package io.github.duckysmacky.featherchat.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer implements Closeable {
    private ServerSocket serverSocket;
    private final Thread connectionListener;
    private final Thread consoleListener;
    private final ExecutorService connectionManager;
    private final Map<String, ChatClient> clients;

    public ChatServer() {
        this.clients = new HashMap<>();
        this.connectionManager = Executors.newSingleThreadExecutor();

        this.connectionListener = new Thread(() -> {
            System.out.printf("Server is now listening on port %s%n", serverSocket.getLocalPort());

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (serverSocket.isClosed()) return;

                    connectClient(clientSocket);
                } catch (IOException e) {
                    System.err.printf("Unable to accept client connection: %s%n", e.getMessage());
                }
            }
        });

        this.consoleListener = new Thread(() -> {
            Scanner console = new Scanner(System.in);

            while (!serverSocket.isClosed()) {
                if (console.hasNextLine()) {
                    String input = console.nextLine();
                    if (serverSocket.isClosed()) return;

                    clients.values().forEach(client -> {
                        try {
                            client.send("server", input);
                        } catch (IOException e) {
                            System.err.printf("Unable to send a message to client '%s': %s%n", client.getId(), e.getMessage());
                            disconnectClient(client);
                        }
                    });
                }
            }
        });

    }

    private void connectClient(Socket clientSocket) {
        connectionManager.submit(() -> {
            ChatClient client = new ChatClient(clientSocket, this::handleMessage);
            clients.put(client.getId(), client);

            System.out.printf("New client connected: %s%n", client.getId());
        });
    }

    private void disconnectClient(ChatClient client) {
        connectionManager.submit(() -> {
            System.out.printf("Disconnecting from client '%s'...%n", client.getId());

            clients.remove(client.getId());
            client.close();

            System.out.printf("Successfully disconnected from client '%s'%n", client.getId());
        });
    }

    private void handleMessage(ChatClient sender, String message) {
        if (message.equalsIgnoreCase("disconnect")) {
            System.out.printf("Client '%s' requested disconnection%n", sender.getId());
            disconnectClient(sender);
            return;
        }

        clients.values().forEach(client -> {
            if (!client.getId().equals(sender.getId())) {
                try {
                    client.send(sender.getId(), message);
                } catch (IOException e) {
                    System.err.printf("Unable to send a message to client '%s': %s%n", client.getId(), e.getMessage());
                    disconnectClient(client);
                }
            }
        });

        System.out.printf("[%s] %s%n", sender.getId(), message);
    }

    public void start(int port) throws InterruptedException {
        System.out.println("Starting the server...");

        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.printf("Unable to start a server: %s%n", e.getMessage());
            throw new RuntimeException();
        }

        this.connectionListener.start();
        this.consoleListener.start();

        System.out.println("Successfully started the server");

        this.connectionListener.join();
        this.consoleListener.join();
    }

    @Override
    public void close() {
        System.out.println("Closing the server...");

        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.connectionListener.join();
            this.consoleListener.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        clients.values().forEach(ChatClient::close);
        clients.clear();

        System.out.println("Server successfully closed");
    }
}
