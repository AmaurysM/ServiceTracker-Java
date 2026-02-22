package com.amaurysdelossantos.ServiceTracker.models.enums;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import lombok.Getter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public enum ServiceType {

    FUEL("Fuel", new Color(245, 158, 11), "/icons/gas-pump-solid.svg"),
    CATERING("Catering", new Color(16, 185, 129), "/icons/utensils-solid.svg"),
    GPU("GPU", new Color(59, 130, 246), "/icons/car-battery-solid.svg"),
    LAVATORY("Lavatory", new Color(99, 102, 241), "/icons/toilet-solid.svg"),
    POTABLE_WATER("Potable water", new Color(6, 182, 212), "/icons/tint-solid.svg"),
    WINDSHIELD_CLEANING("Windshield Cleaning", new Color(244, 63, 94), "/icons/spray-bottle.svg"),
    OIL_SERVICE("Oil Service", new Color(249, 115, 22), "/icons/oil-can-solid.svg");

    @Getter
    private final String type;
    @Getter
    private final Color primaryColor;
    private final String iconPath;

    ServiceType(String type, Color primaryColor, String iconPath) {
        this.type = type;
        this.primaryColor = primaryColor;
        this.iconPath = iconPath;
    }

    public BufferedImage getImage(int size) {
        SVGLoader loader = new SVGLoader();
        URL url = getClass().getResource(iconPath);
        SVGDocument doc = loader.load(url);

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,      RenderingHints.VALUE_STROKE_PURE);
        doc.render(null, g, new ViewBox(0, 0, size, size));
        g.dispose();
        return image;
    }
}