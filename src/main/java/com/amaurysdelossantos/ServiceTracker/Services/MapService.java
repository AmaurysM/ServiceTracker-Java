package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class MapService {
    private final ObjectProperty<Boolean> tabOpen = new SimpleObjectProperty<>();

    public ObjectProperty<Boolean> tabOpenProperty() {
        return tabOpen;
    }


    @Getter
    private final ObservableList<ServiceItem> items = FXCollections.observableArrayList();

    @Getter
    private final ObjectProperty<ServiceFilter> serviceFilter =
            new SimpleObjectProperty<>(ServiceFilter.ALL);

    @Getter
    private final ObjectProperty<TimeFilter> timeFilter =
            new SimpleObjectProperty<>(TimeFilter.TODAY);

    @Getter
    private final StringProperty searchText = new SimpleStringProperty("");

    /** "all" | "placed" | "notPlaced" */
    @Getter
    private final StringProperty placementFilter =
            new SimpleStringProperty("all");
}
