package com.amaurysdelossantos.ServiceTracker.controllers.settings;

import com.amaurysdelossantos.ServiceTracker.Services.AuthService;
import com.amaurysdelossantos.ServiceTracker.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class StatisticsViewController {

    // ── Company info ────────────────────────────────────────────
    @FXML private Label companyNameLabel;
    @FXML private Label companyLocationLabel;

    // ── Overview stats ──────────────────────────────────────────
    @FXML private Label totalItemsLabel;
    @FXML private Label completedItemsLabel;
    @FXML private Label activeItemsLabel;
    @FXML private Label completionRateLabel;

    // ── Service breakdown grid (built in Java) ──────────────────
    @FXML private GridPane serviceBreakdownGrid;

    // ── Recent activity ─────────────────────────────────────────
    @FXML private Label items7Label;
    @FXML private Label items30Label;
    @FXML private Label completed7Label;
    @FXML private Label completed30Label;

    // ── Service summary ─────────────────────────────────────────
    @FXML private Label totalServicesLabel;
    @FXML private Label completedServicesLabel;
    @FXML private Label pendingServicesLabel;
    @FXML private Label serviceRateLabel;

    @Autowired private AuthService authService;

    /** Service colours matching the Next.js serviceColors palette. */
    private static final String[] SERVICE_COLOURS = {
            "#f97316", // Fuel      – orange
            "#22c55e", // Catering  – green
            "#8b5cf6", // GPU       – purple
            "#06b6d4", // Lavatory  – cyan
            "#3b82f6", // Water     – blue
            "#ec4899", // Windshield– pink
            "#14b8a6"  // Oil       – teal
    };

    private static final String[] SERVICE_NAMES = {
            "Fuel", "Catering", "GPU", "Lavatory",
            "Potable Water", "Windshield", "Oil Service"
    };

    @FXML
    public void initialize() {
        populateCompanyInfo();
        populateOverview();
        buildServiceBreakdown();
        populateRecentActivity();
        populateServiceSummary();
    }

    // ── Company ─────────────────────────────────────────────────

    private void populateCompanyInfo() {
        User user = authService.getCurrentUser();
        String companyId = (user != null) ? user.getCompanyId() : null;

        // Until a /api/company endpoint is wired, show the company id.
        // Replace with real company name/location when available.
        setText(companyNameLabel,     nullSafe(companyId, "—"));
        setText(companyLocationLabel, "—"); // TODO: fetch from company API
    }

    // ── Overview ─────────────────────────────────────────────────

    private void populateOverview() {
        // TODO: fetch real totals from your API (/api/company/statistics)
        int total     = 0;
        int completed = 0;
        int active    = 0;
        int rate      = total > 0 ? Math.round((float) completed / total * 100) : 0;

        setText(totalItemsLabel,     String.valueOf(total));
        setText(completedItemsLabel, String.valueOf(completed));
        setText(activeItemsLabel,    String.valueOf(active));
        setText(completionRateLabel, rate + "%");
    }

    // ── Service breakdown grid ────────────────────────────────────

    private void buildServiceBreakdown() {
        if (serviceBreakdownGrid == null) return;
        serviceBreakdownGrid.getChildren().clear();

        // TODO: replace with real per-service counts from your API
        Map<String, int[]> breakdown = new LinkedHashMap<>();
        for (String name : SERVICE_NAMES) {
            breakdown.put(name, new int[]{0, 0}); // {total, completed}
        }

        int col = 0, row = 0;
        int ci  = 0;
        for (Map.Entry<String, int[]> entry : breakdown.entrySet()) {
            VBox card = buildServiceCard(
                    entry.getKey(),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    SERVICE_COLOURS[ci % SERVICE_COLOURS.length]
            );
            serviceBreakdownGrid.add(card, col, row);
            col++;
            if (col == 3) { col = 0; row++; }
            ci++;
        }
    }

    private VBox buildServiceCard(String name, int total, int completed, String hexColour) {
        int     pending = total - completed;
        int     pct     = total > 0 ? Math.round((float) completed / total * 100) : 0;
        Color   accent  = Color.web(hexColour);
        String  fxHex   = toFxHex(accent);

        // Card container
        VBox card = new VBox(6);
        card.setStyle(
                "-fx-background-color: " + hexColour + "08;" +
                        "-fx-border-color: "     + hexColour + "40;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 10;"
        );

        // Service name
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + fxHex + ";");

        // Stat rows
        VBox stats = new VBox(3,
                statRow("Total",     String.valueOf(total),     "#374151"),
                statRow("Completed", String.valueOf(completed), "#16a34a"),
                statRow("Pending",   String.valueOf(pending),   "#1d4ed8")
        );

        // Progress bar
        AnchorPane track = new AnchorPane();
        track.setStyle("-fx-background-color: #e5e7eb;");
        track.setPrefHeight(5); track.setMinHeight(5); track.setMaxHeight(5);

        Region fill = new Region();
        fill.setStyle("-fx-background-color: " + fxHex + ";");
        fill.setPrefHeight(5); fill.setMinHeight(5); fill.setMaxHeight(5);
        AnchorPane.setTopAnchor(fill, 0.0);
        AnchorPane.setBottomAnchor(fill, 0.0);
        AnchorPane.setLeftAnchor(fill, 0.0);
        track.getChildren().add(fill);
        track.widthProperty().addListener((obs, o, w) ->
                fill.setPrefWidth(w.doubleValue() * pct / 100.0));

        Label pctLabel = new Label(pct + "%");
        pctLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + fxHex + ";");

        card.getChildren().addAll(nameLabel, stats, track, pctLabel);
        return card;
    }

    private HBox statRow(String caption, String value, String valueColour) {
        Label cap = new Label(caption);
        cap.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + valueColour + ";");

        return new HBox(cap, spacer, val);
    }

    // ── Recent activity ──────────────────────────────────────────

    private void populateRecentActivity() {
        // TODO: real data from /api/company/statistics
        setText(items7Label,     "0");
        setText(items30Label,    "0");
        setText(completed7Label, "0");
        setText(completed30Label,"0");
    }

    // ── Service summary ──────────────────────────────────────────

    private void populateServiceSummary() {
        // TODO: real data
        int totalSvc     = 0;
        int completedSvc = 0;
        int rate         = totalSvc > 0 ? Math.round((float) completedSvc / totalSvc * 100) : 0;

        setText(totalServicesLabel,     String.valueOf(totalSvc));
        setText(completedServicesLabel, String.valueOf(completedSvc));
        setText(pendingServicesLabel,   String.valueOf(totalSvc - completedSvc));
        setText(serviceRateLabel,       rate + "%");
    }

    // ── Util ─────────────────────────────────────────────────────

    private static void setText(Label l, String text) {
        if (l != null) l.setText(text != null ? text : "—");
    }

    private static String nullSafe(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    private static String toFxHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }
}