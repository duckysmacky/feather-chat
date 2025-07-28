package io.github.duckysmacky.featherchat.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Error: not enough arguments provided");
            System.err.println("Usage: client <host> <port>");
            return;
        }

        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);

        System.out.printf("Trying to connect to %s:%s...%n", serverAddress, serverPort);
        try (Socket server = new Socket(serverAddress, serverPort)) {
            System.out.printf("Successfully connected to %s%n", server.getRemoteSocketAddress());

            Thread messageReceiver = getMessageReceiverThread(server);
            Thread messageSender = getMessageSenderThread(server);

            messageReceiver.join();
            messageSender.join();
        } catch (UnknownHostException e) {
            System.err.println("Unable to connect to the server: invalid server IP provided");
        } catch (IOException e) {
            System.err.printf("Unable to connect to the server: %s%n", e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Successfully disconnected from server");
    }

    private static Thread getMessageSenderThread(Socket server) throws IOException {
        Scanner console = new Scanner(System.in);
        BufferedWriter serverIn = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));

        Thread thread = new Thread(() -> {
            try {
                while (!server.isClosed()) {
                    if (console.hasNextLine()) {
                        String input = console.nextLine();

                        if (server.isClosed()) {
                            System.out.println("Server closed the connection. Disconnecting...");
                            return;
                        }

                        serverIn.write(input);
                        serverIn.newLine();
                        serverIn.flush();

                        if (input.equalsIgnoreCase("disconnect")) {
                            System.out.println("Disconnecting from server...");
                            server.shutdownInput();
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

    private static Thread getMessageReceiverThread(Socket server) throws IOException {
        BufferedReader serverOut = new BufferedReader(new InputStreamReader(server.getInputStream()));

        Thread thread = new Thread(() -> {
            try {
                String message;
                while (!server.isClosed() && (message = serverOut.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.err.printf("Error reading server messages: %s%n", e.getMessage());
            }
        });

        thread.start();
        return thread;
    }
}
