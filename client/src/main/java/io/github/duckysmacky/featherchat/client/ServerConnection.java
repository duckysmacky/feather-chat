package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.request.Message;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

/// A class responsible for managing the connection to the FeatherChat server. It represents a connection to the server
/// which is established using a socket connection to a specified host and port upon creation and is closed when no
/// longer needed. It also handles sending and receiving messages over a socket connection.
///
/// When the class is initialized, it creates a socket connection to the specified host and port, and sends a
/// CONNECT message to the server with the unique identifier of the client.
///
/// The class implements `Closeable` to ensure that resources are properly released when the connection is closed.
public class ServerConnection implements Closeable {
    private final UUID id;
    private final Socket socket;
    private final DataInputStream serverOut;
    private final DataOutputStream serverIn;

    public ServerConnection(UUID id, String host, int port) throws IOException {
        this.id = id;
        this.socket = new Socket(host, port);
        this.serverOut = new DataInputStream(socket.getInputStream());
        this.serverIn = new DataOutputStream(socket.getOutputStream());

        Message connectionMessage = Message.connectMessage(id);
        writeMessage(connectionMessage);
    }

    public void writeMessage(Message message) throws IOException {
        if (socket.isClosed()) return;

        byte[] payload = message.intoPayload();

        serverIn.writeInt(payload.length);
        serverIn.write(payload);
        serverIn.flush();
    }

    public Message readMessage() throws IOException {
        int payloadLength = serverOut.readInt();
        byte[] payload = serverOut.readNBytes(payloadLength);

        if (payloadLength == 0) {
            throw new IOException("Received an empty message payload");
        }

        return Message.fromPayload(payload);
    }

    @Override
    public void close() {
        try {
            writeMessage(Message.disconnectMessage(id));
        } catch (Exception e) {
            System.err.printf("Error while sending disconnect message: %s%n", e.getMessage());
        }

        try {
            this.socket.shutdownInput();
            this.socket.close();
        } catch (Exception e) {
            System.err.printf("Error while closing the socket: %s%n", e.getMessage());
        } finally {
            try {
                this.serverOut.close();
                this.serverIn.close();
            } catch (IOException e) {
                System.err.printf("Error while closing streams: %s%n", e.getMessage());
            }
        }
    }
}
