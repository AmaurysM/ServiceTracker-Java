package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Helper.WindowHandler;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Singleton service that owns all context-menu state for the map view.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Holds the {@code markerPane} reference set once by {@link
 *       com.amaurysdelossantos.ServiceTracker.Controllers.Map.MapViewController}</li>
 *   <li>Tracks the single open {@code AircraftContextMenuController} card</li>
 *   <li>Exposes {@link #show(ServiceItem, Node)} — the only call markers need to make</li>
 *   <li>Implements every action handler (VIEW, EDIT, DONE, UNPLACE, REMOVE, service icon)</li>
 * </ul>
 *
 * <p>Markers and the context-menu controller never reference each other directly;
 * everything is mediated through this service.
 */
@Service
public class ContextMenuService {

    // ── Dependencies ───────────────────────────────────────────────────────

    private final MongoTemplate mongoTemplate;
    private final MapService    mapService;

    @Autowired
    public ContextMenuService(MongoTemplate mongoTemplate, MapService mapService) {
        this.mongoTemplate = mongoTemplate;
        this.mapService    = mapService;
    }

    // ── State ──────────────────────────────────────────────────────────────

    /** The pane that cards are added to. Set once by MapViewController on init. */
    private Pane markerPane;

    /** The currently visible card, or {@code null} if none is open. */
    private Object openCard; // typed as Object to avoid circular import; cast internally

    // ── MapViewController registration ────────────────────────────────────

    /**
     * Must be called by {@code MapViewController} during its {@code initialize()}
     * so this service knows which pane to attach cards to.
     */
    public void registerMarkerPane(Pane pane) {
        this.markerPane = pane;
    }

    public Pane getMarkerPane() {
        return markerPane;
    }

    // ── Open / close ───────────────────────────────────────────────────────

    /**
     * Opens a context-menu card for {@code item}, positioned near {@code anchorNode}.
     * Any previously open card is closed first.
     *
     * <p>Called by {@code AircraftMarkerController} on right-click — the only
     * external call site needed.
     */
    public void show(ServiceItem item, Node anchorNode) {
        if (markerPane == null) return;

        close(); // dismiss any existing card

        // Lazily import to avoid circular Spring wiring — the controller is
        // prototype-scoped and loaded via FXMLLoader, not the Spring context.
        com.amaurysdelossantos.ServiceTracker.Controllers.Map.item.AircraftContextMenuController card =
                com.amaurysdelossantos.ServiceTracker.Controllers.Map.item.AircraftContextMenuController
                        .create(item, this);

        openCard = card;
        markerPane.getChildren().add(card.getNode());
        card.positionNear(anchorNode, markerPane);
    }

    /** Removes the open card from the pane and clears the reference. */
    public void close() {
        if (openCard == null) return;
        var card = (com.amaurysdelossantos.ServiceTracker.Controllers.Map.item
                .AircraftContextMenuController) openCard;
        Platform.runLater(() -> markerPane.getChildren().remove(card.getNode()));
        openCard = null;
    }

    /** Refreshes the open card if it is showing data for {@code itemId}. */
    public void refreshIfOpen(String itemId, ServiceItem updated) {
        if (openCard == null) return;
        var card = (com.amaurysdelossantos.ServiceTracker.Controllers.Map.item
                .AircraftContextMenuController) openCard;
        if (card.getItem().getId().equals(itemId)) {
            Platform.runLater(() -> card.refresh(updated));
        }
    }

    /**
     * Returns {@code true} when the given scene coordinates fall inside the
     * currently open card.  Used by the {@code fxMap} event filter so that
     * clicks on the card are not mistakenly treated as bare-map clicks.
     */
    public boolean isClickInsideOpenCard(double sceneX, double sceneY) {
        if (openCard == null) return false;
        var card = (com.amaurysdelossantos.ServiceTracker.Controllers.Map.item
                .AircraftContextMenuController) openCard;
        Bounds bounds = card.getNode().localToScene(card.getNode().getBoundsInLocal());
        return bounds.contains(sceneX, sceneY);
    }

    // ── Action handlers (called by the card's buttons) ─────────────────────

    /** VIEW button — opens the info modal. */
    public void handleView(ServiceItem item) {
        WindowHandler.handleInfo(item);
    }

    /** EDIT button — opens the edit modal with no service pre-selected. */
    public void handleEdit(ServiceItem item) {
        WindowHandler.handleEdit(item);
    }

    /**
     * DONE button — toggles {@code completedAt}, persists, then refreshes
     * the card and marker with the updated item.
     */
    public void handleDone(ServiceItem item) {
        WindowHandler.handleToggleComplete(item, () -> {
            // Pull the freshest copy from the shared observable list
            mapService.getItems().stream()
                    .filter(i -> i.getId().equals(item.getId()))
                    .findFirst()
                    .ifPresent(fresh -> refreshIfOpen(fresh.getId(), fresh));
        });
    }

    /**
     * UNPLACE button — strips the map-position metadata so the aircraft
     * disappears from the map canvas but is NOT deleted from the database.
     */
    public void handleUnplace(ServiceItem item) {
        close();

        // 1. Update in-memory shared list
        mapService.getItems().stream()
                .filter(i -> i.getId().equals(item.getId()))
                .findFirst()
                .ifPresent(i -> {
                    if (i.getMetadata() != null) i.getMetadata().remove("mapPosition");
                });

        // 2. Persist
        Thread t = new Thread(() ->
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(item.getId())),
                        new Update().unset("metadata.mapPosition"),
                        ServiceItem.class));
        t.setDaemon(true);
        t.start();
    }

    /**
     * REMOVE button — opens the delete-confirmation modal.
     * The card is closed first so it does not linger behind the modal.
     */
    public void handleRemove(ServiceItem item) {
        close();
        WindowHandler.handleDelete(item);
    }

    /**
     * Service-icon click — opens the edit modal pre-selected to {@code serviceType}.
     */
    public void handleServiceClicked(ServiceItem item, ServiceType serviceType) {
        WindowHandler.handleEdit(serviceType, item);
    }
}