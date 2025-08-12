package io.github.duckysmacky.featherchat.client;

import java.io.IOException;
import java.util.UUID;

public class Client {
    private final UUID id;
    private ServerConnection serverConnection;

    public Client() {
        this.id = UUID.randomUUID();
    }

    public ServerConnection connect(String host, int port) throws IOException {
        System.out.printf("Connecting to %s:%s...%n", host, port);

        serverConnection = new ServerConnection(id, host, port);

        System.out.printf("Successfully connected to %s:%s with ID '%s'%n", host, port, id);
        return serverConnection;
    }

    public UUID getId() {
        return id;
    }
}
