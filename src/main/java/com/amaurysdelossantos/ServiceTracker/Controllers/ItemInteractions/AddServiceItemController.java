package com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions;

import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.*;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.animation.*;
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
import java.util.UUID;

public class AddServiceItemController {

    @Setter
    private ServiceItemService serviceItemService;

    // ── FXML nodes ──────────────────────────────────────────────────────────────
    @FXML private StackPane  rootPane;
    @FXML private VBox       wizardCard;

    @FXML private Label      headerStepLabel;
    @FXML private Label      tailPreviewLabel;

    @FXML private Button     step1TabBtn;
    @FXML private Button     step2TabBtn;
    @FXML private Button     step3TabBtn;

    @FXML private VBox       step1Pane;
    @FXML private VBox       step2Pane;
    @FXML private VBox       step3Pane;

    // Step 1
    @FXML private TextField  tailField;
    @FXML private DatePicker arrivalDatePicker;
    @FXML private TextField  arrivalTimeField;
    @FXML private DatePicker departureDatePicker;
    @FXML private TextField  departureTimeField;
    @FXML private TextArea   descriptionField;

    // Step 2
    @FXML private FlowPane   serviceSelectionPane;

    // Step 3
    @FXML private HBox       serviceTabBar;
    @FXML private StackPane  serviceConfigPane;

    // Footer
    @FXML private Button     cancelBtn;
    @FXML private Button     backBtn;
    @FXML private Button     nextBtn;
    @FXML private Button     addItemBtn;

    // ── Internal state ───────────────────────────────────────────────────────────
    private enum Step { BASIC, SELECT_SERVICES, CONFIGURE }
    private Step currentStep = Step.BASIC;

    private final List<ServiceType>                    selectedServices = new ArrayList<>();
    private ServiceType                                activeServiceTab = null;
    private final Map<ServiceType, Node>               configPanes  = new EnumMap<>(ServiceType.class);
    private final Map<ServiceType, Map<String,Object>> serviceData  = new EnumMap<>(ServiceType.class);

    @Setter
    private Runnable onSaveCallback;
    //public void setOnSaveCallback(Runnable cb) { this.onSaveCallback = cb; }

    // ── White theme palette ──────────────────────────────────────────────────────
    private static final String WHITE        = "#ffffff";
    private static final String BG_SUBTLE    = "#f8f9fb";
    private static final String BG_FIELD     = "#ffffff";
    private static final String BORDER       = "#c8d4e0";
    private static final String BORDER_MID   = "#dde3eb";
    private static final String TEXT_DARK    = "#1a202c";
    private static final String TEXT_BODY    = "#374151";
    private static final String TEXT_LABEL   = "#4a5568";
    private static final String TEXT_MUTED   = "#8a9ab0";
    private static final String TEXT_PROMPT  = "#a0aec0";
    private static final String NAVY         = "#1e3a5f";
    private static final String BLUE         = "#1d4ed8";

    @FXML
    public void initialize() {
        buildServiceSelectionCards();
        updateFooterButtons();
        showStep(Step.BASIC);
        tailField.textProperty().addListener((obs, o, n) ->
                tailPreviewLabel.setText(n.isBlank() ? "Tail: —" : "✈  " + n.trim().toUpperCase()));
    }

    // ── Navigation ───────────────────────────────────────────────────────────────

    @FXML private void onNext() {
        switch (currentStep) {
            case BASIC -> {
                if (tailField.getText().isBlank()) { showAlert("Tail number is required."); return; }
                showStep(Step.SELECT_SERVICES);
            }
            case SELECT_SERVICES -> {
                if (selectedServices.isEmpty()) { showAlert("Please select at least one service."); return; }
                buildServiceTabBar();
                activateServiceTab(selectedServices.get(0));
                showStep(Step.CONFIGURE);
            }
            case CONFIGURE -> onAddItem();
        }
    }

    @FXML private void onBack() {
        switch (currentStep) {
            case SELECT_SERVICES -> showStep(Step.BASIC);
            case CONFIGURE       -> showStep(Step.SELECT_SERVICES);
            default -> {}
        }
    }

    @FXML private void onCancel()  { closeModal(); }

    @FXML private void onAddItem() {
        if (tailField.getText().isBlank()) { showAlert("Tail number is required."); return; }

        addItemBtn.setDisable(true);
        addItemBtn.setText("Saving…");

        ServiceItem item = buildItem();

        Thread t = new Thread(() -> {
            try {
                serviceItemService.saveService(item);
                Platform.runLater(() -> {
                    if (onSaveCallback != null) onSaveCallback.run();
                    closeModal();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    addItemBtn.setDisable(false);
                    addItemBtn.setText("Add Item");
                    showAlert("Failed to save: " + e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML private void onStep1Tab() { showStep(Step.BASIC); }
    @FXML private void onStep2Tab() {
        if (!tailField.getText().isBlank()) showStep(Step.SELECT_SERVICES);
    }
    @FXML private void onStep3Tab() {
        if (tailField.getText().isBlank() || selectedServices.isEmpty()) return;
        buildServiceTabBar();
        if (activeServiceTab == null) activateServiceTab(selectedServices.get(0));
        showStep(Step.CONFIGURE);
    }

    // ── Step rendering ───────────────────────────────────────────────────────────

    private void showStep(Step step) {
        currentStep = step;

        step1Pane.setVisible(false); step1Pane.setManaged(false);
        step2Pane.setVisible(false); step2Pane.setManaged(false);
        step3Pane.setVisible(false); step3Pane.setManaged(false);

        VBox active = switch (step) {
            case BASIC           -> step1Pane;
            case SELECT_SERVICES -> step2Pane;
            case CONFIGURE       -> step3Pane;
        };
        active.setVisible(true);
        active.setManaged(true);
        fadeIn(active);

        // Blue underline for active tab, plain for inactive
        String on  = "-fx-background-color:" + WHITE + "; -fx-text-fill:" + NAVY + ";"
                + "-fx-font-size:11px; -fx-font-weight:700; -fx-cursor:default;"
                + "-fx-border-color:transparent transparent " + BLUE + " transparent;"
                + "-fx-border-width:0 0 2 0;";
        String off = "-fx-background-color:transparent; -fx-text-fill:" + TEXT_MUTED + ";"
                + "-fx-font-size:11px; -fx-font-weight:600; -fx-cursor:hand;"
                + "-fx-border-width:0;";

        step1TabBtn.setStyle(step == Step.BASIC           ? on : off);
        step2TabBtn.setStyle(step == Step.SELECT_SERVICES ? on : off);
        step3TabBtn.setStyle(step == Step.CONFIGURE       ? on : off);

        headerStepLabel.setText("ADD SERVICE ITEM" + switch (step) {
            case BASIC           -> "  ·  BASIC INFORMATION";
            case SELECT_SERVICES -> "  ·  SELECT SERVICES";
            case CONFIGURE       -> "  ·  CONFIGURE SERVICES";
        });

        updateFooterButtons();
    }

    private void updateFooterButtons() {
        backBtn   .setVisible(currentStep != Step.BASIC);
        backBtn   .setManaged(currentStep != Step.BASIC);
        nextBtn   .setVisible(currentStep != Step.CONFIGURE);
        nextBtn   .setManaged(currentStep != Step.CONFIGURE);
        addItemBtn.setVisible(currentStep == Step.CONFIGURE);
        addItemBtn.setManaged(currentStep == Step.CONFIGURE);
    }

    // ── Step 2 – Service selection cards ─────────────────────────────────────────

    private void buildServiceSelectionCards() {
        serviceSelectionPane.getChildren().clear();
        for (ServiceType st : ServiceType.values()) {
            serviceSelectionPane.getChildren().add(buildServiceCard(st));
        }
    }

    private HBox buildServiceCard(ServiceType st) {
        Color fxColor = toFxColor(st.getPrimaryColor());

        // Left icon box
        ImageView iconView = makeIconView(st, 22);
        tintIcon(iconView, fxColor, 22);

        StackPane iconBox = new StackPane(iconView);
        iconBox.setPrefWidth(52);
        iconBox.setMinWidth(52);
        iconBox.setMaxWidth(52);
        iconBox.setPrefHeight(64);
        iconBox.setMinHeight(64);
        iconBox.setMaxHeight(64);
        iconBox.setStyle("-fx-background-color:" + alphaRgba(fxColor, 0.10) + ";");
        StackPane.setAlignment(iconView, Pos.CENTER);

        // Right text
        Label nameLbl = new Label(getServiceLabel(st));
        nameLbl.setStyle("-fx-font-weight:600; -fx-font-size:12px; -fx-text-fill:" + TEXT_BODY + ";");

        Label selLbl = new Label("✓  Selected");
        selLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#15803d; -fx-font-weight:700;");
        selLbl.setVisible(false);
        selLbl.setManaged(false);

        VBox textBox = new VBox(3, nameLbl, selLbl);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(iconBox, textBox);
        HBox.setMargin(textBox, new Insets(0, 8, 0, 14));
        card.setPrefWidth(224);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefHeight(64);
        card.setMinHeight(64);
        card.setStyle(cardUnselected());
        FlowPane.setMargin(card, new Insets(3));

        card.setOnMouseEntered(e -> {
            if (!selectedServices.contains(st))
                card.setStyle(cardHover());
        });
        card.setOnMouseExited(e -> {
            if (!selectedServices.contains(st)) card.setStyle(cardUnselected());
        });

        card.setOnMouseClicked(e -> {
            if (selectedServices.contains(st)) {
                selectedServices.remove(st);
                card.setStyle(cardUnselected());
                selLbl.setVisible(false);
                selLbl.setManaged(false);
                nameLbl.setStyle("-fx-font-weight:600; -fx-font-size:12px; -fx-text-fill:" + TEXT_BODY + ";");
                iconBox.setStyle("-fx-background-color:" + alphaRgba(fxColor, 0.10) + ";");
            } else {
                selectedServices.add(st);
                card.setStyle(cardSelected(fxColor));
                selLbl.setVisible(true);
                selLbl.setManaged(true);
                nameLbl.setStyle("-fx-font-weight:700; -fx-font-size:12px; -fx-text-fill:" + TEXT_DARK + ";");
                iconBox.setStyle("-fx-background-color:" + alphaRgba(fxColor, 0.15) + ";");
            }
        });

        return card;
    }

    private String cardUnselected() {
        return "-fx-border-color:" + BORDER + "; -fx-border-width:1;"
                + "-fx-background-color:" + WHITE + ";"
                + "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
    }

    private String cardHover() {
        return "-fx-border-color:#94a3b8; -fx-border-width:1;"
                + "-fx-background-color:#f8fafc;"
                + "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
    }

    private String cardSelected(Color accent) {
        return "-fx-border-color:" + toHex(accent) + "; -fx-border-width:1.5;"
                + "-fx-background-color:" + alphaRgba(accent, 0.05) + ";"
                + "-fx-background-radius:0; -fx-border-radius:0; -fx-cursor:hand;";
    }

    private String getServiceLabel(ServiceType st) {
        return switch (st) {
            case FUEL                -> "Fuel";
            case CATERING            -> "Catering";
            case GPU                 -> "GPU";
            case LAVATORY            -> "Lavatory";
            case POTABLE_WATER       -> "Potable Water";
            case WINDSHIELD_CLEANING -> "Windshield Cleaning";
            case OIL_SERVICE         -> "Oil Service";
        };
    }

    // ── Step 3 – Service tab bar ──────────────────────────────────────────────────

    private void buildServiceTabBar() {
        serviceTabBar.getChildren().clear();
        for (ServiceType st : selectedServices) {
            Color fxColor = toFxColor(st.getPrimaryColor());

            ImageView iv = makeIconView(st, 14);
            tintIcon(iv, fxColor, 14);

            Label lbl = new Label(getServiceLabel(st));
            lbl.setStyle("-fx-font-size:11px; -fx-font-weight:600;"
                    + "-fx-text-fill:" + toHex(fxColor) + ";");

            HBox tab = new HBox(6, iv, lbl);
            tab.setAlignment(Pos.CENTER);
            tab.setPadding(new Insets(0, 14, 0, 14));
            tab.setPrefHeight(48);
            tab.setMaxWidth(Double.MAX_VALUE);
            tab.setStyle("-fx-background-color:" + alphaRgba(fxColor, 0.07) + "; -fx-cursor:hand;"
                    + "-fx-border-color:transparent transparent " + BORDER_MID + " transparent;"
                    + "-fx-border-width:0 0 1 0;");
            HBox.setHgrow(tab, Priority.ALWAYS);
            tab.setUserData(st);
            tab.setOnMouseClicked(e -> activateServiceTab(st));
            serviceTabBar.getChildren().add(tab);
        }
    }

    private void activateServiceTab(ServiceType target) {
        activeServiceTab = target;

        for (Node n : serviceTabBar.getChildren()) {
            if (!(n instanceof HBox tab)) continue;
            if (!(tab.getUserData() instanceof ServiceType st)) continue;

            Color fxColor = toFxColor(st.getPrimaryColor());
            ImageView iv  = (ImageView) tab.getChildren().get(0);
            Label     lbl = (Label)     tab.getChildren().get(1);
            boolean   active = st == target;

            if (active) {
                tab.setStyle("-fx-background-color:" + toHex(fxColor) + "; -fx-cursor:default;");
                lbl.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:white;");
                tintIcon(iv, Color.WHITE, 14);
            } else {
                tab.setStyle("-fx-background-color:" + alphaRgba(fxColor, 0.07) + "; -fx-cursor:hand;"
                        + "-fx-border-color:transparent transparent " + BORDER_MID + " transparent;"
                        + "-fx-border-width:0 0 1 0;");
                lbl.setStyle("-fx-font-size:11px; -fx-font-weight:600; -fx-text-fill:" + toHex(fxColor) + ";");
                tintIcon(iv, fxColor, 14);
            }
        }

        configPanes.computeIfAbsent(target, this::buildConfigPane);
        Node pane = configPanes.get(target);
        serviceConfigPane.getChildren().setAll(pane);
        fadeIn(pane);
    }

    // ── Step 3 – Config pane builder ─────────────────────────────────────────────

    private Node buildConfigPane(ServiceType st) {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;"
                + "-fx-border-width:0;");

        VBox form = new VBox(16);
        form.setPadding(new Insets(0, 24, 22, 24));
        form.setStyle("-fx-background-color:" + WHITE + ";");

        Map<String, Object> data = serviceData.computeIfAbsent(st, k -> new LinkedHashMap<>());

        form.getChildren().add(buildConfigHeader(st));
        form.getChildren().add(spacer(8));

        switch (st) {
            case FUEL -> form.getChildren().addAll(
                    fieldRow("Fuel Type",    textInput("type",    data, "")),
                    fieldRow("Gallons",      numberInput("gallons", data)),
                    fieldRow("Weight (lbs)", numberInput("weight",  data)),
                    fieldRow("Note",         textArea("note",     data))
            );
            case CATERING -> {
                Label hint = new Label("Add one row per catering item.");
                hint.setStyle("-fx-text-fill:" + TEXT_MUTED + "; -fx-font-size:11px;");
                VBox rows = new VBox(6);

                Button addRow = styledBtn("+ Add Item", NAVY, WHITE);
                addRow.setOnAction(e -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String,String>> items = (List<Map<String,String>>)
                            data.computeIfAbsent("items", k -> new ArrayList<Map<String,String>>());
                    Map<String,String> entry = new LinkedHashMap<>();
                    entry.put("note", ""); entry.put("cateringNumber", "");
                    items.add(entry);

                    TextField noteF   = styledTextField("Note / description");
                    TextField numberF = styledTextField("#");
                    numberF.setPrefWidth(60); numberF.setMaxWidth(60);
                    noteF  .textProperty().addListener((obs,o,nv) -> entry.put("note",           nv));
                    numberF.textProperty().addListener((obs,o,nv) -> entry.put("cateringNumber", nv));

                    Button del = styledBtn("✕", "#fee2e2", "#dc2626");
                    del.setStyle(del.getStyle()
                            + "-fx-border-color:#fecaca; -fx-border-width:1;");
                    HBox row = new HBox(8, numberF, noteF, del);
                    row.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(noteF, Priority.ALWAYS);
                    del.setOnAction(ev -> { rows.getChildren().remove(row); items.remove(entry); });
                    rows.getChildren().add(row);
                });
                form.getChildren().addAll(hint, rows, addRow);
            }
            case GPU -> form.getChildren().add(fieldRow("Hours", numberInput("hours", data)));
            case LAVATORY -> form.getChildren().addAll(
                    fieldRow("Back-in Gallons", numberInput("backInGallons", data)),
                    fieldRow("Note",            textArea("note", data))
            );
            case POTABLE_WATER    -> form.getChildren().add(fieldRow("Note", textArea("note", data)));
            case WINDSHIELD_CLEANING -> form.getChildren().add(fieldRow("Note", textArea("note", data)));
            case OIL_SERVICE -> form.getChildren().addAll(
                    fieldRow("Oil Type", textInput("type",   data, "")),
                    fieldRow("Quarts",   numberInput("quarts", data))
            );
        }

        scroll.setContent(form);
        return scroll;
    }

    private HBox buildConfigHeader(ServiceType st) {
        Color fxColor = toFxColor(st.getPrimaryColor());

        ImageView iv = makeIconView(st, 22);
        tintIcon(iv, Color.WHITE, 22);

        Label name = new Label(getServiceLabel(st));
        name.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:white;");

        Label divider = new Label("·");
        divider.setStyle("-fx-text-fill:rgba(255,255,255,0.45); -fx-font-size:14px; -fx-padding:0 6;");

        Label subtitle = new Label("Service Configuration");
        subtitle.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.60);");

        HBox header = new HBox(10, iv, name, divider, subtitle);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(header, new Insets(0, -24, 0, -24));
        header.setStyle("-fx-background-color:" + toHex(fxColor) + ";");
        header.setPadding(new Insets(13, 20, 13, 20));

        return header;
    }

    private Region spacer(double height) {
        Region r = new Region();
        r.setPrefHeight(height);
        r.setMinHeight(height);
        r.setMaxHeight(height);
        return r;
    }

    // ── Build ServiceItem from form state ────────────────────────────────────────

    private ServiceItem buildItem() {
        String itemId = UUID.randomUUID().toString();
        String tail   = tailField.getText().trim().toUpperCase();
        Instant now   = Instant.now();

        ServiceItem item = new ServiceItem();
        item.setId(itemId);
        item.setTail(tail);
        item.setDescription(descriptionField.getText());
        item.setArrival(combineToInstant(arrivalDatePicker.getValue(), arrivalTimeField.getText()));
        item.setDeparture(combineToInstant(departureDatePicker.getValue(), departureTimeField.getText()));
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        for (ServiceType st : selectedServices) {
            Map<String, Object> d = serviceData.getOrDefault(st, new HashMap<>());
            switch (st) {
                case FUEL -> {
                    Fuel f = new Fuel();
                    f.setId(UUID.randomUUID().toString());
                    f.setTail(tail);
                    f.setType((String) d.getOrDefault("type", ""));
                    if (d.get("gallons") instanceof Number n) f.setGallons(n.doubleValue());
                    if (d.get("weight")  instanceof Number n) f.setWeight(n.doubleValue());
                    f.setNote((String) d.getOrDefault("note", ""));
                    f.setCreatedAt(now); f.setUpdatedAt(now);
                    item.setFuel(f);
                }
                case GPU -> {
                    GPU gpu = new GPU();
                    gpu.setId(UUID.randomUUID().toString());
                    if (d.get("hours") instanceof Number n) gpu.setHours(n.doubleValue());
                    gpu.setCreatedAt(now); gpu.setUpdatedAt(now);
                    item.setGpu(gpu);
                }
                case LAVATORY -> {
                    Lavatory lav = new Lavatory();
                    lav.setId(UUID.randomUUID().toString());
                    lav.setItemId(itemId);
                    if (d.get("backInGallons") instanceof Number n) lav.setBackInGallons(n.doubleValue());
                    lav.setNote((String) d.getOrDefault("note", ""));
                    lav.setCreatedAt(now); lav.setUpdatedAt(now);
                    item.setLavatory(lav);
                }
                case POTABLE_WATER -> {
                    PotableWater pw = new PotableWater();
                    pw.setId(UUID.randomUUID().toString());
                    pw.setNote((String) d.getOrDefault("note", ""));
                    pw.setCreatedAt(now); pw.setUpdatedAt(now);
                    item.setPotableWater(pw);
                }
                case WINDSHIELD_CLEANING -> {
                    WindshieldCleaning wc = new WindshieldCleaning();
                    wc.setId(UUID.randomUUID().toString());
                    wc.setNote((String) d.getOrDefault("note", ""));
                    wc.setCreatedAt(now); wc.setUpdatedAt(now);
                    item.setWindshieldCleaning(wc);
                }
                case OIL_SERVICE -> {
                    OilService os = new OilService();
                    os.setId(UUID.randomUUID().toString());
                    os.setType((String) d.getOrDefault("type", ""));
                    if (d.get("quarts") instanceof Number n) os.setQuarts(n.doubleValue());
                    os.setCreatedAt(now); os.setUpdatedAt(now);
                    item.setOilService(os);
                }
                case CATERING -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String,String>> raw =
                            (List<Map<String,String>>) d.getOrDefault("items", List.of());
                    List<Catering> list = new ArrayList<>();
                    int counter = 1;
                    for (Map<String,String> ci : raw) {
                        Catering c = new Catering();
                        c.setId(UUID.randomUUID().toString());
                        c.setItemId(itemId);
                        c.setNote(ci.getOrDefault("note", ""));
                        try {
                            c.setCateringNumber(Integer.parseInt(ci.getOrDefault("cateringNumber", "0")));
                        } catch (NumberFormatException ignored) {
                            c.setCateringNumber(counter);
                        }
                        c.setCreatedAt(now); c.setUpdatedAt(now);
                        list.add(c);
                        counter++;
                    }
                    item.setCatering(list);
                }
            }
        }
        return item;
    }

    // ── Form helpers ─────────────────────────────────────────────────────────────

    private HBox fieldRow(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + TEXT_LABEL + "; -fx-font-weight:600;");
        lbl.setMinWidth(160);
        lbl.setPrefWidth(160);
        HBox row = new HBox(12, lbl, input);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);
        return row;
    }

    private TextField textInput(String key, Map<String,Object> data, String def) {
        TextField tf = styledTextField(null);
        tf.setText((String) data.getOrDefault(key, def));
        tf.textProperty().addListener((obs, o, n) -> data.put(key, n));
        return tf;
    }

    private TextField numberInput(String key, Map<String,Object> data) {
        TextField tf = styledTextField(null);
        if (data.containsKey(key)) tf.setText(String.valueOf(data.get(key)));
        tf.textProperty().addListener((obs, o, n) -> {
            try { data.put(key, Double.parseDouble(n)); } catch (NumberFormatException ignored) {}
        });
        return tf;
    }

    private TextArea textArea(String key, Map<String,Object> data) {
        TextArea ta = new TextArea((String) data.getOrDefault(key, ""));
        ta.setStyle(inputStyle());
        ta.setPrefRowCount(3);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.textProperty().addListener((obs, o, n) -> data.put(key, n));
        return ta;
    }

    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        if (prompt != null) tf.setPromptText(prompt);
        tf.setStyle(inputStyle());
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Button styledBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";"
                + "-fx-padding:7 16; -fx-cursor:hand; -fx-font-size:11px; -fx-font-weight:700;"
                + "-fx-background-radius:0; -fx-border-radius:0;");
        return b;
    }

    private String inputStyle() {
        return "-fx-background-color:" + BG_FIELD + "; -fx-border-color:" + BORDER + ";"
                + "-fx-border-width:1; -fx-border-radius:0; -fx-background-radius:0;"
                + "-fx-padding:8 12; -fx-font-size:13px; -fx-text-fill:" + TEXT_DARK + ";"
                + "-fx-prompt-text-fill:" + TEXT_PROMPT + ";"
                + "-fx-faint-focus-color:transparent; -fx-focus-color:transparent;";
    }

    // ── SVG icon rendering ───────────────────────────────────────────────────────

    private ImageView makeIconView(ServiceType st, int size) {
        try {
            BufferedImage bi = st.getImage(size);
            WritableImage wi = SwingFXUtils.toFXImage(bi, null);
            ImageView iv = new ImageView(wi);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) {
            e.printStackTrace();
            ImageView iv = new ImageView();
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            return iv;
        }
    }

    private void tintIcon(ImageView iv, Color color, int size) {
        iv.setEffect(new Blend(
                BlendMode.SRC_ATOP,
                null,
                new ColorInput(0, 0, size, size, color)
        ));
    }

    // ── Color utilities ───────────────────────────────────────────────────────────

    private Color toFxColor(java.awt.Color awt) {
        return Color.rgb(awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }

    private String alphaRgba(Color c, double opacity) {
        return String.format("rgba(%d,%d,%d,%.2f)",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255),
                opacity);
    }

    // ── Misc ─────────────────────────────────────────────────────────────────────

    private Instant combineToInstant(LocalDate date, String time) {
        if (date == null) return null;
        try {
            LocalTime lt = LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("HH:mm"));
            return date.atTime(lt).atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
    }

    private void closeModal() {
        ((Stage) rootPane.getScene().getWindow()).close();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.initOwner(rootPane.getScene().getWindow());
        a.showAndWait();
    }

    private void fadeIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(5);
        new Timeline(new KeyFrame(Duration.millis(180),
                new KeyValue(node.opacityProperty(),    1.0, Interpolator.EASE_OUT),
                new KeyValue(node.translateYProperty(), 0.0, Interpolator.EASE_OUT)
        )).play();
    }
}