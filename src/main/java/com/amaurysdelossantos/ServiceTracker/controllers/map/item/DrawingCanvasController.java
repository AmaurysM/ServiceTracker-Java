package com.amaurysdelossantos.ServiceTracker.controllers.map.item;

import com.amaurysdelossantos.ServiceTracker.Services.DrawingService;
import fxmapcontrol.Location;
import fxmapcontrol.Map;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class DrawingCanvasController {

    @Autowired private DrawingService drawingService;

    private Canvas canvas;
    private Map    fxMap;

    private record GeoPoint(double lat, double lon) {}

    private record Stroke(List<GeoPoint> points, String color, int width, double opacity) {}

    private final Deque<Stroke> undoStack = new ArrayDeque<>();
    private final Deque<Stroke> redoStack = new ArrayDeque<>();
    private Stroke currentStroke = null;

    private double cursorX = -1, cursorY = -1;

    public void install(Map map, Pane parentPane) {
        this.fxMap = map;

        canvas = new Canvas();
        canvas.setMouseTransparent(true);
        canvas.setPickOnBounds(false);
        canvas.widthProperty().bind(map.widthProperty());
        canvas.heightProperty().bind(map.heightProperty());
        map.getChildren().add(canvas);

        canvas.widthProperty().addListener((o, p, n)    -> redrawAll());
        canvas.heightProperty().addListener((o, p, n)   -> redrawAll());
        fxMap.centerProperty().addListener((o, p, n)    -> redrawAll());
        fxMap.zoomLevelProperty().addListener((o, p, n) -> redrawAll());

        drawingService.drawingProperty().addListener((obs, o, active) ->
                Platform.runLater(() -> setDrawingActive(active)));
        drawingService.layersVisibleProperty().addListener((obs, o, n) ->
                Platform.runLater(this::redrawAll));
        drawingService.undoRequestedProperty().addListener((obs, o, n)  -> { if (n) Platform.runLater(this::undo); });
        drawingService.redoRequestedProperty().addListener((obs, o, n)  -> { if (n) Platform.runLater(this::redo); });
        drawingService.clearRequestedProperty().addListener((obs, o, n) -> { if (n) Platform.runLater(this::clearAll); });
        drawingService.saveRequestedProperty().addListener((obs, o, n)  -> { if (n) Platform.runLater(this::saveToFile); });
        drawingService.loadRequestedProperty().addListener((obs, o, n)  -> { if (n) Platform.runLater(this::loadFromFile); });

        canvas.setOnMousePressed(e -> {
            if (!drawingService.isDrawing() || e.getButton() != MouseButton.PRIMARY) return;
            if ("eraser".equals(drawingService.getMode())) {
                eraseAt(e.getX(), e.getY());
            } else {
                startStroke(e.getX(), e.getY());
            }
            e.consume();
        });
        canvas.setOnMouseDragged(e -> {
            if (!drawingService.isDrawing() || e.getButton() != MouseButton.PRIMARY) return;
            if ("eraser".equals(drawingService.getMode())) {
                eraseAt(e.getX(), e.getY());
            } else if (currentStroke != null) {
                extendStroke(e.getX(), e.getY());
            }
            e.consume();
        });
        canvas.setOnMouseReleased(e -> {
            if (!drawingService.isDrawing() || e.getButton() != MouseButton.PRIMARY) return;
            if (!"eraser".equals(drawingService.getMode()) && currentStroke != null) {
                finishStroke();
            }
            e.consume();
        });
        canvas.setOnMouseMoved(e -> {
            if (!drawingService.isDrawing()) return;
            cursorX = e.getX();
            cursorY = e.getY();
            if ("eraser".equals(drawingService.getMode())) redrawAll();
        });
        canvas.setOnMouseExited(e -> {
            cursorX = -1;
            cursorY = -1;
            redrawAll();
        });
    }

    private Location pixelToGeo(double px, double py) {
        return fxMap.viewToLocation(new Point2D(px, py));
    }

    private Point2D geoToPixel(double lat, double lon) {
        return fxMap.locationToView(new Location(lat, lon));
    }

    private void startStroke(double px, double py) {
        Location geo = pixelToGeo(px, py);
        List<GeoPoint> pts = new ArrayList<>();
        pts.add(new GeoPoint(geo.getLatitude(), geo.getLongitude()));
        currentStroke = new Stroke(pts, drawingService.getColor(),
                drawingService.getStrokeWidth(), drawingService.getOpacity() / 100.0);
        redrawAll();
    }

    private void extendStroke(double px, double py) {
        List<GeoPoint> pts = currentStroke.points();
        if (!pts.isEmpty()) {
            GeoPoint last = pts.get(pts.size() - 1);
            Point2D  lPx  = geoToPixel(last.lat(), last.lon());
            double   dx   = px - lPx.getX();
            double   dy   = py - lPx.getY();
            if (dx * dx + dy * dy < 4) return;
        }
        Location geo = pixelToGeo(px, py);
        pts.add(new GeoPoint(geo.getLatitude(), geo.getLongitude()));
        redrawAll();
    }

    private void finishStroke() {
        undoStack.push(currentStroke);
        redoStack.clear();
        currentStroke = null;
        syncServiceCounts();
        redrawAll();
    }

    private void eraseAt(double px, double py) {
        double radius = drawingService.getStrokeWidth() * 3.0 / 2.0;
        boolean changed = undoStack.removeIf(s -> strokeHitTest(s, px, py, radius));
        if (changed) {
            redoStack.clear();
            syncServiceCounts();
            redrawAll();
        }
    }

    private boolean strokeHitTest(Stroke s, double px, double py, double radius) {
        List<GeoPoint> pts = s.points();
        if (pts.isEmpty()) return false;

        List<Point2D> projected = project(pts);

        if (projected.size() == 1) {
            return dist(projected.get(0).getX(), projected.get(0).getY(), px, py) <= radius;
        }

        for (int i = 1; i < projected.size(); i++) {
            if (distToSegment(px, py,
                    projected.get(i - 1).getX(), projected.get(i - 1).getY(),
                    projected.get(i).getX(),     projected.get(i).getY()) <= radius) {
                return true;
            }
        }
        return false;
    }

    private double dist(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double distToSegment(double px, double py,
                                 double ax, double ay,
                                 double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return dist(px, py, ax, ay);
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        return dist(px, py, ax + t * dx, ay + t * dy);
    }

    private void redrawAll() {
        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (!drawingService.isLayersVisible()) {
            drawEraserCursor(gc);
            return;
        }

        List<Stroke> ordered = new ArrayList<>(undoStack);
        java.util.Collections.reverse(ordered);
        for (Stroke s : ordered) renderPenStroke(gc, s);
        if (currentStroke != null) renderPenStroke(gc, currentStroke);

        drawEraserCursor(gc);
    }

    private void renderPenStroke(GraphicsContext gc, Stroke s) {
        List<GeoPoint> pts = s.points();
        if (pts.isEmpty()) return;

        List<Point2D> px = project(pts);

        gc.save();
        gc.setGlobalAlpha(s.opacity());
        gc.setStroke(Color.web(s.color()));
        gc.setLineWidth(s.width());
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        if (px.size() == 1) {
            double r = s.width() / 2.0;
            gc.setFill(Color.web(s.color()));
            gc.fillOval(px.get(0).getX() - r, px.get(0).getY() - r, r * 2, r * 2);
        } else if (px.size() == 2) {
            gc.beginPath();
            gc.moveTo(px.get(0).getX(), px.get(0).getY());
            gc.lineTo(px.get(1).getX(), px.get(1).getY());
            gc.stroke();
        } else {
            gc.beginPath();
            gc.moveTo(px.get(0).getX(), px.get(0).getY());
            for (int i = 1; i < px.size() - 1; i++) {
                Point2D curr = px.get(i);
                Point2D next = px.get(i + 1);
                double  midX = (curr.getX() + next.getX()) / 2.0;
                double  midY = (curr.getY() + next.getY()) / 2.0;
                gc.quadraticCurveTo(curr.getX(), curr.getY(), midX, midY);
            }
            Point2D secondLast = px.get(px.size() - 2);
            Point2D last       = px.get(px.size() - 1);
            gc.quadraticCurveTo(secondLast.getX(), secondLast.getY(),
                    last.getX(), last.getY());
            gc.stroke();
        }
        gc.restore();
    }

    private List<Point2D> project(List<GeoPoint> pts) {
        List<Point2D> px = new ArrayList<>(pts.size());
        for (GeoPoint gp : pts) px.add(geoToPixel(gp.lat(), gp.lon()));
        return px;
    }

    private void drawEraserCursor(GraphicsContext gc) {
        if ("eraser".equals(drawingService.getMode()) && drawingService.isDrawing() && cursorX >= 0) {
            gc.save();
            gc.setStroke(Color.web("#374151"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4, 4);
            double r = drawingService.getStrokeWidth() * 1.5;
            gc.strokeOval(cursorX - r, cursorY - r, r * 2, r * 2);
            gc.restore();
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) { redoStack.push(undoStack.pop()); syncServiceCounts(); redrawAll(); }
    }

    private void redo() {
        if (!redoStack.isEmpty()) { undoStack.push(redoStack.pop()); syncServiceCounts(); redrawAll(); }
    }

    private void clearAll() {
        undoStack.clear();
        redoStack.clear();
        currentStroke = null;
        syncServiceCounts();
        redrawAll();
    }

    private void syncServiceCounts() {
        drawingService.setStrokeCount(undoStack.size());
        drawingService.setCanUndo(!undoStack.isEmpty());
        drawingService.setCanRedo(!redoStack.isEmpty());
    }

    private void saveToFile() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Drawing");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fc.setInitialFileName("drawing.png");
        File file = fc.showSaveDialog(canvas.getScene().getWindow());
        if (file == null) return;
        try {
            ImageIO.write(
                    javafx.embed.swing.SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null),
                    "png", file);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void loadFromFile() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Load Drawing");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fc.showOpenDialog(canvas.getScene().getWindow());
        if (file == null) return;
        try {
            javafx.scene.image.WritableImage img =
                    javafx.embed.swing.SwingFXUtils.toFXImage(ImageIO.read(file), null);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(img, 0, 0, canvas.getWidth(), canvas.getHeight());
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void setDrawingActive(boolean active) {
        canvas.setMouseTransparent(!active);
        canvas.setPickOnBounds(false);
        if (!active) { currentStroke = null; cursorX = -1; cursorY = -1; }
        redrawAll();
    }

    public Canvas getCanvas() { return canvas; }
}