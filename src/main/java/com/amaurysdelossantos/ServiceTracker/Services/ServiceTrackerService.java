package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.models.enums.View;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Component;

@Component
public class ServiceTrackerService {
    private final ObjectProperty<View> activeView = new SimpleObjectProperty<>();
    public ObjectProperty<View> activeViewProperty() { return activeView; }
}
