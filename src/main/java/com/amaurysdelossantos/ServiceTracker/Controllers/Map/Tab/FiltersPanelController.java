package com.amaurysdelossantos.ServiceTracker.Controllers.Map.Tab;

import com.amaurysdelossantos.ServiceTracker.Controllers.Map.Tab.Item.FilterItemController;
import com.amaurysdelossantos.ServiceTracker.Services.DataService;
import com.amaurysdelossantos.ServiceTracker.Services.MapService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
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
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class FiltersPanelController {


    @FXML
    public Text fixedLabelText;
    // ── FXML ───────────────────────────────────────────────────────────────
    @FXML
    private CheckBox showLabelsCheck;
    @FXML
    private CheckBox fixedLabelSizeCheck;    // ── FXML
    // ───────────────────────────────────────────────────────────────
    @FXML
    private Button exitButton;
    @FXML
    private TextField searchBar;
    @FXML
    private ComboBox<String> serviceFilter;
    @FXML
    private Button tabAll;
    @FXML
    private Button tabPlaced;
    @FXML
    private Button tabNotPlaced;
    @FXML
    private Label totalCountLabel;
    @FXML
    private Label onScreenCountLabel;
    @FXML
    private Label onScreenSeparator;
    @FXML
    private Label onScreenText;
    @FXML
    private Text sectionHeading;
    @FXML
    private VBox itemsContainer;


    // ── Spring ─────────────────────────────────────────────────────────────
    @Autowired
    private MapService mapService;
    @Autowired
    private DataService dataService;
    @Autowired
    private ApplicationContext applicationContext;

    // ── State ──────────────────────────────────────────────────────────────
    private String activePlacementFilter = "all";
    private String activeServiceFilter = "ALL";
    private String activeSearchText = "";
    private List<ServiceItem> allItems = List.of();

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        exitButton.setOnAction(e -> mapService.tabOpenProperty().set(false));

        // Placement tab buttons
        tabAll.setOnAction(e -> setPlacementFilter("all"));
        tabPlaced.setOnAction(e -> setPlacementFilter("placed"));
        tabNotPlaced.setOnAction(e -> setPlacementFilter("notPlaced"));
        setPlacementFilter("all"); // set initial active style

        // Service filter combo
        serviceFilter.getItems().setAll("ALL", "FUEL", "CATERING", "GPU",
                "LAVATORY", "POTABLE_WATER", "WINDSHIELD_CLEANING", "OIL_SERVICE");
        serviceFilter.setValue("ALL");
        serviceFilter.valueProperty().addListener((obs, o, n) -> {
            activeServiceFilter = n != null ? n : "ALL";
            refreshList();
        });

        // Search
        searchBar.textProperty().addListener((obs, o, n) -> {
            activeSearchText = n != null ? n : "";
            refreshList();
        });

        // Load today's items
        dataService.reload(null, TimeFilter.TODAY, ServiceFilter.ALL, "",
                items -> {
                    allItems = items;
                    refreshList();
                });

        // Labels / Fixed-size toggle
        showLabelsCheck.selectedProperty().addListener((obs, oldVal, isSelected) -> {
            fixedLabelSizeCheck.setVisible(isSelected);
            fixedLabelSizeCheck.setManaged(isSelected);
        });
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void setPlacementFilter(String filter) {
        activePlacementFilter = filter;

        tabAll.getStyleClass().remove("placement-tab-active");
        tabPlaced.getStyleClass().remove("placement-tab-active");
        tabNotPlaced.getStyleClass().remove("placement-tab-active");

        switch (filter) {
            case "placed" -> tabPlaced.getStyleClass().add("placement-tab-active");
            case "notPlaced" -> tabNotPlaced.getStyleClass().add("placement-tab-active");
            default -> tabAll.getStyleClass().add("placement-tab-active");
        }

        sectionHeading.setText(switch (filter) {
            case "placed" -> "PLACED";
            case "notPlaced" -> "NOT PLACED";
            default -> "ALL AIRCRAFT";
        });

        boolean showOnScreen = !filter.equals("notPlaced");
        onScreenSeparator.setVisible(showOnScreen);
        onScreenSeparator.setManaged(showOnScreen);
        onScreenCountLabel.setVisible(showOnScreen);
        onScreenCountLabel.setManaged(showOnScreen);
        onScreenText.setVisible(showOnScreen);
        onScreenText.setManaged(showOnScreen);

        refreshList();
    }

    private void refreshList() {
        List<ServiceItem> filtered = allItems.stream()
                .filter(this::matchesPlacement)
                .filter(this::matchesServiceFilter)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());

        // Count stats
        long placedCount = allItems.stream().filter(i -> hasMapPosition(i)).count();
        long notPlacedCount = allItems.stream().filter(i -> !hasMapPosition(i)).count();
        long onScreenCount = filtered.stream().filter(i -> hasMapPosition(i)).count();

        totalCountLabel.setText(String.valueOf(filtered.size()));
        onScreenCountLabel.setText(String.valueOf(onScreenCount));

        tabAll.setText("All (" + allItems.size() + ")");
        tabPlaced.setText("Placed (" + placedCount + ")");
        tabNotPlaced.setText("Not Placed (" + notPlacedCount + ")");

        // Rebuild item rows
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

    // ── Filter predicates ──────────────────────────────────────────────────

    private boolean matchesPlacement(ServiceItem item) {
        return switch (activePlacementFilter) {
            case "placed" -> hasMapPosition(item);
            case "notPlaced" -> !hasMapPosition(item);
            default -> true;
        };
    }

    private boolean matchesServiceFilter(ServiceItem item) {
        return switch (activeServiceFilter) {
            case "FUEL" -> item.getFuel() != null;
            case "CATERING" -> item.getCatering() != null && !item.getCatering().isEmpty();
            case "GPU" -> item.getGpu() != null;
            case "LAVATORY" -> item.getLavatory() != null;
            case "POTABLE_WATER" -> item.getPotableWater() != null;
            case "WINDSHIELD_CLEANING" -> item.getWindshieldCleaning() != null;
            case "OIL_SERVICE" -> item.getOilService() != null;
            default -> true; // ALL
        };
    }

    private boolean matchesSearch(ServiceItem item) {
        if (activeSearchText.isBlank()) return true;
        String lower = activeSearchText.toLowerCase();
        return item.getTail() != null && item.getTail().toLowerCase().contains(lower);
    }

    private boolean hasMapPosition(ServiceItem item) {
        if (item.getMetadata() == null) return false;
        return item.getMetadata().containsKey("lat") && item.getMetadata().containsKey("lng");
    }
}