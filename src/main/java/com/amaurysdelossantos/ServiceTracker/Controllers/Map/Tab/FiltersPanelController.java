package com.amaurysdelossantos.ServiceTracker.Controllers.Map.Tab;

import com.amaurysdelossantos.ServiceTracker.Controllers.Map.Tab.Item.FilterItemController;
import com.amaurysdelossantos.ServiceTracker.Helper.Lib;
import com.amaurysdelossantos.ServiceTracker.Services.MapService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class FiltersPanelController {

    @FXML public Text fixedLabelText;
    @FXML private CheckBox showLabelsCheck;
    @FXML private CheckBox fixedLabelSizeCheck;
    @FXML private Button exitButton;
    @FXML private TextField searchBar;
    @FXML private ComboBox<String> serviceFilter;
    @FXML private ComboBox<TimeFilter> timeFilter;
    @FXML private Button tabAll;
    @FXML private Button tabPlaced;
    @FXML private Button tabNotPlaced;
    @FXML private Label totalCountLabel;
    @FXML private Label onScreenCountLabel;
    @FXML private Label onScreenSeparator;
    @FXML private Label onScreenText;
    @FXML private Text sectionHeading;
    @FXML private VBox itemsContainer;

    @Autowired private MapService mapService;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private ServiceItemService serviceItemService;

    @FXML
    public void initialize() {
        exitButton.setOnAction(e -> mapService.tabOpenProperty().set(false));

        // ── Placement tab buttons — write to MapControlsService ────────────
        tabAll.setOnAction(e -> mapService.getPlacementFilter().set("all"));
        tabPlaced.setOnAction(e -> mapService.getPlacementFilter().set("placed"));
        tabNotPlaced.setOnAction(e -> mapService.getPlacementFilter().set("notPlaced"));

        // ── Service filter combo — write to MapControlsService ─────────────
        serviceFilter.getItems().setAll("ALL", "FUEL", "CATERING", "GPU",
                "LAVATORY", "POTABLE_WATER", "WINDSHIELD_CLEANING", "OIL_SERVICE");
        serviceFilter.setValue(mapService.getServiceFilter().get().name());
        serviceFilter.valueProperty().addListener((obs, o, n) -> {
            try {
                mapService.getServiceFilter().set(ServiceFilter.valueOf(n != null ? n : "ALL"));
            } catch (IllegalArgumentException ignored) {
                mapService.getServiceFilter().set(ServiceFilter.ALL);
            }
        });

        // ── Time filter combo — write to MapControlsService ────────────────
        timeFilter.getItems().setAll(TimeFilter.values());
        timeFilter.setValue(mapService.getTimeFilter().get());
        timeFilter.valueProperty().addListener((obs, o, n) ->
                mapService.getTimeFilter().set(n != null ? n : TimeFilter.TODAY));

        // ── Search — write to MapControlsService ───────────────────────────
        searchBar.setText(mapService.getSearchText().get());
        searchBar.textProperty().addListener((obs, o, n) ->
                mapService.getSearchText().set(n != null ? n : ""));

        // ── React to shared item list changes ──────────────────────────────
        mapService.getItems().addListener(
                (ListChangeListener<ServiceItem>) change -> refreshList());

        // ── React to placement filter changes ─────────────────────────────
        mapService.getPlacementFilter().addListener((obs, o, n) -> {
            updatePlacementTabStyles(n);
            refreshList();
        });

        // ── Labels toggle ──────────────────────────────────────────────────
        showLabelsCheck.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            fixedLabelSizeCheck.setVisible(isSelected);
            fixedLabelSizeCheck.setManaged(isSelected);
        });

        // ── Initial render ─────────────────────────────────────────────────
        updatePlacementTabStyles(mapService.getPlacementFilter().get());
        refreshList();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private void updatePlacementTabStyles(String filter) {
        tabAll.getStyleClass().remove("placement-tab-active");
        tabPlaced.getStyleClass().remove("placement-tab-active");
        tabNotPlaced.getStyleClass().remove("placement-tab-active");

        switch (filter) {
            case "placed"    -> tabPlaced.getStyleClass().add("placement-tab-active");
            case "notPlaced" -> tabNotPlaced.getStyleClass().add("placement-tab-active");
            default          -> tabAll.getStyleClass().add("placement-tab-active");
        }

        sectionHeading.setText(switch (filter) {
            case "placed"    -> "PLACED";
            case "notPlaced" -> "NOT PLACED";
            default          -> "ALL AIRCRAFT";
        });

        boolean showOnScreen = !filter.equals("notPlaced");
        onScreenSeparator.setVisible(showOnScreen);
        onScreenSeparator.setManaged(showOnScreen);
        onScreenCountLabel.setVisible(showOnScreen);
        onScreenCountLabel.setManaged(showOnScreen);
        onScreenText.setVisible(showOnScreen);
        onScreenText.setManaged(showOnScreen);
    }

    private void refreshList() {
        List<ServiceItem> source = mapService.getItems();

        List<ServiceItem> filtered = source.stream()
                .filter(this::matchesPlacement)
                .filter(this::matchesServiceFilter)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());

        long placedCount    = source.stream().filter(this::hasMapPosition).count();
        long notPlacedCount = source.stream().filter(i -> !hasMapPosition(i)).count();
        long onScreenCount  = filtered.stream().filter(this::hasMapPosition).count();

        totalCountLabel.setText(String.valueOf(filtered.size()));
        onScreenCountLabel.setText(String.valueOf(onScreenCount));
        tabAll.setText("All (" + source.size() + ")");
        tabPlaced.setText("Placed (" + placedCount + ")");
        tabNotPlaced.setText("Not Placed (" + notPlacedCount + ")");

        itemsContainer.getChildren().clear();
        for (ServiceItem item : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/components/map/tab/item/filter-item.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                VBox node = loader.load();
                FilterItemController ctrl = loader.getController();
                ctrl.setItem(item);
                itemsContainer.getChildren().add(node);
            } catch (Exception ex) {
                System.err.println("FiltersPanelController — failed to load FilterItem: " + ex.getMessage());
            }
        }
    }

    private boolean matchesPlacement(ServiceItem item) {
        return switch (mapService.getPlacementFilter().get()) {
            case "placed"    -> hasMapPosition(item);
            case "notPlaced" -> !hasMapPosition(item);
            default          -> true;
        };
    }

    private boolean matchesServiceFilter(ServiceItem item) {
        String raw = mapService.getServiceFilter().get().name();
        if (raw.equals("ALL")) return true;
        ServiceType type;
        try { type = ServiceType.valueOf(raw); }
        catch (IllegalArgumentException e) { return true; }
        Map<ServiceType, Boolean> activeServices = Lib.getActiveServices(item);
        return activeServices.containsKey(type) && !activeServices.get(type);
    }

    private boolean matchesSearch(ServiceItem item) {
        String search = mapService.getSearchText().get();
        if (search == null || search.isBlank()) return true;
        return item.getTail() != null && item.getTail().toLowerCase().contains(search.toLowerCase());
    }

    private boolean hasMapPosition(ServiceItem item) {
        if (item.getMetadata() == null) return false;
        return item.getMetadata().containsKey("lat") && item.getMetadata().containsKey("lng");
    }
}