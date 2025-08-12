package io.github.duckysmacky.featherchat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ClientApplication extends Application {
    private final String serverAddress = "127.0.0.1";
    private final int serverPort = 8080;
    private Client client;
    private VBox messagesBox;

    public static void main(String[] args) {
        launch(args);
    }

    private void appendMessage(String content) {
        Platform.runLater(() -> {
            Label messageLabel = new Label(content);
            messageLabel.setWrapText(true);
            messagesBox.getChildren().add(messageLabel);
        });
    }

    @Override
    public void start(Stage primaryStage) {
        messagesBox = new VBox(5);
        messagesBox.setPrefHeight(400);

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);

        TextField inputField = new TextField();
        inputField.setPromptText("Write a message...");
        Button sendButton = new Button("Send");

        HBox inputArea = new HBox(5, inputField, sendButton);
        inputArea.setPrefHeight(40);

        VBox root = new VBox(5, scrollPane, inputArea);
        Scene scene = new Scene(root, 500, 500);

        sendButton.setOnAction(_ -> {
            String messageContent = inputField.getText().trim();
            if (!messageContent.isEmpty()) {
                appendMessage("You: " + messageContent);
                inputField.clear();
            }
        });

        CompletableFuture.runAsync(() -> {
            client = new Client();

            try {
                client.connect(serverAddress, serverPort);
            } catch (IOException e) {
                appendMessage("Unable to connect to the server: " + e.getMessage());
            }
        });

        primaryStage.setTitle("FeatherChat Client");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(_ -> {
            client.disconnect();
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

}
