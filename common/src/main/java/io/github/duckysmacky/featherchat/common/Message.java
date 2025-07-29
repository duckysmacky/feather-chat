package io.github.duckysmacky.featherchat.common;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Message {
    private MessageType type;
    private String senderId;
    private LocalDateTime timeSent;
    private boolean received;
    private String content;

    public Message(String senderId, String content) {
        this.type = getMessageType(content);
        this.senderId = senderId;
        this.timeSent = LocalDateTime.now();
        this.received = false;
        this.content = content;
    }

    private Message(MessageType type, String senderId, LocalDateTime timeSent, boolean received, String content) {
        this.type = type;
        this.senderId = senderId;
        this.timeSent = timeSent;
        this.received = received;
        this.content = content;
    }

    private MessageType getMessageType(String content) {
        if (content.equalsIgnoreCase("disconnect"))
            return MessageType.DISCONNECT;

        return MessageType.TEXT;
    }

    /// Parses the provided payload and returns the message. Will throw an exception if the parsing of the payload will
    /// fair
    ///
    /// Format: `<type|sender_id|time_sent|received|content>``
    /// Rules:
    /// - Fields must not be null
    /// - Field values must not contain '|'
    /// - Message is surrounded by angle brackets (`<...>`)
    /// - DateTime is formatted as ISO-8601 string (`2025-07-29T21:15:00`)
    /// - `received` is lowercase "true"/"false"
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
            String senderId = tokenizer.nextToken();
            LocalDateTime timeSent = LocalDateTime.parse(tokenizer.nextToken());
            boolean received = Boolean.parseBoolean(tokenizer.nextToken());
            String content = tokenizer.nextToken();

            return new Message(type, senderId, timeSent, received, content);
        } catch (NoSuchElementException e) {
            throw new InvalidPayloadException("Payload missing fields");
        }
    }

    /// Serializes the message into a custom payload format
    ///
    /// Custom payload format: `<type|sender_id|time_sent|received|content>`
    public byte[] intoPayload() {
        return String.format("<%s|%s|%s|%s|%s>", type, senderId, timeSent, received, content).getBytes(StandardCharsets.UTF_8);
    }

    public void markRecieved() {
        this.received = true;
    }

    public MessageType getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public LocalDateTime getTimeSent() {
        return timeSent;
    }

    public boolean isReceived() {
        return received;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        if (type == MessageType.TEXT)
            return String.format("[%s] <%s> %s", timeSent.toLocalTime().truncatedTo(ChronoUnit.SECONDS), senderId, content);

        return String.format("[%s] <%s> {%s}", timeSent.toLocalTime().truncatedTo(ChronoUnit.SECONDS), senderId, type.toString().toUpperCase());
    }
}