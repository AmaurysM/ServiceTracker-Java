package com.amaurysdelossantos.ServiceTracker.Controllers;

//import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;

import com.amaurysdelossantos.ServiceTracker.Services.ServiceTrackerService;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
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

import static com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler.handleAdd;

@Component
public class ServiceTrackerController {

    @FXML
    public AnchorPane centerPane;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private StandardControlsService topStateService;
    @FXML
    private Text userInitials;
    @FXML
    private Button addButton;
    @Autowired
    private ServiceTrackerService serviceTrackerService;

    @FXML
    public void initialize() {
        userInitials.setText("AD");

        serviceTrackerService.activeViewProperty().addListener((e, oldItem, newItem) -> {
            onMainViewChange(newItem);
        });

        addButton.setOnAction(e -> {
            handleAdd();
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