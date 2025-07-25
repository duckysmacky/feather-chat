package io.github.duckysmacky.featherchat.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        int serverPort = 8080;

        System.out.printf("Creating a server on %s...%n", serverPort);
        try (ServerSocket server = new ServerSocket(serverPort)) {
            System.out.printf("Listening for clients on %s%n", serverPort);

            Socket client = server.accept();
            System.out.printf("Successfully connected to %s%n", client.getRemoteSocketAddress());

            Thread messageReceiver = getMessageReceiverThread(client);
            messageReceiver.join();

            client.close();
            System.out.println("Connection terminated. Closing server...");
        } catch (IOException e) {
            System.out.printf("Unable to create server: %s%n", e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Thread getMessageReceiverThread(Socket client) throws IOException {
        BufferedReader clientOut = new BufferedReader(new InputStreamReader(client.getInputStream()));

        Thread thread = new Thread(() -> {
            String message;
            try {
                while ((message = clientOut.readLine()) != null) {
                    if (message.equalsIgnoreCase("disconnect")) {
                        System.out.println("Client requested disconnection. Closing client connection...");
                        break;
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
