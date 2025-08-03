package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.request.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ServerConnection implements Closeable {
    private final Socket socket;
    private final DataInputStream serverOut;
    private final DataOutputStream serverIn;
    private final Thread incomingMessageListener;

    public ServerConnection(String host, int port, BlockingQueue<Message> incomingMessagePool) throws IOException {
        this.socket = new Socket(host, port);
        this.serverOut = new DataInputStream(socket.getInputStream());
        this.serverIn = new DataOutputStream(socket.getOutputStream());

        this.incomingMessageListener = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    int payloadLength = serverOut.readInt();
                    byte[] payload = serverOut.readNBytes(payloadLength);
                    incomingMessagePool.add(Message.fromPayload(payload));
                } catch (IOException e) {
                    String msg = e.getMessage();
                    if (msg != null) {
                        if (msg.strip().equalsIgnoreCase("socket closed")) break;

                        System.err.printf("Unable to read a message from server: %s%n", msg);
                    }
                }
            }
        }, "Server message Listener");

        this.incomingMessageListener.start();
    }

    public void sendMessage(Message message) throws IOException {
        byte[] payload = message.intoPayload();

        serverIn.writeInt(payload.length);
        serverIn.write(payload);
        serverIn.flush();
    }

    @Override
    public void close() {
        try {
            this.socket.shutdownInput();
            this.socket.close();

            this.incomingMessageListener.interrupt();
            this.incomingMessageListener.join();
        } catch (IOException e) {
            System.err.printf("Unable to close server socket: %s%n", e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
