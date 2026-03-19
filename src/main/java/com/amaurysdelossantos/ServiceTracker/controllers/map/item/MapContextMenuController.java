package com.amaurysdelossantos.ServiceTracker.controllers.map.item;

import com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler;
import com.amaurysdelossantos.ServiceTracker.Services.MapContextMenuService;
import com.amaurysdelossantos.ServiceTracker.Services.PendingMapPositionService;
import fxmapcontrol.Location;
import fxmapcontrol.Map;
import fxmapcontrol.MapTileLayer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class MapContextMenuController {

    @FXML private VBox   root;
    @FXML private HBox   header;
    @FXML private Label  coordLabel;
    @FXML private Button closeBtn;

    @FXML private HBox addBtn;

    @FXML private HBox  flyToBtn;
    @FXML private HBox  zoomInBtn;
    @FXML private HBox  zoomOutBtn;
    @FXML private HBox  resetNorthBtn;
    @FXML private Label zoomLevelLabel;

    @FXML private Button zoomStreetBtn;
    @FXML private Button zoomCityBtn;
    @FXML private Button zoomRegionBtn;
    @FXML private Button zoomCountryBtn;

    @FXML private Button styleStreetsBtn;
    @FXML private Button styleSatelliteBtn;
    @FXML private Button styleTopoBtn;

    @FXML private Label footerZoomLabel;
    @FXML private Label footerCenterLabel;

    private double lat, lon;
    private Pane   mapPane;
    private Map    fxMap;

    private static final String TILE_STREETS   =
            "https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=7p902bvM8BZds2RvNgE0";
    private static final String TILE_SATELLITE =
            "https://api.maptiler.com/maps/hybrid/{z}/{x}/{y}.jpg?key=7p902bvM8BZds2RvNgE0";
    private static final String TILE_TOPO      =
            "https://api.maptiler.com/maps/topo/{z}/{x}/{y}.png?key=7p902bvM8BZds2RvNgE0";

    private PendingMapPositionService pendingMapPosition;
    private MapContextMenuService     mapContextMenuService;
    private String currentTileUrl = "";

    private double dragAnchorX, dragAnchorY;
    private double dragNodeX,   dragNodeY;

    public static MapContextMenuController create(double lat, double lon,
                                                  double clickX, double clickY,
                                                  Pane mapPane, Map fxMap,
                                                  PendingMapPositionService pendingMapPosition,
                                                  String currentTileUrl,
                                                  MapContextMenuService mapContextMenuService) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MapContextMenuController.class.getResource(
                            "/components/map/item/map-context-menu.fxml"));
            loader.setControllerFactory(clazz -> new MapContextMenuController());
            loader.load();

            MapContextMenuController ctrl = loader.getController();
            ctrl.pendingMapPosition    = pendingMapPosition;
            ctrl.currentTileUrl        = currentTileUrl;
            ctrl.mapContextMenuService = mapContextMenuService;
            ctrl.init(lat, lon, clickX, clickY, mapPane, fxMap);
            return ctrl;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load map-context-menu.fxml", e);
        }
    }

    public VBox getNode() { return root; }

    @FXML public void initialize() {}

    private void init(double lat, double lon,
                      double clickX, double clickY,
                      Pane mapPane, Map fxMap) {
        this.lat = lat; this.lon = lon;
        this.mapPane = mapPane; this.fxMap = fxMap;
        populate();
        detectActiveStyle();
        wireButtons();
        wireDrag();
        positionAt(clickX, clickY);
    }

    private void populate() {
        coordLabel.setText(String.format("%.5f,  %.5f", lat, lon));

        double zoom = fxMap.getZoomLevel();
        zoomLevelLabel.setText(String.format("%.0f → %.0f", zoom, zoom + 2));

        footerZoomLabel.setText(String.format("Z %.0f", zoom));
        Location c = fxMap.getCenter();
        footerCenterLabel.setText(String.format("%.3f, %.3f", c.getLatitude(), c.getLongitude()));
    }

    private void detectActiveStyle() {
        Button active;
        if (currentTileUrl.contains("hybrid")) {
            active = styleSatelliteBtn;
        } else if (currentTileUrl.contains("topo")) {
            active = styleTopoBtn;
        } else {
            active = styleStreetsBtn;
        }
        setActiveStyleBtn(active);
    }


    private void wireButtons() {

        addBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss();
            pendingMapPosition.set(lat, lon);
            WindowHandler.handleAdd();
            e.consume();
        });

        flyToBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss();
            fxMap.setCenter(new Location(lat, lon));
            e.consume();
        });

        zoomInBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss();
            fxMap.setCenter(new Location(lat, lon));
            fxMap.setZoomLevel(Math.min(fxMap.getZoomLevel() + 2, 20));
            e.consume();
        });

        zoomOutBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss();
            fxMap.setZoomLevel(Math.max(fxMap.getZoomLevel() - 1, 1));
            e.consume();
        });

        resetNorthBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss();
            fxMap.setHeading(0);
            e.consume();
        });

        zoomStreetBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss(); fxMap.setCenter(new Location(lat, lon)); fxMap.setZoomLevel(17); e.consume();
        });
        zoomCityBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss(); fxMap.setCenter(new Location(lat, lon)); fxMap.setZoomLevel(13); e.consume();
        });
        zoomRegionBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss(); fxMap.setCenter(new Location(lat, lon)); fxMap.setZoomLevel(9); e.consume();
        });
        zoomCountryBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            dismiss(); fxMap.setCenter(new Location(lat, lon)); fxMap.setZoomLevel(5); e.consume();
        });

        styleStreetsBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            switchTileLayer(TILE_STREETS); setActiveStyleBtn(styleStreetsBtn); e.consume();
        });
        styleSatelliteBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            switchTileLayer(TILE_SATELLITE); setActiveStyleBtn(styleSatelliteBtn); e.consume();
        });
        styleTopoBtn.setOnMouseReleased(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            switchTileLayer(TILE_TOPO); setActiveStyleBtn(styleTopoBtn); e.consume();
        });

        closeBtn.setOnAction(e -> dismiss());

        scaleOnHover(addBtn);
        scaleOnHover(flyToBtn);
        scaleOnHover(zoomInBtn);
        scaleOnHover(zoomOutBtn);
        scaleOnHover(resetNorthBtn);

        root.setOnMousePressed(e -> e.consume());
        root.setOnScroll(e -> e.consume());
    }


    private void switchTileLayer(String urlTemplate) {
        fxMap.getChildren().removeIf(n -> n instanceof MapTileLayer);
        fxMap.getChildren().add(0, new MapTileLayer("custom", urlTemplate));
        if (mapContextMenuService != null) {
            mapContextMenuService.notifyTileUrlChanged(urlTemplate);
        }
    }

    private void setActiveStyleBtn(Button active) {
        for (Button b : new Button[]{styleStreetsBtn, styleSatelliteBtn, styleTopoBtn}) {
            b.getStyleClass().remove("mcm-chip-active");
        }
        active.getStyleClass().add("mcm-chip-active");
    }

    private void wireDrag() {
        header.setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;
            dragAnchorX = e.getSceneX(); dragAnchorY = e.getSceneY();
            dragNodeX = root.getLayoutX(); dragNodeY = root.getLayoutY();
            header.setCursor(Cursor.MOVE);
            e.consume();
        });
        header.setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown()) return;
            double menuW = root.getWidth()  > 0 ? root.getWidth()  : root.prefWidth(-1);
            double menuH = root.getHeight() > 0 ? root.getHeight() : root.prefHeight(-1);
            root.setLayoutX(Math.max(20, Math.min(dragNodeX + e.getSceneX() - dragAnchorX, mapPane.getWidth()  - menuW - 20)));
            root.setLayoutY(Math.max(20, Math.min(dragNodeY + e.getSceneY() - dragAnchorY, mapPane.getHeight() - menuH - 20)));
            e.consume();
        });
        header.setOnMouseReleased(e -> { header.setCursor(Cursor.DEFAULT); e.consume(); });
    }


    private void positionAt(double sceneClickX, double sceneClickY) {
        Platform.runLater(() -> {
            Bounds b      = mapPane.localToScene(mapPane.getBoundsInLocal());
            double localX = sceneClickX - b.getMinX();
            double localY = sceneClickY - b.getMinY();
            double menuW  = root.prefWidth(-1);
            double menuH  = root.prefHeight(-1);
            double vpW    = mapPane.getWidth();
            double vpH    = mapPane.getHeight();

            double left = localX + 8;
            double top  = localY - menuH / 2.0;
            if (left + menuW > vpW - 20) left = localX - menuW - 8;
            left = Math.max(20, Math.min(left, vpW - menuW - 20));
            top  = Math.max(20, Math.min(top,  vpH - menuH - 20));

            root.setLayoutX(left);
            root.setLayoutY(top);
        });
    }

    private void dismiss() {
        if (mapPane != null) mapPane.getChildren().remove(root);
    }

    private static void scaleOnHover(Node n) {
        n.setOnMouseEntered(e -> animate(n, 1.02));
        n.setOnMouseExited(e  -> animate(n, 1.00));
    }

    private static void animate(Node n, double target) {
        new Timeline(new KeyFrame(Duration.millis(100),
                new KeyValue(n.scaleXProperty(), target),
                new KeyValue(n.scaleYProperty(), target))).play();
    }
}