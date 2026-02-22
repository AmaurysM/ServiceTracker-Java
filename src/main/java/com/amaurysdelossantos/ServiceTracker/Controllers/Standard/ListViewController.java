package com.amaurysdelossantos.ServiceTracker.Controllers.Standard;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

@Component
public class ListViewController {
    @FXML
    public ScrollPane mainScrollPane;
    @FXML
    public VBox mainVBox;
}
