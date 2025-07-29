package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.Message;
import io.github.duckysmacky.featherchat.common.MessageType;

import java.io.IOException;
import java.util.Scanner;

public class Client {
    private ServerConnection server;
    private final Thread consoleListener;
    private boolean isConnected;

    public Client() {
        this.isConnected = false;

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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleInput(String input) {
        Message message = new Message(String.valueOf(server.getLocalPort()), input);

        try {
            server.sendMessage(message);
        } catch (IOException e) {
            System.err.printf("Unable to send a message to server: %s%n", e.getMessage());
        }


        if (message.getType() == MessageType.DISCONNECT)
            disconnect();
        else
            System.out.println(message);
    }

    public void connect(String host, int port) throws IOException, InterruptedException {
        System.out.printf("Connecting to %s:%s...%n", host, port);
        this.server = new ServerConnection(host, port, System.out::println);

        this.isConnected = true;
        this.consoleListener.start();

        System.out.printf("Successfully connected to %s:%s%n", host, port);

        this.consoleListener.join();
    }

    public void disconnect() {
        System.out.println("Disconnecting from server...");

        server.close();
        this.isConnected = false;

        System.out.println("Successfully disconnected from server");
    }
}
