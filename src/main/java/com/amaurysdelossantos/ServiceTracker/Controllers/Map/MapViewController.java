package com.amaurysdelossantos.ServiceTracker.Controllers.Map;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import fxmapcontrol.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MapViewController {

    @FXML private Map fxMap;
    @FXML private Text coordText;
    @FXML private Text itemCountText;

    @FXML
    public void initialize() {
        TileImageLoader.setCache(new ImageFileCache());

        fxMap.setProjection(new WebMercatorProjection());

        MapTileLayer tileLayer = new MapTileLayer(
                "MapTiler",
                "https://api.maptiler.com/maps/streets/{z}/{x}/{y}.png?key=7p902bvM8BZds2RvNgE0"
        );

        fxMap.getChildren().add(0, tileLayer);

        Platform.runLater(() -> fxMap.requestLayout());

        System.out.println("Map initialized");
    }

    public void flyTo(double lat, double lon, double zoom) {
        Platform.runLater(() -> {
            fxMap.setCenter(new Location(lat, lon));
            fxMap.setZoomLevel(zoom);
        });
    }

    public void invalidateSize() {
        Platform.runLater(() -> fxMap.requestLayout());
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> getMapPosition(ServiceItem item) {
        if (item.getMetadata() == null) return null;
        Object mp = item.getMetadata().get("mapPosition");
        return (mp instanceof java.util.Map) ? (java.util.Map<String, Object>) mp : null;
    }

}