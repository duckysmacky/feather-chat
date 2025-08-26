package io.github.duckysmacky.featherchat.client.components;

import io.github.duckysmacky.featherchat.client.components.message.MessageBubble;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ConversationView extends ScrollPane {
    private final VBox messagesBox;

    public ConversationView() {
        this.messagesBox = new VBox(5);
        messagesBox.setPrefHeight(800);

        setContent(messagesBox);
        setFitToWidth(true);
        setVbarPolicy(ScrollBarPolicy.ALWAYS);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setPannable(true);
        setStyle("-fx-background-color: transparent; -fx-padding: 10px;");
    }

    public void appendMessageBubble(MessageBubble messageBubble, Pos alignment) {
        HBox messageContainer = new HBox(messageBubble);
        messageContainer.widthProperty().add(messagesBox.widthProperty());
        messageContainer.setAlignment(alignment);
        Platform.runLater(() -> messagesBox.getChildren().add(messageContainer));
    }
}
