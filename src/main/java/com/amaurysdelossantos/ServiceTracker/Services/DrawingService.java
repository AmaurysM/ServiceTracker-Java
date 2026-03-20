package com.amaurysdelossantos.ServiceTracker.Services;

import javafx.beans.property.*;
import org.springframework.stereotype.Service;

@Service
public class DrawingService {

    private final BooleanProperty drawing = new SimpleBooleanProperty(false);
    public BooleanProperty drawingProperty()  { return drawing; }
    public boolean  isDrawing()               { return drawing.get(); }
    public void     setDrawing(boolean v)     { drawing.set(v); }
    public void     toggleDrawing()           { drawing.set(!drawing.get()); }

    private final StringProperty mode = new SimpleStringProperty("pen");
    public StringProperty modeProperty()      { return mode; }
    public String  getMode()                  { return mode.get(); }
    public void    setMode(String m)          { mode.set(m); }

    private final StringProperty color = new SimpleStringProperty("#FF0000");
    public StringProperty colorProperty()     { return color; }
    public String  getColor()                 { return color.get(); }
    public void    setColor(String c)         { color.set(c); }

    private final IntegerProperty strokeWidth = new SimpleIntegerProperty(4);
    public IntegerProperty strokeWidthProperty() { return strokeWidth; }
    public int  getStrokeWidth()                 { return strokeWidth.get(); }
    public void setStrokeWidth(int w)            { strokeWidth.set(w); }

    private final IntegerProperty opacity = new SimpleIntegerProperty(100);
    public IntegerProperty opacityProperty() { return opacity; }
    public int  getOpacity()                 { return opacity.get(); }
    public void setOpacity(int o)            { opacity.set(o); }

    private final BooleanProperty layersVisible = new SimpleBooleanProperty(true);
    public BooleanProperty layersVisibleProperty() { return layersVisible; }
    public boolean  isLayersVisible()              { return layersVisible.get(); }
    public void     toggleLayers()                 { layersVisible.set(!layersVisible.get()); }

    private final IntegerProperty strokeCount = new SimpleIntegerProperty(0);
    public IntegerProperty strokeCountProperty() { return strokeCount; }
    public int  getStrokeCount()                 { return strokeCount.get(); }
    public void setStrokeCount(int n)            { strokeCount.set(n); }

    private final BooleanProperty canUndo = new SimpleBooleanProperty(false);
    public BooleanProperty canUndoProperty() { return canUndo; }
    public boolean  isCanUndo()              { return canUndo.get(); }
    public void     setCanUndo(boolean v)    { canUndo.set(v); }

    private final BooleanProperty canRedo = new SimpleBooleanProperty(false);
    public BooleanProperty canRedoProperty() { return canRedo; }
    public boolean  isCanRedo()              { return canRedo.get(); }
    public void     setCanRedo(boolean v)    { canRedo.set(v); }

    private final BooleanProperty undoRequested  = new SimpleBooleanProperty(false);
    private final BooleanProperty redoRequested  = new SimpleBooleanProperty(false);
    private final BooleanProperty clearRequested = new SimpleBooleanProperty(false);
    private final BooleanProperty saveRequested  = new SimpleBooleanProperty(false);
    private final BooleanProperty loadRequested  = new SimpleBooleanProperty(false);

    public BooleanProperty undoRequestedProperty()  { return undoRequested; }
    public BooleanProperty redoRequestedProperty()  { return redoRequested; }
    public BooleanProperty clearRequestedProperty() { return clearRequested; }
    public BooleanProperty saveRequestedProperty()  { return saveRequested; }
    public BooleanProperty loadRequestedProperty()  { return loadRequested; }

    private void pulse(BooleanProperty p) {
        p.set(true);
        p.set(false);
    }

    public void requestUndo()  { pulse(undoRequested); }
    public void requestRedo()  { pulse(redoRequested); }
    public void requestClear() { pulse(clearRequested); }
    public void requestSave()  { pulse(saveRequested); }
    public void requestLoad()  { pulse(loadRequested); }
}