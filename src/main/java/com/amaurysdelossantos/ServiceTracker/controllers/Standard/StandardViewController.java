package com.amaurysdelossantos.ServiceTracker.controllers.Standard;

import com.amaurysdelossantos.ServiceTracker.Services.DataService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceTrackerService;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.ServiceView;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.StandardView;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;

import static com.amaurysdelossantos.ServiceTracker.Helper.Lib.*;

@Component
public class StandardViewController {

    @FXML
    public AnchorPane centerPane;
    @FXML
    public Button onSwitchViewButton;
    @FXML
    private ToggleButton ListViewToggleButton;
    @FXML
    private ToggleButton activeCompletedToggleButton;
    @FXML
    private ToggleButton activeServiceToggleButton;
    @FXML
    private Text ammountOfItemsInViewText;
    @FXML
    private ToggleButton CardViewToggleButton;
    @FXML
    private ComboBox<ServiceFilter> filterServiceChoiceBox;
    @FXML
    private ComboBox<TimeFilter> filterTimeChoiceBox;
    @FXML
    private TextField searchtextField;
    @FXML
    private ToggleGroup statusGroup;
    @FXML
    private ToggleGroup viewGroup;
    @FXML
    private Button clearButton;

    @Autowired
    private StandardControlsService stateService;
    @Autowired
    private DataService dataService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ServiceTrackerService serviceTrackerService;

    private ObservableList<ServiceItem> allItems;

    @FXML
    public void initialize() {

        CardViewToggleButton.setOnAction(e -> onViewToggleChanged(StandardView.CARD));
        ListViewToggleButton.setOnAction(e -> onViewToggleChanged(StandardView.LIST));
        viewGroup.selectToggle(CardViewToggleButton);

        activeCompletedToggleButton.setOnAction(e -> stateService.getActivityFilter().set(ActivityFilter.DONE));
        activeServiceToggleButton.setOnAction(e -> stateService.getActivityFilter().set(ActivityFilter.ACTIVE));
        statusGroup.selectToggle(activeServiceToggleButton);

        filterServiceChoiceBox.getItems().setAll(ServiceFilter.values());
        filterServiceChoiceBox.setValue(ServiceFilter.ALL);
        filterServiceChoiceBox.valueProperty().addListener((obs, oldVal, newVal) ->
                stateService.getServiceFilter().set(newVal));

        filterTimeChoiceBox.getItems().setAll(TimeFilter.values());
        filterTimeChoiceBox.setValue(TimeFilter.TODAY);
        filterTimeChoiceBox.valueProperty().addListener((obs, oldVal, newVal) ->
                stateService.getTimeFilter().set(newVal));

        searchtextField.textProperty().addListener((obs, oldVal, newVal) ->
                stateService.getSearchText().set(newVal));

        clearButton.setOnAction(e -> clearFilters());

        onSwitchViewButton.setOnMouseClicked(e ->
                serviceTrackerService.activeViewProperty().setValue(ServiceView.MAP));

        allItems = stateService.getItems();
        ammountOfItemsInViewText.textProperty().bind(
                Bindings.size(allItems).asString("SHOWING %d ITEMS"));

        attachButtonAnimation(clearButton);
        //attachButtonAnimation(onSwitchViewButton);
        attachToggleAnimation(ListViewToggleButton);
        attachToggleAnimation(activeCompletedToggleButton);
        attachToggleAnimation(activeServiceToggleButton);
        attachToggleAnimation(CardViewToggleButton);
        attachSearchFieldAnimation(searchtextField);

        preventDeselection(statusGroup);
        preventDeselection(viewGroup);

        stateService.getActivityFilter().addListener((obs, o, n) -> reloadAndRestart());
        stateService.getServiceFilter().addListener((obs, o, n) -> reloadAndRestart());
        stateService.getTimeFilter().addListener((obs, o, n) -> reloadAndRestart());
        stateService.getSearchText().addListener((obs, o, n) -> reloadAndRestart());

        // Reset active view so the guard in onViewToggleChanged never blocks
        // the initial render when returning from another view (e.g. Map view).
        stateService.getActiveView().set(null);

        onViewToggleChanged(StandardView.CARD);

        stateService.getActivityFilter().setValue(ActivityFilter.ACTIVE);
        stateService.getTimeFilter().setValue(TimeFilter.TODAY);

        reload();
        dataService.startChangeStream(this::handleChange);
    }

    private void reloadAndRestart() {
        reload();
        dataService.restartChangeStream(this::handleChange);
    }

    private void reload() {
        dataService.reload(
                stateService.getActivityFilter().get(),
                stateService.getTimeFilter().get(),
                stateService.getServiceFilter().get(),
                stateService.getSearchText().get(),
                stateService.getItems()::setAll
        );
    }

    private void handleChange(ChangeStreamDocument<Document> change) {
        OperationType op = change.getOperationType();

        if (op == OperationType.DELETE) {
            BsonValue idValue = change.getDocumentKey().get("_id");
            String deletedId = idValue.getBsonType() == BsonType.OBJECT_ID
                    ? idValue.asObjectId().getValue().toString()
                    : idValue.asString().getValue();
            Platform.runLater(() -> stateService.getItems().removeIf(i -> i.getId().equals(deletedId)));
            return;
        }

        Document doc = change.getFullDocument();
        if (doc == null) return;

        ServiceItem updatedItem = mongoTemplate.getConverter().read(ServiceItem.class, doc);
        Platform.runLater(() -> applyChange(updatedItem, op));
    }

    private void applyChange(ServiceItem updatedItem, OperationType op) {
        ObservableList<ServiceItem> items = stateService.getItems();
        int index = findIndexById(updatedItem.getId(), items);
        boolean matches = matchesCurrentFilter(updatedItem);

        switch (op) {
            case INSERT -> {
                if (matches) items.add(updatedItem);
            }
            case UPDATE, REPLACE -> {
                if (index != -1 && matches) items.set(index, updatedItem);
                else if (index != -1) items.remove(index);
                else if (matches) items.add(updatedItem);
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

        ActivityFilter activity = stateService.getActivityFilter().get();
        if (activity == ActivityFilter.ACTIVE && item.getCompletedAt() != null) return false;
        if (activity == ActivityFilter.DONE && item.getCompletedAt() == null) return false;

        ServiceFilter service = stateService.getServiceFilter().get();
        if (service != null && service != ServiceFilter.ALL) {
            boolean matches = switch (service) {
                case FUEL -> item.getFuel() != null;
                case GPU -> item.getGpu() != null;
                case LAVATORY -> item.getLavatory() != null;
                case POTABLE_WATER -> item.getPotableWater() != null;
                case CATERING -> item.getCatering() != null && !item.getCatering().isEmpty();
                case WINDSHIELD_CLEANING -> item.getWindshieldCleaning() != null;
                case OIL_SERVICE -> item.getOilService() != null;
                default -> true;
            };
            if (!matches) return false;
        }

        TimeFilter time = stateService.getTimeFilter().get();
        if (time != null && item.getCreatedAt() != null) {
            ZonedDateTime now = ZonedDateTime.now();
            Instant start = switch (time) {
                case TODAY -> now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
                case WEEK -> now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay(now.getZone()).toInstant();
                case MONTH -> now.toLocalDate().withDayOfMonth(1).atStartOfDay(now.getZone()).toInstant();
            };
            if (item.getCreatedAt().isBefore(start)) return false;
            if (time == TimeFilter.TODAY) {
                Instant end = now.toLocalDate().atStartOfDay(now.getZone()).plusDays(1).toInstant();
                if (!item.getCreatedAt().isBefore(end)) return false;
            }
        }

        String search = stateService.getSearchText().get();
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            boolean tailMatch = item.getTail() != null && item.getTail().toLowerCase().contains(lower);
            boolean descMatch = item.getDescription() != null && item.getDescription().toLowerCase().contains(lower);
            if (!tailMatch && !descMatch) return false;
        }

        return true;
    }

    private void clearFilters() {
        statusGroup.selectToggle(activeServiceToggleButton);
        stateService.getActivityFilter().setValue(ActivityFilter.ACTIVE);

        filterServiceChoiceBox.setValue(ServiceFilter.ALL);
        stateService.getServiceFilter().setValue(ServiceFilter.ALL);

        filterTimeChoiceBox.setValue(TimeFilter.TODAY);
        stateService.getTimeFilter().setValue(TimeFilter.TODAY);

        searchtextField.textProperty().setValue("");
        stateService.getSearchText().setValue("");
    }

    private void onViewToggleChanged(StandardView newView) {
        if (centerPane == null) return;
        // Guard removed: stateService is a singleton and retains its value across
        // view loads. Without resetting it first (done above in initialize()), this
        // guard would cause the child view to never render when returning from Map view.
        if (stateService.getActiveView().get() == newView) return;

        centerPane.getChildren().clear();

        if (newView == StandardView.CARD) {
            try {
                stateService.getActiveView().set(StandardView.CARD);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/components/standard/grid-view.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Node child = loader.load();

                GridViewController controller = loader.getController();
                controller.populate();

                AnchorPane.setTopAnchor(child, 0.0);
                AnchorPane.setBottomAnchor(child, 0.0);
                AnchorPane.setLeftAnchor(child, 0.0);
                AnchorPane.setRightAnchor(child, 0.0);

                centerPane.getChildren().add(child);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (newView == StandardView.LIST) {
            try {
                stateService.getActiveView().set(StandardView.LIST);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/components/standard/list-view.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Node child = loader.load();

                ListViewController controller = loader.getController();
                controller.populate();

                AnchorPane.setTopAnchor(child, 0.0);
                AnchorPane.setBottomAnchor(child, 0.0);
                AnchorPane.setLeftAnchor(child, 0.0);
                AnchorPane.setRightAnchor(child, 0.0);

                centerPane.getChildren().add(child);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}