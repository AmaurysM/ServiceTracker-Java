package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Repository.ServiceItemRepo;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.StandardView;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Autowired
    private ServiceItemRepo serviceItemRepo;
    @Autowired
    private MongoTemplate mongoTemplate;
    private volatile boolean changeStreamRunning = false;
    private volatile Thread changeStreamThread;
    private volatile MongoChangeStreamCursor<ChangeStreamDocument<Document>> currentCursor;

    public void loadInitialData() {
        activityFilter.addListener((obs, oldVal, newVal) -> {
            reload();
            restartChangeStream();
        });
        serviceFilter.addListener((obs, oldVal, newVal) -> {
            reload();
            restartChangeStream();
        });
        timeFilter.addListener((obs, oldVal, newVal) -> {
            reload();
            restartChangeStream();
        });
        searchText.addListener((obs, oldVal, newVal) -> {
            reload();
            restartChangeStream();
        });

        reload();
        startChangeStreamListener();
    }

    private void reload() {
        ActivityFilter activity = activityFilter.get();
        TimeFilter time = timeFilter.get();
        ServiceFilter service = serviceFilter.get();
        String search = searchText.get();

        executor.submit(() -> {
            try {
                List<ServiceItem> results = serviceItemRepo.findWithFilters(activity, time, service, search);
                Platform.runLater(() -> items.setAll(results));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void restartChangeStream() {
        changeStreamRunning = false;
        if (currentCursor != null) {
            try {
                currentCursor.close(); // close the cursor directly, no interrupt
            } catch (Exception e) {
                // ignore, we're closing intentionally
            }
        }
        startChangeStreamListener();
    }


    private void startChangeStreamListener() {
        changeStreamRunning = true;

        Thread thread = new Thread(() -> {
            try {
                MongoCollection<Document> collection =
                        mongoTemplate.getCollection(
                                mongoTemplate.getCollectionName(ServiceItem.class)
                        );

                var cursor = collection.watch()
                        .fullDocument(FullDocument.UPDATE_LOOKUP)
                        .cursor();

                currentCursor = cursor; // save reference so we can close it

                while (changeStreamRunning) {
                    ChangeStreamDocument<Document> change;
                    try {
                        if (!cursor.hasNext()) continue;
                        change = cursor.next();
                    } catch (Exception e) {
                        break; // cursor was closed, exit cleanly
                    }

                    OperationType op = change.getOperationType();

                    if (op == OperationType.DELETE) {
                        assert change.getDocumentKey() != null;
                        org.bson.BsonValue idValue = change.getDocumentKey().get("_id");
                        String deletedId;
                        if (idValue.getBsonType() == org.bson.BsonType.OBJECT_ID) {
                            deletedId = idValue.asObjectId().getValue().toString();
                        } else {
                            deletedId = idValue.asString().getValue();
                        }
                        Platform.runLater(() ->
                                items.removeIf(i -> i.getId().equals(deletedId))
                        );
                        continue;
                    }

                    Document doc = change.getFullDocument();
                    if (doc == null) continue;

                    ServiceItem updatedItem =
                            mongoTemplate.getConverter().read(ServiceItem.class, doc);

                    Platform.runLater(() -> applyChange(updatedItem, op));
                }

            } catch (Exception e) {
                if (changeStreamRunning) {
                    e.printStackTrace(); // only log if it was unexpected
                }
            }
        });

        thread.setDaemon(true);
        thread.setName("change-stream-listener");
        changeStreamThread = thread;
        thread.start();
    }

    private void applyChange(ServiceItem updatedItem, OperationType op) {
        int index = findIndexById(updatedItem.getId());
        boolean matches = matchesCurrentFilter(updatedItem);

        switch (op) {
            case INSERT -> {
                if (matches) items.add(updatedItem);
            }
            case UPDATE, REPLACE -> {
                if (index != -1 && matches) items.set(index, updatedItem);
                else if (index != -1) items.remove(index);
                else if (matches) items.add(updatedItem);
            }
        }
    }

    private int findIndexById(String id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private boolean matchesCurrentFilter(ServiceItem item) {
        if (item.getDeletedAt() != null) return false;

        ActivityFilter activity = activityFilter.get();
        if (activity == ActivityFilter.ACTIVE && item.getCompletedAt() != null) return false;
        if (activity == ActivityFilter.DONE && item.getCompletedAt() == null) return false;

        ServiceFilter service = serviceFilter.get();
        if (service != null && service != ServiceFilter.ALL) {
            boolean matches = switch (service) {
                case FUEL -> item.getFuel() != null;
                case GPU -> item.getGpu() != null;
                case LAVATORY -> item.getLavatory() != null;
                case POTABLE_WATER -> item.getPotableWater() != null;
                case CATERING -> item.getCatering() != null && !item.getCatering().isEmpty();
                case WINDSHIELD_CLEANING -> item.getWindshieldCleaning() != null;
                case OIL_SERVICE -> item.getOilService() != null;
                default -> true;
            };
            if (!matches) return false;
        }

        TimeFilter time = timeFilter.get();
        if (time != null && item.getCreatedAt() != null) {
            ZonedDateTime now = ZonedDateTime.now();
            Instant start = switch (time) {
                case TODAY -> now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
                case WEEK -> now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay(now.getZone()).toInstant();
                case MONTH -> now.toLocalDate().withDayOfMonth(1).atStartOfDay(now.getZone()).toInstant();
            };
            if (item.getCreatedAt().isBefore(start)) return false;
            if (time == TimeFilter.TODAY) {
                Instant end = now.toLocalDate().atStartOfDay(now.getZone()).plusDays(1).toInstant();
                if (!item.getCreatedAt().isBefore(end)) return false;
            }
        }

        String search = searchText.get();
        if (search != null && !search.isBlank()) {
            String lower = search.toLowerCase();
            boolean tailMatch = item.getTail() != null && item.getTail().toLowerCase().contains(lower);
            boolean descMatch = item.getDescription() != null && item.getDescription().toLowerCase().contains(lower);
            if (!tailMatch && !descMatch) return false;
        }

        return true;
    }
}