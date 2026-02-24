package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.models.enums.views.ServiceView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

@Component
public class ServiceTrackerService {
    private final ObjectProperty<ServiceView> activeView = new SimpleObjectProperty<>();

    public ObjectProperty<ServiceView> activeViewProperty() {
        return activeView;
    }
}
