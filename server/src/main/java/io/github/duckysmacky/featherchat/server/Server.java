package io.github.duckysmacky.featherchat.server;

import io.github.duckysmacky.featherchat.common.listeners.ConsoleInputListener;
import io.github.duckysmacky.featherchat.common.request.Message;
import io.github.duckysmacky.featherchat.common.listeners.MessageListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private final static UUID SERVER_ID = new UUID(0, 0);
    private final Map<UUID, ClientConnection> clients;
    private final BlockingQueue<Message> messagePool;
    private final ExecutorService connectionManager;
    private final ExecutorService messageManager;
    private final AtomicBoolean isRunning;
    private final Thread consoleInputListener;
    private final Thread messagePoolListener;
    private final Thread connectionListener;
    private ServerSocket serverSocket;

    public Server() {
        this.clients = new Hashtable<>();
        this.messagePool = new LinkedBlockingQueue<>();
        this.connectionManager = Executors.newFixedThreadPool(10);
        this.messageManager = Executors.newFixedThreadPool(10);
        this.isRunning = new AtomicBoolean();

        this.consoleInputListener = new Thread(new ConsoleInputListener(
            isRunning::get,
            input -> messagePool.add(Message.textMessage(SERVER_ID, input)),
            () -> new Thread(this::stop, "Server stopper").start()
        ), "Console input Listener");

        this.messagePoolListener = new Thread(new MessageListener(isRunning::get, messagePool, this::handleMessage), "Message pool Listener");

        this.connectionListener = new Thread(() -> {
            System.out.printf("Server is now listening on port %s%n", serverSocket.getLocalPort());

            while (isRunning.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (!isRunning.get()) return;

                    connectClient(clientSocket);
                } catch (IOException e) {
                    String msg = e.getMessage();

                    if (msg != null && !msg.equals("Socket closed")) {
                        System.err.printf("Unable to accept client connection: %s%n", e.getMessage());
                    }
                }
            }
        }, "Connection Listener");
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

                try {
                    client.sendMessage(Message.connectMessage(SERVER_ID));
                } catch (IOException e) {
                    System.err.printf("Unable to send CONNECT message to client '%s': %s%n", client.getId(), e.getMessage());
                }

                System.out.printf("New client connected: %s%n", client.getId());
            } catch (IOException e) {
                System.err.printf("Unable to connect the client: %s%n", e.getMessage());
            }
        });
    }

    private void disconnectClient(ClientConnection client) {
        connectionManager.submit(() -> {
            ClientConnection connectedClient = clients.remove(client.getId());
            if (connectedClient == null) return;

            System.out.printf("Disconnecting from client '%s'...%n", connectedClient.getId());

            try {
                connectedClient.sendMessage(Message.disconnectMessage(SERVER_ID));
            } catch (IOException _) {}

            connectedClient.close();
            System.out.printf("Successfully disconnected from client '%s'%n", connectedClient.getId());
        });
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case TEXT -> clients.values().forEach(client -> messageManager.submit(() -> {
                if (client.getId().equals(message.getSenderId())) return;

                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    System.err.printf("Unable to send a TEXT message to client '%s': %s%n", client.getId(), e.getMessage());
                    disconnectClient(client);
                }
            }));
            case DISCONNECT -> {
                if (message.getSenderId().equals(SERVER_ID)) break;
                System.out.printf("Client '%s' requested disconnection%n", message.getSenderId());

                ClientConnection client = clients.get(message.getSenderId());
                if (client != null)
                    disconnectClient(client);
            }
        }

        System.out.println(message);
    }

    public void start(int port) throws IOException {
        System.out.println("Starting the server...");

        this.serverSocket = new ServerSocket(port);

        this.isRunning.set(true);
        this.consoleInputListener.start();
        this.messagePoolListener.start();
        this.connectionListener.start();

        System.out.println("Successfully started the server");
        System.out.println("Press CTRL + D to stop");
    }

    public void stop() {
        System.out.println("Stopping the server...");

        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.isRunning.set(false);
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

        this.connectionManager.shutdown();
        this.messageManager.shutdown();

        System.out.println("Server successfully stopped");
    }
}
