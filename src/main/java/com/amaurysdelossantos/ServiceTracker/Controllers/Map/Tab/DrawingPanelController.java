package com.amaurysdelossantos.ServiceTracker.Controllers.Map.Tab;

import com.amaurysdelossantos.ServiceTracker.Services.MapService;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DrawingPanelController {
    @FXML
    private AnchorPane exitButton;

    @Autowired
    private MapService mapService;

    public void initialize() {
        exitButton.setOnMouseClicked(e -> {
            handledExit();
        });
    }

    private void handledExit() {
        mapService.tabOpenProperty().set(false);
        System.out.println("HandleExit: CLOSE");
    }
}
