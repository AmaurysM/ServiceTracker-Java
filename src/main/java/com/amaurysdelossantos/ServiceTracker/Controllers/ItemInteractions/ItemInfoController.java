package com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions;

import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ItemInfoController {

    @FXML
    public Button editBtn;
    @FXML
    public Button closeBtn;
    @FXML
    private StackPane rootPane;
    @FXML
    private Label tailLabel;
    @FXML
    private VBox flightInfoBox;
    @FXML
    private VBox servicesBox;
    @FXML
    private VBox timestampsBox;

    @Setter
    private ServiceItem item;
    @Setter
    private ServiceItemService serviceItemService;
    @Setter
    private Runnable onEditCallback;
    @Setter
    private Consumer<ServiceType> onEditServiceCallback;

    private Consumer<ServiceItem> onDeleteCallback;

    @Setter
    private Runnable refreshCallback;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy  HH:mm").withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        editBtn.setOnAction(e -> {
            handleEdit();
        });

        closeBtn.setOnAction(e -> {
            closeModal();
        });
    }

    public void populate() {
        if (item == null) return;

        tailLabel.setText(item.getTail() != null ? item.getTail() : "—");

        buildFlightInfo();
        buildServices();
        buildTimestamps();

        fadeIn(rootPane);
    }

    // ── Flight info ────────────────────────────────────────────────────────────

    private void buildFlightInfo() {
        flightInfoBox.getChildren().clear();

        boolean hasArrival = item.getArrival() != null;
        boolean hasDeparture = item.getDeparture() != null;
        boolean hasDesc = item.getDescription() != null && !item.getDescription().isBlank();

        if (!hasArrival && !hasDeparture && !hasDesc) {
            flightInfoBox.getChildren().add(
                    mutedLabel("No flight information recorded."));
            return;
        }

        if (hasArrival) flightInfoBox.getChildren().add(infoRow("Arrival", FMT.format(item.getArrival()), false));
        if (hasDeparture)
            flightInfoBox.getChildren().add(infoRow("Departure", FMT.format(item.getDeparture()), !hasDesc));
        if (hasDesc) {
            VBox descBlock = new VBox(4);
            descBlock.setPadding(new Insets(10, 14, 10, 14));
            Label lbl = new Label("Description");
            lbl.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#374151;");
            Label val = new Label(item.getDescription());
            val.setStyle("-fx-font-size:12px; -fx-text-fill:#6b7280;");
            val.setWrapText(true);
            descBlock.getChildren().addAll(lbl, val);
            flightInfoBox.getChildren().add(descBlock);
        }
    }

    // ── Services ───────────────────────────────────────────────────────────────

    private void buildServices() {
        servicesBox.getChildren().clear();
        boolean any = false;

        for (ServiceType st : ServiceType.values()) {
            if (!isActive(st)) continue;
            any = true;
            boolean done = isCompleted(st);

            java.awt.Color awtColor = st.getPrimaryColor();
            Color fxColor = Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

            // Icon
            ImageView iv = makeIcon(st, 20, fxColor);

            // Service name
            Label nameLbl = new Label(label(st));
            nameLbl.getStyleClass().add("service-row-name");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            // Status badge
            Label badge = new Label(done ? "Completed" : "Pending");
            badge.getStyleClass().add(done ? "service-badge-completed" : "service-badge-pending");

            // Row — color-tinted left border matching Next.js
            HBox row = new HBox(12, iv, nameLbl, badge);
            row.getStyleClass().add("service-row");
            row.setStyle("-fx-border-color:" + toHex(fxColor, 0.40)
                    + "; -fx-border-width:0 0 0 3;");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setOnMouseClicked(e -> {
                closeModal();
                if (onEditServiceCallback != null) {
                    Platform.runLater(() -> onEditServiceCallback.accept(st));
                }
            });
            row.setStyle(row.getStyle() + "; -fx-cursor: hand;");

            row.setOnMouseClicked(e -> {
                handleEdit(st);
            });

            servicesBox.getChildren().add(row);
        }

        if (!any) {
            servicesBox.getChildren().add(mutedLabel("No services required."));
        }
    }

    // ── Timestamps ─────────────────────────────────────────────────────────────

    private void buildTimestamps() {
        timestampsBox.getChildren().clear();
        boolean hasDeleted = item.getDeletedAt() != null;

        timestampsBox.getChildren().add(
                infoRow("Created", item.getCreatedAt() != null ? FMT.format(item.getCreatedAt()) : "—", false));
        timestampsBox.getChildren().add(
                infoRow("Updated", item.getUpdatedAt() != null ? FMT.format(item.getUpdatedAt()) : "—", !hasDeleted));

        if (hasDeleted) {
            HBox row = infoRow("Deleted", FMT.format(item.getDeletedAt()), true);
            row.setStyle("-fx-background-color:#fef2f2; -fx-border-color:transparent transparent #fecaca transparent; -fx-border-width:0 0 1 0;");
            // Tint the value label red
            if (row.getChildren().size() >= 2 && row.getChildren().get(1) instanceof Label v)
                v.setStyle("-fx-font-size:12px; -fx-text-fill:#dc2626;");
            timestampsBox.getChildren().add(row);
        }
    }

    // ── actions ──────────────────────────────────────────────────────────
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/components/ItemInteraction/item-edit-modal.fxml")
            );
            loader.setClassLoader(getClass().getClassLoader());
            Parent root = loader.load();
            ItemEditController controller = loader.getController();

            controller.setItem(item);
            controller.populate();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("Edit: " + item.getTail());
            stage.initModality(Modality.APPLICATION_MODAL);
            //stage.setResizable(false);

            stage.showAndWait(); // Wait until closed


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEdit(ServiceType serviceType) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/components/ItemInteraction/item-edit-modal.fxml")
            );
            loader.setClassLoader(getClass().getClassLoader());
            Parent root = loader.load();
            ItemEditController controller = loader.getController();

            controller.setItem(item);
            controller.setInitialService(serviceType);
            controller.populate();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("Edit: " + item.getTail());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.showAndWait(); // Wait until closed


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onEdit() {
        // Close this stage first, then schedule Edit to open on the next pulse
        // so showAndWait() fully unwinds before the new window is constructed.
        Stage thisStage = (Stage) rootPane.getScene().getWindow();
        thisStage.close();
        if (onEditCallback != null) {
            Platform.runLater(onEditCallback::run);
        }
    }

    private void onClose() {
        closeModal();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Two-column info row matching the Next.js `<div className="flex justify-between">` pattern
     */
    private HBox infoRow(String key, String value, boolean isLast) {
        Label k = new Label(key);
        k.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#374151;");

        Label v = new Label(value);
        v.setStyle("-fx-font-size:12px; -fx-text-fill:#4a5568;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, k, spacer, v);
        row.setPadding(new Insets(9, 14, 9, 14));
        if (!isLast) {
            row.setStyle("-fx-border-color:transparent transparent #e8ecf0 transparent; -fx-border-width:0 0 1 0;");
        }
        return row;
    }

    private Label mutedLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#9ca3af; -fx-padding:4 0;");
        return l;
    }

    private ImageView makeIcon(ServiceType st, int size, Color tint) {
        try {
            BufferedImage bi = st.getImage(size * 2);
            // Tint
            java.awt.Color awt = st.getPrimaryColor();
            BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < bi.getHeight(); y++) {
                for (int x = 0; x < bi.getWidth(); x++) {
                    int argb = bi.getRGB(x, y);
                    int a = (argb >> 24) & 0xff;
                    if (a > 0)
                        out.setRGB(x, y, (a << 24) | (awt.getRed() << 16) | (awt.getGreen() << 8) | awt.getBlue());
                }
            }
            WritableImage wi = SwingFXUtils.toFXImage(out, null);
            ImageView iv = new ImageView(wi);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) {
            return new ImageView();
        }
    }

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

    private String toHex(Color c, double alpha) {
        int a = (int) (alpha * 255);
        return String.format("#%02x%02x%02x%02x",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255), a);
    }

    private void closeModal() {
        ((Stage) rootPane.getScene().getWindow()).close();
    }

    private void fadeIn(Node node) {
        node.setOpacity(0);
        new Timeline(new KeyFrame(Duration.millis(180),
                new KeyValue(node.opacityProperty(), 1.0, Interpolator.EASE_OUT))).play();
    }
}