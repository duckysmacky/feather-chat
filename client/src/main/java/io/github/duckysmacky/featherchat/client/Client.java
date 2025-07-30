package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.Message;
import io.github.duckysmacky.featherchat.common.MessageType;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private final UUID id;
    private final BlockingQueue<Message> incomingMessagePool;
    private final Thread consoleListener;
    private final Thread incomingMessageListener;
    private ServerConnection server;
    private boolean isConnected;

    public Client() {
        this.id = UUID.randomUUID();
        this.incomingMessagePool = new LinkedBlockingQueue<>();

        this.consoleListener = new Thread(() -> {
            Scanner console = new Scanner(System.in);

            while (isConnected) {
                if (console.hasNextLine()) {
                    String input = console.nextLine();

                    if (!isConnected) return;
                    if (input == null || input.isBlank()) continue;

                    handleInput(input);
                }
            }
        });

        this.incomingMessageListener = new Thread(() -> {
           while (isConnected) {
               try {
                   handleMessage(incomingMessagePool.take());
               } catch (InterruptedException e) {
                   throw new RuntimeException(e);
               }
           }
        });

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

    private void handleInput(String input) {
        Message message;
        if (input.equalsIgnoreCase("disconnect"))
            message = Message.disconnectMessage(id);
        else
            message = Message.textMessage(id, input);

        try {
            server.sendMessage(message);
        } catch (IOException e) {
            System.err.printf("Unable to send a message to server: %s%n", e.getMessage());
        }

        if (message.getType() == MessageType.DISCONNECT) {
            try {
                disconnect();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else
            System.out.println(message);
    }

    private void handleMessage(Message message) {
        if (message.getType() == MessageType.TEXT)
            System.out.println(message);
    }

    public void connect(String host, int port) throws IOException {
        System.out.printf("Connecting to %s:%s...%n", host, port);
        this.server = new ServerConnection(host, port, incomingMessagePool);

        Message connectionMessage = Message.connectMessage(id);
        server.sendMessage(connectionMessage);

        this.isConnected = true;
        this.consoleListener.start();
        this.incomingMessageListener.start();

        System.out.printf("Successfully connected to %s:%s with ID '%s'%n", host, port, id);
    }

    public void disconnect() throws InterruptedException {
        System.out.println("Disconnecting from server...");

        this.server.close();

        this.isConnected = false;
        this.consoleListener.join();
        this.incomingMessageListener.join();

        System.out.println("Successfully disconnected from server");
    }
}
