package io.github.duckysmacky.featherchat.server;

public class Main {
    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        try {
            server.start(8080);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        server.close();
    }
}