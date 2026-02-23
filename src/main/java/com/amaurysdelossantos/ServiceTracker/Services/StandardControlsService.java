package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Repository.ServiceItemRepo;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.StandardView;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class StandardControlsService {

    @Getter
    private final ObservableList<ServiceItem> items =
            FXCollections.observableArrayList();
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
    @Autowired
    private ServiceItemRepo serviceItemRepo;
    @Autowired
    private MongoTemplate mongoTemplate;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void loadInitialData() {
        activityFilter.addListener((obs, oldVal, newVal) -> loadByActivityFilter(newVal));
        loadByActivityFilter(activityFilter.get());

        startChangeStreamListener(); // 👈 start realtime listener
    }

    public List<ServiceItem> getActiveItems() {
        return serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNull();
    }

    public void loadByActivityFilter(ActivityFilter filter) {
        if (filter == null) {
            items.setAll(serviceItemRepo.findByDeletedAtIsNull());
            return;
        }
        switch (filter) {
            case ACTIVE -> items.setAll(serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNull());
            case DONE -> items.setAll(serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNotNull());
        }
    }

    private void startChangeStreamListener() {
        executor.submit(() -> {

            MongoCollection<org.bson.Document> collection =
                    mongoTemplate.getCollection(
                            mongoTemplate.getCollectionName(ServiceItem.class)
                    );

            collection.watch()
                    .fullDocument(FullDocument.UPDATE_LOOKUP)
                    .forEach(change -> {

                        org.bson.Document doc = change.getFullDocument();
                        if (doc == null) return;

                        ServiceItem updatedItem =
                                mongoTemplate.getConverter()
                                        .read(ServiceItem.class, doc);

                        Platform.runLater(() -> {
                            applyChange(updatedItem, change.getOperationType());
                        });
                    });
        });
    }

    private void applyChange(ServiceItem updatedItem, OperationType operationType) {

        switch (operationType) {

            case INSERT -> {
                if (matchesCurrentFilter(updatedItem)) {
                    items.add(updatedItem);
                }
            }

            case UPDATE, REPLACE -> {
                int index = findIndexById(updatedItem.getId());
                if (index != -1) {
                    if (matchesCurrentFilter(updatedItem)) {
                        items.set(index, updatedItem);
                    } else {
                        items.remove(index);
                    }
                }
            }

            case DELETE -> {
                items.removeIf(i -> i.getId().equals(updatedItem.getId()));
            }
        }
    }

    private int findIndexById(String id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesCurrentFilter(ServiceItem item) {
        ActivityFilter filter = activityFilter.get();

        if (filter == null) return true;

        return switch (filter) {
            case ACTIVE -> item.getCompletedAt() == null && item.getDeletedAt() == null;
            case DONE -> item.getCompletedAt() != null && item.getDeletedAt() == null;
        };
    }

}
