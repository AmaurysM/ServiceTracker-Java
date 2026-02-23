package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Repository.ServiceItemRepo;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.StandardView;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StandardControlsService {

    @Autowired
    private ServiceItemRepo serviceItemRepo;

    @Getter
    private final ObservableList<ServiceItem> items =
            FXCollections.observableArrayList();

    public void loadInitialData() {
        activityFilter.addListener((obs, oldVal, newVal) -> loadByActivityFilter(newVal));
        loadByActivityFilter(activityFilter.get());
    }

    public List<ServiceItem> getActiveItems() {
        return serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNull();
    }

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
    @Getter
    private final ObservableMap<ServiceItem, Node> nodeCache = new SimpleMapProperty<>();

//    public ObservableMap<ServiceItem, Node> getNodeCache() {return nodeCache;}
//
//    public StringProperty searchTextProperty() {
//        return searchText;
//    }
//
//    public ObjectProperty<StandardView> activeViewProperty() {
//        return activeView;
//    }
//
//    public ObjectProperty<ActivityFilter> activeActivity() {
//        return activityFilter;
//    }

    public void loadByActivityFilter(ActivityFilter filter) {
        if (filter == null) {
            items.setAll(serviceItemRepo.findByDeletedAtIsNull());
            return;
        }
        switch (filter) {
            case ACTIVE ->    items.setAll(serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNull());
            case DONE -> items.setAll(serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNotNull());
        }
    }

}
