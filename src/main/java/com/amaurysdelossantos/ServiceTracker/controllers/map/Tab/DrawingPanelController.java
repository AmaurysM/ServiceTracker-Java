package com.amaurysdelossantos.ServiceTracker.controllers.map.Tab;

import com.amaurysdelossantos.ServiceTracker.Services.DrawingService;
import com.amaurysdelossantos.ServiceTracker.Services.MapService;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class DrawingPanelController {

    @FXML private AnchorPane  headerPane;
    @FXML private Label       headerTitle;

    @FXML private Button      closeBtn;

    @FXML private Button      drawToggleBtn;

    @FXML private VBox        drawingControls;

    @FXML private Button      penBtn;
    @FXML private Button      eraserBtn;

    @FXML private VBox        colorSection;
    @FXML private HBox        colorGrid;
    @FXML private Rectangle   activeColorSwatch;
    @FXML private Label       activeColorName;

    @FXML private Label       widthLabel;
    @FXML private Slider      widthSlider;
    @FXML private Label       opacityLabel;
    @FXML private Slider      opacitySlider;

    @FXML private Label       historyLabel;
    @FXML private Button      undoBtn;
    @FXML private Button      redoBtn;
    @FXML private Button      toggleLayersBtn;
    @FXML private Button      clearBtn;
    @FXML private Button      saveBtn;
    @FXML private Button      loadBtn;

    @Autowired private MapService     mapService;
    @Autowired private DrawingService drawingService;

    private static final String[][] PALETTE = {
            {"#FF0000", "Red"},
            {"#0000FF", "Blue"},
            {"#00FF00", "Green"},
            {"#FFFF00", "Yellow"},
            {"#FF6B00", "Orange"},
            {"#9C27B0", "Purple"},
            {"#000000", "Black"},
            {"#FFFFFF", "White"}
    };

    @FXML
    public void initialize() {
        buildColorSwatches();
        bindServiceToUI();
        wireButtons();
        refreshModeButtons();
        refreshLayerButton();
        refreshActionButtons();

        drawingControls.setVisible(drawingService.isDrawing());
        drawingControls.setManaged(drawingService.isDrawing());
    }

    private void buildColorSwatches() {
        colorGrid.getChildren().clear();
        for (String[] entry : PALETTE) {
            String hex  = entry[0];
            String name = entry[1];

            Pane swatch = new Pane();
            swatch.setPrefSize(22, 22);
            swatch.setMinSize(22, 22);
            swatch.setMaxSize(22, 22);
            swatch.setStyle(buildSwatchStyle(hex, drawingService.getColor().equals(hex)));
            swatch.setUserData(hex);

            swatch.setOnMouseClicked(e -> {
                drawingService.setColor(hex);
                refreshSwatches();
                updateActiveColorPreview(hex, name);
            });

            colorGrid.getChildren().add(swatch);
        }
        updateActiveColorFromService();
    }

    private String buildSwatchStyle(String hex, boolean selected) {
        String border = selected
                ? "-fx-border-color: #1E293B; -fx-border-width: 2; -fx-effect: dropshadow(gaussian,#3B82F6,4,0.6,0,0);"
                : "-fx-border-color: #D1D5DB; -fx-border-width: 1;";
        return "-fx-background-color: " + hex + "; " + border;
    }

    private void refreshSwatches() {
        String current = drawingService.getColor();
        for (javafx.scene.Node node : colorGrid.getChildren()) {
            if (node instanceof Pane swatch) {
                String hex = (String) swatch.getUserData();
                swatch.setStyle(buildSwatchStyle(hex, hex.equals(current)));
            }
        }
    }

    private void updateActiveColorFromService() {
        String hex = drawingService.getColor();
        for (String[] entry : PALETTE) {
            if (entry[0].equals(hex)) {
                updateActiveColorPreview(hex, entry[1]);
                return;
            }
        }
        updateActiveColorPreview(hex, "Custom");
    }

    private void updateActiveColorPreview(String hex, String name) {
        activeColorSwatch.setFill(javafx.scene.paint.Color.web(hex));
        activeColorSwatch.setStroke(javafx.scene.paint.Color.web("#D1D5DB"));
        activeColorName.setText(name);
    }

    private void bindServiceToUI() {

        drawingService.drawingProperty().addListener((obs, o, active) -> {
            Platform.runLater(() -> {
                drawingControls.setVisible(active);
                drawingControls.setManaged(active);
                refreshToggleButton(active);
                refreshHeader(active);
            });
        });
        refreshToggleButton(drawingService.isDrawing());
        refreshHeader(drawingService.isDrawing());

        widthSlider.setValue(drawingService.getStrokeWidth());
        widthSlider.valueProperty().addListener((obs, o, n) -> {
            int w = n.intValue();
            drawingService.setStrokeWidth(w);
            refreshWidthLabel(w);
        });
        drawingService.strokeWidthProperty().addListener((obs, o, n) ->
                Platform.runLater(() -> {
                    widthSlider.setValue(n.intValue());
                    refreshWidthLabel(n.intValue());
                }));
        refreshWidthLabel(drawingService.getStrokeWidth());

        opacitySlider.setValue(drawingService.getOpacity());
        opacitySlider.valueProperty().addListener((obs, o, n) -> {
            int op = n.intValue();
            drawingService.setOpacity(op);
            opacityLabel.setText("OPACITY: " + op + "%");
        });
        drawingService.opacityProperty().addListener((obs, o, n) ->
                Platform.runLater(() -> {
                    opacitySlider.setValue(n.intValue());
                    opacityLabel.setText("OPACITY: " + n.intValue() + "%");
                }));
        opacityLabel.setText("OPACITY: " + drawingService.getOpacity() + "%");

        drawingService.strokeCountProperty().addListener((obs, o, n) ->
                Platform.runLater(() -> {
                    historyLabel.setText("HISTORY (" + n.intValue() + ")");
                    refreshActionButtons();
                }));
        historyLabel.setText("HISTORY (" + drawingService.getStrokeCount() + ")");

        drawingService.canUndoProperty().addListener((obs, o, n) ->
                Platform.runLater(this::refreshActionButtons));
        drawingService.canRedoProperty().addListener((obs, o, n) ->
                Platform.runLater(this::refreshActionButtons));

        drawingService.modeProperty().addListener((obs, o, n) ->
                Platform.runLater(() -> {
                    refreshModeButtons();
                    refreshColorSectionVisibility();
                    refreshWidthLabel(drawingService.getStrokeWidth());
                }));
        refreshColorSectionVisibility();

        drawingService.layersVisibleProperty().addListener((obs, o, n) ->
                Platform.runLater(this::refreshLayerButton));
    }

    private void wireButtons() {
        closeBtn.setOnAction(e -> handleExit());

        drawToggleBtn.setOnAction(e -> drawingService.toggleDrawing());

        penBtn.setOnAction(e -> {
            drawingService.setMode("pen");
        });
        eraserBtn.setOnAction(e -> {
            drawingService.setMode("eraser");
        });

        undoBtn.setOnAction(e -> drawingService.requestUndo());
        redoBtn.setOnAction(e -> drawingService.requestRedo());

        toggleLayersBtn.setOnAction(e -> {
            drawingService.toggleLayers();
        });

        clearBtn.setOnAction(e -> {
            int count = drawingService.getStrokeCount();
            if (count == 0) return;
            String plural = count != 1 ? "s" : "";
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "Delete all " + count + " drawing" + plural + "?",
                    javafx.scene.control.ButtonType.OK,
                    javafx.scene.control.ButtonType.CANCEL
            );
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == javafx.scene.control.ButtonType.OK) {
                    drawingService.requestClear();
                }
            });
        });

        saveBtn.setOnAction(e -> drawingService.requestSave());
        loadBtn.setOnAction(e -> drawingService.requestLoad());
    }

    private void refreshHeader(boolean active) {
        if (active) {
            headerPane.getStyleClass().removeAll("panel-header");
            if (!headerPane.getStyleClass().contains("panel-header-drawing"))
                headerPane.getStyleClass().add("panel-header-drawing");
        } else {
            headerPane.getStyleClass().removeAll("panel-header-drawing");
            if (!headerPane.getStyleClass().contains("panel-header"))
                headerPane.getStyleClass().add("panel-header");
        }
    }

    private void refreshToggleButton(boolean active) {
        drawToggleBtn.getStyleClass().removeAll("draw-toggle-off", "draw-toggle-on");
        if (active) {
            drawToggleBtn.getStyleClass().add("draw-toggle-on");
            drawToggleBtn.setText("STOP DRAWING");
        } else {
            drawToggleBtn.getStyleClass().add("draw-toggle-off");
            drawToggleBtn.setText("START DRAWING");
        }
    }

    private void refreshModeButtons() {
        boolean isPen = "pen".equals(drawingService.getMode());

        penBtn.getStyleClass().removeAll("mode-btn-active-pen", "mode-btn");
        eraserBtn.getStyleClass().removeAll("mode-btn-active-eraser", "mode-btn");

        if (isPen) {
            penBtn.getStyleClass().add("mode-btn-active-pen");
            eraserBtn.getStyleClass().add("mode-btn");
        } else {
            eraserBtn.getStyleClass().add("mode-btn-active-eraser");
            penBtn.getStyleClass().add("mode-btn");
        }
    }

    private void refreshColorSectionVisibility() {
        boolean isPen = "pen".equals(drawingService.getMode());
        colorSection.setVisible(isPen);
        colorSection.setManaged(isPen);
    }

    private void refreshWidthLabel(int w) {
        boolean isPen = "pen".equals(drawingService.getMode());
        widthLabel.setText((isPen ? "WIDTH" : "ERASER") + ": " + w + "px");
    }

    private void refreshLayerButton() {
        boolean visible = drawingService.isLayersVisible();
        toggleLayersBtn.setText(visible ? "HIDE" : "SHOW");
    }

    private void refreshActionButtons() {
        boolean hasStrokes = drawingService.getStrokeCount() > 0;

        undoBtn.setDisable(!drawingService.isCanUndo());
        redoBtn.setDisable(!drawingService.isCanRedo());

        clearBtn.setDisable(!hasStrokes);
        if (hasStrokes) {
            clearBtn.getStyleClass().removeAll("ctrl-btn");
            if (!clearBtn.getStyleClass().contains("ctrl-btn-danger"))
                clearBtn.getStyleClass().add("ctrl-btn-danger");
        } else {
            clearBtn.getStyleClass().removeAll("ctrl-btn-danger");
            if (!clearBtn.getStyleClass().contains("ctrl-btn"))
                clearBtn.getStyleClass().add("ctrl-btn");
        }

        saveBtn.setDisable(!hasStrokes);
        if (hasStrokes) {
            saveBtn.getStyleClass().removeAll("ctrl-btn");
            if (!saveBtn.getStyleClass().contains("ctrl-btn-green"))
                saveBtn.getStyleClass().add("ctrl-btn-green");
        } else {
            saveBtn.getStyleClass().removeAll("ctrl-btn-green");
            if (!saveBtn.getStyleClass().contains("ctrl-btn"))
                saveBtn.getStyleClass().add("ctrl-btn");
        }
    }

    private void handleExit() {
        mapService.tabOpenProperty().set(false);
    }
}