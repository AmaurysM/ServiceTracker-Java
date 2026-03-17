package com.amaurysdelossantos.ServiceTracker.Controllers.Map;

import com.amaurysdelossantos.ServiceTracker.Controllers.Map.Tab.PlacementPanelController;
import com.amaurysdelossantos.ServiceTracker.Services.ContextMenuService;
import com.amaurysdelossantos.ServiceTracker.Services.DataService;
import com.amaurysdelossantos.ServiceTracker.Services.MapService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceTrackerService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.ServiceView;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import fxmapcontrol.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class MapViewController {

    private static final double PANEL_WIDTH = 260.0;

    private final Map<String, AircraftMarkerController> markers = new HashMap<>();

    @FXML private fxmapcontrol.Map fxMap;
    @FXML private Text   coordText;
    @FXML private Text   itemCountText;
    @FXML private Button backBtn;
    @FXML private TabPane rightTabPane;
    @FXML private Tab    dummyTab;
    @FXML private Tab    tabFilters;
    @FXML private Tab    tabDrawing;
    @FXML private Tab    tabPlacement;
    @FXML private VBox   tabContent;

    @Autowired private ServiceTrackerService serviceTrackerService;
    @Autowired private ServiceItemService    serviceItemService;
    @Autowired private DataService           dataService;
    @Autowired private MongoTemplate         mongoTemplate;
    @Autowired private ApplicationContext    applicationContext;
    @Autowired private MapService            mapService;
    @Autowired private ContextMenuService    contextMenuService;  // ← injected here

    private Pane   markerPane;
    private String selectedId = null;

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

        // ── Register the marker pane so ContextMenuService can attach cards ──
        contextMenuService.registerMarkerPane(markerPane);

        PlacementPanelController.installMapDropTarget(fxMap, fxMap, (itemId, lat, lon) -> {
            mapService.getItems().stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst()
                    .ifPresent(item -> {
                        if (item.getMetadata() == null) item.setMetadata(new HashMap<>());
                        Map<String, Object> mp = new HashMap<>();
                        mp.put("x", lat);
                        mp.put("y", lon);
                        mp.put("rotation", 0.0);
                        item.getMetadata().put("mapPosition", mp);
                        Platform.runLater(() -> addOrUpdateMarker(item));
                    });
            persistPosition(itemId, lat, lon);
        });

        fxMap.centerProperty().addListener((o, p, n)    -> relocateAllAsync());
        fxMap.zoomLevelProperty().addListener((o, p, n) -> relocateAllAsync());
        fxMap.widthProperty().addListener((o, p, n)     -> relocateAllAsync());
        fxMap.heightProperty().addListener((o, p, n)    -> relocateAllAsync());

        mapService.getItems().forEach(this::addOrUpdateMarker);
        updateCount();

        mapService.getItems().addListener((ListChangeListener<ServiceItem>) change -> {
            while (change.next()) {
                if (change.wasRemoved())
                    change.getRemoved().forEach(i -> removeMarker(i.getId()));
                if (change.wasAdded() || change.wasUpdated())
                    change.getAddedSubList().forEach(this::addOrUpdateMarker);
            }
            updateCount();
        });

        reload();
        dataService.startChangeStream(this::handleChange);

        mapService.getTimeFilter().addListener((obs, o, n)    -> reloadAndRestart());
        mapService.getServiceFilter().addListener((obs, o, n) -> reloadAndRestart());
        mapService.getSearchText().addListener((obs, o, n)    -> reloadAndRestart());

        mapService.tabOpenProperty().addListener((e, oldVal, newVal) -> {
            if (!newVal) collapsePanel(); else expandPanel();
        });

        // ── Close card + deselect on any click that lands on the map background ──
        // We use an event FILTER on fxMap so it fires even when the tile layer
        // or the transparent markerPane absorbs the click.  Before dismissing we
        // check whether the click target is inside the context-menu card — if it
        // is, the card's own handlers should run undisturbed.
        fxMap.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (contextMenuService.isClickInsideOpenCard(e.getSceneX(), e.getSceneY())) {
                return;   // let the card handle it
            }
            contextMenuService.close();
            if (selectedId != null) {
                AircraftMarkerController prev = markers.get(selectedId);
                if (prev != null) prev.setSelected(false);
                selectedId = null;
            }
            // Do NOT consume — the map still needs to pan / zoom
        });

        backBtn.setOnAction(e -> {
            dataService.stopChangeStream();
            serviceTrackerService.activeViewProperty().set(ServiceView.STANDARD);
        });

        rightTabPane.getSelectionModel().select(dummyTab);
        Platform.runLater(this::attachTabToggleHandlers);
        Platform.runLater(() -> fxMap.requestLayout());
    }

    // ── Data loading ───────────────────────────────────────────────────────

    private void reloadAndRestart() {
        reload();
        dataService.restartChangeStream(this::handleChange);
    }

    private void reload() {
        dataService.reload(
                null,
                mapService.getTimeFilter().get(),
                mapService.getServiceFilter().get(),
                mapService.getSearchText().get(),
                mapService.getItems()::setAll
        );
    }

    // ── Change stream ──────────────────────────────────────────────────────

    private void handleChange(ChangeStreamDocument<Document> change) {
        OperationType op = change.getOperationType();

        if (op == OperationType.DELETE) {
            BsonValue idValue = change.getDocumentKey().get("_id");
            String deletedId  = idValue.getBsonType() == BsonType.OBJECT_ID
                    ? idValue.asObjectId().getValue().toString()
                    : idValue.asString().getValue();
            Platform.runLater(() ->
                    mapService.getItems().removeIf(i -> i.getId().equals(deletedId)));
            return;
        }

        Document doc = change.getFullDocument();
        if (doc == null) return;

        ServiceItem updated = mongoTemplate.getConverter().read(ServiceItem.class, doc);
        Platform.runLater(() -> {
            applyChange(updated, op);
            // Keep the open context-menu card in sync with live data
            contextMenuService.refreshIfOpen(updated.getId(), updated);
        });
    }

    private void applyChange(ServiceItem updated, OperationType op) {
        ObservableList<ServiceItem> items = mapService.getItems();
        int     index   = findIndexById(updated.getId(), items);
        boolean matches = matchesCurrentFilter(updated);

        switch (op) {
            case INSERT -> { if (matches) items.add(updated); }
            case UPDATE, REPLACE -> {
                if (index != -1 && matches) items.set(index, updated);
                else if (index != -1)       items.remove(index);
                else if (matches)           items.add(updated);
            }
        }
    }

    private int findIndexById(String id, ObservableList<ServiceItem> items) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).getId().equals(id)) return i;
        return -1;
    }

    private boolean matchesCurrentFilter(ServiceItem item) {
        if (item.getDeletedAt() != null) return false;

        ServiceFilter service = mapService.getServiceFilter().get();
        if (service != null && service != ServiceFilter.ALL) {
            boolean matches = switch (service) {
                case FUEL                -> item.getFuel() != null;
                case GPU                 -> item.getGpu() != null;
                case LAVATORY            -> item.getLavatory() != null;
                case POTABLE_WATER       -> item.getPotableWater() != null;
                case CATERING            -> item.getCatering() != null && !item.getCatering().isEmpty();
                case WINDSHIELD_CLEANING -> item.getWindshieldCleaning() != null;
                case OIL_SERVICE         -> item.getOilService() != null;
                default                  -> true;
            };
            if (!matches) return false;
        }

        TimeFilter time = mapService.getTimeFilter().get();
        if (time != null && item.getCreatedAt() != null) {
            ZonedDateTime now   = ZonedDateTime.now();
            Instant       start = switch (time) {
                case TODAY -> now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
                case WEEK  -> now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay(now.getZone()).toInstant();
                case MONTH -> now.toLocalDate().withDayOfMonth(1).atStartOfDay(now.getZone()).toInstant();
            };
            if (item.getCreatedAt().isBefore(start)) return false;
            if (time == TimeFilter.TODAY) {
                Instant end = now.toLocalDate().atStartOfDay(now.getZone()).plusDays(1).toInstant();
                if (!item.getCreatedAt().isBefore(end)) return false;
            }
        }

        String search = mapService.getSearchText().get();
        if (search != null && !search.isBlank()) {
            String  lower     = search.toLowerCase();
            boolean tailMatch = item.getTail()        != null && item.getTail().toLowerCase().contains(lower);
            boolean descMatch = item.getDescription() != null && item.getDescription().toLowerCase().contains(lower);
            if (!tailMatch && !descMatch) return false;
        }

        return true;
    }

    // ── Marker management ──────────────────────────────────────────────────

    private void relocateAllAsync() {
        markers.values().forEach(AircraftMarkerController::relocateAsync);
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

        // Pass the ContextMenuService — no mapPane or callback wiring needed
        AircraftMarkerController ctrl =
                AircraftMarkerController.create(item, fxMap, contextMenuService);

        ctrl.setOnSelect(c -> {
            // Close the open card if a different marker is selected
            if (selectedId != null && !selectedId.equals(c.getId()))
                contextMenuService.close();

            if (selectedId != null && !selectedId.equals(c.getId())) {
                AircraftMarkerController prev = markers.get(selectedId);
                if (prev != null) prev.setSelected(false);
            }
            selectedId = c.getId();
            c.setSelected(true);
        });

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

    private void updateCount() {
        if (itemCountText != null)
            itemCountText.setText(String.valueOf(markers.size()));
    }

    // ── Persistence ────────────────────────────────────────────────────────

    private void persistPosition(String itemId, double x, double y) {
        mapService.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> {
                    if (item.getMetadata() == null) item.setMetadata(new HashMap<>());
                    Map<String, Object> mp = (Map<String, Object>)
                            item.getMetadata().computeIfAbsent("mapPosition", k -> new HashMap<>());
                    mp.put("x", x);
                    mp.put("y", y);
                });

        AircraftMarkerController ctrl = markers.get(itemId);
        if (ctrl != null) {
            Map<String, Object> mp = AircraftMarkerController.mapPosition(ctrl.getItem());
            if (mp != null) { mp.put("x", x); mp.put("y", y); }
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
        mapService.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> {
                    if (item.getMetadata() != null) {
                        Map<String, Object> mp = (Map<String, Object>)
                                item.getMetadata().get("mapPosition");
                        if (mp != null) mp.put("rotation", rotation);
                    }
                });

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

    // ── Panel / tab helpers ────────────────────────────────────────────────

    private void attachTabToggleHandlers() {
        rightTabPane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> {
                    if (newTab == null || newTab == dummyTab) collapsePanel();
                    else expandPanel();
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
            Tab selected = rightTabPane.getSelectionModel().getSelectedItem();
            String fxml  = null;
            if      (selected.equals(tabFilters))   fxml = "/components/map/tab/filters-panel.fxml";
            else if (selected.equals(tabDrawing))   fxml = "/components/map/tab/drawing-panel.fxml";
            else if (selected.equals(tabPlacement)) fxml = "/components/map/tab/placement-panel.fxml";

            tabContent.getChildren().clear();
            if (fxml != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                loader.setControllerFactory(applicationContext::getBean);
                tabContent.getChildren().add(loader.load());
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

    public void flyTo(double lat, double lon, double zoom) {
        Platform.runLater(() -> {
            fxMap.setCenter(new Location(lat, lon));
            fxMap.setZoomLevel(zoom);
        });
    }

    public void invalidateSize() {
        Platform.runLater(() -> fxMap.requestLayout());
    }
}