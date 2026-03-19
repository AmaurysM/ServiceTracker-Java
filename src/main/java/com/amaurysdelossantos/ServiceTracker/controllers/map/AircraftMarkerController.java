package com.amaurysdelossantos.ServiceTracker.controllers.map;

import com.amaurysdelossantos.ServiceTracker.Helper.Lib;
import com.amaurysdelossantos.ServiceTracker.Services.ContextMenuService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import fxmapcontrol.Location;
import fxmapcontrol.MapBase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
@Scope("prototype")
public class AircraftMarkerController {

    @FXML private StackPane root;
    @FXML private StackPane innerRotating;
    @FXML private Circle    selectionRing;
    @FXML private Circle    rotateHandle;
    @FXML private SVGPath   planeIcon;
    @FXML private StackPane labelContainer;
    @FXML private Label     tailLabel;
    @FXML private HBox      serviceBar;

    @Autowired
    private ContextMenuService contextMenuService;

    private double dragLatOffset, dragLngOffset;
    private double posX, posY, rotation = 0;

    @Getter private ServiceItem item;
    private MapBase map;
    private boolean selected  = false;
    private DragMode dragMode = DragMode.NONE;

    private double  rotateCenterX, rotateCenterY, rotationOffset;
    private boolean hasDragged = false;

    @Setter private Consumer<AircraftMarkerController>        onSelect;
    @Setter private TriConsumer<String, Double, Double>       onPositionUpdate;
    @Setter private BiConsumer<String, Double>                onRotationUpdate;

    public static AircraftMarkerController create(ServiceItem item,
                                                  MapBase map,
                                                  ContextMenuService contextMenuService) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AircraftMarkerController.class.getResource(
                            "/components/map/aircraft-marker.fxml"));
            loader.setControllerFactory(clazz -> {
                AircraftMarkerController ctrl = new AircraftMarkerController();
                ctrl.contextMenuService = contextMenuService;
                return ctrl;
            });
            loader.load();
            AircraftMarkerController ctrl = loader.getController();
            ctrl.init(item, map);
            return ctrl;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load aircraft-marker.fxml", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapPosition(ServiceItem item) {
        if (item.getMetadata() == null) return null;
        Object mp = item.getMetadata().get("mapPosition");
        return (mp instanceof Map<?, ?> m) ? (Map<String, Object>) m : null;
    }

    public static boolean hasMapPosition(ServiceItem item) {
        return mapPosition(item) != null;
    }

    private static double angleTo(double cx, double cy, double px, double py) {
        return Math.toDegrees(Math.atan2(py - cy, px - cx)) + 90.0;
    }

    private static double norm(double a) {
        double n = a % 360;
        return n < 0 ? n + 360 : n;
    }

    private static String serviceColor(ServiceType type) {
        return switch (type) {
            case FUEL                -> "#f59e0b";
            case CATERING            -> "#10b981";
            case GPU                 -> "#8b5cf6";
            case LAVATORY            -> "#06b6d4";
            case POTABLE_WATER       -> "#3b82f6";
            case WINDSHIELD_CLEANING -> "#f97316";
            case OIL_SERVICE         -> "#ef4444";
        };
    }

    @FXML
    public void initialize() { }

    private void init(ServiceItem item, MapBase map) {
        this.item = item;
        this.map  = map;
        applyClipping();
        readPosition();
        rebuildLabel();
        wireEvents();
        refreshVisuals();
        Platform.runLater(this::relocate);
    }

    public StackPane getNode() { return root; }
    public String    getId()   { return item.getId(); }

    public void setSelected(boolean sel) {
        selected = sel;
        refreshVisuals();
    }

    public void refresh(ServiceItem updated) {
        double oldX = posX, oldY = posY, oldRot = rotation;
        this.item = updated;
        readPosition();
        rebuildLabel();
        refreshVisuals();
        if (posX != oldX || posY != oldY || rotation != oldRot) relocate();
    }

    public void relocate() {
        Point2D pt = toPixel(posX, posY);
        if (pt == null) return;
        double w = root.getWidth()  > 0 ? root.getWidth()  : root.prefWidth(-1);
        double h = root.getHeight() > 0 ? root.getHeight() : root.prefHeight(-1);
        root.setLayoutX(pt.getX() - w / 2.0);
        root.setLayoutY(pt.getY() - h / 2.0);
    }

    public void relocateAsync() { Platform.runLater(this::relocate); }

    private void applyClipping() {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        clip.widthProperty().bind(labelContainer.widthProperty());
        clip.heightProperty().bind(labelContainer.heightProperty());
        labelContainer.setClip(clip);
    }

    private void refreshVisuals() {
        boolean allDone = Lib.areAllServicesCompleted(item);
        selectionRing.setVisible(selected);
        rotateHandle.setVisible(selected);
        String fill = selected ? "#2563eb" : allDone ? "#16a34a" : "#1e293b";
        planeIcon.setStyle("-fx-fill: " + fill + ";");
        innerRotating.setRotate(rotation);
    }

    private void rebuildLabel() {
        tailLabel.setText(item.getTail() != null ? item.getTail() : item.getId());
        serviceBar.getChildren().clear();
        Map<ServiceType, Boolean> active = Lib.getActiveServices(item);
        boolean hasIncomplete = active.values().stream().anyMatch(done -> !done);

        if (hasIncomplete) {
            active.entrySet().stream()
                    .filter(e -> !e.getValue())
                    .forEach(e -> {
                        Pane seg = new Pane();
                        seg.setPrefHeight(3);
                        seg.setMinHeight(3);
                        seg.setStyle("-fx-background-color: " + serviceColor(e.getKey()) + ";");
                        HBox.setHgrow(seg, Priority.ALWAYS);
                        serviceBar.getChildren().add(seg);
                    });
            serviceBar.setVisible(true);
            serviceBar.setManaged(true);
        } else {
            serviceBar.setVisible(false);
            serviceBar.setManaged(false);
        }
    }

    private void wireEvents() {
        root.setCursor(Cursor.HAND);

        root.setOnMousePressed(e -> {

            if (e.isSecondaryButtonDown()) {
                e.consume();
                return;
            }

            if (e.getTarget() == rotateHandle) return;

            contextMenuService.close();

            Location cursorLoc = toLatLng(e.getSceneX(), e.getSceneY());
            if (cursorLoc == null) return;

            hasDragged    = false;
            dragMode      = DragMode.MOVE;
            dragLatOffset = cursorLoc.getLatitude()  - posX;
            dragLngOffset = cursorLoc.getLongitude() - posY;
            root.setCursor(Cursor.CLOSED_HAND);
            e.consume();
        });

        root.setOnMouseDragged(e -> {
            if (dragMode == DragMode.MOVE) {
                hasDragged = true;
                Location cursorLoc = toLatLng(e.getSceneX(), e.getSceneY());
                if (cursorLoc != null) {
                    posX = cursorLoc.getLatitude()  - dragLatOffset;
                    posY = cursorLoc.getLongitude() - dragLngOffset;
                    relocate();
                }
            } else if (dragMode == DragMode.ROTATE) {
                hasDragged = true;
                double angle = angleTo(rotateCenterX, rotateCenterY,
                        e.getSceneX(), e.getSceneY()) + rotationOffset;
                if (e.isShiftDown()) angle = Math.round(angle / 15.0) * 15.0;
                rotation = norm(angle);
                innerRotating.setRotate(rotation);
            }
            e.consume();
        });

        root.setOnMouseReleased(e -> {
            if (dragMode == DragMode.MOVE && onPositionUpdate != null)
                onPositionUpdate.accept(getId(), posX, posY);
            if (dragMode == DragMode.ROTATE && onRotationUpdate != null)
                onRotationUpdate.accept(getId(), rotation);
            dragMode = DragMode.NONE;
            root.setCursor(Cursor.HAND);
            e.consume();
        });

        root.setOnMouseClicked(e -> {
            if (hasDragged) return;
            switch (e.getButton()) {
                case PRIMARY -> {
                    if (onSelect != null) onSelect.accept(this);
                }
                case SECONDARY -> {
                    if (onSelect != null) onSelect.accept(this);
                    contextMenuService.show(item, root);
                }
                default -> { }
            }
            e.consume();
        });

        root.setOnScroll(e -> {
            if (!selected) return;
            double step = e.isShiftDown() ? 15.0 : 5.0;
            rotation = norm(rotation + (e.getDeltaY() > 0 ? step : -step));
            innerRotating.setRotate(rotation);
            if (onRotationUpdate != null) onRotationUpdate.accept(getId(), rotation);
            e.consume();
        });

        rotateHandle.setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;
            dragMode = DragMode.ROTATE;
            Bounds b = innerRotating.localToScene(innerRotating.getBoundsInLocal());
            rotateCenterX  = b.getCenterX();
            rotateCenterY  = b.getCenterY();
            rotationOffset = rotation
                    - angleTo(rotateCenterX, rotateCenterY, e.getSceneX(), e.getSceneY());
            rotateHandle.setCursor(Cursor.CROSSHAIR);
            e.consume();
        });
    }

    private void readPosition() {
        Map<String, Object> mp = mapPosition(item);
        if (mp == null) return;
        if (mp.get("x")        instanceof Number n) posX     = n.doubleValue();
        if (mp.get("y")        instanceof Number n) posY     = n.doubleValue();
        if (mp.get("rotation") instanceof Number n) rotation = n.doubleValue();
    }

    private Point2D toPixel(double lat, double lng) {
        try { return map.locationToView(new Location(lat, lng)); }
        catch (Exception e) { return null; }
    }

    private Location toLatLng(double sceneX, double sceneY) {
        try {
            Point2D local = map.sceneToLocal(sceneX, sceneY);
            return map.viewToLocation(local);
        } catch (Exception e) { return null; }
    }

    private enum DragMode { NONE, MOVE, ROTATE }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> { void accept(A a, B b, C c); }
}