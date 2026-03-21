package com.amaurysdelossantos.ServiceTracker.controllers.settings;

import com.amaurysdelossantos.ServiceTracker.Services.AuthService;
import com.amaurysdelossantos.ServiceTracker.Services.MainService;
import com.amaurysdelossantos.ServiceTracker.models.User;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.MainView;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

@Component
@Scope("prototype")
public class SettingsViewController {

    @FXML private StackPane  initialsPane;
    @FXML private Text       initialsText;
    @FXML private Button     profileTabBtn;
    @FXML private Button     statisticsTabBtn;
    @FXML private Button     backBtn;
    @FXML private Label      messageBanner;
    @FXML private AnchorPane contentPane;

    @Autowired private AuthService        authService;
    @Autowired private MainService        mainService;
    @Autowired private ApplicationContext applicationContext;

    private Popup userOptionsPopup;

    private enum Tab { PROFILE, STATISTICS }
    private Tab activeTab;

    @FXML
    public void initialize() {

        // ── Initials ────────────────────────────────────────────
        User user = authService.getCurrentUser();
        if (user != null) {
            initialsText.setText(buildInitials(user));
        } else {
            initialsText.setText("?");
        }

        // ── User-options popup (same pattern as ServiceTrackerController) ──
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/components/standard/user-options.fxml")
            );
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
            throw new RuntimeException("Failed to load user-options popup", e);
        }

        // ── Default tab ─────────────────────────────────────────
        showTab(Tab.PROFILE);
    }

    // ── Initials click ──────────────────────────────────────────

    @FXML
    public void onInitialsClicked() {
        if (userOptionsPopup == null) return;

        if (userOptionsPopup.isShowing()) {
            userOptionsPopup.hide();
            return;
        }

        var bounds = initialsPane.localToScreen(initialsPane.getBoundsInLocal());
        userOptionsPopup.show(initialsPane, bounds.getMinX(), bounds.getMaxY() + 5);
    }

    // ── Back button — returns to SERVICES ──────────────────────

    @FXML
    public void onBack() {
        mainService.activeViewProperty().setValue(MainView.SERVICES);
    }

    // ── Tab switching ───────────────────────────────────────────

    @FXML public void onProfileTab()    { showTab(Tab.PROFILE); }
    @FXML public void onStatisticsTab() { showTab(Tab.STATISTICS); }

    private void showTab(Tab tab) {
        if (tab == activeTab) return;
        activeTab = tab;

        boolean isProfile = tab == Tab.PROFILE;
        setTabActive(profileTabBtn,    isProfile);
        setTabActive(statisticsTabBtn, !isProfile);

        String fxml = isProfile
                ? "/components/settings/profile-view.fxml"
                : "/components/settings/statistics-view.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(applicationContext::getBean);
            Node child = loader.load();

            AnchorPane.setTopAnchor(child, 0.0);
            AnchorPane.setBottomAnchor(child, 0.0);
            AnchorPane.setLeftAnchor(child, 0.0);
            AnchorPane.setRightAnchor(child, 0.0);
            contentPane.getChildren().setAll(child);

            if (isProfile) {
                ProfileViewController ctrl = loader.getController();
                ctrl.setBannerCallback(this::showBanner);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load settings tab: " + fxml, e);
        }
    }

    private void setTabActive(Button btn, boolean active) {
        btn.getStyleClass().removeAll("settings-tab-active");
        if (active) btn.getStyleClass().add("settings-tab-active");
    }

    // ── Banner ──────────────────────────────────────────────────

    public void showBanner(String text, boolean success) {
        messageBanner.setText(text);
        messageBanner.getStyleClass().removeAll(
                "settings-banner-hidden", "settings-banner-success", "settings-banner-error");
        messageBanner.getStyleClass().add(
                success ? "settings-banner-success" : "settings-banner-error");
        messageBanner.setVisible(true);
        messageBanner.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> hideBanner());
        pause.play();
    }

    private void hideBanner() {
        messageBanner.setVisible(false);
        messageBanner.setManaged(false);
        messageBanner.getStyleClass().removeAll("settings-banner-success", "settings-banner-error");
        messageBanner.getStyleClass().add("settings-banner-hidden");
    }

    // ── Helpers ─────────────────────────────────────────────────

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
}