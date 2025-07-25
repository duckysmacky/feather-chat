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

            BufferedReader clientOut = new BufferedReader(new InputStreamReader(client.getInputStream()));

            String message;
            while ((message = clientOut.readLine()) != null) {
                if (message.equalsIgnoreCase("disconnect")) {
                    System.out.println("Client requested disconnection. Closing client connection...");
                    break;
                }

                System.out.printf("[client] %s%n", message);
            }

            client.close();
            System.out.println("Connection terminated. Closing server...");
        } catch (IOException e) {
            System.out.printf("Unable to create server: %s%n", e.getMessage());
        }
    }
}
