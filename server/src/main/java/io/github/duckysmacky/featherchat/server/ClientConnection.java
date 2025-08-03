package io.github.duckysmacky.featherchat.server;

import io.github.duckysmacky.featherchat.common.request.Message;
import io.github.duckysmacky.featherchat.common.request.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class ClientConnection implements Closeable {
    private final Socket socket;
    private final DataInputStream clientOut;
    private final DataOutputStream clientIn;
    private final UUID id;
    private final Thread messageListener;

    public ClientConnection(Socket clientSocket, BlockingQueue<Message> messagePool) throws IOException {
        this.socket = clientSocket;

        try {
            this.clientOut = new DataInputStream(socket.getInputStream());
            this.clientIn = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.printf("Unable to get stream for client: %s%n", e.getMessage());
            throw e;
        }

        // wait for the CONNECT message from client to get the ID
        try {
            while (true) {
                int payloadLength = clientOut.readInt();
                byte[] payload = clientOut.readNBytes(payloadLength);
                Message message = Message.fromPayload(payload);

                if (message.getType() == MessageType.CONNECT) {
                    this.id = message.getSenderId();
                    messagePool.add(message);
                    break;
                }
            }
        } catch (IOException e) {
            String msg = e.getMessage();

            if (msg != null && msg.equals("Socket closed")) {
                throw new IOException("Client disconnected before sending the CONNECT message");
            }
            throw e;
        }

        this.messageListener = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    int payloadLength = clientOut.readInt();
                    byte[] payload = clientOut.readNBytes(payloadLength);
                    messagePool.add(Message.fromPayload(payload));
                } catch (IOException e) {
                    String msg = e.getMessage();
                    if (msg != null) {
                        if (msg.strip().equalsIgnoreCase("socket closed")) break;

                        System.err.printf("Unable to read a message from client '%s': %s%n", id, msg);
                    }
                }
            }
        }, String.format("Client '%s' message Listener", id));

        this.messageListener.start();
    }

    public void sendMessage(Message message) throws IOException {
        byte[] payload = message.intoPayload();

        clientIn.writeInt(payload.length);
        clientIn.write(payload);
        clientIn.flush();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public void close() {
        try {
            this.socket.shutdownInput();
            this.socket.close();
        } catch (IOException e) {
            System.err.printf("Unable to close client '%s' socket: %s%n", id, e.getMessage());
        }

        try {
            this.messageListener.interrupt();
            this.messageListener.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
