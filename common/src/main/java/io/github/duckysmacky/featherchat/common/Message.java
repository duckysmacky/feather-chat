package io.github.duckysmacky.featherchat.common;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.UUID;

public class Message {
    private MessageType type;
    private UUID senderId;
    private LocalDateTime timeSent;
    private String content;

    private Message(MessageType type, UUID senderId, LocalDateTime timeSent, String content) {
        this.type = type;
        this.senderId = senderId;
        this.timeSent = timeSent;
        this.content = content;
    }

    /// Creates a new instance of a TEXT message
    public static Message textMessage(UUID senderId, String content) {
        return new Message(MessageType.TEXT, senderId, LocalDateTime.now(), content);
    }

    /// Creates a new instance of a CONNECT message
    public static Message connectMessage(UUID senderId) {
        return new Message(MessageType.CONNECT, senderId, LocalDateTime.now(), "Requesting connection");
    }

    /// Creates a new instance of a DISCONNECT message
    public static Message disconnectMessage(UUID senderId) {
        return new Message(MessageType.DISCONNECT, senderId, LocalDateTime.now(), "Requesting disconnection");
    }

    /// Parses the provided payload and returns the message. Will throw an exception if the parsing of the payload will
    /// fair
    ///
    /// Format: `<type|sender_id|time_sent|content>``
    /// Rules:
    /// - Fields must not be null
    /// - Field values must not contain '|'
    /// - Message is surrounded by angle brackets (`<...>`)
    /// - DateTime is formatted as ISO-8601 string (`2025-07-29T21:15:00`)
    ///
    /// @throws InvalidPayloadException if the payload structure is invalid (e.g. missing required delimiters)
    /// @throws IllegalArgumentException if the payload contains invalid data (e.g. invalid enum type or invalid time
    /// format)
    public static Message fromPayload(byte[] payloadBytes) throws InvalidPayloadException, IllegalArgumentException {
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);

        int start = payload.indexOf('<');
        if (start == -1)
            throw new InvalidPayloadException("No starting '<' present");

        int end = payload.lastIndexOf('>');
        if (end == -1)
            throw new InvalidPayloadException("No ending '>' present");

        StringTokenizer tokenizer = new StringTokenizer(payload.substring(start + 1, end), "|");
        try {
            MessageType type = MessageType.valueOf(tokenizer.nextToken());
            UUID senderId = UUID.fromString(tokenizer.nextToken());
            LocalDateTime timeSent = LocalDateTime.parse(tokenizer.nextToken());
            String content = tokenizer.nextToken();

            return new Message(type, senderId, timeSent, content);
        } catch (NoSuchElementException e) {
            throw new InvalidPayloadException("Payload missing fields");
        }
    }

    /// Serializes the message into a custom payload format
    ///
    /// Custom payload format: `<type|sender_id|time_sent|received|content>`
    public byte[] intoPayload() {
        return String.format("<%s|%s|%s|%s>", type, senderId, timeSent, content).getBytes(StandardCharsets.UTF_8);
    }

    public MessageType getType() {
        return type;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public LocalDateTime getTimeSent() {
        return timeSent;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        String time = timeSent.toLocalTime().truncatedTo(ChronoUnit.SECONDS).toString();
        String id = senderId.toString().substring(0, 8);

        if (type == MessageType.TEXT)
            return String.format("[%s] <%s> %s", time, id, content);

        return String.format("[%s] <%s> {%s}", time, id, type.toString().toUpperCase());
    }
}