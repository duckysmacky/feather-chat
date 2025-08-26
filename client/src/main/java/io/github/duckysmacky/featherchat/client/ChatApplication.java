package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.client.components.ConversationView;
import io.github.duckysmacky.featherchat.client.components.message.ErrorMessageBubble;
import io.github.duckysmacky.featherchat.client.components.message.InfoMessageBubble;
import io.github.duckysmacky.featherchat.client.components.message.MessageBubble;
import io.github.duckysmacky.featherchat.client.components.message.UserMessageBubble;
import io.github.duckysmacky.featherchat.common.request.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatApplication extends Application {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private Client client;
    private ServerConnection serverConnection;
    private MessageHandler messageHandler;
    private ConversationView conversationView;

    public static void main(String[] args) {
        launch(args);
    }

    private Scene getScene() {
        TextField inputField = new TextField();
        inputField.setPromptText("Write a message...");

        Button sendButton = new Button("Send");
        sendButton.setOnAction(_ -> {
            String messageContent = inputField.getText().trim();
            if (!messageContent.isEmpty()) {
                messageHandler.handleOutgoingMessage(Message.textMessage(client.getId(), messageContent));
                inputField.clear();
            }
        });

        HBox inputArea = new HBox(5, inputField, sendButton);
        inputArea.setPrefHeight(40);

        conversationView = new ConversationView();

        VBox root = new VBox(5, conversationView, inputArea);
        return new Scene(root, 1000, 1000);
    }

    public void displayMessage(Message message) {
        boolean isCurrentUser = client.getId().equals(message.getSenderId());
        String username = message.getSenderId().toString().substring(0, 8);
        String timestamp = message.getTimeSent().format(TIME_FORMATTER);

        MessageBubble messageBubble = new UserMessageBubble(username, message.getContent(), timestamp, isCurrentUser);
        conversationView.appendMessageBubble(messageBubble, isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    }

    public void displayInfo(String infoText) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        MessageBubble errorBubble = new InfoMessageBubble(infoText, timestamp);
        conversationView.appendMessageBubble(errorBubble, Pos.CENTER);
    }

    public void displayError(String errorText) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        MessageBubble errorBubble = new ErrorMessageBubble(errorText, timestamp);
        conversationView.appendMessageBubble(errorBubble, Pos.CENTER);
    }

    @Override
    public void start(Stage primaryStage) {
        client = new Client();

        try {
            serverConnection = client.connect(SERVER_ADDRESS, SERVER_PORT);
            messageHandler = new MessageHandler(serverConnection, this);
        } catch (IOException e) {
            displayError(String.format("Unable to connect to server at %s:%d: %s", SERVER_ADDRESS, SERVER_PORT, e.getMessage()));
        }

        primaryStage.setTitle("FeatherChat Client");
        primaryStage.setOnCloseRequest(_ -> {
            serverConnection.close();
            messageHandler.stop();
            Platform.exit();
            System.exit(0);
        });

        Scene scene = getScene();

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
