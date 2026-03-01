package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Repository.ServiceItemRepo;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import javafx.application.Platform;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class DataService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    @Autowired
    private ServiceItemRepo serviceItemRepo;
    @Autowired
    private MongoTemplate mongoTemplate;
    private volatile boolean changeStreamRunning = false;
    private volatile MongoChangeStreamCursor<ChangeStreamDocument<Document>> currentCursor;

    public void reload(ActivityFilter activity, TimeFilter time,
                       ServiceFilter service, String search,
                       Consumer<List<ServiceItem>> onResult) {
        executor.submit(() -> {
            try {
                List<ServiceItem> results = serviceItemRepo.findWithFilters(activity, time, service, search);
                Platform.runLater(() -> onResult.accept(results));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void startChangeStream(Consumer<ChangeStreamDocument<Document>> onChange) {
        changeStreamRunning = true;

        Thread thread = new Thread(() -> {
            try {
                MongoCollection<Document> collection =
                        mongoTemplate.getCollection(
                                mongoTemplate.getCollectionName(ServiceItem.class));

                var cursor = collection.watch()
                        .fullDocument(FullDocument.UPDATE_LOOKUP)
                        .cursor();

                currentCursor = cursor;

                while (changeStreamRunning) {
                    try {
                        if (!cursor.hasNext()) continue;
                        onChange.accept(cursor.next());
                    } catch (Exception e) {
                        break;
                    }
                }
            } catch (Exception e) {
                if (changeStreamRunning) e.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.setName("change-stream-listener");
        thread.start();
    }

    public void stopChangeStream() {
        changeStreamRunning = false;
        if (currentCursor != null) {
            try {
                currentCursor.close();
            } catch (Exception ignored) {
            }
        }
    }

    public void restartChangeStream(Consumer<ChangeStreamDocument<Document>> onChange) {
        stopChangeStream();
        startChangeStream(onChange);
    }
}