package com.amaurysdelossantos.ServiceTracker.controllers;

import com.amaurysdelossantos.ServiceTracker.controllers.map.MapViewController;
import com.amaurysdelossantos.ServiceTracker.Services.AuthService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceTrackerService;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.User;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.ServiceView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

import static com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler.handleAdd;

@Component
@Scope("prototype")
public class ServiceTrackerController {

    @FXML public AnchorPane centerPane;
    @FXML public StackPane  initialsStackPane;
    @FXML private Text      userInitials;
    @FXML private Button    addButton;

    @Autowired private ApplicationContext      applicationContext;
    @Autowired private StandardControlsService topStateService;
    @Autowired private ServiceTrackerService   serviceTrackerService;
    @Autowired private AuthService             authService;

    private Popup userOptionsPopup;

    @FXML
    public void initialize() {

        // ── Initials + tooltip ──────────────────────────────────
        User user = authService.getCurrentUser();
        if (user != null) {
            userInitials.setText(buildInitials(user));
            String tooltipText = buildDisplayName(user)
                    + (user.getEmail() != null && !user.getEmail().isEmpty()
                    ? "\n" + user.getEmail() : "");
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setShowDelay(Duration.millis(300));
            tooltip.setStyle(
                    "-fx-font-size: 12px;" +
                            "-fx-background-color: #1e2230;" +
                            "-fx-text-fill: #f0f2f7;" +
                            "-fx-border-color: #2a2f3e;" +
                            "-fx-border-width: 1;"
            );
            Tooltip.install(initialsStackPane, tooltip);
        } else {
            userInitials.setText("?");
        }

        // ── Service view listener ───────────────────────────────
        // Guard against null: MainViewController resets this property to null
        // before reloading the FXML. The previous (discarded) instance's
        // listener still fires on that null transition — without the guard
        // the switch crashes with NPE on newView.ordinal().
        serviceTrackerService.activeViewProperty().addListener((e, oldItem, newItem) -> {
            if (newItem == null) return;
            onMainViewChange(newItem);
        });

        // ── Add button ──────────────────────────────────────────
        addButton.setOnAction(e -> handleAdd());

        // ── User options popup ──────────────────────────────────
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/components/standard/user-options.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            VBox dropdown = loader.load();
            dropdown.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/styles/standard/user-options.css")
                    ).toExternalForm()
            );
            userOptionsPopup = new Popup();
            userOptionsPopup.getContent().add(dropdown);
            userOptionsPopup.setAutoHide(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        initialsStackPane.setOnMouseClicked(e -> toggleUserOptions());

        serviceTrackerService.activeViewProperty().setValue(ServiceView.STANDARD);
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private String buildInitials(User user) {
        String first = user.getFirstname() != null ? user.getFirstname().trim() : "";
        String last  = user.getLastname()  != null ? user.getLastname().trim()  : "";
        if (!first.isEmpty() && !last.isEmpty())
            return (String.valueOf(first.charAt(0)) + String.valueOf(last.charAt(0))).toUpperCase();
        if (!first.isEmpty())
            return String.valueOf(first.charAt(0)).toUpperCase();
        String username = user.getUsername() != null ? user.getUsername().trim() : "";
        if (!username.isEmpty())
            return String.valueOf(username.charAt(0)).toUpperCase();
        return "?";
    }

    private String buildDisplayName(User user) {
        String first = user.getFirstname() != null ? user.getFirstname().trim() : "";
        String last  = user.getLastname()  != null ? user.getLastname().trim()  : "";
        String full  = (first + " " + last).trim();
        if (!full.isEmpty()) return full;
        return user.getUsername() != null ? user.getUsername() : "";
    }

    // ─── Popup toggle ──────────────────────────────────────────────

    private void toggleUserOptions() {
        if (userOptionsPopup.isShowing()) {
            userOptionsPopup.hide();
            return;
        }
        var bounds = initialsStackPane.localToScreen(initialsStackPane.getBoundsInLocal());
        userOptionsPopup.show(initialsStackPane, bounds.getMinX(), bounds.getMaxY() + 5);
    }

    // ─── View switching ────────────────────────────────────────────

    private void onMainViewChange(ServiceView newView) {
        try {
            switch (newView) {
                case STANDARD -> {
                    centerPane.getChildren().clear();
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/components/standard/standard-view.fxml"));
                    loader.setControllerFactory(applicationContext::getBean);
                    Node child = loader.load();
                    anchor(child);
                    centerPane.getChildren().add(child);
                }
                case MAP -> {
                    centerPane.getChildren().clear();
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/components/map/map-view.fxml"));
                    loader.setControllerFactory(applicationContext::getBean);
                    Node child = loader.load();
                    anchor(child);
                    centerPane.getChildren().add(child);
                    MapViewController mapCtrl = loader.getController();
                    Platform.runLater(mapCtrl::invalidateSize);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void anchor(Node child) {
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);
        AnchorPane.setRightAnchor(child, 0.0);
    }
}