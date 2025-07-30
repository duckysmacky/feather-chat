package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.Message;
import io.github.duckysmacky.featherchat.common.MessageType;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.*;

public class Client {
    private final UUID id;
    private final Thread consoleInputListener;
    private final Thread incomingMessageListener;
    private final Thread outgoingMessageListener;
    private final ExecutorService messageHandler;
    private ServerConnection server;
    private BlockingQueue<Message> incomingMessagePool;
    private BlockingQueue<Message> outgoingMessagePool;
    private boolean isConnected;

    public Client() {
        this.id = UUID.randomUUID();

        this.consoleInputListener = new Thread(() -> {
            Scanner console = new Scanner(System.in);

            while (isConnected) {
                if (console.hasNextLine()) {
                    String input = console.nextLine();
                    if (!isConnected) break;

                    if (input != null && !input.isBlank()) {
                        Message message = parseInput(input);
                        outgoingMessagePool.add(message);

                        if (message.getType() == MessageType.DISCONNECT) break;
                    }
                }
            }
        });

        this.incomingMessageListener = new Thread(() -> {
           while (isConnected) {
               try {
                   Message message = incomingMessagePool.take();
                   handleIncomingMessage(message);
               } catch (InterruptedException e) {
                   break;
               }
           }
        });

        this.outgoingMessageListener = new Thread(() -> {
            while (isConnected) {
                try {
                    Message message = outgoingMessagePool.take();
                    handleOutgoingMessage(message);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        this.messageHandler = Executors.newSingleThreadExecutor();
        this.isConnected = false;
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

    private Message parseInput(String input) {
        if (input.equalsIgnoreCase("disconnect"))
            return Message.disconnectMessage(id);

        return Message.textMessage(id, input);
    }

    private void handleIncomingMessage(Message message) {
        messageHandler.submit(() -> {
            if (message.getType() == MessageType.TEXT)
                System.out.println(message);
        });
    }

    private void handleOutgoingMessage(Message message) {
        messageHandler.submit(() -> {
            if (!isConnected) return;

            if (message.getType() == MessageType.DISCONNECT) {
                disconnect();
                return;
            }

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

        this.incomingMessagePool = new LinkedBlockingQueue<>();
        this.outgoingMessagePool = new SynchronousQueue<>();
        this.server = new ServerConnection(host, port, incomingMessagePool);

        Message connectionMessage = Message.connectMessage(id);
        server.sendMessage(connectionMessage);

        this.isConnected = true;
        this.consoleInputListener.start();
        this.incomingMessageListener.start();
        this.outgoingMessageListener.start();

        System.out.printf("Successfully connected to %s:%s with ID '%s'%n", host, port, id);
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

        this.isConnected = false;
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
