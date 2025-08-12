package io.github.duckysmacky.featherchat.client;

import io.github.duckysmacky.featherchat.common.request.Message;
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

public class ChatApplication extends Application {
    private final String serverAddress = "127.0.0.1";
    private final int serverPort = 8080;
    private Client client;
    private ServerConnection serverConnection;
    private MessageHandler messageHandler;
    private VBox messagesBox;

    public static void main(String[] args) {
        launch(args);
    }
    private Scene getScene() {
        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);

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

        VBox root = new VBox(5, scrollPane, inputArea);
        return new Scene(root, 500, 500);
    }


    public void appendMessageBox(String messageContent) {
        Platform.runLater(() -> {
            Label messageLabel = new Label(messageContent);
            messageLabel.setWrapText(true);
            messagesBox.getChildren().add(messageLabel);
        });
    }

    @Override
    public void start(Stage primaryStage) {
        client = new Client();

        messagesBox = new VBox(5);
        messagesBox.setPrefHeight(400);

        try {
            serverConnection = client.connect(serverAddress, serverPort);
            messageHandler = new MessageHandler(serverConnection, this);
        } catch (IOException e) {
            appendMessageBox("Unable to connect to the server: " + e.getMessage());
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
