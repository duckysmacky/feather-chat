package io.github.duckysmacky.featherchat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        int serverPort = 8080;

        System.out.printf("Creating a server on %s...%n", serverPort);
        try (ServerSocket server = new ServerSocket(serverPort)) {
            while (true) {
                System.out.printf("Listening for clients on %s%n", serverPort);

                Socket client = server.accept();
                System.out.printf("Successfully connected to %s%n", client.getRemoteSocketAddress());

                Thread messageReceiver = getMessageReceiverThread(client);
                Thread messageSender = getMessageSenderThread(client);

                messageReceiver.join();
                messageSender.join();

                System.out.println("Successfully disconnected from client");
            }
        } catch (IOException e) {
            System.out.printf("Unable to create server: %s%n", e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Thread getMessageSenderThread(Socket client) throws IOException {
        Scanner console = new Scanner(System.in);
        BufferedWriter clientIn = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

        Thread thread = new Thread(() -> {
            try {
                while (!client.isClosed()) {
                    if (console.hasNextLine()) {
                        String input = console.nextLine();

                        if (client.isClosed()) {
                            System.out.println("Client closed the connection. Disconnecting...");
                            return;
                        }

                        clientIn.write(input);
                        clientIn.newLine();
                        clientIn.flush();

                        if (input.equalsIgnoreCase("disconnect")) {
                            System.out.println("Disconnecting from client...");
                            client.shutdownInput();
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                System.err.printf("Error reading console input: %s%n", e.getMessage());
            }
        });

        thread.start();
        return thread;
    }

    private static Thread getMessageReceiverThread(Socket client) throws IOException {
        BufferedReader clientOut = new BufferedReader(new InputStreamReader(client.getInputStream()));

        Thread thread = new Thread(() -> {
            try {
                String message;
                while (!client.isClosed() && (message = clientOut.readLine()) != null) {
                    if (message.equalsIgnoreCase("disconnect")) {
                        System.out.println("Client requested disconnection. Press any key to continue");
                        client.close();
                        return;
                    }

                    System.out.printf("[client] %s%n", message);
                }
            } catch (IOException e) {
                System.err.printf("Error reading client messages: %s%n", e.getMessage());
            }
        });

        thread.start();
        return thread;
    }
}
