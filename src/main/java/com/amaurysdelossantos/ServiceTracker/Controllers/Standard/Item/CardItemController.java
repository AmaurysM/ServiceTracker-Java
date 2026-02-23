package com.amaurysdelossantos.ServiceTracker.Controllers.Standard.Item;

import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemDeleteController;
import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemEditController;
import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemInfoController;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.amaurysdelossantos.ServiceTracker.Helper.Lib.*;
import static com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler.*;

@Component
@Scope("prototype")
public class CardItemController {

    @Autowired
    private ServiceItemService serviceItemService;

    @FXML
    public HBox headerBox;
    @FXML
    public Label tailLabel;
    @FXML
    public HBox actionButtonsBox;
    @FXML
    public HBox serviceIconsBox;
    @FXML
    public Label arrivalLabel;
    @FXML
    public Label departureLabel;
    @FXML
    public Label descriptionLabel;
    @FXML
    public HBox tailHeaderBox;

    public ServiceItem item;

    public void setItem(ServiceItem item) {
        this.item = item;
        populateCard();
    }

    @FXML
    public void initialize() {}

    public void populateCard() {
        tailLabel.setText(item.getTail());

        arrivalLabel.setText("");
        departureLabel.setText("");
        descriptionLabel.setText("");

        if (item.getArrival() != null)
            arrivalLabel.setText("↓ " + FMT.format(item.getArrival()));

        if (item.getDeparture() != null)
            departureLabel.setText("↑ " + FMT.format(item.getDeparture()));

        if (item.getDescription() != null)
            descriptionLabel.setText(item.getDescription());

        String headerColor = item.getCompletedAt() != null ? "#166534" : "#0f172a";
        headerBox.setStyle(
                "-fx-background-color: " + headerColor + "; " +
                        "-fx-background-radius: 6 6 0 0;"
        );

        renderServices();
        renderActions();

    }

    // ─── Services ────────────────────────────────────────────────────────────────

    private void renderServices() {
        serviceIconsBox.getChildren().clear();

        Map<ServiceType, Boolean> activeServices = getActiveServices(item);

        if (activeServices.isEmpty()) {
            Label noServices = new Label("No services");
            noServices.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
            serviceIconsBox.getChildren().add(noServices);
            return;
        }

        for (Map.Entry<ServiceType, Boolean> entry : activeServices.entrySet()) {
            ServiceType serviceType = entry.getKey();
            boolean completed = entry.getValue();

            java.awt.Color awtColor = serviceType.getPrimaryColor();
            String hex = String.format("#%02x%02x%02x",
                    awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

            String bgAlpha = completed ? "20" : "26";
            String borderAlpha = completed ? "66" : "4d";
            String borderStyle = completed ? "dashed" : "solid";

            StackPane cell = new StackPane();
            HBox.setHgrow(cell, Priority.ALWAYS);
            cell.setMinHeight(56);
            cell.setAlignment(Pos.CENTER);
            cell.setStyle(String.format(
                    "-fx-background-color: %s%s; -fx-border-color: %s%s; " +
                            "-fx-border-width: 1.5; -fx-border-style: %s;",
                    hex, bgAlpha, hex, borderAlpha, borderStyle
            ));

            BufferedImage buffered = serviceType.getImage(22);
            ImageView icon = new ImageView(SwingFXUtils.toFXImage(buffered, null));
            icon.setFitWidth(22);
            icon.setFitHeight(22);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            if (completed) icon.setOpacity(0.6);

            cell.getChildren().add(icon);

            if (completed) {
                Line strike = new Line(-14, 0, 14, 0);
                strike.setStroke(Color.rgb(34, 197, 94));
                strike.setStrokeWidth(2);
                strike.setRotate(45);
                cell.getChildren().add(strike);
            }

            Tooltip.install(cell, new Tooltip(
                    completed ? "✓ Completed - " + serviceType.getType() : serviceType.getType()
            ));

            cell.setOnMouseEntered(e -> cell.setScaleX(1.08));
            cell.setOnMouseExited(e -> cell.setScaleX(1.0));

            cell.setOnMouseClicked(e -> {
                handleEdit(serviceType,item);
            });

            attachButtonAnimation(cell);

            serviceIconsBox.getChildren().add(cell);
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────────

    private void renderActions() {
        actionButtonsBox.getChildren().clear();

        boolean allCompleted = areAllServicesCompleted(item);

        if (allCompleted && item.getCompletedAt() == null) {
            Button doneBtn = createActionButton("DONE", "#16a34a", "#15803d", false);
            doneBtn.setOnAction(e -> handleToggleComplete());
            actionButtonsBox.getChildren().add(doneBtn);
        }

        if (item.getCompletedAt() != null) {
            Button undoBtn = createActionButton("UNDO", "#ca8a04", "#a16207", false);
            undoBtn.setOnAction(e -> handleToggleComplete());
            actionButtonsBox.getChildren().add(undoBtn);
        }

        Button editBtn = createActionButton("EDIT", "#2563eb", "#1d4ed8", false);
        editBtn.setOnAction(e -> handleEdit(item));
        actionButtonsBox.getChildren().add(editBtn);

        Button delBtn = createActionButton("DEL", "#dc2626", "#b91c1c", true);
        delBtn.setOnAction(e -> handleDelete(item));
        actionButtonsBox.getChildren().add(delBtn);

        tailHeaderBox.setOnMouseClicked(e -> handleInfo(item));
    }

    private Button createActionButton(String text, String bgColor, String hoverColor, boolean endItem) {
        Button btn = new Button(text);
        btn.setMinHeight(48);
        btn.setMinWidth(60);
        String base = String.format(
                "-fx-background-color: %s;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 0;" +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 0 16;" +
                        (endItem ? "-fx-background-radius: 0 4 0 0;" : ""),
                bgColor
        );

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(bgColor, hoverColor)));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    // ─── Handlers ────────────────────────────────────────────────────────────────

//    private void handleInfo() {
//        try {
//            FXMLLoader loader = new FXMLLoader(
//                    getClass().getResource("/components/ItemInteraction/item-info-modal.fxml")
//            );
//            System.out.println("loader: " + loader + getClass().getResource("/components/ItemInteraction/item-info-modal.fxml"));
//            loader.setClassLoader(getClass().getClassLoader());
//            Parent root = loader.load();
//            ItemInfoController controller = loader.getController();
//
//            controller.setItem(item);
//            controller.populate();
//
//            Stage stage = new Stage();
//            stage.setScene(new Scene(root));
//
//            stage.setTitle("Info: " + item.getTail());
//            stage.initModality(Modality.APPLICATION_MODAL);
//
//            stage.show();
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }


//    private void handleEdit() {
//        try {
//            FXMLLoader loader = new FXMLLoader(
//                    getClass().getResource("/components/ItemInteraction/item-edit-modal.fxml")
//            );
//            loader.setClassLoader(getClass().getClassLoader());
//            Parent root = loader.load();
//            ItemEditController controller = loader.getController();
//
//            controller.setItem(item);
//            controller.populate();
//
//            Stage stage = new Stage();
//            stage.setScene(new Scene(root));
//
//            stage.setTitle("Edit: " + item.getTail());
//            stage.initModality(Modality.APPLICATION_MODAL);
//
//            stage.show();
//
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private void handleEdit(ServiceType serviceType) {
//        try {
//            FXMLLoader loader = new FXMLLoader(
//                    getClass().getResource("/components/ItemInteraction/item-edit-modal.fxml")
//            );
//            loader.setClassLoader(getClass().getClassLoader());
//            Parent root = loader.load();
//            ItemEditController controller = loader.getController();
//
//            controller.setItem(item);
//            controller.setInitialService(serviceType);
//            controller.populate();
//
//            Stage stage = new Stage();
//            stage.setScene(new Scene(root));
//
//            stage.setTitle("Edit: " + item.getTail());
//            stage.initModality(Modality.APPLICATION_MODAL);
//
//            stage.show();
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//
//    private void handleDelete() {
//        try {
//            FXMLLoader loader = new FXMLLoader(
//                    getClass().getResource("/components/ItemInteraction/item-delete-modal.fxml")
//            );
//            loader.setClassLoader(getClass().getClassLoader());
//            Parent root = loader.load();
//            ItemDeleteController controller = loader.getController();
//
//            controller.setItem(item);
//            controller.populate();
//
//            Stage stage = new Stage();
//            stage.setScene(new Scene(root));
//
//            stage.setTitle("Confirm Deletion");
//            stage.initModality(Modality.APPLICATION_MODAL);
//            // stage.initOwner(deleteButton.getScene().getWindow()); // Lock parent window
//            stage.setResizable(false);
//
//            stage.show();
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────
//
//    private Map<ServiceType, Boolean> getActiveServices(ServiceItem item) {
//        Map<ServiceType, Boolean> map = new LinkedHashMap<>();
//        if (item.getFuel() != null)
//            map.put(ServiceType.FUEL, isServiceCompleted(ServiceType.FUEL));
//        if (item.getCatering() != null && !item.getCatering().isEmpty())
//            map.put(ServiceType.CATERING, isServiceCompleted(ServiceType.CATERING));
//        if (item.getGpu() != null)
//            map.put(ServiceType.GPU, isServiceCompleted(ServiceType.GPU));
//        if (item.getLavatory() != null)
//            map.put(ServiceType.LAVATORY, isServiceCompleted(ServiceType.LAVATORY));
//        if (item.getPotableWater() != null)
//            map.put(ServiceType.POTABLE_WATER, isServiceCompleted(ServiceType.POTABLE_WATER));
//        if (item.getWindshieldCleaning() != null)
//            map.put(ServiceType.WINDSHIELD_CLEANING, isServiceCompleted(ServiceType.WINDSHIELD_CLEANING));
//        if (item.getOilService() != null)
//            map.put(ServiceType.OIL_SERVICE, isServiceCompleted(ServiceType.OIL_SERVICE));
//        return map;
//    }
//
//    private boolean isServiceCompleted(ServiceType serviceType) {
//        return switch (serviceType) {
//            case FUEL -> item.getFuel().getCompletedAt() != null;
//            case CATERING -> item.getCatering().stream().allMatch(c -> c.getCompletedAt() != null);
//            case GPU -> item.getGpu().getCompletedAt() != null;
//            case LAVATORY -> item.getLavatory().getCompletedAt() != null;
//            case POTABLE_WATER -> item.getPotableWater().getCompletedAt() != null;
//            case WINDSHIELD_CLEANING -> item.getWindshieldCleaning().getCompletedAt() != null;
//            case OIL_SERVICE -> item.getOilService().getCompletedAt() != null;
//        };
//    }
//
//    private boolean areAllServicesCompleted(ServiceItem item) {
//        Map<ServiceType, Boolean> active = getActiveServices(item);
//        return !active.isEmpty() && active.values().stream().allMatch(Boolean::booleanValue);
//    }

//    private static final DateTimeFormatter FMT =
//            DateTimeFormatter.ofPattern("MMM dd, HH:mm")
//                    .withZone(ZoneId.systemDefault());
}