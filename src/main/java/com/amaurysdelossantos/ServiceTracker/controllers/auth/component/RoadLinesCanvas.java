package com.amaurysdelossantos.ServiceTracker.controllers.auth.component;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Replicates the AnimatedTaxiLines Next.js canvas background.
 * Asphalt gradient bg + animated bezier road lane lines in yellow/white.
 */
public class RoadLinesCanvas extends Canvas {

    private static final int MAX_LINES = 8;
    private static final Random RNG = new Random();

    private final List<RoadLine> lines = new ArrayList<>();
    private AnimationTimer timer;

    public RoadLinesCanvas() {
        // Bind size to parent later via AuthController
        widthProperty().addListener(e -> redraw());
        heightProperty().addListener(e -> redraw());
        startAnimation();
    }

    public void startAnimation() {
        if (timer != null) timer.stop();
        // Seed with a few lines
        for (int i = 0; i < 3; i++) lines.add(createLine());

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                animateTick();
            }
        };
        timer.start();
    }

    public void stopAnimation() {
        if (timer != null) timer.stop();
    }

    private void animateTick() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // ── Asphalt background ──────────────────────────────────
        javafx.scene.paint.LinearGradient asphalt = new javafx.scene.paint.LinearGradient(
                0, 0, 0, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#1f2937")),
                new javafx.scene.paint.Stop(1, Color.web("#020617"))
        );
        gc.setFill(asphalt);
        gc.fillRect(0, 0, w, h);

        // Subtle aggregate texture overlay (dot pattern)
        gc.setFill(Color.rgb(255, 255, 255, 0.025));
        for (double x = 0; x < w; x += 6) {
            for (double y = 0; y < h; y += 6) {
                if (RNG.nextFloat() < 0.25) {
                    gc.fillOval(x, y, 1.2, 1.2);
                }
            }
        }

        // ── Spawn new lines ─────────────────────────────────────
        if (lines.size() < MAX_LINES && RNG.nextDouble() < 0.02) {
            lines.add(createLine());
        }

        // ── Draw and advance lines ──────────────────────────────
        lines.removeIf(line -> {
            line.progress = Math.min(line.progress + line.speed, 1.0);
            line.age++;

            double opacity = 1.0;

            // Fade out when complete
            if (line.progress >= 1.0) {
                if (line.fadeStartMs < 0) line.fadeStartMs = System.currentTimeMillis() + 500;
            }
            if (line.fadeStartMs > 0) {
                long elapsed = System.currentTimeMillis() - line.fadeStartMs;
                opacity = Math.max(0, 1.0 - elapsed / 800.0);
            }

            // Age-based fade (last 30% of life)
            double ageRatio = (double) line.age / line.maxAge;
            if (ageRatio > 0.7) {
                double ageOpacity = 1.0 - (ageRatio - 0.7) / 0.3;
                opacity = Math.min(opacity, ageOpacity);
            }

            if (opacity <= 0 || line.age >= line.maxAge) return true;

            drawTaxiLine(gc, line, opacity, w, h);
            return false;
        });
    }

    private void drawTaxiLine(GraphicsContext gc, RoadLine line, double opacity,
                              double w, double h) {
        int segments = 100;
        double edgeOffset = 14;
        int steps = (int) (segments * line.progress);

        // White edge lines
        gc.setStroke(Color.rgb(255, 255, 255, opacity * 0.35));
        gc.setLineWidth(1.8);
        gc.setLineDashes();

        drawOffsetCurve(gc, line, steps, segments, -edgeOffset);
        drawOffsetCurve(gc, line, steps, segments, edgeOffset);

        // Yellow center dashed line
        gc.setStroke(Color.rgb(250, 204, 21, opacity * 0.50));
        gc.setLineWidth(2.8);
        gc.setLineDashes(18, 12);
        gc.setLineDashOffset(0);

        drawOffsetCurve(gc, line, steps, segments, 0);

        gc.setLineDashes(); // reset dash
    }

    private void drawOffsetCurve(GraphicsContext gc, RoadLine line,
                                 int steps, int segments, double offset) {
        gc.beginPath();
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / segments;
            if (t > line.progress) break;

            double[] pt = getPointOnCurve(line, t, offset);
            if (i == 0) gc.moveTo(pt[0], pt[1]);
            else gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
    }

    private double[] getPointOnCurve(RoadLine line, double t, double offset) {
        double x = bezier(t, line.sx, line.cp1x, line.cp2x, line.ex);
        double y = bezier(t, line.sy, line.cp1y, line.cp2y, line.ey);
        if (offset == 0) return new double[]{x, y};

        double dx = bezierTangent(t, line.sx, line.cp1x, line.cp2x, line.ex);
        double dy = bezierTangent(t, line.sy, line.cp1y, line.cp2y, line.ey);
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return new double[]{x, y};
        return new double[]{x + (-dy / len) * offset, y + (dx / len) * offset};
    }

    private double bezier(double t, double p0, double p1, double p2, double p3) {
        double mt = 1 - t;
        return mt * mt * mt * p0 + 3 * mt * mt * t * p1
                + 3 * mt * t * t * p2 + t * t * t * p3;
    }

    private double bezierTangent(double t, double p0, double p1, double p2, double p3) {
        double mt = 1 - t;
        return -3 * mt * mt * p0 + 3 * mt * mt * p1
                - 6 * mt * t * p1 - 3 * t * t * p2
                + 6 * mt * t * p2 + 3 * t * t * p3;
    }

    private RoadLine createLine() {
        double w = Math.max(getWidth(), 800);
        double h = Math.max(getHeight(), 600);
        double margin = 200;

        double sx, sy, ex, ey;
        int edge = RNG.nextInt(4);
        switch (edge) {
            case 0 -> { sx = rand(-margin, w + margin); sy = -margin;    ex = rand(-margin, w + margin); ey = h + margin; }
            case 1 -> { sx = w + margin; sy = rand(-margin, h + margin); ex = -margin;    ey = rand(-margin, h + margin); }
            case 2 -> { sx = rand(-margin, w + margin); sy = h + margin; ex = rand(-margin, w + margin); ey = -margin; }
            default-> { sx = -margin;   sy = rand(-margin, h + margin); ex = w + margin; ey = rand(-margin, h + margin); }
        }

        double off = 300 + RNG.nextDouble() * 200;
        double cp1x = sx + (ex - sx) * 0.33 + (RNG.nextDouble() - 0.5) * off;
        double cp1y = sy + (ey - sy) * 0.33 + (RNG.nextDouble() - 0.5) * off;
        double cp2x = sx + (ex - sx) * 0.66 + (RNG.nextDouble() - 0.5) * off;
        double cp2y = sy + (ey - sy) * 0.66 + (RNG.nextDouble() - 0.5) * off;

        RoadLine l = new RoadLine();
        l.sx = sx; l.sy = sy; l.ex = ex; l.ey = ey;
        l.cp1x = cp1x; l.cp1y = cp1y; l.cp2x = cp2x; l.cp2y = cp2y;
        l.speed = 0.002 + RNG.nextDouble() * 0.003;
        l.maxAge = 400 + (int) (RNG.nextDouble() * 300);
        return l;
    }

    private double rand(double min, double max) {
        return min + RNG.nextDouble() * (max - min);
    }

    private void redraw() { /* triggered by size change — animation handles it */ }

    // ── Inner data class ─────────────────────────────────────────
    private static class RoadLine {
        double sx, sy, ex, ey;
        double cp1x, cp1y, cp2x, cp2y;
        double progress = 0;
        double speed;
        int age = 0;
        int maxAge;
        long fadeStartMs = -1;
    }
}