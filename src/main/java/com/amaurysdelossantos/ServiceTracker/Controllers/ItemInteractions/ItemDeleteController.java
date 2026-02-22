package com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions;

import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import lombok.Setter;

import java.time.Instant;

public class ItemDeleteController {

    @FXML
    private StackPane rootPane;
    @FXML
    private Label tailLabel;
    @FXML
    private Label completionCountLabel;
    @FXML
    private Label itemStatusLabel;
    @FXML
    private GridPane servicesGrid;

    @Setter
    private ServiceItemService serviceItemService;
    @Setter
    private Runnable onDeletedCallback;

    @Setter
    private ServiceItem item;

    @FXML
    public void initialize() {
    }

    public void populate() {
        if (item == null) return;

        tailLabel.setText(item.getTail() != null ? item.getTail() : "—");

        // Completion stats
        int total = 0, done = 0;
        for (ServiceType st : ServiceType.values()) {
            if (!isActive(st)) continue;
            total++;
            if (isCompleted(st)) done++;
        }

        completionCountLabel.setText(done + " / " + total);
        boolean itemDone = item.getCompletedAt() != null;
        itemStatusLabel.setText(itemDone ? "Completed" : "Not Completed");
        itemStatusLabel.setStyle(itemDone
                ? "-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#15803d;"
                : "-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#dc2626;");

        // Services 2-column grid
        buildServicesGrid();
    }

    private void buildServicesGrid() {
        servicesGrid.getChildren().clear();
        servicesGrid.getColumnConstraints().clear();

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        servicesGrid.getColumnConstraints().addAll(col1, col2);

        int col = 0, row = 0;
        for (ServiceType st : ServiceType.values()) {
            if (!isActive(st)) continue;
            boolean done = isCompleted(st);

            java.awt.Color awtColor = st.getPrimaryColor();

            // Color dot
            Region dot = new Region();
            dot.setPrefSize(10, 10);
            dot.setMinSize(10, 10);
            dot.setMaxSize(10, 10);
            dot.setStyle("-fx-background-color: rgb("
                    + awtColor.getRed() + "," + awtColor.getGreen() + "," + awtColor.getBlue() + ");"
                    + "-fx-background-radius: 5;");

            Label nameLbl = new Label(label(st));
            nameLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#374151;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            Label statusLbl = new Label(done ? "✓" : "✗");
            statusLbl.setStyle(done
                    ? "-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#15803d;"
                    : "-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#dc2626;");

            HBox chip = new HBox(8, dot, nameLbl, statusLbl);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setPadding(new Insets(8, 12, 8, 12));
            chip.getStyleClass().add(done ? "service-chip-done" : "service-chip");
            GridPane.setHgrow(chip, Priority.ALWAYS);
            GridPane.setFillWidth(chip, true);

            servicesGrid.add(chip, col, row);
            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }
    }

    @FXML
    private void onDelete() {
        Thread t = new Thread(() -> {
            try {
                item.setDeletedAt(Instant.now());
                item.setUpdatedAt(Instant.now());
                serviceItemService.saveService(item);
                Platform.runLater(() -> {
                    if (onDeletedCallback != null) onDeletedCallback.run();
                    closeModal();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onCancel() {
        closeModal();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private boolean isActive(ServiceType t) {
        return switch (t) {
            case FUEL -> item.getFuel() != null;
            case CATERING -> item.getCatering() != null && !item.getCatering().isEmpty();
            case GPU -> item.getGpu() != null;
            case LAVATORY -> item.getLavatory() != null;
            case POTABLE_WATER -> item.getPotableWater() != null;
            case WINDSHIELD_CLEANING -> item.getWindshieldCleaning() != null;
            case OIL_SERVICE -> item.getOilService() != null;
        };
    }

    private boolean isCompleted(ServiceType t) {
        return switch (t) {
            case FUEL -> item.getFuel() != null && item.getFuel().getCompletedAt() != null;
            case CATERING -> item.getCatering() != null && !item.getCatering().isEmpty()
                    && item.getCatering().stream().allMatch(c -> c.getCompletedAt() != null);
            case GPU -> item.getGpu() != null && item.getGpu().getCompletedAt() != null;
            case LAVATORY -> item.getLavatory() != null && item.getLavatory().getCompletedAt() != null;
            case POTABLE_WATER -> item.getPotableWater() != null && item.getPotableWater().getCompletedAt() != null;
            case WINDSHIELD_CLEANING ->
                    item.getWindshieldCleaning() != null && item.getWindshieldCleaning().getCompletedAt() != null;
            case OIL_SERVICE -> item.getOilService() != null && item.getOilService().getCompletedAt() != null;
        };
    }

    private String label(ServiceType t) {
        return switch (t) {
            case FUEL -> "Fuel";
            case CATERING -> "Catering";
            case GPU -> "GPU";
            case LAVATORY -> "Lavatory";
            case POTABLE_WATER -> "Potable Water";
            case WINDSHIELD_CLEANING -> "Windshield Cleaning";
            case OIL_SERVICE -> "Oil Service";
        };
    }

    private void closeModal() {
        ((Stage) rootPane.getScene().getWindow()).close();
    }
}