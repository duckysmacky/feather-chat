package io.github.duckysmacky.featherchat.server;

import io.github.duckysmacky.featherchat.common.Message;
import io.github.duckysmacky.featherchat.common.MessageListener;
import io.github.duckysmacky.featherchat.common.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private final static UUID SERVER_ID = new UUID(0, 0);
    private final Map<UUID, ClientConnection> clients;
    private final BlockingQueue<Message> messagePool;
    private final ExecutorService connectionManager;
    private final ExecutorService messageManager;
    private final Thread consoleInputListener;
    private final Thread messagePoolListener;
    private final Thread connectionListener;
    private ServerSocket serverSocket;

    public Server() {
        this.clients = new HashMap<>();
        this.messagePool = new LinkedBlockingQueue<>();
        this.connectionManager = Executors.newSingleThreadExecutor();
        this.messageManager = Executors.newFixedThreadPool(10);

        this.consoleInputListener = new Thread(() -> {
            Scanner console = new Scanner(System.in);

            while (!serverSocket.isClosed()) {
                if (console.hasNextLine()) {
                    String input = console.nextLine();
                    if (serverSocket.isClosed()) return;

                    if (input != null && !input.isBlank())
                        messagePool.add(Message.textMessage(SERVER_ID, input));
                } else {
                    connectionManager.submit(this::stop);
                    break;
                }
            }
        });

        this.messagePoolListener = new Thread(new MessageListener(() -> !serverSocket.isClosed(), messagePool, this::handleMessage));

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
    }

    public static void main(String[] args) {
        Server server = new Server();

        try {
            server.start(8080);
        } catch (IOException e) {
            System.err.printf("Unable to start a server: %s%n", e.getMessage());
        }
    }

    private void connectClient(Socket clientSocket) {
        connectionManager.submit(() -> {
            try {
                ClientConnection client = new ClientConnection(clientSocket, messagePool);

                clients.put(client.getId(), client);
                System.out.printf("New client connected: %s%n", client.getId());
            } catch (IOException e) {
                System.err.printf("Unable to connect the client: %s%n", e.getMessage());
            }
        });
    }

    private void disconnectClient(ClientConnection client) {
        connectionManager.submit(() -> {
            ClientConnection disconnectedClient = clients.remove(client.getId());

            if (disconnectedClient != null) {
                System.out.printf("Disconnecting from client '%s'...%n", disconnectedClient.getId());
                disconnectedClient.close();
                System.out.printf("Successfully disconnected from client '%s'%n", disconnectedClient.getId());
            }
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

        clients.values().forEach(client -> messageManager.submit(() -> {
            if (!client.getId().equals(message.getSenderId())) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    System.err.printf("Unable to send a message to client '%s': %s%n", client.getId(), e.getMessage());
                    disconnectClient(client);
                }
            }
        }));

        System.out.println(message);
    }

    public void start(int port) throws IOException {
        System.out.println("Starting the server...");

        this.serverSocket = new ServerSocket(port);

        this.consoleInputListener.start();
        this.messagePoolListener.start();
        this.connectionListener.start();

        System.out.println("Successfully started the server");
    }

    public void stop() {
        System.out.println("Stopping the server...");

        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.messagePoolListener.interrupt();
        this.connectionListener.interrupt();

        try {
            this.consoleInputListener.join();
            this.messagePoolListener.join();
            this.connectionListener.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        this.clients.values().forEach(this::disconnectClient);
        this.clients.clear();

        this.connectionManager.shutdown();
        this.messageManager.shutdown();

        System.out.println("Server successfully stopped");
    }
}
