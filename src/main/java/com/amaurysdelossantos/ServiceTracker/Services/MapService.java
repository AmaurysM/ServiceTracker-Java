package com.amaurysdelossantos.ServiceTracker.Services;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

@Component
public class MapService {
    private final ObjectProperty<Boolean> tabOpen = new SimpleObjectProperty<>();

    public ObjectProperty<Boolean> tabOpenProperty() {
        return tabOpen;
    }
}
