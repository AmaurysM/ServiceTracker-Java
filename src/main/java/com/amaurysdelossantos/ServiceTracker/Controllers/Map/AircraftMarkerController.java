package com.amaurysdelossantos.ServiceTracker.Controllers.Map;

import com.amaurysdelossantos.ServiceTracker.Helper.Lib;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
@Scope("prototype")
public class AircraftMarkerController {

    private static final long DBL_CLICK_MS = 300;

    @FXML private StackPane root;
    @FXML private StackPane innerRotating;
    @FXML private Circle selectionRing;
    @FXML private Circle rotateHandle;
    @FXML private SVGPath planeIcon;
    @FXML private StackPane labelContainer;
    @FXML private Label tailLabel;
    @FXML private HBox serviceBar;

    private double posX;
    private double posY;
    private double rotation = 0;
    private ServiceItem item;
    private MapBase map;
    private boolean selected = false;
    private DragMode dragMode = DragMode.NONE;

    private double dragOffsetX, dragOffsetY;
    private double rotateCenterX, rotateCenterY;
    private double rotationOffset;
    private boolean hasDragged = false;
    private long lastClickMs = 0;

    private ContextMenu contextMenu;

    private Consumer<AircraftMarkerController> onSelect;
    private Consumer<AircraftMarkerController> onContextMenu;
    private TriConsumer<String, Double, Double> onPositionUpdate;
    private BiConsumer<String, Double> onRotationUpdate;

    public static AircraftMarkerController create(ServiceItem item, MapBase map) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AircraftMarkerController.class.getResource(
                            "/components/map/aircraft-marker.fxml"));
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
            case FUEL              -> "#f59e0b";
            case CATERING          -> "#10b981";
            case GPU               -> "#8b5cf6";
            case LAVATORY          -> "#06b6d4";
            case POTABLE_WATER     -> "#3b82f6";
            case WINDSHIELD_CLEANING -> "#f97316";
            case OIL_SERVICE       -> "#ef4444";
        };
    }


    @FXML
    public void initialize() {  }

    private void init(ServiceItem item, MapBase map) {
        this.item = item;
        this.map  = map;
        readPosition();
        rebuildLabel();
        buildContextMenu();
        wireEvents();
        refreshVisuals();
        relocate();
    }

    public StackPane getNode()  { return root; }
    public ServiceItem getItem() { return item; }
    public String getId()        { return item.getId(); }

    public void setOnSelect(Consumer<AircraftMarkerController> cb)              { onSelect = cb; }
    public void setOnContextMenu(Consumer<AircraftMarkerController> cb)         { onContextMenu = cb; }
    public void setOnPositionUpdate(TriConsumer<String, Double, Double> cb)     { onPositionUpdate = cb; }
    public void setOnRotationUpdate(BiConsumer<String, Double> cb)              { onRotationUpdate = cb; }

    public void setSelected(boolean sel) {
        selected = sel;
        refreshVisuals();
    }

    public void refresh(ServiceItem updated) {
        this.item = updated;
        readPosition();
        rebuildLabel();
        refreshVisuals();
        relocate();
    }

    public void relocate() {
        Platform.runLater(() -> {
            Point2D pt = toPixel(posX, posY);
            if (pt == null) return;
            root.setLayoutX(pt.getX() - root.getWidth()  / 2.0);
            root.setLayoutY(pt.getY() - root.getHeight() / 2.0);
        });
    }

    private void buildContextMenu() {
        contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("marker-context-menu");

        MenuItem r0   = new MenuItem("Rotate — North  (0°)");
        MenuItem r90  = new MenuItem("Rotate — East   (90°)");
        MenuItem r180 = new MenuItem("Rotate — South  (180°)");
        MenuItem r270 = new MenuItem("Rotate — West   (270°)");

        r0.setOnAction(e   -> applyRotation(0));
        r90.setOnAction(e  -> applyRotation(90));
        r180.setOnAction(e -> applyRotation(180));
        r270.setOnAction(e -> applyRotation(270));

        MenuItem resetRotation = new MenuItem("Reset rotation");
        resetRotation.setOnAction(e -> applyRotation(0));

        MenuItem removeFromMap = new MenuItem("Remove from map");
        removeFromMap.getStyleClass().add("menu-item-danger");
        removeFromMap.setOnAction(e -> {
            if (onContextMenu != null) onContextMenu.accept(this);
        });

        contextMenu.getItems().addAll(
                r0, r90, r180, r270,
                new SeparatorMenuItem(),
                resetRotation,
                new SeparatorMenuItem(),
                removeFromMap
        );
    }

    private void applyRotation(double degrees) {
        rotation = norm(degrees);
        innerRotating.setRotate(rotation);
        if (onRotationUpdate != null) onRotationUpdate.accept(getId(), rotation);
    }


    private void refreshVisuals() {
        boolean allDone = Lib.areAllServicesCompleted(item);

        selectionRing.setVisible(selected);
        rotateHandle.setVisible(selected);

        String fill = selected ? "#2563eb" : allDone ? "#16a34a" : "#1e293b";
        planeIcon.setStyle("-fx-fill: " + fill + ";");

        labelContainer.getStyleClass()
                .removeAll("label-default", "label-selected", "label-completed");
        if (selected)       labelContainer.getStyleClass().add("label-selected");
        else if (allDone)   labelContainer.getStyleClass().add("label-completed");
        else                labelContainer.getStyleClass().add("label-default");

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
            contextMenu.hide();

            if (!e.isPrimaryButtonDown()) return;
            hasDragged = false;
            dragMode   = DragMode.MOVE;
            Point2D pt = toPixel(posX, posY);
            if (pt != null) {
                dragOffsetX = e.getSceneX() - pt.getX();
                dragOffsetY = e.getSceneY() - pt.getY();
            }
            root.setCursor(Cursor.CLOSED_HAND);
            e.consume();
        });

        root.setOnMouseDragged(e -> {
            if (dragMode == DragMode.MOVE) {
                hasDragged = true;
                double sx  = e.getSceneX() - dragOffsetX;
                double sy  = e.getSceneY() - dragOffsetY;
                Location loc = toLatLng(sx, sy);
                if (loc != null) {
                    posX = loc.getLatitude();
                    posY = loc.getLongitude();
                    root.setLayoutX(sx - root.getWidth()  / 2.0);
                    root.setLayoutY(sy - root.getHeight() / 2.0);
                    if (onPositionUpdate != null)
                        onPositionUpdate.accept(getId(), posX, posY);
                }
            } else if (dragMode == DragMode.ROTATE) {
                hasDragged = true;
                double angle = angleTo(rotateCenterX, rotateCenterY,
                        e.getSceneX(), e.getSceneY()) + rotationOffset;
                if (e.isShiftDown()) angle = Math.round(angle / 15.0) * 15.0;
                rotation = norm(angle);
                innerRotating.setRotate(rotation);
                if (onRotationUpdate != null)
                    onRotationUpdate.accept(getId(), rotation);
            }
            e.consume();
        });

        root.setOnMouseReleased(e -> {
            if (dragMode == DragMode.MOVE && onPositionUpdate != null)
                onPositionUpdate.accept(getId(), posX, posY);   // final save
            if (dragMode == DragMode.ROTATE && onRotationUpdate != null)
                onRotationUpdate.accept(getId(), rotation);     // final save
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
                    contextMenu.show(root, e.getScreenX(), e.getScreenY());
                }
                default -> { }
            }
            e.consume();
        });

        root.setOnScroll(e -> {
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
        try {
            return map.locationToView(new Location(lat, lng));
        } catch (Exception e) {
            return null;
        }
    }

    private Location toLatLng(double sceneX, double sceneY) {
        try {
            Point2D local = map.sceneToLocal(sceneX, sceneY);
            return map.viewToLocation(local);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private enum DragMode { NONE, MOVE, ROTATE }

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}