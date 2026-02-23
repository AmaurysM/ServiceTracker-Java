package com.amaurysdelossantos.ServiceTracker.Controllers.Standard;

import com.amaurysdelossantos.ServiceTracker.Controllers.Standard.Item.ListItemController;
import com.amaurysdelossantos.ServiceTracker.Services.StandardControlsService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class ListViewController {

    private final Map<ServiceItem, Node> nodeCache = new LinkedHashMap<>();

    @FXML
    public VBox mainVBox;

    @FXML
    public ScrollPane mainScrollPane;

    public ObservableList<ServiceItem> items;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private StandardControlsService standardControlsService;

    public void populate() {
        items = standardControlsService.getItems();

        items.addListener((ListChangeListener<ServiceItem>) change -> {
            syncItems();
        });

        syncItems();
    }

    private void syncItems() {
        // 1. Build nodes for any new items
        for (ServiceItem item : items) {
            nodeCache.computeIfAbsent(item, e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/components/standard/item/list-item.fxml")
                    );
                    loader.setControllerFactory(applicationContext::getBean);
                    Node node = loader.load();
                    ListItemController controller = loader.getController();
                    controller.setItem(e);
                    return node;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        // 2. Remove stale cache entries (items no longer in the list)
        nodeCache.keySet().retainAll(items);

        // 3. Rebuild VBox children in the exact order of `items`
        List<Node> newChildren = items.stream()
                .map(nodeCache::get)
                .toList();

        mainVBox.getChildren().setAll(newChildren);
    }
}
