package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.models.enums.views.MainView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

@Component
public class MainService {
    private final ObjectProperty<MainView> activeView = new SimpleObjectProperty<>();

    public ObjectProperty<MainView> activeViewProperty() {
        return activeView;
    }
}
