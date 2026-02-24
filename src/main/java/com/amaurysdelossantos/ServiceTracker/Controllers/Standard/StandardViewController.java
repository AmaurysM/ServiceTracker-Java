package com.amaurysdelossantos.ServiceTracker.Controllers.Standard;

//import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;

import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.StandardView;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static com.amaurysdelossantos.ServiceTracker.Helper.Lib.*;

@Component
public class StandardViewController {

    @FXML
    public AnchorPane centerPane;

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
    private StandardControlsService topStateService;

    @Autowired
    private ApplicationContext applicationContext;

    private ObservableList<ServiceItem> allItems;
    private List<ServiceItem> filterItems;


    @FXML
    public void initialize() {

        // View toggles
        CardViewToggleButton.setOnAction(e -> onViewToggleChanged(StandardView.CARD));
        ListViewToggleButton.setOnAction(e -> onViewToggleChanged(StandardView.LIST));
        viewGroup.selectToggle(CardViewToggleButton);

        // Activity toggles
        activeCompletedToggleButton.setOnAction(e -> topStateService.getActivityFilter().set(ActivityFilter.DONE));
        activeServiceToggleButton.setOnAction(e -> topStateService.getActivityFilter().set(ActivityFilter.ACTIVE));
        statusGroup.selectToggle(activeServiceToggleButton);

        // ✅ Wire up service filter combobox
        filterServiceChoiceBox.getItems().setAll(ServiceFilter.values());
        filterServiceChoiceBox.setValue(ServiceFilter.ALL);
        filterServiceChoiceBox.valueProperty().addListener((obs, oldVal, newVal) ->
                topStateService.getServiceFilter().set(newVal)
        );

        // ✅ Wire up time filter combobox
        filterTimeChoiceBox.getItems().setAll(TimeFilter.values());
        filterTimeChoiceBox.setValue(TimeFilter.TODAY);
        filterTimeChoiceBox.valueProperty().addListener((obs, oldVal, newVal) ->
                topStateService.getTimeFilter().set(newVal)
        );

        // ✅ Wire up search field
        searchtextField.textProperty().addListener((obs, oldVal, newVal) ->
                topStateService.getSearchText().set(newVal)
        );

        clearButton.setOnAction(e -> clearFilters());

        allItems = topStateService.getItems();
        ammountOfItemsInViewText.textProperty().bind(
                Bindings.size(allItems).asString("SHOWING %d ITEMS")
        );

        attachButtonAnimation(clearButton);
        attachToggleAnimation(ListViewToggleButton);
        attachToggleAnimation(activeCompletedToggleButton);
        attachToggleAnimation(activeServiceToggleButton);
        attachToggleAnimation(CardViewToggleButton);
        attachSearchFieldAnimation(searchtextField);

        preventDeselection(statusGroup);
        preventDeselection(viewGroup);

        onViewToggleChanged(StandardView.CARD);
        topStateService.getActivityFilter().setValue(ActivityFilter.ACTIVE);
        topStateService.getTimeFilter().setValue(TimeFilter.TODAY); // ✅ also set initial value on service
        topStateService.loadInitialData();
    }

    private void isSelectedChoice(StandardView newVal) {
        if (newVal == StandardView.CARD) {
            CardViewToggleButton.setSelected(true);
            return;
        }

        if (newVal == StandardView.LIST) {
            ListViewToggleButton.setSelected(true);
            return;
        }

    }

    private void clearFilters() {

        statusGroup.selectToggle(activeServiceToggleButton);
        topStateService.getActivityFilter().setValue(ActivityFilter.ACTIVE);

//        viewGroup.selectToggle(CardViewToggleButton);
//        topStateService.getServiceFilter().setValue(ServiceFilter.ALL);

        filterTimeChoiceBox.setValue(TimeFilter.TODAY);
        topStateService.getTimeFilter().setValue(TimeFilter.TODAY);

        searchtextField.textProperty().setValue("");
        topStateService.getSearchText().setValue("");
    }

    private void onViewToggleChanged(StandardView newView) {
        if (centerPane == null) return;

        if (topStateService.getActiveView().get() == newView) return;

        centerPane.getChildren().clear();

        if (newView == StandardView.CARD) {
            try {
                topStateService.getActiveView().set(StandardView.CARD);

                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/components/standard/grid-view.fxml"));
                loader2.setControllerFactory(applicationContext::getBean);
                Node child2 = loader2.load();

                GridViewController controller = loader2.getController();
                controller.populate();

                AnchorPane.setTopAnchor(child2, 0.0);
                AnchorPane.setBottomAnchor(child2, 0.0);
                AnchorPane.setLeftAnchor(child2, 0.0);
                AnchorPane.setRightAnchor(child2, 0.0);

                centerPane.getChildren().add(child2);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (newView == StandardView.LIST) {
            try {
                topStateService.getActiveView().set(StandardView.LIST);

                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/components/standard/list-view.fxml"));
                loader2.setControllerFactory(applicationContext::getBean);
                Node child2 = loader2.load();

                ListViewController controller = loader2.getController();
                controller.populate();

                AnchorPane.setTopAnchor(child2, 0.0);
                AnchorPane.setBottomAnchor(child2, 0.0);
                AnchorPane.setLeftAnchor(child2, 0.0);
                AnchorPane.setRightAnchor(child2, 0.0);

                centerPane.getChildren().add(child2);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}