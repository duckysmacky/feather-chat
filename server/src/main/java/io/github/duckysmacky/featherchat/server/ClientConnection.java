package io.github.duckysmacky.featherchat.server;

import io.github.duckysmacky.featherchat.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientConnection implements Closeable {
    private Socket socket;
    private DataInputStream clientOut;
    private DataOutputStream clientIn;
    private String id;
    private Thread messageListener;

    public ClientConnection(Socket clientSocket, Consumer<Message> onMessage) {
        this.socket = clientSocket;
        this.id = String.valueOf(clientSocket.getPort());

        try {
            this.clientOut = new DataInputStream(socket.getInputStream());
            this.clientIn = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.printf("Unable to get stream for client '%s': %s%n", id, e.getMessage());
            throw new RuntimeException();
        }

        this.messageListener = new Thread(() -> {
            try {
                while (!socket.isClosed()) {
                    int payloadLength = clientOut.readInt();
                    byte[] payload = clientOut.readNBytes(payloadLength);
                    Message message = Message.fromPayload(payload);

                    onMessage.accept(message);
                }
            } catch (IOException e) {
                String msg = e.getMessage();
                if (msg != null) {
                    if (msg.equals("Socket closed")) return;

                    System.err.printf("Unable to read client '%s' messages: %s%n", id, e.getMessage());
                }
            }
        });

        this.messageListener.start();
    }

    public void sendMessage(Message message) throws IOException {
        byte[] payload = message.intoPayload();

        clientIn.writeInt(payload.length);
        clientIn.write(payload);
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
