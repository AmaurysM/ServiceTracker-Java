package com.amaurysdelossantos.ServiceTracker.controllers.map.Tab;

import com.amaurysdelossantos.ServiceTracker.Services.MapService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;  // ← changed
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Scope("prototype")
public class PlacementPanelController {

    static final DataFormat AIRCRAFT_ID_FORMAT =
            new DataFormat("application/x-aircraft-id");

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());

    @FXML private Button closeBtn;
    @FXML private VBox emptyState;
    @FXML private ListView<ServiceItem> aircraftList;

    @Autowired private MapService mapService;
    @Autowired private ServiceItemService serviceItemService;

    public void initialize() {
        closeBtn.setOnAction(e -> mapService.tabOpenProperty().set(false));

        aircraftList.setCellFactory(lv -> new AircraftCell());
        aircraftList.setPlaceholder(new Label(""));

        refreshList();

        mapService.getItems().addListener(
                (ListChangeListener<ServiceItem>) c -> Platform.runLater(this::refreshList));
    }

    private void refreshList() {
        List<ServiceItem> unplaced = mapService.getItems().stream()  // ← changed
                .filter(item -> !hasMapPosition(item))
                .sorted(Comparator.comparing(ServiceItem::getTail, String.CASE_INSENSITIVE_ORDER))
                .toList();

        aircraftList.getItems().setAll(unplaced);
        emptyState.setVisible(unplaced.isEmpty());
        emptyState.setManaged(unplaced.isEmpty());
        aircraftList.setVisible(!unplaced.isEmpty());
        aircraftList.setManaged(!unplaced.isEmpty());
    }

    static boolean hasMapPosition(ServiceItem item) {
        if (item.getMetadata() == null) return false;
        Object mp = item.getMetadata().get("mapPosition");
        if (!(mp instanceof Map<?, ?> pos)) return false;
        return pos.containsKey("x") && pos.containsKey("y");
    }


    public static void installMapDropTarget(Node mapPane,
                                            fxmapcontrol.Map fxMap,
                                            TriConsumer onDrop) {
        mapPane.setOnDragOver(e -> {
            if (e.getDragboard().hasContent(AIRCRAFT_ID_FORMAT)) {
                e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
                return;
            }
            e.consume();
        });

        mapPane.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(AIRCRAFT_ID_FORMAT)) {
                String itemId = (String) db.getContent(AIRCRAFT_ID_FORMAT);
                double[] latLon = pixelToLatLon(
                        fxMap, e.getX(), e.getY(),
                        fxMap.getWidth(), fxMap.getHeight());
                onDrop.accept(itemId, latLon[0], latLon[1]);
                e.setDropCompleted(true);
            }
            e.consume();
        });
    }

    private static double[] pixelToLatLon(fxmapcontrol.Map fxMap,
                                          double dropX, double dropY,
                                          double viewWidth, double viewHeight) {
        fxmapcontrol.Location center = fxMap.getCenter();
        double zoom     = fxMap.getZoomLevel();
        double tileSize = 256.0;
        double mapSize  = tileSize * Math.pow(2.0, zoom);

        double centerPx = lonToWorldPx(center.getLongitude(), mapSize);
        double centerPy = latToWorldPy(center.getLatitude(),  mapSize);

        double dropWorldX = centerPx + (dropX - viewWidth  / 2.0);
        double dropWorldY = centerPy + (dropY - viewHeight / 2.0);

        return new double[]{ worldPyToLat(dropWorldY, mapSize),
                worldPxToLon(dropWorldX, mapSize) };
    }

    private static double lonToWorldPx(double lon, double mapSize) {
        return (lon + 180.0) / 360.0 * mapSize;
    }

    private static double latToWorldPy(double lat, double mapSize) {
        double sinLat = Math.sin(Math.toRadians(lat));
        double log    = Math.log((1.0 + sinLat) / (1.0 - sinLat));
        return (0.5 - log / (4.0 * Math.PI)) * mapSize;
    }

    private static double worldPxToLon(double px, double mapSize) {
        return px / mapSize * 360.0 - 180.0;
    }

    private static double worldPyToLat(double py, double mapSize) {
        double n = Math.PI - 2.0 * Math.PI * py / mapSize;
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    @FunctionalInterface
    public interface TriConsumer {
        void accept(String itemId, double lat, double lon);
    }

    private class AircraftCell extends ListCell<ServiceItem> {

        private final VBox  root          = new VBox();
        private final HBox  header        = new HBox();
        private final Label tailLabel     = new Label();
        private final Label progressLabel = new Label();
        private final HBox  timesRow      = new HBox(6);

        AircraftCell() {
            root.setSpacing(0);
            root.setStyle("-fx-cursor: move;");

            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(4, 10, 4, 10));
            header.setStyle("-fx-background-color: #1E293B;");
            HBox.setHgrow(tailLabel, Priority.ALWAYS);

            tailLabel.setStyle(
                    "-fx-text-fill: white; -fx-font-family: Monospace; " +
                            "-fx-font-size: 11px; -fx-font-weight: bold;");
            progressLabel.setStyle(
                    "-fx-text-fill: rgba(255,255,255,0.6); " +
                            "-fx-font-family: Monospace; -fx-font-size: 9px;");
            header.getChildren().addAll(tailLabel, progressLabel);

            timesRow.setPadding(new Insets(3, 10, 3, 10));
            timesRow.setStyle("-fx-background-color: #F9FAFB;");
            timesRow.setAlignment(Pos.CENTER_LEFT);

            root.getChildren().addAll(header, timesRow);

            root.setOnDragDetected(e -> {
                ServiceItem item = getItem();
                if (item == null) return;

                Dragboard db = root.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.put(AIRCRAFT_ID_FORMAT, item.getId());
                db.setContent(cc);
                db.setDragView(root.snapshot(null, null), e.getX(), e.getY());
                e.consume();
            });
        }

        @Override
        protected void updateItem(ServiceItem item, boolean empty) {
            super.updateItem(item, empty);
            timesRow.getChildren().clear();

            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            tailLabel.setText(item.getTail());

            int total = 0, done = 0;
            if (item.getFuel()               != null) { total++; done++; }
            if (item.getGpu()                != null) { total++; done++; }
            if (item.getLavatory()           != null) { total++; done++; }
            if (item.getPotableWater()       != null) { total++; done++; }
            if (item.getWindshieldCleaning() != null) { total++; done++; }
            if (item.getOilService()         != null) { total++; done++; }
            if (item.getCatering() != null && !item.getCatering().isEmpty()) { total++; done++; }
            progressLabel.setText(done + "/" + total);

            if (item.getArrival() != null) {
                Label arr = new Label("↓" + DT_FMT.format(item.getArrival()));
                arr.setStyle("-fx-font-family: Monospace; -fx-font-size: 9px; -fx-text-fill: #1F2937;");
                timesRow.getChildren().add(arr);
            }
            if (item.getDeparture() != null) {
                Label dep = new Label("↑" + DT_FMT.format(item.getDeparture()));
                dep.setStyle("-fx-font-family: Monospace; -fx-font-size: 9px; -fx-text-fill: #1F2937;");
                timesRow.getChildren().add(dep);
            }
            if (item.getDescription() != null && !item.getDescription().isBlank()) {
                Label desc = new Label(item.getDescription());
                desc.setStyle("-fx-font-size: 9px; -fx-text-fill: #6B7280;");
                desc.setMaxWidth(160);
                timesRow.getChildren().add(desc);
            }

            timesRow.setVisible(!timesRow.getChildren().isEmpty());
            timesRow.setManaged(!timesRow.getChildren().isEmpty());

            setGraphic(root);
            setPadding(Insets.EMPTY);
        }
    }
}