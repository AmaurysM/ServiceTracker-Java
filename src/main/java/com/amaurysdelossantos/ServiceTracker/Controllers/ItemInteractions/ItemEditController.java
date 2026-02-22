package com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions;

import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.*;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorInput;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ItemEditController {

    // ── FXML ────────────────────────────────────────────────────────────────────
    @FXML private StackPane  rootPane;
    @FXML private Label      tailPreviewLabel;
    @FXML private HBox       tabBar;
    @FXML private VBox       basicPane;
    @FXML private StackPane  servicePane;
    @FXML private VBox       serviceFormBox;

    // Basic info fields
    @FXML private TextField  tailField;
    @FXML private DatePicker arrivalDatePicker;
    @FXML private TextField  arrivalTimeField;
    @FXML private DatePicker departureDatePicker;
    @FXML private TextField  departureTimeField;
    @FXML private TextArea   descriptionField;

    // ── Dependencies ───────────────────────────────────────────────────────────
    @Setter private ServiceItemService serviceItemService;
    @Setter private Runnable onSavedCallback;

    private ServiceItem item;

    @Setter
    private ServiceType initialService = null;

    // ── State ──────────────────────────────────────────────────────────────────
    private enum ActiveTab { BASIC, SERVICE }
    private ActiveTab currentTab = ActiveTab.BASIC;
    private ServiceType activeService = null;

    // Local copies of service data (edited in place, committed on save)
    private Fuel            fuel;
    private List<Catering>  catering;
    private GPU             gpu;
    private Lavatory        lavatory;
    private PotableWater    potableWater;
    private WindshieldCleaning windshieldCleaning;
    private OilService      oilService;

    private static final String NAVY  = "#1e3a5f";
    private static final String BLUE  = "#1d4ed8";
    private static final String GREY  = "#f8f9fb";
    private static final String BORD  = "#dde3eb";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Public API ──────────────────────────────────────────────────────────────

    public void setItem(ServiceItem item) {
        this.item = item;
        copyServiceState();
    }

    @FXML
    public void initialize() { }

    /** Called after setItem() — wires up all UI from item state */
    public void populate() {
        if (item == null) return;

        // Pre-fill basic fields
        tailField.setText(item.getTail() != null ? item.getTail() : "");
        tailPreviewLabel.setText(item.getTail() != null ? "✈  " + item.getTail() : "Tail: —");
        tailField.textProperty().addListener((obs, o, n) ->
                tailPreviewLabel.setText(n.isBlank() ? "Tail: —" : "✈  " + n.trim().toUpperCase()));

        if (item.getArrival() != null) {
            LocalDate ld = item.getArrival().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime lt = item.getArrival().atZone(ZoneId.systemDefault()).toLocalTime();
            arrivalDatePicker.setValue(ld);
            arrivalTimeField.setText(lt.format(TIME_FMT));
        }
        if (item.getDeparture() != null) {
            LocalDate ld = item.getDeparture().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime lt = item.getDeparture().atZone(ZoneId.systemDefault()).toLocalTime();
            departureDatePicker.setValue(ld);
            departureTimeField.setText(lt.format(TIME_FMT));
        }
        if (item.getDescription() != null) descriptionField.setText(item.getDescription());

        buildTabBar();
        if (initialService != null && isActive(initialService)) {
            showServiceTab(initialService);
        } else {
            showBasicTab();
        }
    }

    // ── Tab bar ─────────────────────────────────────────────────────────────────

    private void buildTabBar() {
        tabBar.getChildren().clear();

        // Basic info tab (purple tint like Next.js)
        tabBar.getChildren().add(buildBasicTab());

        // One tab per active service
        for (ServiceType st : ServiceType.values()) {
            if (!isActive(st)) continue;
            tabBar.getChildren().add(buildServiceTab(st));
        }
    }

    private HBox buildBasicTab() {
        Label lbl = new Label("ℹ  Basic Info");
        lbl.setStyle("-fx-font-size:11px; -fx-font-weight:700;");

        HBox tab = new HBox(lbl);
        tab.setAlignment(Pos.CENTER);
        tab.setPadding(new Insets(0, 16, 0, 16));
        tab.setPrefHeight(42);
        tab.setStyle(activeTabStyle("#7c3aed", "#ede9fe", true));
        tab.setOnMouseClicked(e -> showBasicTab());
        tab.setUserData("basic");
        HBox.setHgrow(tab, Priority.NEVER);
        return tab;
    }

    private HBox buildServiceTab(ServiceType st) {
        java.awt.Color awtColor = st.getPrimaryColor();
        Color fxColor = Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

        ImageView iv = makeIcon(st, 14, awtColor);

        boolean done = isCompleted(st);
        Label nameLbl = new Label(label(st));
        nameLbl.setStyle("-fx-font-size:11px; -fx-font-weight:700;");

        HBox content = new HBox(6, iv, nameLbl);
        content.setAlignment(Pos.CENTER);

        // Green dot indicator if completed
        if (done) {
            Region dot = new Region();
            dot.setPrefSize(6, 6); dot.setMinSize(6, 6); dot.setMaxSize(6, 6);
            dot.setStyle("-fx-background-color:#16a34a; -fx-background-radius:3;");
            content.getChildren().add(dot);
        }

        HBox tab = new HBox(content);
        tab.setAlignment(Pos.CENTER);
        tab.setPadding(new Insets(0, 14, 0, 14));
        tab.setPrefHeight(42);
        tab.setMaxWidth(Double.MAX_VALUE);
        tab.setStyle(activeTabStyle(toHex(fxColor), alphaHex(fxColor, 0.10), false));
        tab.setUserData(st);
        tab.setOnMouseClicked(e -> showServiceTab(st));
        HBox.setHgrow(tab, Priority.ALWAYS);
        return tab;
    }

    private void refreshTabStyles() {
        for (Node n : tabBar.getChildren()) {
            if (!(n instanceof HBox tab)) continue;

            if ("basic".equals(tab.getUserData())) {
                boolean active = currentTab == ActiveTab.BASIC;
                tab.setStyle(activeTabStyle("#7c3aed", "#ede9fe", active));
                if (tab.getChildren().get(0) instanceof Label l)
                    l.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:"
                            + (active ? "white" : "#7c3aed") + ";");

            } else if (tab.getUserData() instanceof ServiceType st) {
                boolean active = currentTab == ActiveTab.SERVICE && activeService == st;
                java.awt.Color awtColor = st.getPrimaryColor();
                Color fxColor = Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                tab.setStyle(activeTabStyle(toHex(fxColor), alphaHex(fxColor, 0.10), active));

                if (!tab.getChildren().isEmpty() && tab.getChildren().get(0) instanceof HBox inner) {
                    if (!inner.getChildren().isEmpty() && inner.getChildren().get(0) instanceof ImageView iv)
                        tintIcon(iv, active ? Color.WHITE : fxColor, 14);
                    if (inner.getChildren().size() > 1 && inner.getChildren().get(1) instanceof Label l)
                        l.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:"
                                + (active ? "white" : toHex(fxColor)) + ";");
                }
            }
        }
    }

    private String activeTabStyle(String solidColor, String lightBg, boolean active) {
        if (active) {
            return "-fx-background-color:" + solidColor + "; -fx-cursor:default;";
        } else {
            return "-fx-background-color:" + lightBg + "; -fx-cursor:hand;"
                    + "-fx-border-color:transparent " + BORD + " transparent transparent;"
                    + "-fx-border-width:0 1 0 0;";
        }
    }

    // ── Tab switching ────────────────────────────────────────────────────────────

    private void showBasicTab() {
        currentTab = ActiveTab.BASIC;
        activeService = null;
        basicPane.setVisible(true);  basicPane.setManaged(true);
        servicePane.setVisible(false); servicePane.setManaged(false);
        refreshTabStyles();
        fadeIn(basicPane);
    }

    private void showServiceTab(ServiceType st) {
        currentTab = ActiveTab.SERVICE;
        activeService = st;
        basicPane.setVisible(false);  basicPane.setManaged(false);
        servicePane.setVisible(true); servicePane.setManaged(true);

        buildServiceForm(st);
        refreshTabStyles();
        fadeIn(servicePane);
    }

    // ── Service form builder ─────────────────────────────────────────────────────

    private void buildServiceForm(ServiceType st) {
        serviceFormBox.getChildren().clear();

        // Colored header band
        serviceFormBox.getChildren().add(buildServiceHeader(st));
        serviceFormBox.getChildren().add(spacer(8));

        // Action buttons row: Mark Complete / Mark Incomplete + Remove Service
        boolean done = isCompleted(st);
        Button toggleBtn = new Button(done ? "Mark Incomplete" : "Mark Complete");
        toggleBtn.getStyleClass().add(done ? "btn-mark-incomplete" : "btn-mark-complete");
        toggleBtn.setOnAction(e -> {
            toggleCompletion(st);
            buildTabBar();  // refresh dot indicator
            buildServiceForm(st);
        });

        Button removeBtn = new Button("Remove Service");
        removeBtn.getStyleClass().add("btn-remove-service");
        removeBtn.setOnAction(e -> {
            if (confirmRemove(st)) {
                removeService(st);
                buildTabBar();
                showBasicTab();
            }
        });

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionRow = new HBox(8, spacer, toggleBtn, removeBtn);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(4, 0, 4, 0));
        serviceFormBox.getChildren().add(actionRow);

        // Fields
        switch (st) {
            case FUEL -> buildFuelForm();
            case CATERING -> buildCateringForm();
            case GPU -> buildGpuForm();
            case LAVATORY -> buildLavatoryForm();
            case POTABLE_WATER -> buildPotableWaterForm();
            case WINDSHIELD_CLEANING -> buildWindshieldForm();
            case OIL_SERVICE -> buildOilForm();
        }
    }

    private void buildFuelForm() {
        if (fuel == null) return;
        serviceFormBox.getChildren().addAll(
                fieldRow("Fuel Type", boundTextField(fuel.getType(), v -> fuel.setType(v))),
                fieldRow("Gallons",   doubleField(fuel.getGallons(), v -> fuel.setGallons(v))),
                fieldRow("Weight (lbs)", doubleField(fuel.getWeight(), v -> fuel.setWeight(v))),
                fieldRow("Note",      boundTextArea(fuel.getNote(), v -> fuel.setNote(v)))
        );
    }

    private void buildCateringForm() {
        Label hint = new Label("Add one row per catering item.");
        hint.setStyle("-fx-font-size:11px; -fx-text-fill:#8a9ab0;");
        VBox rows = new VBox(6);

        // Render existing catering entries
        for (Catering c : catering) {
            rows.getChildren().add(cateringRow(c, rows));
        }

        Button addBtn = new Button("+ Add Item");
        addBtn.setStyle("-fx-background-color:#1e3a5f; -fx-text-fill:white; -fx-font-size:11px;"
                + "-fx-font-weight:700; -fx-padding:7 16; -fx-cursor:hand;"
                + "-fx-background-radius:0; -fx-border-radius:0;");
        addBtn.setOnAction(e -> {
            Catering c = new Catering();
            c.setId(UUID.randomUUID().toString());
            c.setItemId(item.getId());
            c.setNote(""); c.setCateringNumber(catering.size() + 1);
            c.setCreatedAt(Instant.now()); c.setUpdatedAt(Instant.now());
            catering.add(c);
            rows.getChildren().add(cateringRow(c, rows));
        });

        serviceFormBox.getChildren().addAll(hint, rows, addBtn);
    }

    private HBox cateringRow(Catering c, VBox rows) {
        TextField numF  = styledField("#");
        numF.setPrefWidth(60); numF.setMaxWidth(60);
        numF.setText(c.getCateringNumber() > 0 ? String.valueOf(c.getCateringNumber()) : "");
        numF.textProperty().addListener((obs, o, n) -> {
            try { c.setCateringNumber(Integer.parseInt(n)); } catch (Exception ignored) {}
        });

        TextField noteF = styledField("Note / description");
        noteF.setText(c.getNote() != null ? c.getNote() : "");
        noteF.textProperty().addListener((obs, o, n) -> c.setNote(n));
        HBox.setHgrow(noteF, Priority.ALWAYS);

        Button del = new Button("✕");
        del.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626; -fx-font-size:11px;"
                + "-fx-font-weight:700; -fx-padding:7 10; -fx-cursor:hand;"
                + "-fx-background-radius:0; -fx-border-color:#fecaca; -fx-border-width:1;");

        HBox row = new HBox(8, numF, noteF, del);
        row.setAlignment(Pos.CENTER_LEFT);
        del.setOnAction(e -> { catering.remove(c); rows.getChildren().remove(row); });
        return row;
    }

    private void buildGpuForm() {
        if (gpu == null) return;
        serviceFormBox.getChildren().add(
                fieldRow("Hours", doubleField(gpu.getHours(), v -> gpu.setHours(v))));
    }

    private void buildLavatoryForm() {
        if (lavatory == null) return;
        serviceFormBox.getChildren().addAll(
                fieldRow("Back-in Gallons", doubleField(lavatory.getBackInGallons(), v -> lavatory.setBackInGallons(v))),
                fieldRow("Note", boundTextArea(lavatory.getNote(), v -> lavatory.setNote(v)))
        );
    }

    private void buildPotableWaterForm() {
        if (potableWater == null) return;
        serviceFormBox.getChildren().add(
                fieldRow("Note", boundTextArea(potableWater.getNote(), v -> potableWater.setNote(v))));
    }

    private void buildWindshieldForm() {
        if (windshieldCleaning == null) return;
        serviceFormBox.getChildren().add(
                fieldRow("Note", boundTextArea(windshieldCleaning.getNote(), v -> windshieldCleaning.setNote(v))));
    }

    private void buildOilForm() {
        if (oilService == null) return;
        serviceFormBox.getChildren().addAll(
                fieldRow("Oil Type", boundTextField(oilService.getType(), v -> oilService.setType(v))),
                fieldRow("Quarts",   doubleField(oilService.getQuarts(), v -> oilService.setQuarts(v)))
        );
    }

    // ── Service header band ──────────────────────────────────────────────────────

    private HBox buildServiceHeader(ServiceType st) {
        java.awt.Color awtColor = st.getPrimaryColor();
        Color fxColor = Color.rgb(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());

        ImageView iv = makeIcon(st, 22, awtColor);
        tintIcon(iv, Color.WHITE, 22);

        Label name = new Label(label(st));
        name.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:white;");

        Label sep = new Label("·");
        sep.setStyle("-fx-text-fill:rgba(255,255,255,0.45); -fx-font-size:14px; -fx-padding:0 6;");

        Label sub = new Label("Service Configuration");
        sub.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.60);");

        HBox header = new HBox(10, iv, name, sep, sub);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:" + toHex(fxColor) + ";");
        header.setPadding(new Insets(13, 20, 13, 20));
        VBox.setMargin(header, new Insets(0, -24, 0, -24));
        return header;
    }

    // ── Save / Cancel ────────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        String tail = tailField.getText().trim().toUpperCase();
        if (tail.isBlank()) { showAlert("Tail number is required."); return; }

        item.setTail(tail);
        item.setDescription(descriptionField.getText());
        item.setArrival(toInstant(arrivalDatePicker.getValue(), arrivalTimeField.getText()));
        item.setDeparture(toInstant(departureDatePicker.getValue(), departureTimeField.getText()));
        item.setUpdatedAt(Instant.now());

        // Push local copies back into item
        item.setFuel(fuel);
        item.setCatering(catering.isEmpty() ? null : catering);
        item.setGpu(gpu);
        item.setLavatory(lavatory);
        item.setPotableWater(potableWater);
        item.setWindshieldCleaning(windshieldCleaning);
        item.setOilService(oilService);

        Thread t = new Thread(() -> {
            try {
                serviceItemService.saveService(item);
                Platform.runLater(() -> {
                    if (onSavedCallback != null) onSavedCallback.run();
                    closeModal();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showAlert("Failed to save: " + ex.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onCancel() { closeModal(); }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void copyServiceState() {
        fuel               = item.getFuel();
        catering           = item.getCatering() != null ? new ArrayList<>(item.getCatering()) : new ArrayList<>();
        gpu                = item.getGpu();
        lavatory           = item.getLavatory();
        potableWater       = item.getPotableWater();
        windshieldCleaning = item.getWindshieldCleaning();
        oilService         = item.getOilService();
    }

    private boolean isActive(ServiceType t) {
        return switch (t) {
            case FUEL                -> fuel               != null;
            case CATERING            -> catering           != null && !catering.isEmpty();
            case GPU                 -> gpu                != null;
            case LAVATORY            -> lavatory           != null;
            case POTABLE_WATER       -> potableWater       != null;
            case WINDSHIELD_CLEANING -> windshieldCleaning != null;
            case OIL_SERVICE         -> oilService         != null;
        };
    }

    private boolean isCompleted(ServiceType t) {
        return switch (t) {
            case FUEL                -> fuel               != null && fuel.getCompletedAt()               != null;
            case CATERING            -> !catering.isEmpty() && catering.stream().allMatch(c -> c.getCompletedAt() != null);
            case GPU                 -> gpu                != null && gpu.getCompletedAt()                != null;
            case LAVATORY            -> lavatory           != null && lavatory.getCompletedAt()           != null;
            case POTABLE_WATER       -> potableWater       != null && potableWater.getCompletedAt()       != null;
            case WINDSHIELD_CLEANING -> windshieldCleaning != null && windshieldCleaning.getCompletedAt() != null;
            case OIL_SERVICE         -> oilService         != null && oilService.getCompletedAt()         != null;
        };
    }

    private void toggleCompletion(ServiceType t) {
        Instant now = Instant.now();
        switch (t) {
            case FUEL -> { if (fuel != null) fuel.setCompletedAt(fuel.getCompletedAt() == null ? now : null); }
            case CATERING -> {
                boolean allDone = !catering.isEmpty() && catering.stream().allMatch(c -> c.getCompletedAt() != null);
                catering.forEach(c -> c.setCompletedAt(allDone ? null : now));
            }
            case GPU -> { if (gpu != null) gpu.setCompletedAt(gpu.getCompletedAt() == null ? now : null); }
            case LAVATORY -> { if (lavatory != null) lavatory.setCompletedAt(lavatory.getCompletedAt() == null ? now : null); }
            case POTABLE_WATER -> { if (potableWater != null) potableWater.setCompletedAt(potableWater.getCompletedAt() == null ? now : null); }
            case WINDSHIELD_CLEANING -> { if (windshieldCleaning != null) windshieldCleaning.setCompletedAt(windshieldCleaning.getCompletedAt() == null ? now : null); }
            case OIL_SERVICE -> { if (oilService != null) oilService.setCompletedAt(oilService.getCompletedAt() == null ? now : null); }
        }
    }

    private void removeService(ServiceType t) {
        switch (t) {
            case FUEL                -> fuel               = null;
            case CATERING            -> catering           = new ArrayList<>();
            case GPU                 -> gpu                = null;
            case LAVATORY            -> lavatory           = null;
            case POTABLE_WATER       -> potableWater       = null;
            case WINDSHIELD_CLEANING -> windshieldCleaning = null;
            case OIL_SERVICE         -> oilService         = null;
        }
    }

    private boolean confirmRemove(ServiceType st) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + label(st) + " service?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.initOwner(rootPane.getScene().getWindow());
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private HBox fieldRow(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:#4a5568; -fx-font-weight:600;");
        lbl.setMinWidth(160); lbl.setPrefWidth(160);
        HBox row = new HBox(12, lbl, input);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);
        return row;
    }

    private TextField boundTextField(String initial, java.util.function.Consumer<String> setter) {
        TextField tf = styledField(null);
        if (initial != null) tf.setText(initial);
        tf.textProperty().addListener((obs, o, n) -> setter.accept(n));
        return tf;
    }

    private TextField doubleField(Double initial, java.util.function.Consumer<Double> setter) {
        TextField tf = styledField(null);
        if (initial != null) tf.setText(String.valueOf(initial));
        tf.textProperty().addListener((obs, o, n) -> {
            try { setter.accept(Double.parseDouble(n)); } catch (Exception ignored) {}
        });
        return tf;
    }

    private TextArea boundTextArea(String initial, java.util.function.Consumer<String> setter) {
        TextArea ta = new TextArea(initial != null ? initial : "");
        ta.setStyle("-fx-background-color:#ffffff; -fx-border-color:#c8d4e0; -fx-border-width:1;"
                + "-fx-border-radius:0; -fx-background-radius:0; -fx-font-size:13px;"
                + "-fx-text-fill:#1a202c; -fx-prompt-text-fill:#a0aec0;"
                + "-fx-faint-focus-color:transparent; -fx-focus-color:transparent;");
        ta.setPrefRowCount(3); ta.setMaxWidth(Double.MAX_VALUE);
        ta.textProperty().addListener((obs, o, n) -> setter.accept(n));
        return ta;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        if (prompt != null) tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#ffffff; -fx-border-color:#c8d4e0; -fx-border-width:1;"
                + "-fx-border-radius:0; -fx-background-radius:0; -fx-padding:8 12;"
                + "-fx-font-size:13px; -fx-text-fill:#1a202c; -fx-prompt-text-fill:#a0aec0;"
                + "-fx-faint-focus-color:transparent; -fx-focus-color:transparent;");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Region spacer(double h) {
        Region r = new Region(); r.setPrefHeight(h); r.setMinHeight(h); r.setMaxHeight(h); return r;
    }

    private Instant toInstant(LocalDate date, String time) {
        if (date == null) return null;
        try {
            LocalTime lt = LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            return date.atTime(lt).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
    }

    private String label(ServiceType t) {
        return switch (t) {
            case FUEL -> "Fuel"; case CATERING -> "Catering"; case GPU -> "GPU";
            case LAVATORY -> "Lavatory"; case POTABLE_WATER -> "Potable Water";
            case WINDSHIELD_CLEANING -> "Windshield Cleaning"; case OIL_SERVICE -> "Oil Service";
        };
    }

    private ImageView makeIcon(ServiceType st, int size, java.awt.Color tint) {
        try {
            BufferedImage bi = st.getImage(size * 2);
            BufferedImage out = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < bi.getHeight(); y++) for (int x = 0; x < bi.getWidth(); x++) {
                int argb = bi.getRGB(x, y); int a = (argb >> 24) & 0xff;
                if (a > 0) out.setRGB(x, y, (a << 24) | (tint.getRed() << 16) | (tint.getGreen() << 8) | tint.getBlue());
            }
            WritableImage wi = SwingFXUtils.toFXImage(out, null);
            ImageView iv = new ImageView(wi);
            iv.setFitWidth(size); iv.setFitHeight(size); iv.setPreserveRatio(true); iv.setSmooth(true);
            return iv;
        } catch (Exception e) { return new ImageView(); }
    }

    private void tintIcon(ImageView iv, Color color, int size) {
        iv.setEffect(new Blend(BlendMode.SRC_ATOP, null, new ColorInput(0, 0, size, size, color)));
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    private String alphaHex(Color c, double a) {
        return String.format("rgba(%d,%d,%d,%.2f)", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), a);
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null); a.initOwner(rootPane.getScene().getWindow()); a.showAndWait();
    }

    private void closeModal() { ((Stage) rootPane.getScene().getWindow()).close(); }

    private void fadeIn(Node node) {
        node.setOpacity(0); node.setTranslateY(5);
        new Timeline(new KeyFrame(Duration.millis(160),
                new KeyValue(node.opacityProperty(),    1.0, Interpolator.EASE_OUT),
                new KeyValue(node.translateYProperty(), 0.0, Interpolator.EASE_OUT)
        )).play();
    }
}