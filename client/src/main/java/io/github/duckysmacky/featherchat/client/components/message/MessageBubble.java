package io.github.duckysmacky.featherchat.client.components.message;

import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public abstract class MessageBubble extends VBox {
    protected static final double MAX_WIDTH = 500;
    protected static final double CORNER_RADIUS = 15;
    protected static final double PADDING = 10;

    public MessageBubble(Color backgroundColor) {
        setSpacing(5);
        setMaxWidth(MAX_WIDTH);
        setStyle(
            "-fx-background-color: " + toRgbString(backgroundColor) + ";" +
            "-fx-background-radius: " + CORNER_RADIUS + ";" +
            "-fx-padding: " + PADDING + "px;"
        );
    }

    protected void initialize() {
        VBox messageContent = createContentSection();

        getChildren().add(messageContent);
    }

    private String toRgbString(Color color) {
        return String.format("rgb(%d,%d,%d)",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    protected abstract VBox createContentSection();
}