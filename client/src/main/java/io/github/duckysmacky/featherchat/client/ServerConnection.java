package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerConnection implements Closeable {
    private final Socket socket;
    private final int localPort;
    private final DataInputStream serverOut;
    private final DataOutputStream serverIn;
    private final Thread messageListener;

    public ServerConnection(String host, int port, Consumer<Message> onMessage) throws IOException {
        this.socket = new Socket(host, port);
        this.localPort = socket.getLocalPort();

        try {
            this.serverOut = new DataInputStream(socket.getInputStream());
            this.serverIn = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.printf("Unable to get server's streams: %s%n", e.getMessage());
            throw new RuntimeException(e);
        }

        this.messageListener = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    int payloadLength = serverOut.readInt();
                    byte[] payload = serverOut.readNBytes(payloadLength);
                    Message message = Message.fromPayload(payload);
                    onMessage.accept(message);
                }
            } catch (IOException e) {
                if (e.getMessage().equals("Socket closed")) return;
                System.err.printf("Unable to read server messages: %s%n", e.getMessage());
            }
        });

        this.messageListener.start();
    }

    public void sendMessage(Message message) throws IOException {
        byte[] payload = message.intoPayload();

        serverIn.writeInt(payload.length);
        serverIn.write(payload);
        serverIn.flush();
    }

    public int getLocalPort() {
        return localPort;
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
