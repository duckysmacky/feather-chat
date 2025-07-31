# Feather Chat

A lightweight multithreaded socket-based chat application

## About

> [!NOTE]
> The project is currently in very early stages of development. For now, only the basic client-to-sever message
> broadcast logic finished

The application works by connecting clients to a central server, which routes messages from client to client. The 
messages are transferred instantly via socket connection. Users will be able to send messages to a selected recipient
or connect to a chat room in which multiple users can chat with each other.

## Planned features

- Client-to-client messages
- Chat rooms
- Double-ended message encryption
- User authentification (accounts, usernames, etc.)
- Clean client-sided GUI