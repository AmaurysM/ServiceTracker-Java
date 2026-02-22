package com.amaurysdelossantos.ServiceTracker.Controllers;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import java.util.List;
import java.util.Map;

/**
 * Map view using JavaFX WebView + Leaflet.js + OpenStreetMap.
 * <p>
 * NOTE: buildLeafletHtml() uses plain string concatenation — NOT String.formatted()
 * or String.format() — because the HTML/CSS/JS contains many literal '%' characters
 * and '{' '}' braces that would be misinterpreted as format specifiers.
 */
public class MapViewController {

    @FXML
    private StackPane mapContainer;

    @FXML
    private Text coordText;     // optional — null if info bar is hidden in FXML
    @FXML
    private Text itemCountText; // optional — null if info bar is hidden in FXML

    private WebView webView;
    private WebEngine engine;
    private boolean mapReady = false;
    private List<ServiceItem> pendingItems = null;

    private static final double DEFAULT_LAT = 25.7959;
    private static final double DEFAULT_LON = -80.2870;
    private static final int DEFAULT_ZOOM = 14;

    private static final Interpolator EASE = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        webView = new WebView();
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.setContextMenuEnabled(false);
        engine = webView.getEngine();

        mapContainer.getChildren().add(webView);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Expose Java bridge to JS as window.java
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("java", new JavaBridge());
                mapReady = true;

                if (pendingItems != null) {
                    //renderPins(pendingItems);
                    pendingItems = null;
                }
            }
        });

        engine.loadContent(buildLeafletHtml());

    }

    // ── Public API ─────────────────────────────────────────────────────────────

//    public void setItems(List<ServiceItem> items) {
//        if (!mapReady) {
//            pendingItems = items;
//            return;
//        }
//        //renderPins(items);
//    }

    /**
     * Tells Leaflet to re-measure its container and fill any missing tiles.
     * Call every time the map panel transitions from hidden → visible.
     */
    public void invalidateSize() {
        runJs("if (typeof map !== 'undefined') { map.invalidateSize(); }");
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

//    private void renderPins(List<ServiceItem> items) {
//        long count = items.stream().filter(i -> extractCoords(i) != null).count();
//
//        String pinsJson = items.stream()
//                .filter(i -> extractCoords(i) != null)
//                .map(item -> {
//                    Double[] c   = extractCoords(item);
//                    Double   rot = extractRotation(item);
//                    return "{lat:" + c[0]
//                            + ",lng:" + c[1]
//                            + ",rotation:" + (rot != null ? rot : 0.0)
//                            + ",tail:\"" + escapeJs(item.getTail() != null ? item.getTail() : "?") + "\""
//                            + ",tooltip:\"" + escapeJs(buildTooltip(item)) + "\"}";
//                })
//                .collect(Collectors.joining(",", "[", "]"));
//
//        String countText = count + " location" + (count == 1 ? "" : "s") + " on map";
//
//        // Build JS with plain concatenation — no String.format() calls
//        String js = "(function() {"
//                + "  if (window.pinMarkers) {"
//                + "    window.pinMarkers.forEach(function(m){ map.removeLayer(m); });"
//                + "  }"
//                + "  window.pinMarkers = [];"
//                + "  var pins = " + pinsJson + ";"
//                + "  pins.forEach(function(p) {"
//                + "    var html = '<div class=\"pin-wrap\">'"
//                + "      + '<div class=\"pin-icon\" style=\"transform:rotate(' + (p.rotation - 90) + 'deg)\">&#9992;</div>'"
//                + "      + '<div class=\"pin-label\">' + p.tail + '</div></div>';"
//                + "    var icon = L.divIcon({"
//                + "      className: '',"
//                + "      html: html,"
//                + "      iconAnchor: [24, 44],"
//                + "      iconSize: [48, 48]"
//                + "    });"
//                + "    var marker = L.marker([p.lat, p.lng], {icon: icon})"
//                + "      .bindTooltip(p.tooltip, {direction:'top', offset:[0,-40]})"
//                + "      .addTo(map);"
//                + "    window.pinMarkers.push(marker);"
//                + "  });"
//                + "  if (pins.length > 0) {"
//                + "    map.setView([pins[0].lat, pins[0].lng], map.getZoom());"
//                + "  }"
//                + "  if (window.java) window.java.updateCount('" + escapeJs(countText) + "');"
//                + "})();";
//
//        runJs(js);
//    }

    // ── Coordinate helpers ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Double[] extractCoords(ServiceItem item) {
        Map<String, Object> pos = getMapPosition(item);
        if (pos == null) return null;
        Object x = pos.get("x"), y = pos.get("y");
        if (!(x instanceof Number) || !(y instanceof Number)) return null;
        return new Double[]{((Number) x).doubleValue(), ((Number) y).doubleValue()};
    }

    @SuppressWarnings("unchecked")
    private Double extractRotation(ServiceItem item) {
        Map<String, Object> pos = getMapPosition(item);
        if (pos == null) return null;
        Object r = pos.get("rotation");
        return (r instanceof Number) ? ((Number) r).doubleValue() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapPosition(ServiceItem item) {
        if (item.getMetadata() == null) return null;
        Object mp = item.getMetadata().get("mapPosition");
        return (mp instanceof Map) ? (Map<String, Object>) mp : null;
    }

    // ── JS helpers ────────────────────────────────────────────────────────────

    private void runJs(String script) {
        Platform.runLater(() -> {
            try {
                engine.executeScript(script);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String buildTooltip(ServiceItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tail: ").append(item.getTail() != null ? item.getTail() : "N/A").append("\n");
        if (item.getFuel() != null) sb.append("Fuel: ✓\n");
        if (item.getCatering() != null && !item.getCatering().isEmpty())
            sb.append("Catering: ").append(item.getCatering().size()).append(" item(s)\n");
        if (item.getGpu() != null) sb.append("GPU: ✓\n");
        if (item.getLavatory() != null) sb.append("Lavatory: ✓\n");
        if (item.getPotableWater() != null) sb.append("Potable Water: ✓\n");
        if (item.getWindshieldCleaning() != null) sb.append("Windshield: ✓\n");
        if (item.getOilService() != null) sb.append("Oil Service: ✓\n");
        if (item.getDescription() != null && !item.getDescription().isBlank())
            sb.append("Note: ").append(item.getDescription());
        return sb.toString().trim();
    }

    // ── Java ↔ JS Bridge ─────────────────────────────────────────────────────

    /**
     * Public inner class — methods callable from JS as window.java.xxx()
     */
    public class JavaBridge {
        public void updateCoords(String text) {
            Platform.runLater(() -> {
                if (coordText != null) coordText.setText(text);
            });
        }

        public void updateCount(String text) {
            Platform.runLater(() -> {
                if (itemCountText != null) itemCountText.setText(text);
            });
        }
    }

    // ── Button animations ─────────────────────────────────────────────────────

    private void attachBtnAnim(Button btn) {
        btn.setOnMouseEntered(e -> scale(btn, 1.12));
        btn.setOnMouseExited(e -> scale(btn, 1.00));
        btn.setOnMousePressed(e -> scale(btn, 0.93));
        btn.setOnMouseReleased(e -> scale(btn, 1.12));
    }

    private void scale(Node n, double s) {
        new Timeline(new KeyFrame(Duration.millis(120),
                new KeyValue(n.scaleXProperty(), s, EASE),
                new KeyValue(n.scaleYProperty(), s, EASE))).play();
    }

    // ── Embedded Leaflet HTML ─────────────────────────────────────────────────
    //
    // IMPORTANT: Do NOT use String.format() / .formatted() on this string.
    // CSS uses '%' (e.g. in @keyframes), and JS uses '{s}' patterns — both
    // would be misread as Java format specifiers and throw UnknownFormatConversionException.
    // Values are injected via plain string concatenation only.
    //
    private String buildLeafletHtml() {
        return "<!DOCTYPE html>"
                + "<html><head>"
                + "<meta charset=\"utf-8\"/>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "html,body,#map { width:100%; height:100%; }"

                // Loading overlay
                + "#loading {"
                + "  position:absolute; inset:0; z-index:9999;"
                + "  display:flex; flex-direction:column;"
                + "  align-items:center; justify-content:center;"
                + "  background:#eef2f7; gap:14px;"
                + "  font-family:system-ui,sans-serif;"
                + "}"
                + ".spinner {"
                + "  width:42px; height:42px;"
                + "  border:4px solid #c8d5e8;"
                + "  border-top-color:#2E86DE;"
                + "  border-radius:50%;"
                + "  animation:spin 0.75s linear infinite;"
                + "}"
                + "@keyframes spin { to { transform:rotate(360deg); } }"
                + "#loading p { font-size:14px; color:#4a5568; font-weight:500; }"

                // Aircraft pin
                + ".pin-wrap {"
                + "  display:flex; flex-direction:column; align-items:center;"
                + "  filter:drop-shadow(0 2px 6px rgba(0,0,0,0.4));"
                + "  cursor:pointer;"
                + "  transition:transform 0.15s ease;"
                + "}"
                + ".pin-wrap:hover { transform:translateY(-3px); }"
                + ".pin-icon {"
                + "  font-size:26px; line-height:1; color:#2E86DE;"
                + "  display:inline-block; transition:color 0.15s;"
                + "}"
                + ".pin-wrap:hover .pin-icon { color:#1a5fa8; }"
                + ".pin-label {"
                + "  margin-top:2px; font-size:10px; font-weight:700;"
                + "  color:#1E2A3A; background:rgba(255,255,255,0.92);"
                + "  padding:1px 5px; border-radius:4px;"
                + "  border:1px solid rgba(30,42,58,0.2);"
                + "  white-space:nowrap; pointer-events:none;"
                + "}"

                // Leaflet tooltip overrides
                + ".leaflet-tooltip {"
                + "  font-family:system-ui,sans-serif; font-size:12px;"
                + "  background:#1E2A3A; color:#fff; border:none;"
                + "  border-radius:6px; padding:7px 11px;"
                + "  box-shadow:0 4px 14px rgba(0,0,0,0.3);"
                + "  white-space:pre-line; line-height:1.5;"
                + "}"
                + ".leaflet-tooltip::before { border-top-color:#1E2A3A !important; }"
                + "</style>"

                // Leaflet from CDN
                + "<link rel=\"stylesheet\""
                + " href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\"/>"
                + "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>"
                + "</head><body>"

                // Loading overlay markup
                + "<div id=\"loading\">"
                + "  <div class=\"spinner\"></div>"
                + "  <p>Loading map\u2026</p>"
                + "</div>"

                + "<div id=\"map\"></div>"

                + "<script>"
                + "var map = L.map('map', {"
                + "  center: [" + DEFAULT_LAT + ", " + DEFAULT_LON + "],"
                + "  zoom: " + DEFAULT_ZOOM + ","
                + "  zoomControl: false"
                + "});"

                + "var tileLayer = L.tileLayer("
                + "  'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {"
                + "    attribution: '\u00a9 <a href=\"https://openstreetmap.org\">OpenStreetMap</a> contributors',"
                + "    maxZoom: 19"
                + "  }).addTo(map);"

                // Leaflet measures the container size at init time. In a WebView the container
                // may still be 0x0 at that moment, causing gray/missing tiles.
                // invalidateSize() tells Leaflet to re-measure and re-request the correct tiles.
                + "map.invalidateSize();"
                + "setTimeout(function() { map.invalidateSize(); }, 150);"
                + "setTimeout(function() { map.invalidateSize(); }, 600);"

                // Hide loading once tiles start rendering
                + "tileLayer.once('load', function() {"
                + "  map.invalidateSize();"
                + "  var el = document.getElementById('loading');"
                + "  if (el) el.style.display = 'none';"
                + "});"
                // Fallback: hide after 4 s regardless
                + "setTimeout(function() {"
                + "  map.invalidateSize();"
                + "  var el = document.getElementById('loading');"
                + "  if (el) el.style.display = 'none';"
                + "}, 4000);"

                // Live coordinate readout → Java bridge
                + "map.on('mousemove', function(e) {"
                + "  if (window.java) {"
                + "    window.java.updateCoords("
                + "      'Lat: ' + e.latlng.lat.toFixed(4)"
                + "      + '   Lon: ' + e.latlng.lng.toFixed(4));"
                + "  }"
                + "});"

                + "window.pinMarkers = [];"
                + "</script>"
                + "</body></html>";
    }
}