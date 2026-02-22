package com.amaurysdelossantos.ServiceTracker.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class ViewController {
    @FXML
    private GridPane mainGridPane;

    @FXML
    private ScrollPane mainScrollPane;

    @FXML
    private VBox mainVBox;
}
