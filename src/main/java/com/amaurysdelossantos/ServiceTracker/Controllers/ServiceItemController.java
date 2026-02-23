//package com.amaurysdelossantos.ServiceTracker.Controllers;
//
//import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemDeleteController;
//import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemEditController;
//import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemInfoController;
//import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
//import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
//import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
//import javafx.animation.Interpolator;
//import javafx.animation.KeyFrame;
//import javafx.animation.KeyValue;
//import javafx.animation.Timeline;
//import javafx.application.Platform;
//import javafx.embed.swing.SwingFXUtils;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.control.Label;
//import javafx.scene.control.Tooltip;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.layout.Region;
//import javafx.scene.layout.StackPane;
//import javafx.scene.shape.Line;
//import javafx.stage.Modality;
//import javafx.stage.Stage;
//import javafx.stage.StageStyle;
//import javafx.util.Duration;
//import lombok.Setter;
//
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//
//public class ServiceItemController {
//
//    // Card view nodes
//    @FXML
//    private HBox headerBox;
//    @FXML
//    private HBox footerBox;
//    @FXML
//    private Label tailLabel;
//    @FXML
//    private HBox actionButtonsBox;
//    @FXML
//    private HBox serviceIconsBox;
//    @FXML
//    private Label arrivalLabel;
//    @FXML
//    private Label departureLabel;
//    @FXML
//    private Label descriptionLabel;
//
//    // List view nodes
//    @FXML
//    private Region accentBar;
//    @FXML
//    private Button doneButton;
//    @FXML
//    private Button undoButton;
//    @FXML
//    private Button editButton;
//    @FXML
//    private Button deleteButton;
//    @FXML
//    private HBox listRow;
//
//    private ServiceItem serviceItem;
//
//    /**
//     * Injected so modals can save / refresh
//     */
//    @Setter
//    private ServiceItemService serviceItemService;
//
//    /**
//     * Called when any modal makes a change that requires the parent list to reload
//     */
//    @Setter
//    private Runnable refreshCallback;
//
//    private static final DateTimeFormatter FORMATTER =
//            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());
//
//
//    private static final Duration TRANSITION_DURATION = Duration.millis(150);
//    private static final Interpolator EASE = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
//
//    private boolean isCardView() {
//        return headerBox != null;
//    }
//
//    private boolean isListView() {
//        return accentBar != null;
//    }
//
//    public void setServiceItem(ServiceItem item) {
//        this.serviceItem = item;
//        populate();
//    }
//
//    @FXML
//    public void initialize() {
//        if (serviceItem != null) populate();
//    }
//
//    private void populate() {
//        if (isCardView()) populateCard();
//        else if (isListView()) populateList();
//
//    }
//
//    // ─────────────────────────────────────────────────────────
//    // Card view
//    // ─────────────────────────────────────────────────────────
//
//    private void populateCard() {
//        boolean isDone = serviceItem.getCompletedAt() != null;
//        String headerBg = isDone ? "#166534" : "#0f172a";
//        headerBox.setStyle("-fx-background-color: " + headerBg + "; -fx-background-radius: 6 6 0 0;");
//
//        headerBox.setOnMouseClicked(e -> openInfoModal());
//        tailLabel.setText(serviceItem.getTail() != null ? serviceItem.getTail() : "—");
//
//        actionButtonsBox.getChildren().clear();
//        if (areAllServicesCompleted() && !isDone)
//            actionButtonsBox.getChildren().add(makeActionButton("DONE", "#16a34a", "#15803d", "0", this::openDone));
//        if (isDone)
//            actionButtonsBox.getChildren().add(makeActionButton("UNDO", "#ca8a04", "#a16207", "0", this::openUndo));
//
//        // EDIT button
//        actionButtonsBox.getChildren().add(makeActionButton("EDIT", "#2563eb", "#1d4ed8", "0", this::openEditModal));
//        // DEL button
//        actionButtonsBox.getChildren().add(makeActionButton("DEL", "#dc2626", "#b91c1c", "0 6 0 0", this::openDeleteModal));
//
//        populateServiceIcons(56.0);
//
//        boolean hasArrival = serviceItem.getArrival() != null;
//        boolean hasDeparture = serviceItem.getDeparture() != null;
//        boolean hasDescription = serviceItem.getDescription() != null && !serviceItem.getDescription().isBlank();
//        boolean hasFooter = hasArrival || hasDeparture || hasDescription;
//
//        footerBox.setVisible(hasFooter);
//        footerBox.setManaged(hasFooter);
//
//        arrivalLabel.setText(hasArrival ? "↓ " + FORMATTER.format(serviceItem.getArrival()) : "");
//        departureLabel.setText(hasDeparture ? "↑ " + FORMATTER.format(serviceItem.getDeparture()) : "");
//        descriptionLabel.setText(hasDescription ? serviceItem.getDescription() : "");
//    }
//
//
//    // ─────────────────────────────────────────────────────────
//    // List view
//    // ─────────────────────────────────────────────────────────
//
//    private void populateList() {
//        boolean isDone = serviceItem.getCompletedAt() != null;
//        accentBar.setStyle("-fx-background-color: " + (isDone ? "#166534" : "#0f172a") + ";");
//        tailLabel.setText(serviceItem.getTail() != null ? serviceItem.getTail() : "—");
//        tailLabel.setOnMouseClicked(e -> openInfoModal());
//
//        arrivalLabel.setText(serviceItem.getArrival() != null ? "↓ " + FORMATTER.format(serviceItem.getArrival()) : "");
//        departureLabel.setText(serviceItem.getDeparture() != null ? "↑ " + FORMATTER.format(serviceItem.getDeparture()) : "");
//        populateServiceIcons(40.0);
//
//        boolean showDone = areAllServicesCompleted() && !isDone;
//        boolean showUndo = isDone;
//        doneButton.setVisible(showDone);
//        doneButton.setManaged(showDone);
//        undoButton.setVisible(showUndo);
//        undoButton.setManaged(showUndo);
//        editButton.setVisible(true);
//        deleteButton.setVisible(true);
//
//        attachButtonAnimation(doneButton, "#16a34a", "#15803d");
//        attachButtonAnimation(undoButton, "#ca8a04", "#a16207");
//        attachButtonAnimation(editButton, "#2563eb", "#1d4ed8");
//        attachButtonAnimation(deleteButton, "#dc2626", "#b91c1c");
//
//        doneButton.setOnAction(e -> openDone());
//        undoButton.setOnAction(e -> openUndo());
//        editButton.setOnAction(e -> openEditModal());
//        deleteButton.setOnAction(e -> openDeleteModal());
//    }
//
//    // ─────────────────────────────────────────────────────────
//    // Modal launchers
//    // ─────────────────────────────────────────────────────────
//
//    private void openInfoModal() {
//        openModal("/components/ItemInteraction/item-info-modal.fxml", "Service Item – " + serviceItem.getTail(),
//                600, 620, new ControllerConfigurator() {
//                    public void configure(FXMLLoader loader) {
//                        ItemInfoController ctrl = loader.getController();
//                        ctrl.setItem(serviceItem);
//                        ctrl.setServiceItemService(serviceItemService);
//                        ctrl.setOnEditCallback(ServiceItemController.this::openEditModal);
//                        ctrl.setOnEditServiceCallback(ServiceItemController.this::openEditModalWithDestination);
//                    }
//
//                    public void populate(FXMLLoader loader) {
//                        ((ItemInfoController) loader.getController()).populate();
//                    }
//                });
//    }
//
//    private void openEditModal() {
//        openModal("/components/ItemInteraction/item-edit-modal.fxml", "Edit – " + serviceItem.getTail(),
//                820, 640, new ControllerConfigurator() {
//                    public void configure(FXMLLoader loader) {
/// /                        ItemEditController ctrl = loader.getController();
/// /                        ctrl.setItem(serviceItem);
/// /                        ctrl.setServiceItemService(serviceItemService);
/// /                        ctrl.setOnSavedCallback(() -> {
/// /                            if (refreshCallback != null) refreshCallback.run();
/// /                        });
//                    }
//
//                    public void populate(FXMLLoader loader) {
//                        ((ItemEditController) loader.getController()).populate();
//                    }
//                });
//    }
//
//    private void openEditModalWithDestination(ServiceType targetService) {
//        openModal("/components/ItemInteraction/item-edit-modal.fxml", "Edit – " + serviceItem.getTail(),
//                820, 640, new ControllerConfigurator() {
//                    public void configure(FXMLLoader loader) {
//                        ItemEditController ctrl = loader.getController();
//                        ctrl.setItem(serviceItem);
//                        ctrl.setServiceItemService(serviceItemService);
//                        ctrl.setInitialService(targetService);          // <-- new
//                        ctrl.setOnSavedCallback(() -> {
//                            if (refreshCallback != null) refreshCallback.run();
//                        });
//                    }
//
//                    public void populate(FXMLLoader loader) {
//                        ((ItemEditController) loader.getController()).populate();
//                    }
//                });
//    }
//
//    private void openDeleteModal() {
//        openModal("/components/ItemInteraction/item-delete-modal.fxml", "Delete – " + serviceItem.getTail(),
//                620, 520, new ControllerConfigurator() {
//                    public void configure(FXMLLoader loader) {
//                        ItemDeleteController ctrl = loader.getController();
//                        ctrl.setItem(serviceItem);
//                        ctrl.setServiceItemService(serviceItemService);
//                        ctrl.setOnDeletedCallback(() -> {
//                            if (refreshCallback != null) refreshCallback.run();
//                        });
//                    }
//
//                    public void populate(FXMLLoader loader) {
//                        ((ItemDeleteController) loader.getController()).populate();
//                    }
//                });
//    }
//
//    private void openDone() {
//        // Mark item complete and all services — then refresh
//        java.time.Instant now = java.time.Instant.now();
//        serviceItem.setCompletedAt(now);
//        serviceItem.setUpdatedAt(now);
//        if (serviceItemService != null) {
//            Thread t = new Thread(() -> {
//                try {
//                    serviceItemService.saveService(serviceItem);
//                    Platform.runLater(() -> {
//                        if (refreshCallback != null) refreshCallback.run();
//                    });
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//            t.setDaemon(true);
//            t.start();
//        }
//    }
//
//    private void openUndo() {
//        serviceItem.setCompletedAt(null);
//        serviceItem.setUpdatedAt(java.time.Instant.now());
//        if (serviceItemService != null) {
//            Thread t = new Thread(() -> {
//                try {
//                    serviceItemService.saveService(serviceItem);
//                    Platform.runLater(() -> {
//                        if (refreshCallback != null) refreshCallback.run();
//                    });
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//            t.setDaemon(true);
//            t.start();
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────
//    // Generic modal opener
//    // ─────────────────────────────────────────────────────────
//
//    /**
//     * Two-phase configurator: configure() injects dependencies, populate() hydrates the UI
//     */
//    interface ControllerConfigurator {
//        /**
//         * Inject services, callbacks, and item reference — called before show()
//         */
//        void configure(FXMLLoader loader) throws Exception;
//
//        /**
//         * Fill all UI nodes with data — called after the window is shown and laid out
//         */
//        void populate(FXMLLoader loader) throws Exception;
//    }
//
//    private void openModal(String fxmlPath, String title,
//                           double prefW, double prefH,
//                           ControllerConfigurator configurator) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
//            loader.setControllerFactory(cls -> {
//                try {
//                    return cls.getDeclaredConstructor().newInstance();
//                } catch (Exception ex) {
//                    throw new RuntimeException(ex);
//                }
//            });
//
//            Parent root = loader.load();
//
//            Stage modal = new Stage(StageStyle.DECORATED);
//            modal.setTitle(title);
//            modal.initModality(Modality.APPLICATION_MODAL);
//
//            // Use the main app window as owner, not any modal that may be mid-close
//            javafx.scene.Node anchor = headerBox != null ? headerBox : accentBar;
//            if (anchor != null && anchor.getScene() != null
//                    && anchor.getScene().getWindow() != null
//                    && anchor.getScene().getWindow().isShowing()) {
//                modal.initOwner(anchor.getScene().getWindow());
//            }
//
//            Scene scene = new Scene(root, prefW, prefH);
//            modal.setScene(scene);
//            modal.setResizable(false);
//
//            // Step 1: inject dependencies (no populate yet)
//            configurator.configure(loader);
//
//            // Step 2: show the window so the JavaFX layout engine runs its
//            // first full pass — then populate on the very next pulse.
//            modal.setOnShown(e -> Platform.runLater(() -> {
//                try {
//                    configurator.populate(loader);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }));
//
//            // Exit the nested event loop when the window closes
//            modal.setOnHidden(e -> Platform.exitNestedEventLoop(modal, null));
//
//            modal.show();
//            modal.toFront();
//
//            // Block the FX thread here (replaces showAndWait) so callers behave
//            // identically — returns when setOnHidden fires above.
//            Platform.enterNestedEventLoop(modal);
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────
//    // Service icons
//    // ─────────────────────────────────────────────────────────
//
//    private void populateServiceIcons(double tileHeight) {
//        serviceIconsBox.getChildren().clear();
//        boolean any = false;
//
//        for (ServiceType st : ServiceType.values()) {
//            if (!isServiceActive(st)) continue;
//            any = true;
//            boolean completed = isServiceCompleted(st);
//            java.awt.Color awtColor = st.getPrimaryColor();
//
//            StackPane tile = buildServiceTile(st, awtColor, completed, tileHeight);
//            tile.setOnMouseClicked(e -> openEditModalWithDestination(st));
//            Tooltip.install(tile, new Tooltip(
//                    completed ? "✓ " + st.getType() + " — completed" : st.getType()));
//            serviceIconsBox.getChildren().add(tile);
//        }
//
//        if (!any) {
//            Label none = new Label("No services");
//            none.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px; -fx-padding: 0 12;");
//            serviceIconsBox.getChildren().add(none);
//        }
//    }
//
//    private StackPane buildServiceTile(ServiceType st, java.awt.Color awtColor,
//                                       boolean completed, double tileHeight) {
//        StackPane tile = new StackPane();
//        HBox.setHgrow(tile, Priority.ALWAYS);
//        tile.setMinHeight(tileHeight);
//        tile.setMaxHeight(tileHeight);
//
//        String bgAlpha = hexWithAlpha(awtColor, completed ? 0.06f : 0.10f);
//        String borderColor = hexWithAlpha(awtColor, completed ? 0.40f : 0.33f);
//        tile.setStyle("-fx-background-color:" + bgAlpha + "; -fx-border-color:" + borderColor
//                + "; -fx-border-width:1.5; -fx-border-style:" + (completed ? "dashed" : "solid") + ";");
//
//        double iconSize = Math.min(22, tileHeight * 0.4);
//        int renderSize = (int) (iconSize * 2);
//
//        BufferedImage img;
//        try {
//            img = st.getImage(renderSize);
//            img = tintImage(img, awtColor);
//        } catch (Exception ex) {
//            img = new BufferedImage(renderSize, renderSize, BufferedImage.TYPE_INT_ARGB);
//            Graphics2D g2 = img.createGraphics();
//            g2.setColor(awtColor);
//            g2.fillRect(0, 0, renderSize, renderSize);
//            g2.dispose();
//        }
//
//        ImageView iv = new ImageView(SwingFXUtils.toFXImage(img, null));
//        iv.setFitWidth(iconSize);
//        iv.setFitHeight(iconSize);
//        iv.setPreserveRatio(true);
//        iv.setSmooth(true);
//        iv.setOpacity(completed ? 0.5 : 1.0);
//        tile.getChildren().add(iv);
//
//        if (completed) {
//            double ll = tileHeight * 0.6;
//            Line strike = new Line(-ll / 2, 0, ll / 2, 0);
//            strike.setStyle("-fx-stroke: #22c55e; -fx-stroke-width: 2;");
//            strike.setRotate(45);
//            tile.getChildren().add(strike);
//        }
//
//        Timeline[] hover = {null};
//        tile.setOnMouseEntered(e -> {
//            if (hover[0] != null) hover[0].stop();
//            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
//                    new KeyValue(tile.scaleXProperty(), 1.08, EASE),
//                    new KeyValue(tile.scaleYProperty(), 1.08, EASE)));
//            hover[0].play();
//        });
//        tile.setOnMouseExited(e -> {
//            if (hover[0] != null) hover[0].stop();
//            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
//                    new KeyValue(tile.scaleXProperty(), 1.0, EASE),
//                    new KeyValue(tile.scaleYProperty(), 1.0, EASE),
//                    new KeyValue(tile.opacityProperty(), completed ? 0.7 : 1.0, EASE)));
//            hover[0].play();
//        });
//
//        // Click on service tile → open info modal
//        tile.setOnMouseClicked(e -> openInfoModal());
//
//        return tile;
//    }
//
//    // ─────────────────────────────────────────────────────────
//    // Button builders
//    // ─────────────────────────────────────────────────────────
//
//    private Button makeActionButton(String text, String bg, String hoverBg, String radius, Runnable action) {
//        Button btn = new Button(text);
//        String base = "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:10px;"
//                + "-fx-background-radius:" + radius + "; -fx-cursor:hand;"
//                + "-fx-min-height:48; -fx-min-width:42; -fx-padding:0 12;";
//        btn.setStyle("-fx-background-color:" + bg + "; " + base);
//
//        Timeline[] hover = {null};
//        btn.setOnMouseEntered(e -> {
//            if (hover[0] != null) hover[0].stop();
//            btn.setStyle("-fx-background-color:" + hoverBg + "; " + base);
//            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
//                    new KeyValue(btn.scaleXProperty(), 1.04, EASE),
//                    new KeyValue(btn.scaleYProperty(), 1.04, EASE)));
//            hover[0].play();
//        });
//        btn.setOnMouseExited(e -> {
//            if (hover[0] != null) hover[0].stop();
//            btn.setStyle("-fx-background-color:" + bg + "; " + base);
//            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
//                    new KeyValue(btn.scaleXProperty(), 1.0, EASE),
//                    new KeyValue(btn.scaleYProperty(), 1.0, EASE)));
//            hover[0].play();
//        });
//        btn.setOnAction(e -> action.run());
//        return btn;
//    }
//
//    private void attachButtonAnimation(Button btn, String bg, String hoverBg) {
//        String base = btn.getStyle();
//        Timeline[] hover = {null};
//        btn.setOnMouseEntered(e -> {
//            if (hover[0] != null) hover[0].stop();
//            btn.setStyle(base + "-fx-background-color:" + hoverBg + ";");
//            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
//                    new KeyValue(btn.scaleXProperty(), 1.04, EASE),
//                    new KeyValue(btn.scaleYProperty(), 1.04, EASE)));
//            hover[0].play();
//        });
//        btn.setOnMouseExited(e -> {
//            if (hover[0] != null) hover[0].stop();
//            btn.setStyle(base + "-fx-background-color:" + bg + ";");
//            hover[0] = new Timeline(new KeyFrame(TRANSITION_DURATION,
//                    new KeyValue(btn.scaleXProperty(), 1.0, EASE),
//                    new KeyValue(btn.scaleYProperty(), 1.0, EASE)));
//            hover[0].play();
//        });
//    }
//
//    // ─────────────────────────────────────────────────────────
//    // Service state checks
//    // ─────────────────────────────────────────────────────────
//
//    private boolean isServiceActive(ServiceType t) {
//        return switch (t) {
//            case FUEL -> serviceItem.getFuel() != null;
//            case CATERING -> serviceItem.getCatering() != null && !serviceItem.getCatering().isEmpty();
//            case GPU -> serviceItem.getGpu() != null;
//            case LAVATORY -> serviceItem.getLavatory() != null;
//            case POTABLE_WATER -> serviceItem.getPotableWater() != null;
//            case WINDSHIELD_CLEANING -> serviceItem.getWindshieldCleaning() != null;
//            case OIL_SERVICE -> serviceItem.getOilService() != null;
//        };
//    }
//
//    private boolean isServiceCompleted(ServiceType t) {
//        return switch (t) {
//            case FUEL -> serviceItem.getFuel() != null && serviceItem.getFuel().getCompletedAt() != null;
//            case CATERING -> serviceItem.getCatering() != null && !serviceItem.getCatering().isEmpty()
//                    && serviceItem.getCatering().stream().allMatch(c -> c.getCompletedAt() != null);
//            case GPU -> serviceItem.getGpu() != null && serviceItem.getGpu().getCompletedAt() != null;
//            case LAVATORY -> serviceItem.getLavatory() != null && serviceItem.getLavatory().getCompletedAt() != null;
//            case POTABLE_WATER ->
//                    serviceItem.getPotableWater() != null && serviceItem.getPotableWater().getCompletedAt() != null;
//            case WINDSHIELD_CLEANING ->
//                    serviceItem.getWindshieldCleaning() != null && serviceItem.getWindshieldCleaning().getCompletedAt() != null;
//            case OIL_SERVICE ->
//                    serviceItem.getOilService() != null && serviceItem.getOilService().getCompletedAt() != null;
//        };
//    }
//
//    private boolean areAllServicesCompleted() {
//        boolean anyActive = false;
//        for (ServiceType t : ServiceType.values()) {
//            if (isServiceActive(t)) {
//                anyActive = true;
//                if (!isServiceCompleted(t)) return false;
//            }
//        }
//        return anyActive;
//    }
//
//    // ─────────────────────────────────────────────────────────
//    // Color helpers
//    // ─────────────────────────────────────────────────────────
//
//    private String hexWithAlpha(java.awt.Color c, float alpha) {
//        return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), Math.round(alpha * 255));
//    }
//
//    private BufferedImage tintImage(BufferedImage src, java.awt.Color tint) {
//        int w = src.getWidth(), h = src.getHeight();
//        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//        for (int y = 0; y < h; y++)
//            for (int x = 0; x < w; x++) {
//                int argb = src.getRGB(x, y);
//                int a = (argb >> 24) & 0xff;
//                if (a > 0)
//                    out.setRGB(x, y, (a << 24) | (tint.getRed() << 16) | (tint.getGreen() << 8) | tint.getBlue());
//            }
//        return out;
//    }
//}