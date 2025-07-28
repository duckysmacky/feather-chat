package io.github.duckysmacky.featherchat.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerConnection implements Closeable {
    private final Socket socket;
    private final BufferedReader serverOut;
    private final BufferedWriter serverIn;
    private final Thread messageListener;

    public ServerConnection(String host, int port, Consumer<String> onMessage) throws IOException {
        this.socket = new Socket(host, port);

        try {
            this.serverOut = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.serverIn = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            System.err.printf("Unable to get server's streams: %s%n", e.getMessage());
            throw new RuntimeException(e);
        }

        this.messageListener = new Thread(() -> {
            try {
                String message;
                while (!socket.isClosed() && (message = serverOut.readLine()) != null) {
                    onMessage.accept(message);
                }
            } catch (IOException e) {
                if (e.getMessage().equals("Socket closed")) return;
                System.err.printf("Unable to read server messages: %s%n", e.getMessage());
            }
        });

        this.messageListener.start();
    }

    public void send(String message) throws IOException {
        serverIn.write(message);
        serverIn.newLine();
        serverIn.flush();
    }

    @Override
    public void close() {
        try {
            this.socket.shutdownInput();
            this.socket.close();

            this.messageListener.join();
        } catch (IOException e) {
            System.err.printf("Unable to close server socket: %s%n", e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
