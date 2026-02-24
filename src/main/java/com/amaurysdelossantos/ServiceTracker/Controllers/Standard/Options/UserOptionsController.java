package com.amaurysdelossantos.ServiceTracker.Controllers.Standard.Options;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for settings-view.fxml.
 *
 * Mirrors the Next.js settings page:
 *   – Profile tab  : read-only account info + editable profile form
 *   – Statistics tab: company overview, service breakdown, recent activity
 *
 * Wire your real services / API calls where the TODO comments appear.
 */
@Component
public class UserOptionsController {

    // ── Header / tabs ────────────────────────────────────────────────────────
    @FXML private Button  backBtn;
    @FXML private Button  profileTabBtn;
    @FXML private Button  statisticsTabBtn;

    // ── Tab panes ─────────────────────────────────────────────────────────────
    @FXML private VBox    profilePane;
    @FXML private VBox    statisticsPane;

    // ── Notification banner ───────────────────────────────────────────────────
    @FXML private Label   messageBanner;

    // ── Account info labels ───────────────────────────────────────────────────
    @FXML private Label   roleLabel;
    @FXML private Label   companyIdLabel;
    @FXML private Label   memberSinceLabel;
    @FXML private Label   lastUpdatedLabel;

    // ── Profile form fields ───────────────────────────────────────────────────
    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private TextField nicknameField;
    @FXML private Button    saveBtn;

    // ── Company info ──────────────────────────────────────────────────────────
    @FXML private Label companyNameLabel;
    @FXML private Label companyLocationLabel;

    // ── Overview tiles ────────────────────────────────────────────────────────
    @FXML private Label totalItemsLabel;
    @FXML private Label completedItemsLabel;
    @FXML private Label activeItemsLabel;
    @FXML private Label completionRateLabel;

    // ── Service breakdown container (rows injected in code) ───────────────────
    @FXML private VBox serviceBreakdownContainer;

    // ── Recent activity ───────────────────────────────────────────────────────
    @FXML private Label items7Label;
    @FXML private Label items30Label;
    @FXML private Label completed7Label;
    @FXML private Label completed30Label;

    // ── Service summary ───────────────────────────────────────────────────────
    @FXML private Label totalServicesLabel;
    @FXML private Label completedServicesLabel;
    @FXML private Label pendingServicesLabel;
    @FXML private Label serviceRateLabel;

    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Back button label (the dark square shows the first initial)
        // backBtn.setText("S");   // TODO: replace with session user initial
//
//        populateProfileData();
//        populateStatisticsData();
//        showTab(Tab.PROFILE);
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    private enum Tab { PROFILE, STATISTICS }

    @FXML
    private void onProfileTab() {
        showTab(Tab.PROFILE);
    }

    @FXML
    private void onStatisticsTab() {
        showTab(Tab.STATISTICS);
    }

    private void showTab(Tab tab) {
        boolean isProfile = tab == Tab.PROFILE;

        profilePane.setVisible(isProfile);
        profilePane.setManaged(isProfile);
        statisticsPane.setVisible(!isProfile);
        statisticsPane.setManaged(!isProfile);

        profileTabBtn.getStyleClass().remove("tab-btn-active");
        statisticsTabBtn.getStyleClass().remove("tab-btn-active");

        if (isProfile) {
            profileTabBtn.getStyleClass().add("tab-btn-active");
        } else {
            statisticsTabBtn.getStyleClass().add("tab-btn-active");
        }
    }

    // ── Profile tab ───────────────────────────────────────────────────────────

    /**
     * Populate fields with real data.
     * TODO: inject your UserService / session bean and call it here.
     */
    private void populateProfileData() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        // Account info (read-only)
        roleLabel.setText("Admin");                        // TODO: session.getRole()
        companyIdLabel.setText("COMP-001");                // TODO: session.getCompanyId()
        memberSinceLabel.setText(LocalDate.now().minusYears(1).format(fmt)); // TODO: user.getCreatedAt()
        lastUpdatedLabel.setText(LocalDate.now().format(fmt));               // TODO: user.getUpdatedAt()

        // Editable profile fields
        firstnameField.setText("John");                    // TODO: user.getFirstname()
        lastnameField.setText("Doe");                      // TODO: user.getLastname()
        emailField.setText("johndoe@example.com");         // TODO: user.getEmail()
        usernameField.setText("johndoe");                  // TODO: user.getUsername()
        nicknameField.setText("");                         // TODO: user.getNickname()
    }

    @FXML
    private void onSaveProfile() {
        String firstname = firstnameField.getText().trim();
        String lastname  = lastnameField.getText().trim();
        String email     = emailField.getText().trim();
        String username  = usernameField.getText().trim();
        String nickname  = nicknameField.getText().trim();

        if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty() || username.isEmpty()) {
            showBanner("Please fill in all required fields.", false);
            return;
        }
        if (username.length() < 3) {
            showBanner("Username must be at least 3 characters.", false);
            return;
        }

        saveBtn.setDisable(true);
        saveBtn.setText("SAVING…");

        // TODO: call your UserService.updateProfile(firstname, lastname, email, username, nickname)
        //       on a background thread, then on the JavaFX thread:
        Platform.runLater(() -> {
            // Simulate success
            showBanner("Profile updated successfully.", true);
            saveBtn.setDisable(false);
            saveBtn.setText("SAVE PROFILE");
        });
    }

    // ── Statistics tab ────────────────────────────────────────────────────────

    /**
     * Populate statistics with real data.
     * TODO: inject your CompanyService and call it here.
     */
    private void populateStatisticsData() {
        // Company info
        companyNameLabel.setText("Acme Aviation");       // TODO: companyStats.getName()
        companyLocationLabel.setText("Miami, FL");       // TODO: companyStats.getLocation()

        // Overview
        int total     = 120;   // TODO: companyStats.getTotalItems()
        int completed = 87;    // TODO: companyStats.getCompletedItems()
        int active    = 33;    // TODO: companyStats.getActiveItems()
        int rate      = total > 0 ? (int) Math.round((completed * 100.0) / total) : 0;

        totalItemsLabel.setText(String.valueOf(total));
        completedItemsLabel.setText(String.valueOf(completed));
        activeItemsLabel.setText(String.valueOf(active));
        completionRateLabel.setText(rate + "%");

        // Service breakdown  TODO: replace with real service map
        Map<String, int[]> breakdown = new LinkedHashMap<>();
        breakdown.put("Fuel",              new int[]{30, 22});
        breakdown.put("Catering",          new int[]{25, 20});
        breakdown.put("GPU",               new int[]{15, 12});
        breakdown.put("Lavatory",          new int[]{18, 14});
        breakdown.put("Potable Water",     new int[]{12,  9});
        breakdown.put("Windshield",        new int[]{10,  6});
        breakdown.put("Oil Service",       new int[]{ 8,  4});

        // Accent colours matching the Next.js serviceColors palette
        String[] colours = {
                "#f97316", // Fuel     – orange
                "#22c55e", // Catering – green
                "#8b5cf6", // GPU      – purple
                "#06b6d4", // Lavatory – cyan
                "#3b82f6", // Water    – blue
                "#ec4899", // Windshield – pink
                "#14b8a6"  // Oil      – teal
        };

        serviceBreakdownContainer.getChildren().clear();
        int ci = 0;
        for (Map.Entry<String, int[]> entry : breakdown.entrySet()) {
            serviceBreakdownContainer.getChildren()
                    .add(buildServiceRow(entry.getKey(), entry.getValue()[0],
                            entry.getValue()[1], colours[ci % colours.length]));
            ci++;
        }

        // Recent activity  TODO: real data
        items7Label.setText("12");
        items30Label.setText("48");
        completed7Label.setText("9");
        completed30Label.setText("39");

        // Service summary  TODO: real data
        int totalSvc     = 118;
        int completedSvc = 87;
        totalServicesLabel.setText(String.valueOf(totalSvc));
        completedServicesLabel.setText(String.valueOf(completedSvc));
        pendingServicesLabel.setText(String.valueOf(totalSvc - completedSvc));
        serviceRateLabel.setText(totalSvc > 0
                ? (int) Math.round((completedSvc * 100.0) / totalSvc) + "%"
                : "0%");
    }

    /**
     * Builds a single service-breakdown row:
     *   [colour icon] Name  |  total/completed/pending  |  progress bar  N%
     */
    private VBox buildServiceRow(String name, int total, int completed, String hexColour) {
        Color accent = Color.web(hexColour);
        String fxColour = toFxHex(accent);

        // ── Colour badge (square, like the icon container in Next.js)
        Rectangle badge = new Rectangle(28, 28);
        badge.setFill(Color.web(hexColour + "33")); // 20 % opacity
        badge.setArcWidth(0);
        badge.setArcHeight(0);

        Label initial = new Label(name.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: " + fxColour + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        StackPane iconBox = new StackPane(badge, initial);
        iconBox.setPrefSize(28, 28);
        iconBox.setMaxSize(28, 28);
        iconBox.setMinSize(28, 28);

        // ── Service name
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("service-label");

        VBox nameBox = new VBox(nameLabel);
        nameBox.setSpacing(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        // ── Stats column
        int pending = total - completed;
        int pct     = total > 0 ? (int) Math.round((completed * 100.0) / total) : 0;

        Label totalLbl     = buildStatLine("Total",     String.valueOf(total),     "#374151");
        Label completedLbl = buildStatLine("Completed", String.valueOf(completed), "#15803d");
        Label pendingLbl   = buildStatLine("Pending",   String.valueOf(pending),   "#1d4ed8");

        // ── Progress bar
        AnchorPane track = new AnchorPane();
        track.getStyleClass().add("progress-track");
        track.setPrefHeight(8); track.setMinHeight(8); track.setMaxHeight(8);
        track.setPrefWidth(200);

        Region fill = new Region();
        fill.getStyleClass().add("progress-fill");
        fill.setStyle("-fx-background-color: " + fxColour + ";");
        fill.setPrefHeight(8); fill.setMinHeight(8); fill.setMaxHeight(8);
        AnchorPane.setTopAnchor(fill, 0.0);
        AnchorPane.setBottomAnchor(fill, 0.0);
        AnchorPane.setLeftAnchor(fill, 0.0);
        track.getChildren().add(fill);

        // Bind fill width to track width × percentage
        track.widthProperty().addListener((obs, oldW, newW) ->
                fill.setPrefWidth(newW.doubleValue() * pct / 100.0));

        Label pctLabel = new Label(pct + "%");
        pctLabel.setStyle("-fx-text-fill: " + fxColour + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        HBox progressRow = new HBox(8, track, pctLabel);
        progressRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(track, Priority.ALWAYS);

        VBox statsBox = new VBox(3, totalLbl, completedLbl, pendingLbl, progressRow);

        // ── Top header row
        HBox headerRow = new HBox(10, iconBox, nameBox);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── Outer card (border tinted with accent)
        VBox card = new VBox(8, headerRow, statsBox);
        card.setStyle(
                "-fx-background-color: " + hexColour + "0d;" +  // ~5 % opacity fill
                        "-fx-border-color: " + hexColour + "40;" +       // ~25 % opacity border
                        "-fx-border-width: 1;" +
                        "-fx-padding: 12;"
        );
        return card;
    }

    /** Creates a right-aligned stat line "label  value" */
    private Label buildStatLine(String caption, String value, String valueColour) {
        Label l = new Label(caption + "   " + value);
        l.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-text-fill: #6b7280;"
        );
        // Colour just the value part would need a TextFlow; keep it simple with a label
        // for a cleaner implementation use a TextFlow or two Labels in an HBox.
        HBox row = new HBox();
        Label cap = new Label(caption);
        cap.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + valueColour + ";");
        row.getChildren().addAll(cap, spacer, val);
        row.setMaxWidth(Double.MAX_VALUE);
        // Return a wrapper label-compatible node — we return the row as a Label proxy
        // by encoding the text (simpler for FXML-less injection)
        Label proxy = new Label();
        proxy.setGraphic(row);
        proxy.setMaxWidth(Double.MAX_VALUE);
        return proxy;
    }

    // ── Banner helper ─────────────────────────────────────────────────────────

    private void showBanner(String text, boolean success) {
        messageBanner.setText(text);
        messageBanner.getStyleClass().removeAll("banner-hidden", "banner-success", "banner-error");
        messageBanner.getStyleClass().add(success ? "banner-success" : "banner-error");
        messageBanner.setVisible(true);
        messageBanner.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> hideBanner());
        pause.play();
    }

    private void hideBanner() {
        messageBanner.setVisible(false);
        messageBanner.setManaged(false);
        messageBanner.getStyleClass().removeAll("banner-success", "banner-error");
        messageBanner.getStyleClass().add("banner-hidden");
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Converts a JavaFX Color to a CSS #rrggbb hex string. */
    private static String toFxHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed()   * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue()  * 255));
    }
}