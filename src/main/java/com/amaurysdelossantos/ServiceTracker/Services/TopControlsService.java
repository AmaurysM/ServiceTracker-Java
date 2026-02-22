package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.StandardView;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.springframework.stereotype.Component;

@Component
public class TopControlsService {

    private final StringProperty searchText = new SimpleStringProperty("");
    private final ObjectProperty<ServiceFilter> serviceFilter = new SimpleObjectProperty<>();
    private final ObjectProperty<TimeFilter> timeFilter = new SimpleObjectProperty<>();
    private final ObjectProperty<ActivityFilter> activityFilter = new SimpleObjectProperty<>();
    private final ObjectProperty<StandardView> activeView = new SimpleObjectProperty<>();

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public ObjectProperty<StandardView> activeViewProperty() {
        return activeView;
    }

    public ObjectProperty<ActivityFilter> activeActivity() {
        return activityFilter;
    }

}
