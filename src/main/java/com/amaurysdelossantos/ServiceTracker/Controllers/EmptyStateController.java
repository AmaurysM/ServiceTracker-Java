package com.amaurysdelossantos.ServiceTracker.Controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lombok.Setter;


public class EmptyStateController {

    @FXML private Circle  pulseRing;
    @FXML private Text    headlineText;
    @FXML private Text    subtitleText;
//    @FXML private Button  clearFiltersButton;
//    @FXML private Button  addNewButton;

    @Setter
    private Runnable clearFiltersCallback;

    @Setter
    private Runnable addServiceCallback;

    @FXML
    public void initialize() {
        startPulse();
    }

    public void setHeadline(String text) {
        headlineText.setText(text);
    }

    public void setMessage(String text) {
        subtitleText.setText(text);
    }


//    @FXML
//    private void onClearFiltersClicked() {
//        if (clearFiltersCallback != null) clearFiltersCallback.run();
//    }
//
//    @FXML
//    private void onAddServiceClicked() {
//        if (addServiceCallback != null) addServiceCallback.run();
//    }

    // ─── Animation ───────────────────────────────────────────────────────────

    private void startPulse() {
        // Gentle scale + fade pulse on the outer ring
        ScaleTransition scale = new ScaleTransition(Duration.seconds(2.0), pulseRing);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.25);
        scale.setToY(1.25);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fade = new FadeTransition(Duration.seconds(2.0), pulseRing);
        fade.setFromValue(0.55);
        fade.setToValue(0.0);
        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition pulse = new ParallelTransition(scale, fade);
        pulse.play();
    }
}