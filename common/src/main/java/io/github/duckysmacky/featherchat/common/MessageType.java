package io.github.duckysmacky.featherchat.common;

public enum MessageType {
    /// A simple text message
    TEXT,
    /// A message suggesting that the client has just connected and wants to make itself known
    CONNECT,
    /// A message suggesting that the client wants to disconnect
    DISCONNECT;

    public static MessageType fromString(String value) throws InvalidPayloadException {
        try {
            return MessageType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPayloadException("Unknown message type: " + value);
        }
    }
}
