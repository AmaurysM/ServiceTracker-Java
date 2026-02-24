package com.amaurysdelossantos.ServiceTracker.Controllers;

//import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;

import com.amaurysdelossantos.ServiceTracker.Controllers.Map.MapViewController;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceTrackerService;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.ServiceView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

import static com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler.handleAdd;

@Component
public class ServiceTrackerController {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private StandardControlsService topStateService;

    @FXML
    public AnchorPane centerPane;
    @FXML
    public StackPane initialsStackPane;
    @FXML
    private Text userInitials;
    @FXML
    private Button addButton;
    private Popup userOptionsPopup;

    @Autowired
    private ServiceTrackerService serviceTrackerService;

    @FXML
    public void initialize() {
        System.out.println("9999999999999");

        userInitials.setText("AD");

        serviceTrackerService.activeViewProperty().addListener((e, oldItem, newItem) -> {
            onMainViewChange(newItem);
        });

        addButton.setOnAction(e -> {
            handleAdd();
        });

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/components/standard/user-options.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);

            VBox dropdown = loader.load();
            dropdown.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/styles/standard/user-options.css")).toExternalForm()
            );

            userOptionsPopup = new Popup();
            userOptionsPopup.getContent().add(dropdown);
            userOptionsPopup.setAutoHide(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        initialsStackPane.setOnMouseClicked(e -> toggleUserOptions());

        serviceTrackerService.activeViewProperty().setValue(ServiceView.STANDARD);

    }

    private void toggleUserOptions() {

        if (userOptionsPopup.isShowing()) {
            userOptionsPopup.hide();
            return;
        }

        var bounds = initialsStackPane.localToScreen(
                initialsStackPane.getBoundsInLocal()
        );

        userOptionsPopup.show(
                initialsStackPane,
                bounds.getMinX(),
                bounds.getMaxY() + 5
        );
    }

    private void onMainViewChange(ServiceView newView) {
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
                    loader1.setControllerFactory(applicationContext::getBean);
                    Node child1 = loader1.load();

                    AnchorPane.setTopAnchor(child1, 0.0);
                    AnchorPane.setBottomAnchor(child1, 0.0);
                    AnchorPane.setLeftAnchor(child1, 0.0);
                    AnchorPane.setRightAnchor(child1, 0.0);
                    centerPane.getChildren().add(child1);

                    // Tell the map to re-measure itself after layout is done
                    MapViewController mapCtrl = loader1.getController();
                    Platform.runLater(mapCtrl::invalidateSize);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}