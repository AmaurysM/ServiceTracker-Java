package com.amaurysdelossantos.ServiceTracker.controllers.map.Tab.Item;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

import static com.amaurysdelossantos.ServiceTracker.Helper.Lib.*;

@Component
@Scope("prototype")
public class FilterItemController {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault());
    @FXML
    private HBox rowHeader;
    @FXML
    private Label tailLabel;
    @FXML
    private Label npBadge;
    @FXML
    private HBox serviceDotsBox;
    @FXML
    private Label expandArrow;
    @FXML
    private VBox detailPane;
    @FXML
    private HBox timesBox;
    @FXML
    private Label arrivalLabel;
    @FXML
    private Label departureLabel;
    @FXML
    private HBox serviceChipsBox;
    @FXML
    private Label noteLabel;
    @FXML
    private Label completedBadge;
    private boolean expanded = false;

    @FXML
    private void onToggleExpand() {
        expanded = !expanded;
        detailPane.setVisible(expanded);
        detailPane.setManaged(expanded);
        expandArrow.setText(expanded ? "▼" : "▶");
    }

    public void setItem(ServiceItem item) {

        if (item == null) return;

        expanded = false;
        detailPane.setVisible(false);
        detailPane.setManaged(false);
        expandArrow.setText("▶");

        boolean placed = hasMapPosition(item);
        boolean completed = areAllServicesCompleted(item);

        Map<ServiceType, Boolean> services =
                getActiveServices(item) != null
                        ? getActiveServices(item)
                        : Collections.emptyMap();

        tailLabel.setText(
                item.getTail() != null && !item.getTail().isBlank()
                        ? item.getTail()
                        : "—"
        );

        rowHeader.getStyleClass().removeAll(
                "border-slate",
                "border-orange",
                "border-green"
        );

        rowHeader.setStyle("");

        if (completed) {
            rowHeader.getStyleClass().add("border-green");
            rowHeader.setStyle("-fx-background-color: rgba(240,253,244,0.3);");
        } else if (!placed) {
            rowHeader.getStyleClass().add("border-orange");
            rowHeader.setStyle("-fx-background-color: rgba(255,247,237,0.3);");
        } else {
            rowHeader.getStyleClass().add("border-slate");
        }

        npBadge.setVisible(!placed);
        npBadge.setManaged(!placed);

        serviceDotsBox.getChildren().clear();
        for (Map.Entry<ServiceType, Boolean> entry : services.entrySet()) {
            Rectangle dot = new Rectangle(6, 6);
            String color = colorForService(entry.getKey());
            dot.setStyle("-fx-fill:" + color + ";" +
                    "-fx-opacity:" + (entry.getValue() ? "0.35" : "1.0") + ";");
            serviceDotsBox.getChildren().add(dot);
        }

        boolean hasArrival = item.getArrival() != null;
        boolean hasDeparture = item.getDeparture() != null;

        timesBox.setVisible(hasArrival || hasDeparture);
        timesBox.setManaged(hasArrival || hasDeparture);

        arrivalLabel.setText(
                hasArrival ? TIME_FMT.format(item.getArrival()) : "—"
        );

        departureLabel.setText(
                hasDeparture ? TIME_FMT.format(item.getDeparture()) : "—"
        );

        serviceChipsBox.getChildren().clear();

        for (Map.Entry<ServiceType, Boolean> entry : services.entrySet()) {

            String color = colorForService(entry.getKey());
            boolean done = entry.getValue();

            Label chip = new Label(labelForService(entry.getKey()));

            chip.setStyle(
                    "-fx-background-color:" + color + "33;" +
                            "-fx-text-fill:" + color + ";" +
                            "-fx-font-size:9px;" +
                            "-fx-font-weight:bold;" +
                            "-fx-padding:2 5 2 5;" +
                            (done ? "-fx-strikethrough:true;-fx-opacity:0.45;" : "")
            );

            serviceChipsBox.getChildren().add(chip);
        }

        String note = item.getDescription();
        boolean hasNote = note != null && !note.isBlank();

        noteLabel.setText(hasNote ? note : "");
        noteLabel.setVisible(hasNote);
        noteLabel.setManaged(hasNote);

        completedBadge.setVisible(completed);
        completedBadge.setManaged(completed);
    }

}