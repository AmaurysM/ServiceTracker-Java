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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class AddServiceItemController {

    private final List<ServiceType> selectedServices = new ArrayList<>();
    private final Map<ServiceType, Node> configPanes = new EnumMap<>(ServiceType.class);
    private final Map<ServiceType, Map<String, Object>> serviceData = new EnumMap<>(ServiceType.class);

    @Autowired
    private ServiceItemService serviceItemService;

    // ── FXML nodes ──────────────────────────────────────────────────────────────
    @FXML private StackPane rootPane;
    @FXML private VBox      wizardCard;
    @FXML private Label     headerStepLabel;
    @FXML private Label     tailPreviewLabel;
    @FXML private Button    step1TabBtn;
    @FXML private Button    step2TabBtn;
    @FXML private Button    step3TabBtn;
    @FXML private VBox      step1Pane;
    @FXML private VBox      step2Pane;
    @FXML private VBox      step3Pane;

    // Step 1
    @FXML private TextField  tailField;
    @FXML private DatePicker arrivalDatePicker;
    @FXML private TextField  arrivalTimeField;
    @FXML private DatePicker departureDatePicker;
    @FXML private TextField  departureTimeField;
    @FXML private TextArea   descriptionField;

    // Step 2
    @FXML private FlowPane serviceSelectionPane;

    // Step 3
    @FXML private HBox       serviceTabBar;
    @FXML private StackPane  serviceConfigPane;

    // Footer
    @FXML private Button cancelBtn;
    @FXML private Button backBtn;
    @FXML private Button nextBtn;
    @FXML private Button addItemBtn;

    private Step currentStep = Step.BASIC;
    private ServiceType activeServiceTab = null;

    @Setter
    private Runnable onSaveCallback;

    // ── Init ─────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        buildServiceSelectionCards();
        updateFooterButtons();
        showStep(Step.BASIC);

        tailField.textProperty().addListener((obs, o, n) ->
                tailPreviewLabel.setText(n.isBlank() ? "Tail: —" : "✈  " + n.trim().toUpperCase()));
    }

    // ── Navigation ───────────────────────────────────────────────────────────────

    @FXML
    private void onNext() {
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

    @FXML
    private void onBack() {
        switch (currentStep) {
            case SELECT_SERVICES -> showStep(Step.BASIC);
            case CONFIGURE       -> showStep(Step.SELECT_SERVICES);
            default -> {}
        }
    }

    @FXML private void onCancel()  { closeModal(); }

    @FXML private void onStep1Tab() { showStep(Step.BASIC); }

    @FXML
    private void onStep2Tab() {
        if (!tailField.getText().isBlank()) showStep(Step.SELECT_SERVICES);
    }

    @FXML
    private void onStep3Tab() {
        if (tailField.getText().isBlank() || selectedServices.isEmpty()) return;
        buildServiceTabBar();
        if (activeServiceTab == null) activateServiceTab(selectedServices.get(0));
        showStep(Step.CONFIGURE);
    }

    @FXML
    private void onAddItem() {
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

    // ── Step visibility ───────────────────────────────────────────────────────────

    private void showStep(Step step) {
        currentStep = step;

        step1Pane.setVisible(false); step1Pane.setManaged(false);
        step2Pane.setVisible(false); step2Pane.setManaged(false);
        step3Pane.setVisible(false); step3Pane.setManaged(false);

        VBox active = switch (step) {
            case BASIC            -> step1Pane;
            case SELECT_SERVICES  -> step2Pane;
            case CONFIGURE        -> step3Pane;
        };
        active.setVisible(true);
        active.setManaged(true);
        fadeIn(active);

        // Drive active/inactive state with CSS pseudo-class via style-class swaps
        setTabActive(step1TabBtn, step == Step.BASIC);
        setTabActive(step2TabBtn, step == Step.SELECT_SERVICES);
        setTabActive(step3TabBtn, step == Step.CONFIGURE);

        headerStepLabel.setText("ADD SERVICE ITEM" + switch (step) {
            case BASIC           -> "  ·  BASIC INFORMATION";
            case SELECT_SERVICES -> "  ·  SELECT SERVICES";
            case CONFIGURE       -> "  ·  CONFIGURE SERVICES";
        });

        updateFooterButtons();
    }

    /** Swap the step-tab-active class on/off without touching inline style. */
    private void setTabActive(Button btn, boolean active) {
        btn.getStyleClass().removeAll("step-tab-active");
        if (active) btn.getStyleClass().add("step-tab-active");
    }

    private void updateFooterButtons() {
        backBtn.setVisible(currentStep != Step.BASIC);
        backBtn.setManaged(currentStep != Step.BASIC);
        nextBtn.setVisible(currentStep != Step.CONFIGURE);
        nextBtn.setManaged(currentStep != Step.CONFIGURE);
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
        iconBox.getStyleClass().add("service-card-icon-box");
        StackPane.setAlignment(iconView, Pos.CENTER);

        // Right text
        Label nameLbl = new Label(getServiceLabel(st));
        nameLbl.getStyleClass().add("service-card-name");

        Label selLbl = new Label("✓  Selected");
        selLbl.getStyleClass().add("service-card-selected-badge");
        selLbl.setVisible(false);
        selLbl.setManaged(false);

        VBox textBox = new VBox(3, nameLbl, selLbl);
        textBox.getStyleClass().add("service-card-text");

        HBox card = new HBox();
        card.getStyleClass().add("service-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(iconBox, textBox);
        FlowPane.setMargin(card, new Insets(3));

        card.setOnMouseEntered(e -> {
            if (!selectedServices.contains(st)) card.getStyleClass().add("service-card-hover");
        });
        card.setOnMouseExited(e -> card.getStyleClass().remove("service-card-hover"));

        card.setOnMouseClicked(e -> {
            if (selectedServices.contains(st)) {
                selectedServices.remove(st);
                card.getStyleClass().remove("service-card-selected");
                selLbl.setVisible(false);
                selLbl.setManaged(false);
                nameLbl.getStyleClass().remove("service-card-name-selected");
                tintIcon(iconView, fxColor, 22);
            } else {
                selectedServices.add(st);
                card.getStyleClass().add("service-card-selected");
                selLbl.setVisible(true);
                selLbl.setManaged(true);
                nameLbl.getStyleClass().add("service-card-name-selected");
                tintIcon(iconView, fxColor, 22);
            }
        });

        return card;
    }

    private String getServiceLabel(ServiceType st) {
        return switch (st) {
            case FUEL               -> "Fuel";
            case CATERING           -> "Catering";
            case GPU                -> "GPU";
            case LAVATORY           -> "Lavatory";
            case POTABLE_WATER      -> "Potable Water";
            case WINDSHIELD_CLEANING -> "Windshield Cleaning";
            case OIL_SERVICE        -> "Oil Service";
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
            lbl.getStyleClass().add("service-tab-label");

            HBox tab = new HBox(6, iv, lbl);
            tab.getStyleClass().add("service-type-tab");
            tab.setAlignment(Pos.CENTER);
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

            ImageView iv  = (ImageView) tab.getChildren().get(0);
            Label     lbl = (Label)     tab.getChildren().get(1);
            boolean   active = (st == target);

            tab.getStyleClass().removeAll("service-type-tab-active");
            lbl.getStyleClass().removeAll("service-tab-label-active");

            if (active) {
                tab.getStyleClass().add("service-type-tab-active");
                lbl.getStyleClass().add("service-tab-label-active");
                tintIcon(iv, Color.WHITE, 14);
            } else {
                tintIcon(iv, toFxColor(st.getPrimaryColor()), 14);
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
        scroll.getStyleClass().add("config-scroll");

        VBox form = new VBox(16);
        form.getStyleClass().add("config-form");

        Map<String, Object> data = serviceData.computeIfAbsent(st, k -> new LinkedHashMap<>());

        form.getChildren().add(buildConfigHeader(st));
        form.getChildren().add(spacer(8));

        switch (st) {
            case FUEL -> form.getChildren().addAll(
                    fieldRow("Fuel Type",    textInput("type",    data, "")),
                    fieldRow("Gallons",      numberInput("gallons", data)),
                    fieldRow("Weight (lbs)", numberInput("weight",  data)),
                    fieldRow("Note",         textArea("note",       data))
            );
            case CATERING -> {
                Label hint = new Label("Add one row per catering item.");
                hint.getStyleClass().add("config-hint");

                VBox rows = new VBox(6);

                Button addRow = new Button("+ Add Item");
                addRow.getStyleClass().addAll("config-add-row-btn");

                addRow.setOnAction(e -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> items = (List<Map<String, String>>)
                            data.computeIfAbsent("items", k -> new ArrayList<Map<String, String>>());

                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("note", "");
                    entry.put("cateringNumber", "");
                    items.add(entry);

                    TextField noteF   = styledTextField("Note / description");
                    TextField numberF = styledTextField("#");
                    numberF.getStyleClass().add("catering-number-field");
                    noteF.textProperty().addListener((obs, o, nv) -> entry.put("note", nv));
                    numberF.textProperty().addListener((obs, o, nv) -> entry.put("cateringNumber", nv));

                    Button del = new Button("✕");
                    del.getStyleClass().add("catering-delete-btn");

                    HBox row = new HBox(8, numberF, noteF, del);
                    row.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(noteF, Priority.ALWAYS);

                    del.setOnAction(ev -> {
                        rows.getChildren().remove(row);
                        items.remove(entry);
                    });
                    rows.getChildren().add(row);
                });

                form.getChildren().addAll(hint, rows, addRow);
            }
            case GPU -> form.getChildren().add(fieldRow("Hours", numberInput("hours", data)));
            case LAVATORY -> form.getChildren().addAll(
                    fieldRow("Back-in Gallons", numberInput("backInGallons", data)),
                    fieldRow("Note",            textArea("note", data))
            );
            case POTABLE_WATER       -> form.getChildren().add(fieldRow("Note", textArea("note", data)));
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
        ImageView iv = makeIconView(st, 22);
        tintIcon(iv, Color.WHITE, 22);

        Label name = new Label(getServiceLabel(st));
        name.getStyleClass().add("config-header-name");

        Label divider = new Label("·");
        divider.getStyleClass().add("config-header-divider");

        Label subtitle = new Label("Service Configuration");
        subtitle.getStyleClass().add("config-header-subtitle");

        HBox header = new HBox(10, iv, name, divider, subtitle);
        header.getStyleClass().add("config-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Only the accent color changes per service — keep it as the sole inline style
        Color fxColor = toFxColor(st.getPrimaryColor());
        header.setStyle("-fx-background-color: " + toHex(fxColor) + ";");

        return header;
    }

    // ── Form helpers ─────────────────────────────────────────────────────────────

    private HBox fieldRow(String labelText, Node input) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("config-field-label");

        HBox row = new HBox(12, lbl, input);
        row.getStyleClass().add("config-field-row");
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);
        return row;
    }

    private TextField textInput(String key, Map<String, Object> data, String def) {
        TextField tf = styledTextField(null);
        tf.setText((String) data.getOrDefault(key, def));
        tf.textProperty().addListener((obs, o, n) -> data.put(key, n));
        return tf;
    }

    private TextField numberInput(String key, Map<String, Object> data) {
        TextField tf = styledTextField(null);
        if (data.containsKey(key)) tf.setText(String.valueOf(data.get(key)));
        tf.textProperty().addListener((obs, o, n) -> {
            try { data.put(key, Double.parseDouble(n)); } catch (NumberFormatException ignored) {}
        });
        return tf;
    }

    private TextArea textArea(String key, Map<String, Object> data) {
        TextArea ta = new TextArea((String) data.getOrDefault(key, ""));
        ta.getStyleClass().add("field-textarea");
        ta.setPrefRowCount(3);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.textProperty().addListener((obs, o, n) -> data.put(key, n));
        return ta;
    }

    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        if (prompt != null) tf.setPromptText(prompt);
        tf.getStyleClass().add("field-input");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
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
                    List<Map<String, String>> raw =
                            (List<Map<String, String>>) d.getOrDefault("items", List.of());
                    List<Catering> list = new ArrayList<>();
                    int counter = 1;
                    for (Map<String, String> ci : raw) {
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
        iv.setEffect(new Blend(BlendMode.SRC_ATOP, null,
                new ColorInput(0, 0, size, size, color)));
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
                new KeyValue(node.opacityProperty(),  1.0, Interpolator.EASE_OUT),
                new KeyValue(node.translateYProperty(), 0.0, Interpolator.EASE_OUT)
        )).play();
    }

    // ── Internal state ───────────────────────────────────────────────────────────

    private enum Step { BASIC, SELECT_SERVICES, CONFIGURE }
}