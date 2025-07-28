package io.github.duckysmacky.featherchat.server;

import java.io.*;
import java.net.Socket;
import java.util.function.BiConsumer;

public class ClientConnection implements Closeable {
    private Socket socket;
    private BufferedWriter clientIn;
    private BufferedReader clientOut;
    private String id;
    private Thread messageListener;

    public ClientConnection(Socket clientSocket, BiConsumer<ClientConnection, String> onMessage) {
        this.socket = clientSocket;
        this.id = String.valueOf(clientSocket.getPort());

        try {
            this.clientIn = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.clientOut = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.printf("Unable to get stream for client '%s': %s%n", id, e.getMessage());
            throw new RuntimeException();
        }

        this.messageListener = new Thread(() -> {
            try {
                String message;
                while (!socket.isClosed() && (message = clientOut.readLine()) != null) {
                    onMessage.accept(this, message);
                }
            } catch (IOException e) {
                if (e.getMessage().equals("Socket closed")) return;
                System.err.printf("Unable to read client '%s' messages: %s%n", id, e.getMessage());
            }
        });

        this.messageListener.start();
    }

    public void send(String senderId, String message) throws IOException {
        String payload = String.format("[%s] %s", senderId, message);

        clientIn.write(payload);
        clientIn.newLine();
        clientIn.flush();
    }

    public String getId() {
        return id;
    }

    @Override
    public void close() {
        try {
            this.socket.shutdownInput();
            this.socket.close();

            this.messageListener.join();
        } catch (IOException e) {
            System.err.printf("Unable to close client '%s' socket: %s%n", id, e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
