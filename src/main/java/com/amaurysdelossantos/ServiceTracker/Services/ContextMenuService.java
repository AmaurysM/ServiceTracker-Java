package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler;
import com.amaurysdelossantos.ServiceTracker.controllers.map.item.AircraftContextMenuController;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class ContextMenuService {


    private final MongoTemplate mongoTemplate;
    private final MapService    mapService;

    @Autowired
    public ContextMenuService(MongoTemplate mongoTemplate, MapService mapService) {
        this.mongoTemplate = mongoTemplate;
        this.mapService    = mapService;
    }


    @Getter
    private Pane markerPane;

    private Object openCard;

    public void registerMarkerPane(Pane pane) {
        this.markerPane = pane;
    }

    public void show(ServiceItem item, Node anchorNode) {
        if (markerPane == null) return;

        close();

        AircraftContextMenuController card =
                AircraftContextMenuController
                        .create(item, this);

        openCard = card;
        markerPane.getChildren().add(card.getNode());
        card.positionNear(anchorNode, markerPane);
    }

    public void close() {
        if (openCard == null) return;
        var card = (com.amaurysdelossantos.ServiceTracker.controllers.map.item
                .AircraftContextMenuController) openCard;
        Platform.runLater(() -> markerPane.getChildren().remove(card.getNode()));
        openCard = null;
    }

    public void refreshIfOpen(String itemId, ServiceItem updated) {
        if (openCard == null) return;
        var card = (AircraftContextMenuController) openCard;
        if (card.getItem().getId().equals(itemId)) {
            Platform.runLater(() -> card.refresh(updated));
        }
    }

    public boolean isClickInsideOpenCard(double sceneX, double sceneY) {
        if (openCard == null) return false;
        var card = (AircraftContextMenuController) openCard;
        Bounds bounds = card.getNode().localToScene(card.getNode().getBoundsInLocal());
        return bounds.contains(sceneX, sceneY);
    }

    public void handleView(ServiceItem item) {
        WindowHandler.handleInfo(item);
    }

    public void handleEdit(ServiceItem item) {
        WindowHandler.handleEdit(item);
    }

    public void handleDone(ServiceItem item) {
        WindowHandler.handleToggleComplete(item, () -> {
            mapService.getItems().stream()
                    .filter(i -> i.getId().equals(item.getId()))
                    .findFirst()
                    .ifPresent(fresh -> refreshIfOpen(fresh.getId(), fresh));
        });
    }

    public void handleUnplace(ServiceItem item) {
        close();

        mapService.getItems().stream()
                .filter(i -> i.getId().equals(item.getId()))
                .findFirst()
                .ifPresent(i -> {
                    if (i.getMetadata() != null) i.getMetadata().remove("mapPosition");
                });

        Thread t = new Thread(() ->
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(item.getId())),
                        new Update().unset("metadata.mapPosition"),
                        ServiceItem.class));
        t.setDaemon(true);
        t.start();
    }

    public void handleRemove(ServiceItem item) {
        close();
        WindowHandler.handleDelete(item);
    }

    public void handleServiceClicked(ServiceItem item, ServiceType serviceType) {
        WindowHandler.handleEdit(serviceType, item);
    }
}