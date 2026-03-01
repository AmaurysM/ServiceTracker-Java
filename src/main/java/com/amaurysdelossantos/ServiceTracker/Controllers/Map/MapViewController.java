package com.amaurysdelossantos.ServiceTracker.Controllers.Map;

import com.amaurysdelossantos.ServiceTracker.Services.MapService;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import fxmapcontrol.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class MapViewController {

    private static final double PANEL_WIDTH = 260.0;
    private final Map<String, AircraftMarkerController> markers = new HashMap<>();
    @FXML
    private fxmapcontrol.Map fxMap;
    @FXML
    private Text coordText;
    @FXML
    private Text itemCountText;
    @Autowired
    private StandardControlsService standardControlsService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private MapService mapService;
    private Pane markerPane;
    private String selectedId = null;
    @FXML
    private TabPane rightTabPane;
    @FXML
    private Tab dummyTab;
    @FXML
    private Tab tabFilters;
    @FXML
    private Tab tabDrawing;
    @FXML
    private Tab tabPlacement;
    @FXML
    private VBox tabContent;

    @FXML
    public void initialize() {
        TileImageLoader.setCache(new ImageFileCache());
        fxMap.setProjection(new WebMercatorProjection());

        MapTileLayer tileLayer = new MapTileLayer(
                "MapTiler",
                "https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=7p902bvM8BZds2RvNgE0"
        );
        fxMap.getChildren().add(0, tileLayer);

        markerPane = new Pane();
        markerPane.setPickOnBounds(false);
        markerPane.prefWidthProperty().bind(fxMap.widthProperty());
        markerPane.prefHeightProperty().bind(fxMap.heightProperty());
        fxMap.getChildren().add(markerPane);

        fxMap.centerProperty().addListener((o, p, n) -> relocateAll());
        fxMap.zoomLevelProperty().addListener((o, p, n) -> relocateAll());
        fxMap.widthProperty().addListener((o, p, n) -> relocateAll());
        fxMap.heightProperty().addListener((o, p, n) -> relocateAll());

        standardControlsService.getItems().forEach(this::addOrUpdateMarker);
        updateCount();

        standardControlsService.getItems().addListener((ListChangeListener<ServiceItem>) change -> {
            while (change.next()) {
                if (change.wasRemoved())
                    change.getRemoved().forEach(item -> removeMarker(item.getId()));
                if (change.wasAdded() || change.wasUpdated())
                    change.getAddedSubList().forEach(this::addOrUpdateMarker);
            }
            updateCount();
        });

        mapService.tabOpenProperty().addListener((e, oldVal, newVal) -> {
            if (!newVal) {
                collapsePanel();
            } else {
                expandPanel();
            }
        });

        rightTabPane.getSelectionModel().select(dummyTab);

        Platform.runLater(this::attachTabToggleHandlers);
        Platform.runLater(() -> fxMap.requestLayout());
        System.out.println("Map initialized");
    }

    private void attachTabToggleHandlers() {

        rightTabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> {
                    if (newTab == null || newTab == dummyTab) {
                        collapsePanel();
                    } else {
                        expandPanel();
                    }
                }
        );
    }

    private void expandPanel() {
        mapService.tabOpenProperty().set(true);
        renderTab();
        tabContent.setMaxWidth(PANEL_WIDTH);
    }

    private void renderTab() {
        try {
            if (rightTabPane.getSelectionModel().getSelectedItem().equals(tabFilters)) {
                tabContent.getChildren().clear();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/components/map/tab/filters-panel.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Node child = loader.load();

                tabContent.getChildren().add(child);
                return;
            }

            if (rightTabPane.getSelectionModel().getSelectedItem().equals(tabDrawing)) {
                tabContent.getChildren().clear();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/components/map/tab/drawing-panel.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Node child = loader.load();

                tabContent.getChildren().add(child);
                return;
            }
            if (rightTabPane.getSelectionModel().getSelectedItem().equals(tabPlacement)) {
                tabContent.getChildren().clear();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/components/map/tab/placement-panel.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Node child = loader.load();

                tabContent.getChildren().add(child);
                return;
            }
            if (rightTabPane.getSelectionModel().getSelectedItem().equals(dummyTab)) {
                tabContent.getChildren().clear();
                return;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void collapsePanel() {
        tabContent.setMaxWidth(0);
        tabContent.getChildren().clear();
        rightTabPane.getSelectionModel().select(dummyTab);
    }

    // ── Everything below is unchanged ────────────────────────────────────────

    public void flyTo(double lat, double lon, double zoom) {
        Platform.runLater(() -> {
            fxMap.setCenter(new Location(lat, lon));
            fxMap.setZoomLevel(zoom);
        });
    }

    public void invalidateSize() {
        Platform.runLater(() -> fxMap.requestLayout());
    }

    private void addOrUpdateMarker(ServiceItem item) {
        if (!AircraftMarkerController.hasMapPosition(item)) {
            removeMarker(item.getId());
            return;
        }

        AircraftMarkerController existing = markers.get(item.getId());
        if (existing != null) {
            existing.refresh(item);
            return;
        }

        AircraftMarkerController ctrl = AircraftMarkerController.create(item, fxMap);

        ctrl.setOnSelect(c -> {
            if (selectedId != null && !selectedId.equals(c.getId())) {
                AircraftMarkerController prev = markers.get(selectedId);
                if (prev != null) prev.setSelected(false);
            }
            selectedId = c.getId();
            c.setSelected(true);
        });

        ctrl.setOnContextMenu(c -> System.out.println("Context menu → " + c.getId()));

        ctrl.setOnPositionUpdate(this::persistPosition);
        ctrl.setOnRotationUpdate(this::persistRotation);

        markers.put(item.getId(), ctrl);
        markerPane.getChildren().add(ctrl.getNode());
        ctrl.relocate();
    }

    private void removeMarker(String itemId) {
        AircraftMarkerController ctrl = markers.remove(itemId);
        if (ctrl != null) {
            markerPane.getChildren().remove(ctrl.getNode());
            if (itemId.equals(selectedId)) selectedId = null;
        }
    }

    private void relocateAll() {
        markers.values().forEach(AircraftMarkerController::relocate);
    }

    private void updateCount() {
        if (itemCountText != null)
            itemCountText.setText(String.valueOf(markers.size()));
    }

    private void persistPosition(String itemId, double x, double y) {
        AircraftMarkerController ctrl = markers.get(itemId);
        if (ctrl != null) {
            Map<String, Object> mp = AircraftMarkerController.mapPosition(ctrl.getItem());
            if (mp != null) {
                mp.put("x", x);
                mp.put("y", y);
            }
        }
        Thread t = new Thread(() ->
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(itemId)),
                        new Update()
                                .set("metadata.mapPosition.x", x)
                                .set("metadata.mapPosition.y", y),
                        ServiceItem.class));
        t.setDaemon(true);
        t.start();
    }

    private void persistRotation(String itemId, double rotation) {
        AircraftMarkerController ctrl = markers.get(itemId);
        if (ctrl != null) {
            Map<String, Object> mp = AircraftMarkerController.mapPosition(ctrl.getItem());
            if (mp != null) mp.put("rotation", rotation);
        }
        Thread t = new Thread(() ->
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(itemId)),
                        new Update().set("metadata.mapPosition.rotation", rotation),
                        ServiceItem.class));
        t.setDaemon(true);
        t.start();
    }
}