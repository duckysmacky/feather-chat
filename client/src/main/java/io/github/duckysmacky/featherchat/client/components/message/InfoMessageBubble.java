package io.github.duckysmacky.featherchat.client.components.message;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class InfoMessageBubble extends MessageBubble {
    private final String text;
    private final String timestamp;

    public InfoMessageBubble(String text, String timestamp) {
        super(Color.LIGHTGRAY);
        this.text = text;
        this.timestamp = timestamp;

        initialize();
    }

    protected VBox createContentSection() {
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);

        HBox contentBody = new HBox(5);
        contentBody.setAlignment(Pos.CENTER);

        Label textLabel = new Label(text);
        textLabel.setFont(Font.font("System", 12));
        textLabel.setTextFill(Color.BLACK);
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(MAX_WIDTH - 2 * PADDING);

        contentBody.getChildren().add(textLabel);

        HBox contentFooter = new HBox(5);
        contentFooter.setAlignment(Pos.CENTER);

        Label timestampLabel = new Label(timestamp);
        timestampLabel.setFont(Font.font("System", FontWeight.LIGHT, 12));
        timestampLabel.setTextFill(Color.GRAY);

        contentFooter.getChildren().add(timestampLabel);

        content.getChildren().addAll(contentBody, contentFooter);
        return content;
    }
}