package io.github.duckysmacky.featherchat.server;

import io.github.duckysmacky.featherchat.common.Message;
import io.github.duckysmacky.featherchat.common.MessageType;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Closeable {
    private final static String SERVER_ID = "SERVER";
    private ServerSocket serverSocket;
    private final Thread connectionListener;
    private final Thread consoleListener;
    private final ExecutorService connectionManager;
    private final Map<String, ClientConnection> clients;

    public Server() {
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
                    if (input == null || input.isBlank()) continue;

                    Message message = new Message(SERVER_ID, input);
                    handleMessage(message);
                }
            }
        });
    }

    public static void main(String[] args) {
        Server server = new Server();

        try {
            server.start(8080);
        } catch (IOException e) {
            System.err.printf("Unable to start a server: %s%n", e.getMessage());
            throw new RuntimeException();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        server.close();
    }

    private void connectClient(Socket clientSocket) {
        connectionManager.submit(() -> {
            ClientConnection client = new ClientConnection(clientSocket, this::handleMessage);
            clients.put(client.getId(), client);

            System.out.printf("New client connected: %s%n", client.getId());
        });
    }

    private void disconnectClient(ClientConnection client) {
        connectionManager.submit(() -> {
            System.out.printf("Disconnecting from client '%s'...%n", client.getId());

            clients.remove(client.getId());
            client.close();

            System.out.printf("Successfully disconnected from client '%s'%n", client.getId());
        });
    }

    private void handleMessage(Message message) {
        if (message.getType() == MessageType.DISCONNECT && !message.getSenderId().equals(SERVER_ID)) {
            System.out.printf("Client '%s' requested disconnection%n", message.getSenderId());

            ClientConnection client = clients.get(message.getSenderId());
            if (client != null)
                disconnectClient(client);

            return;
        }

        clients.values().forEach(client -> {
            if (!client.getId().equals(message.getSenderId())) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    System.err.printf("Unable to send a message to client '%s': %s%n", client.getId(), e.getMessage());
                    disconnectClient(client);
                }
            }
        });

        System.out.println(message);
    }

    public void start(int port) throws IOException, InterruptedException {
        System.out.println("Starting the server...");

        this.serverSocket = new ServerSocket(port);

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

        clients.values().forEach(ClientConnection::close);
        clients.clear();

        System.out.println("Server successfully closed");
    }
}
