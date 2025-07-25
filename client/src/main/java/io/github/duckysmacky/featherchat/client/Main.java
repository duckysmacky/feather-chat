package io.github.duckysmacky.featherchat.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int serverPort = 8080;

        System.out.printf("Trying to connect to %s:%s...%n", serverAddress, serverPort);
        try (Socket server = new Socket(serverAddress, serverPort)) {
            System.out.printf("Successfully connected to %s%n", server.getRemoteSocketAddress());

            Thread messageSender = getMessageSenderThread(server);
            messageSender.join();

            System.out.println("Connection terminated. Exiting...");
        } catch (UnknownHostException e) {
            System.err.println("Unable to connect to the server: invalid server IP provided");
        } catch (IOException e) {
            System.err.printf("Unable to connect to the server: %s%n", e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Thread getMessageSenderThread(Socket server) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter serverIn = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));

        Thread thread = new Thread(() -> {
            String input;
            try {
                while ((input = console.readLine()) != null) {
                    serverIn.write(input);
                    serverIn.newLine();
                    serverIn.flush();

                    if (input.equalsIgnoreCase("disconnect")) {
                        System.out.println("Disconnecting from server...");
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.printf("Error reading console input: %s%n", e.getMessage());
            }
        });

        thread.start();
        return thread;
    }
}
