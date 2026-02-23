package com.amaurysdelossantos.ServiceTracker.Controllers;

//import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceTrackerService;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.View;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ServiceTrackerController {
//
//    @Autowired
//    private ServiceItemService serviceItemService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private StandardControlsService topStateService;


    @FXML
    private Text userInitials;
    @FXML
    private Button addButton;
    @FXML
    public AnchorPane centerPane;

    private Node mapViewNode;
    private MapViewController mapViewController;

    private List<ServiceItem> allItems;

    private final List<Node> currentCardNodes = new ArrayList<>();

    private static final double MIN_CARD_WIDTH = 280.0;
    private static final double HGAP = 10.0;
    private static final double VGAP = 10.0;
    private static final double PADDING = 10.0;


    // Track last column count so we only re-grid when it actually changes
    private int lastColCount = -1;

    @Autowired
    private ServiceTrackerService serviceTrackerService;


    //ServiceTrackerService
    @FXML
    public void initialize() {
        userInitials.setText("AD");

        // serviceItemService.loadInitialData();

        serviceTrackerService.activeViewProperty().addListener((e, oldItem, newItem) -> {
            onMainViewChange(newItem);
        });

        serviceTrackerService.activeViewProperty().setValue(View.STANDARD);

    }


    private void onMainViewChange(View newView) {
        try {
            switch (newView) {
                case STANDARD -> {
                    centerPane.getChildren().clear();
                    FXMLLoader loader1 = new FXMLLoader(getClass().getResource("/components/standard/standard-view.fxml"));
                    loader1.setControllerFactory(applicationContext::getBean); // inject Spring context
                    Node child1 = loader1.load();
                    AnchorPane.setTopAnchor(child1, 0.0);
                    AnchorPane.setBottomAnchor(child1, 0.0);
                    AnchorPane.setLeftAnchor(child1, 0.0);
                    AnchorPane.setRightAnchor(child1, 0.0);
                    centerPane.getChildren().addAll(child1);

                }
                case MAP -> {
                    centerPane.getChildren().clear();
                    FXMLLoader loader1 = new FXMLLoader(getClass().getResource("/components/map/map-view.fxml"));
                    loader1.setControllerFactory(applicationContext::getBean); // inject Spring context
                    Node child1 = loader1.load();
                    AnchorPane.setTopAnchor(child1, 0.0);
                    AnchorPane.setBottomAnchor(child1, 0.0);
                    AnchorPane.setLeftAnchor(child1, 0.0);
                    AnchorPane.setRightAnchor(child1, 0.0);
                    centerPane.getChildren().addAll(child1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}