package com.amaurysdelossantos.ServiceTracker.Controllers;

import com.amaurysdelossantos.ServiceTracker.Services.MainService;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.MainView;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.ServiceView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class MainViewController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MainService mainService;

    @FXML
    public AnchorPane containerAnchorPane;

    @FXML
    public void initialize(){

        mainService.activeViewProperty().addListener((e, oldItem, newItem) -> {
            onMainViewChange(newItem);
        });

        mainService.activeViewProperty().setValue(MainView.SERVICES);
    }


    private void onMainViewChange(MainView newView) {System.out.println("Main view picked: " + newView);
        try {
            switch (newView) {
                case SERVICES -> {

                    containerAnchorPane.getChildren().clear();
                    FXMLLoader loader1 = new FXMLLoader(getClass().getResource("/service-tracker.fxml"));
                    loader1.setControllerFactory(applicationContext::getBean);
                    Node child1 = loader1.load();

                    AnchorPane.setTopAnchor(child1, 0.0);
                    AnchorPane.setBottomAnchor(child1, 0.0);
                    AnchorPane.setLeftAnchor(child1, 0.0);
                    AnchorPane.setRightAnchor(child1, 0.0);
                    containerAnchorPane.getChildren().addAll(child1);

                }
                case OPTIONS -> {
                    containerAnchorPane.getChildren().clear();
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/settings-view.fxml"));
                    loader.setControllerFactory(applicationContext::getBean);
                    Node child = loader.load();

                    AnchorPane.setTopAnchor(child, 0.0);
                    AnchorPane.setBottomAnchor(child, 0.0);
                    AnchorPane.setLeftAnchor(child, 0.0);
                    AnchorPane.setRightAnchor(child, 0.0);
                    containerAnchorPane.getChildren().add(child);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
