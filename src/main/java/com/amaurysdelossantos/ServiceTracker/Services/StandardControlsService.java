package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.StandardView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class StandardControlsService {

    @Getter
    private final ObservableList<ServiceItem> items = FXCollections.observableArrayList();
    @Getter
    private final StringProperty searchText = new SimpleStringProperty("");
    @Getter
    private final ObjectProperty<ServiceFilter> serviceFilter = new SimpleObjectProperty<>();
    @Getter
    private final ObjectProperty<TimeFilter> timeFilter = new SimpleObjectProperty<>();
    @Getter
    private final ObjectProperty<ActivityFilter> activityFilter = new SimpleObjectProperty<>();
    @Getter
    private final ObjectProperty<StandardView> activeView = new SimpleObjectProperty<>();
}