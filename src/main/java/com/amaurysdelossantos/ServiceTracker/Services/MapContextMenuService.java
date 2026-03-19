package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.controllers.map.item.MapContextMenuController;
import fxmapcontrol.Map;
import fxmapcontrol.MapTileLayer;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MapContextMenuService {

    @Autowired
    private PendingMapPositionService pendingMapPosition;

    private Pane                     markerPane;
    private Map                      fxMap;
    private MapContextMenuController openCard;

    private String currentTileUrl = "";

    public void registerMarkerPane(Pane markerPane, Map fxMap) {
        this.markerPane = markerPane;
        this.fxMap      = fxMap;

        fxMap.getChildren().stream()
                .filter(n -> n instanceof MapTileLayer)
                .map(n -> (MapTileLayer) n)
                .findFirst()
                .ifPresent(l -> currentTileUrl = l.getName());
    }

    public void installOn(Map fxMap) {
        fxMap.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {

            if (e.isSecondaryButtonDown()) {

                if (isClickInsideMarkerPane(e.getSceneX(), e.getSceneY())) return;

                close();

                fxmapcontrol.Location loc = pixelToLocation(fxMap, e.getX(), e.getY());

                openCard = MapContextMenuController.create(
                        loc.getLatitude(),
                        loc.getLongitude(),
                        e.getSceneX(),
                        e.getSceneY(),
                        markerPane,
                        fxMap,
                        pendingMapPosition,
                        currentTileUrl,
                        this);

                markerPane.getChildren().add(openCard.getNode());
                e.consume();

            } else if (e.isPrimaryButtonDown()) {
                if (!isClickInsideOpenCard(e.getSceneX(), e.getSceneY())) {
                    close();
                }
            }
        });
    }

    public void notifyTileUrlChanged(String newTileUrl) {
        this.currentTileUrl = newTileUrl;
    }

    public void close() {
        if (openCard != null && markerPane != null) {
            markerPane.getChildren().remove(openCard.getNode());
            openCard = null;
        }
    }

    public boolean isClickInsideOpenCard(double sceneX, double sceneY) {
        if (openCard == null) return false;
        VBox node = openCard.getNode();
        if (node.getScene() == null || !node.isVisible()) return false;
        Bounds b = node.localToScene(node.getBoundsInLocal());
        return b.contains(sceneX, sceneY);
    }

    private boolean isClickInsideMarkerPane(double sceneX, double sceneY) {
        if (markerPane == null) return false;
        return markerPane.getChildren().stream()
                .filter(n -> n.isVisible() && n.getScene() != null)
                .anyMatch(n -> {
                    Bounds b = n.localToScene(n.getBoundsInLocal());
                    return b.contains(sceneX, sceneY);
                });
    }

    private static fxmapcontrol.Location pixelToLocation(
            fxmapcontrol.Map map, double localX, double localY) {
        double worldSize = Math.pow(2.0, map.getZoomLevel()) * 256.0;
        fxmapcontrol.Location centre = map.getCenter();
        double cx     = (centre.getLongitude() / 360.0 + 0.5) * worldSize;
        double sinLat = Math.sin(Math.toRadians(centre.getLatitude()));
        double cy     = (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * worldSize;
        double wx = cx + (localX - map.getWidth()  / 2.0);
        double wy = cy + (localY - map.getHeight() / 2.0);
        double lon = (wx / worldSize - 0.5) * 360.0;
        double lat = Math.toDegrees(
                Math.atan(Math.sinh(Math.PI - 2 * Math.PI * wy / worldSize)));
        return new fxmapcontrol.Location(lat, lon);
    }
}