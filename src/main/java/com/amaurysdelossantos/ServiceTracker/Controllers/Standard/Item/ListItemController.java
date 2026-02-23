package com.amaurysdelossantos.ServiceTracker.Controllers.Standard.Item;

import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.Map;

import static com.amaurysdelossantos.ServiceTracker.Helper.Lib.*;
import static com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler.*;

@Component
@Scope("prototype")
public class ListItemController {

    @FXML
    public HBox listRow;
    @FXML
    public Region accentBar;
    @FXML
    public Label tailLabel;
    @FXML
    public Label arrivalLabel;
    @FXML
    public Label departureLabel;
    @FXML
    public HBox serviceIconsBox;
    @FXML
    public HBox actionButtonsBox;
    @FXML
    public HBox tailHeaderBox;

    @Autowired
    public ServiceItemService serviceItemService;

    public ServiceItem item;

    public void setItem(ServiceItem item) {
        this.item = item;
        populateCard();
    }

    public void populateCard() {
        tailLabel.setText(item.getTail());

        arrivalLabel.setText("");
        departureLabel.setText("");

        if (item.getArrival() != null)
            arrivalLabel.setText("↓ " + FMT.format(item.getArrival()));

        if (item.getDeparture() != null)
            departureLabel.setText("↑ " + FMT.format(item.getDeparture()));

        String headerColor = item.getCompletedAt() != null ? "#166534" : "#0f172a";
        accentBar.setStyle(
                "-fx-background-color: " + headerColor + "; "
        );

        renderServices();
        renderActions();

    }

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
            cell.setMinHeight(40);
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
                handleEdit(serviceType, item);
            });

            attachButtonAnimation(cell);

            serviceIconsBox.getChildren().add(cell);
        }
    }

    private void renderActions() {
        actionButtonsBox.getChildren().clear();

        boolean allCompleted = areAllServicesCompleted(item);

        if (allCompleted && item.getCompletedAt() == null) {
            Button doneBtn = createActionButton("DONE", "#16a34a", "#15803d");
            doneBtn.setOnAction(e -> {
                setLoadingState(doneBtn, "...");
                handleToggleComplete(item, () -> {
                    doneBtn.setDisable(false);
                    // change stream will call populateCard() automatically
                });
            });
            actionButtonsBox.getChildren().add(doneBtn);
        }

        if (item.getCompletedAt() != null) {
            Button undoBtn = createActionButton("UNDO", "#ca8a04", "#a16207");
            undoBtn.setOnAction(e -> {
                setLoadingState(undoBtn, "...");
                handleToggleComplete(item, () -> {
                    undoBtn.setDisable(false);
                });
            });
            actionButtonsBox.getChildren().add(undoBtn);
        }

        Button editBtn = createActionButton("EDIT", "#2563eb", "#1d4ed8");
        editBtn.setOnAction(e -> handleEdit(item));
        actionButtonsBox.getChildren().add(editBtn);

        Button delBtn = createActionButton("DEL", "#dc2626", "#b91c1c");
        delBtn.setOnAction(e -> handleDelete(item));
        actionButtonsBox.getChildren().add(delBtn);

        tailHeaderBox.setOnMouseClicked(e -> handleInfo(item));
    }

    private void setLoadingState(Button btn, String loadingText) {
        btn.setText(loadingText);
        btn.setDisable(true);
        btn.setOpacity(0.6);
    }

    private Button createActionButton(String text, String bgColor, String hoverColor) {
        Button btn = new Button(text);
        btn.setMinHeight(39);
        btn.setMinWidth(60);
        String base = String.format(
                "-fx-background-color: %s;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 0;" +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 0 16;",
                bgColor
        );

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(bgColor, hoverColor)));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

}
