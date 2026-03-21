package com.amaurysdelossantos.ServiceTracker.controllers.settings;

import com.amaurysdelossantos.ServiceTracker.Services.AuthService;
import com.amaurysdelossantos.ServiceTracker.models.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

@Component
@Scope("prototype")
public class ProfileViewController {

    @FXML private Label roleLabel;
    @FXML private Label companyIdLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label lastUpdatedLabel;

    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private TextField nicknameField;
    @FXML private Button    saveBtn;

    @Autowired private AuthService authService;

    /** Callback wired by SettingsViewController to surface the banner in the parent. */
    private BiConsumer<String, Boolean> bannerCallback;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy").withZone(ZoneId.systemDefault());

    public void setBannerCallback(BiConsumer<String, Boolean> cb) {
        this.bannerCallback = cb;
    }

    @FXML
    public void initialize() {
        populateFields();
    }

    private void populateFields() {
        User user = authService.getCurrentUser();
        if (user == null) return;

        setText(roleLabel,        capitalise(user.getRole() != null ? user.getRole().name() : "—"));
        setText(companyIdLabel,   nullSafe(user.getCompanyId(), "—"));
        setText(memberSinceLabel, user.getCreatedAt() != null ? DATE_FMT.format(user.getCreatedAt()) : "—");
        setText(lastUpdatedLabel, user.getUpdatedAt() != null ? DATE_FMT.format(user.getUpdatedAt()) : "—");

        setField(firstnameField, user.getFirstname());
        setField(lastnameField,  user.getLastname());
        setField(emailField,     user.getEmail());
        setField(usernameField,  user.getUsername());
        setField(nicknameField,  user.getNickname());
    }

    @FXML
    private void onSaveProfile() {
        String firstname = firstnameField.getText().trim();
        String lastname  = lastnameField.getText().trim();
        String email     = emailField.getText().trim();
        String username  = usernameField.getText().trim();
        String nickname  = nicknameField.getText().trim();

        if (firstname.isEmpty() || lastname.isEmpty()
                || email.isEmpty() || username.isEmpty()) {
            showBanner("Please fill in all required fields.", false);
            return;
        }
        if (username.length() < 3) {
            showBanner("Username must be at least 3 characters.", false);
            return;
        }
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showBanner("Please enter a valid email address.", false);
            return;
        }

        saveBtn.setDisable(true);
        saveBtn.setText("SAVING…");

        // TODO: call a UserService.updateProfile() API endpoint on a background thread
        // For now, update the in-memory user and show success
        Platform.runLater(() -> {
            User user = authService.getCurrentUser();
            if (user != null) {
                user.setFirstname(firstname);
                user.setLastname(lastname);
                user.setEmail(email);
                user.setUsername(username);
                user.setNickname(nickname);
            }
            showBanner("Profile updated successfully.", true);
            saveBtn.setDisable(false);
            saveBtn.setText("SAVE PROFILE");
        });
    }

    // ── Helpers ─────────────────────────────────────────────────

    private void showBanner(String text, boolean success) {
        if (bannerCallback != null) bannerCallback.accept(text, success);
    }

    private static void setText(Label l, String text) {
        if (l != null) l.setText(text != null ? text : "—");
    }

    private static void setField(TextField f, String text) {
        if (f != null) f.setText(text != null ? text : "");
    }

    private static String nullSafe(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) return "—";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}