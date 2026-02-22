package com.amaurysdelossantos.ServiceTracker.Controllers.Standard;

import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.Services.TopControlsService;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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

    @Autowired
    private TopControlsService topStateService;

    @Autowired
    private ServiceItemService serviceItemService;

    @Autowired
    private ApplicationContext applicationContext;

    private ObservableList<ServiceItem> allItems;
    private List<ServiceItem> filterItems;

    @FXML
    public void initialize() {

        CardViewToggleButton.setOnAction(e -> {
            topStateService.activeViewProperty().set(StandardView.CARD);
            onViewToggleChanged(StandardView.CARD);
        });
        ListViewToggleButton.setOnAction(e -> {
            topStateService.activeViewProperty().set(StandardView.LIST);
            onViewToggleChanged(StandardView.LIST);
        });

        viewGroup.selectToggle(CardViewToggleButton);

        activeCompletedToggleButton.setOnAction(e -> topStateService.activeActivity().set(ActivityFilter.DONE));

        activeServiceToggleButton.setOnAction(e -> topStateService.activeActivity().set(ActivityFilter.ACTIVE));

        statusGroup.selectToggle(activeServiceToggleButton);

        filterServiceChoiceBox.getItems().setAll(ServiceFilter.values());
        filterServiceChoiceBox.setValue(ServiceFilter.ALL);

        filterTimeChoiceBox.getItems().setAll(TimeFilter.values());
        filterTimeChoiceBox.setValue(TimeFilter.TODAY);

        allItems = serviceItemService.getItems();

        ammountOfItemsInViewText.textProperty().bind(
                Bindings.size(allItems)
                        .asString("SHOWING %d ITEMS")
        );

        attachToggleAnimation(ListViewToggleButton);
        attachToggleAnimation(activeCompletedToggleButton);
        attachToggleAnimation(activeServiceToggleButton);
        attachToggleAnimation(CardViewToggleButton);

        attachSearchFieldAnimation(searchtextField);

        preventDeselection(statusGroup);
        preventDeselection(viewGroup);

        onViewToggleChanged(StandardView.CARD);

    }

//    private void renderItems() {
//
//    }

//    private void fetchItems() {
//
//        boolean showActive = statusGroup.getSelectedToggle() == activeServiceToggleButton;
//
//        Thread loadThread = new Thread(() -> {
//            try {
//
//                List<ServiceItem> items = showActive
//                        ? serviceItemService.getActiveItems()
//                        : serviceItemService.getCompletedItems();
//
//                Platform.runLater(() -> {
//                    allItems = items;
//                    ammountOfItemsInViewText.setText(
//                            "SHOWING " + " OF " + allItems.size()
//                    );
//                });
//
//            } catch (Exception ex) {
//                ex.printStackTrace();
//
//                Platform.runLater(() -> {
//                    Label errorLabel = new Label("Failed to load services: " + ex.getMessage());
//                    errorLabel.setStyle("-fx-text-fill: red;");
//                });
//            }
//        });
//
//
//
//        loadThread.setDaemon(true);
//        loadThread.start();
//
//
//    }

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

    private void onViewToggleChanged(StandardView newView) {
        if (centerPane == null) return;

        centerPane.getChildren().clear();

        if (newView == StandardView.CARD) {
            try {

                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/components/standard/grid-view.fxml"));
                loader2.setControllerFactory(applicationContext::getBean);
                Node child2 = loader2.load();

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
                FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/components/standard/list-view.fxml"));
                loader2.setControllerFactory(applicationContext::getBean); // inject Spring context
                Node child2 = loader2.load();
                centerPane.getChildren().add(child2);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}
