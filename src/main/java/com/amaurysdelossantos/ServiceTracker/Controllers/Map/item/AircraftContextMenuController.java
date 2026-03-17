package com.amaurysdelossantos.ServiceTracker.Controllers.Map.item;

import com.amaurysdelossantos.ServiceTracker.Helper.Lib;
import com.amaurysdelossantos.ServiceTracker.Services.ContextMenuService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for the custom aircraft context-menu card.
 *
 * <p>This controller is fully self-contained. Callers only need to pass a
 * {@link ServiceItem}; all action logic is delegated to the injected
 * {@link ContextMenuService}. The pane the card lives on is also sourced from
 * the service — no external wiring required.
 *
 * <p><b>Only valid entry-point:</b>
 * <pre>{@code
 * // From ContextMenuService (the only caller):
 * AircraftContextMenuController card =
 *         AircraftContextMenuController.create(item, contextMenuService);
 * }</pre>
 */
@Component
@Scope("prototype")
public class AircraftContextMenuController {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MMM dd, HH:mm").withZone(ZoneId.systemDefault());

    // ── Spring dependency ──────────────────────────────────────────────────
    @Autowired
    private ContextMenuService contextMenuService;

    // ── FXML nodes ─────────────────────────────────────────────────────────
    @FXML private VBox   root;
    @FXML private HBox   header;
    @FXML private Label  tailLabel;
    @FXML private Label  progressLabel;
    @FXML private Button closeBtn;
    @FXML private HBox   serviceBar;
    @FXML private HBox   timeRow;
    @FXML private Label  arrivalLabel;
    @FXML private Label  departureLabel;
    @FXML private Label  descLabel;
    @FXML private Button viewBtn;
    @FXML private Button editBtn;
    @FXML private Button doneBtn;
    @FXML private Button unplaceBtn;
    @FXML private Button removeBtn;

    // ── State ──────────────────────────────────────────────────────────────
    private ServiceItem item;
    private Pane        mapPane;   // sourced from ContextMenuService

    // Drag state
    private double dragAnchorX, dragAnchorY;
    private double dragNodeX,   dragNodeY;

    // ── Factory ───────────────────────────────────────────────────────────
    /**
     * Creates and initialises a card for {@code item}.
     * The {@code ContextMenuService} is used to source the {@code mapPane}
     * and is stored for button-action delegation.
     *
     * <p>Because this is a {@code @Component @Scope("prototype")} the FXMLLoader
     * must use the Spring application context as its controller factory.
     * We achieve this by injecting the service and having the service inject
     * itself into the controller during {@link #init}.
     */
    public static AircraftContextMenuController create(ServiceItem item,
                                                       ContextMenuService service) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AircraftContextMenuController.class.getResource(
                            "/components/map/item/aircraft-context-menu.fxml"));
            loader.setControllerFactory(clazz -> {
                AircraftContextMenuController ctrl = new AircraftContextMenuController();
                ctrl.contextMenuService = service;
                return ctrl;
            });
            loader.load();
            AircraftContextMenuController ctrl = loader.getController();
            ctrl.init(item, service.getMarkerPane());
            return ctrl;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load aircraft-context-menu.fxml", e);
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public VBox        getNode() { return root; }
    public ServiceItem getItem() { return item; }

    /** Re-populate the card with fresh item data without recreating it. */
    public void refresh(ServiceItem updated) {
        this.item = updated;
        populate();
    }

    /**
     * Positions the card to the right of {@code anchorNode}, flipping left
     * if it would overflow, and clamping to the pane bounds.
     */
    public void positionNear(Node anchorNode, Pane pane) {
        Platform.runLater(() -> {
            double menuW = root.prefWidth(-1);
            double menuH = root.prefHeight(-1);
            double gap   = 12;
            double vpW   = pane.getWidth();
            double vpH   = pane.getHeight();

            Bounds b    = anchorNode.localToParent(anchorNode.getBoundsInLocal());
            double left = b.getMaxX() + gap;
            double top  = b.getCenterY() - menuH / 2.0;

            if (left + menuW > vpW - 20) left = b.getMinX() - menuW - gap;

            left = Math.max(20, Math.min(left, vpW - menuW - 20));
            top  = Math.max(20, Math.min(top,  vpH - menuH - 20));

            root.setLayoutX(left);
            root.setLayoutY(top);
        });
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @FXML
    public void initialize() { /* real init happens in init() after item is set */ }

    private void init(ServiceItem item, Pane mapPane) {
        this.item    = item;
        this.mapPane = mapPane;
        populate();
        wireDrag();
        wireButtons();
    }

    // ── Populate ───────────────────────────────────────────────────────────

    private void populate() {
        Map<ServiceType, Boolean> activeServices = Lib.getActiveServices(item);
        boolean allDone = Lib.areAllServicesCompleted(item);

        // ── Header ──
        tailLabel.setText(item.getTail() != null ? item.getTail() : item.getId());

        int total     = activeServices.size();
        int completed = (int) activeServices.values().stream().filter(Boolean::booleanValue).count();
        int pct       = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        progressLabel.setText(completed + "/" + total + " services (" + pct + "%)");

        header.getStyleClass().remove("acm-header-complete");
        if (allDone && total > 0) header.getStyleClass().add("acm-header-complete");

        // ── Service icon strip ──
        serviceBar.getChildren().clear();
        if (activeServices.isEmpty()) {
            Label empty = new Label("No services assigned");
            empty.getStyleClass().add("acm-service-empty");
            HBox.setHgrow(empty, Priority.ALWAYS);
            serviceBar.getChildren().add(empty);
        } else {
            activeServices.forEach((type, done) -> {
                // Derive hex colour from ServiceType.getPrimaryColor() — same as CardItemController
                java.awt.Color awtColor = type.getPrimaryColor();
                String hex = String.format("#%02x%02x%02x",
                        awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

                String bgAlpha     = done ? "20" : "26";
                String borderAlpha = done ? "66" : "4d";
                String borderStyle = done ? "dashed" : "solid";

                StackPane cell = new StackPane();
                cell.getStyleClass().add("acm-service-cell");
                HBox.setHgrow(cell, Priority.ALWAYS);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMinHeight(48);
                cell.setAlignment(Pos.CENTER);
                cell.setStyle(String.format(
                        "-fx-background-color: %s%s; -fx-border-color: %s%s; " +
                                "-fx-border-width: 1.5; -fx-border-style: %s;",
                        hex, bgAlpha, hex, borderAlpha, borderStyle));

                // Real SVG icon via ServiceType.getImage() — same pattern as CardItemController
                ImageView icon = new ImageView(
                        SwingFXUtils.toFXImage(type.getImage(22), null));
                icon.setFitWidth(22);
                icon.setFitHeight(22);
                icon.setPreserveRatio(true);
                icon.setSmooth(true);
                if (done) icon.setOpacity(0.55);

                cell.getChildren().add(icon);

                // Green diagonal strike-through when completed — mirrors CardItemController
                if (done) {
                    Line strike = new Line(-14, 0, 14, 0);
                    strike.setStroke(Color.rgb(34, 197, 94));
                    strike.setStrokeWidth(2);
                    strike.setRotate(45);
                    strike.setMouseTransparent(true);
                    cell.getChildren().add(strike);
                }

                // Tooltip matching CardItemController style
                Tooltip.install(cell, new Tooltip(
                        done ? "✓ Completed - " + type.getType() : type.getType()));

                cell.setCursor(Cursor.HAND);
                cell.setOnMouseClicked(e -> {
                    contextMenuService.handleServiceClicked(item, type);
                    e.consume();
                });
                cell.setOnMouseEntered(e -> scaleNode(cell, 1.08));
                cell.setOnMouseExited(e  -> scaleNode(cell, 1.00));

                serviceBar.getChildren().add(cell);
            });
        }

        // ── Time row ──
        boolean hasTime = item.getArrival() != null || item.getDeparture() != null;
        timeRow.setVisible(hasTime);
        timeRow.setManaged(hasTime);
        if (hasTime) {
            if (item.getArrival() != null) {
                arrivalLabel.setText("↓ " + DT_FMT.format(item.getArrival()));
                arrivalLabel.setVisible(true);
                arrivalLabel.setManaged(true);
            } else {
                arrivalLabel.setVisible(false);
                arrivalLabel.setManaged(false);
            }
            if (item.getDeparture() != null) {
                departureLabel.setText("↑ " + DT_FMT.format(item.getDeparture()));
                departureLabel.setVisible(true);
                departureLabel.setManaged(true);
            } else {
                departureLabel.setVisible(false);
                departureLabel.setManaged(false);
            }
        }

        // ── Description ──
        boolean hasDesc = item.getDescription() != null && !item.getDescription().isBlank();
        descLabel.setVisible(hasDesc);
        descLabel.setManaged(hasDesc);
        if (hasDesc) descLabel.setText(item.getDescription());

        // ── DONE button — only shown when all services are complete ──
        boolean showDone = total > 0 && completed == total;
        doneBtn.setVisible(showDone);
        doneBtn.setManaged(showDone);
    }

    // ── Drag (header is the handle) ────────────────────────────────────────

    private void wireDrag() {
        header.setOnMousePressed(e -> {
            if (!e.isPrimaryButtonDown()) return;
            dragAnchorX = e.getSceneX();
            dragAnchorY = e.getSceneY();
            dragNodeX   = root.getLayoutX();
            dragNodeY   = root.getLayoutY();
            header.setCursor(Cursor.MOVE);
            e.consume();
        });

        header.setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown()) return;
            double dx   = e.getSceneX() - dragAnchorX;
            double dy   = e.getSceneY() - dragAnchorY;
            double menuW = root.getWidth()  > 0 ? root.getWidth()  : root.prefWidth(-1);
            double menuH = root.getHeight() > 0 ? root.getHeight() : root.prefHeight(-1);
            double vpW   = mapPane.getWidth();
            double vpH   = mapPane.getHeight();
            root.setLayoutX(Math.max(20, Math.min(dragNodeX + dx, vpW - menuW - 20)));
            root.setLayoutY(Math.max(20, Math.min(dragNodeY + dy, vpH - menuH - 20)));
            e.consume();
        });

        header.setOnMouseReleased(e -> {
            header.setCursor(Cursor.DEFAULT);
            e.consume();
        });

        // Block map pan / zoom events from leaking through the card
        root.setOnMousePressed(e -> e.consume());
        root.setOnMouseClicked(e -> e.consume());
        root.setOnScroll(e -> e.consume());
    }

    // ── Button wiring — all actions delegated to ContextMenuService ────────

    private void wireButtons() {
        closeBtn.setOnAction(e   -> contextMenuService.close());
        viewBtn.setOnAction(e    -> contextMenuService.handleView(item));
        editBtn.setOnAction(e    -> contextMenuService.handleEdit(item));
        doneBtn.setOnAction(e    -> contextMenuService.handleDone(item));
        unplaceBtn.setOnAction(e -> contextMenuService.handleUnplace(item));
        removeBtn.setOnAction(e  -> contextMenuService.handleRemove(item));
    }

    // ── Animation helpers ──────────────────────────────────────────────────

    private static void scaleNode(Node n, double target) {
        new Timeline(new KeyFrame(Duration.millis(120),
                new KeyValue(n.scaleXProperty(), target),
                new KeyValue(n.scaleYProperty(), target))).play();
    }
}