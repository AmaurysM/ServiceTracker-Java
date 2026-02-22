package com.amaurysdelossantos.ServiceTracker.Helper;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

public class Lib {


    static public boolean FXMLExistsInNode(String target, Pane location) {
        String targetId = getAnchorPaneIdFromFXML(target);
        if (targetId == null) return false;

        String selector = targetId.startsWith("#") ? targetId : "#" + targetId;
        Node found = location.lookup(selector);
        return found != null;
    }

    static public String getAnchorPaneIdFromFXML(String resourcePath) {
        try (InputStream is = Lib.class.getResourceAsStream(resourcePath)) {

            if (is == null) {
                System.err.println("Resource not found: " + resourcePath);
                return null;
            }

            // Create a document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Important for handling fx namespace
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);

            // Find all AnchorPane elements
            NodeList anchorPanes = document.getElementsByTagName("AnchorPane");

            if (anchorPanes.getLength() == 0) {
                return null; // No AnchorPane found
            }

            // Get the first AnchorPane element
            Element anchorPane = (Element) anchorPanes.item(0);

            // Get the fx:id attribute (namespace aware)
            String fxId = anchorPane.getAttributeNS("http://javafx.com/fxml", "id");

            return !fxId.isEmpty() ? fxId : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final Duration TRANSITION_DURATION = Duration.millis(150);
    private static final Interpolator EASE = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);

    static public void attachButtonAnimation(Region node) {
        Timeline[] hover = {null};
        node.setOnMouseEntered(e -> {
            if (hover[0] != null) hover[0].stop();
            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
                    new KeyValue(node.scaleXProperty(), 1.05, EASE),
                    new KeyValue(node.scaleYProperty(), 1.05, EASE)));
            hover[0].play();
        });
        node.setOnMouseExited(e -> {
            if (hover[0] != null) hover[0].stop();
            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
                    new KeyValue(node.scaleXProperty(), 1.0, EASE),
                    new KeyValue(node.scaleYProperty(), 1.0, EASE)));
            hover[0].play();
        });
        node.setOnMousePressed(e -> {
            if (hover[0] != null) hover[0].stop();
            hover[0] = new Timeline(new KeyFrame(Duration.millis(80),
                    new KeyValue(node.scaleXProperty(), 0.95, EASE),
                    new KeyValue(node.scaleYProperty(), 0.95, EASE)));
            hover[0].play();
        });
        node.setOnMouseReleased(e -> {
            if (hover[0] != null) hover[0].stop();
            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
                    new KeyValue(node.scaleXProperty(), 1.05, EASE),
                    new KeyValue(node.scaleYProperty(), 1.05, EASE)));
            hover[0].play();
        });
    }

    static public void attachToggleAnimation(ToggleButton toggle) {
        Timeline[] hover = {null};
        toggle.setOnMouseEntered(e -> {
            if (hover[0] != null) hover[0].stop();
            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
                    new KeyValue(toggle.scaleXProperty(), 1.04, EASE),
                    new KeyValue(toggle.scaleYProperty(), 1.04, EASE)));
            hover[0].play();
        });
        toggle.setOnMouseExited(e -> {
            if (hover[0] != null) hover[0].stop();
            double target = toggle.isSelected() ? 1.02 : 1.0;
            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
                    new KeyValue(toggle.scaleXProperty(), target, EASE),
                    new KeyValue(toggle.scaleYProperty(), target, EASE)));
            hover[0].play();
        });
        toggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (hover[0] != null) hover[0].stop();
            double target = isSelected ? 1.02 : 1.0;
            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
                    new KeyValue(toggle.scaleXProperty(), target, EASE),
                    new KeyValue(toggle.scaleYProperty(), target, EASE)));
            hover[0].play();
        });
    }

    static public void attachSearchFieldAnimation(TextField field) {
        Timeline[] focus = {null};
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (focus[0] != null) focus[0].stop();
            double target = isFocused ? 1.01 : 1.0;
            focus[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
                    new KeyValue(field.scaleXProperty(), target, EASE),
                    new KeyValue(field.scaleYProperty(), target, EASE)));
            focus[0].play();
        });
    }

    static public void preventDeselection(ToggleGroup group) {
        group.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) group.selectToggle(oldToggle);
        });
    }
}
