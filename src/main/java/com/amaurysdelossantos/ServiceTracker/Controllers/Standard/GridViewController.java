package com.amaurysdelossantos.ServiceTracker.Controllers.Standard;

import com.amaurysdelossantos.ServiceTracker.Controllers.Standard.Item.CardItemController;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Scope;

@Component
@Scope("prototype")
public class GridViewController {

    @FXML
    public GridPane mainGridPane;
    @FXML
    public ScrollPane mainScrollPane;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private StandardControlsService standardControlsService;

    public ObservableList<ServiceItem> items;
    private final Map<ServiceItem, Node> nodeCache = new LinkedHashMap<>();
    private int currentColumns = 0;

    @FXML
    public void initialize() {}

    public void populate(){
        items = standardControlsService.getItems();

        items.addListener((ListChangeListener<ServiceItem>) change -> {
            syncItems();
        });

        mainScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            int newColumns = calculateColumns(newVal.doubleValue());
            if (newColumns != currentColumns) {
                currentColumns = newColumns;
                applyLayout();
            }
        });

        syncItems();
    }



    private void syncItems() {
        items.forEach(item -> {
            nodeCache.computeIfAbsent(item, e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/components/standard/item/card-item.fxml"));
                    loader.setControllerFactory(applicationContext::getBean);
                    Node node = loader.load();
                    CardItemController controller = loader.getController();
                    controller.setItem(e);
                    mainGridPane.getChildren().add(node);
                    return node;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        });

        // Remove nodes for deleted items
        nodeCache.keySet().retainAll(items);
        mainGridPane.getChildren().retainAll(nodeCache.values());

        applyLayout();
    }

    private void applyLayout() {
        double availableWidth = mainScrollPane.getWidth();
        if (availableWidth <= 0) return;

        int columns = calculateColumns(availableWidth);
        currentColumns = columns;

        // Rebuild column constraints
        mainGridPane.getColumnConstraints().clear();
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            mainGridPane.getColumnConstraints().add(cc);
        }

        // Just reposition — never remove/re-add nodes
        List<Node> children = new ArrayList<>(nodeCache.values());
        for (int i = 0; i < children.size(); i++) {
            GridPane.setColumnIndex(children.get(i), i % columns);
            GridPane.setRowIndex(children.get(i), i / columns);
        }
    }

    private int calculateColumns(double availableWidth) {
        double cardMinWidth = 300;
        double hgap = 10;
        int cols = (int) ((availableWidth + hgap) / (cardMinWidth + hgap));
        return Math.max(1, cols);
    }
}