package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Repository.ServiceItemRepo;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import javafx.application.Platform;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
public class DataService {

    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    @Autowired private ServiceItemRepo serviceItemRepo;
    @Autowired private MongoTemplate mongoTemplate;

    private volatile boolean changeStreamRunning = false;
    private volatile MongoChangeStreamCursor<ChangeStreamDocument<Document>> currentCursor;

    /**
     * Fetches items matching the given filters, scoped to the company.
     * companyId is mandatory — if null or blank, the callback receives an empty list.
     */
    public void reload(ActivityFilter activity, TimeFilter time,
                       ServiceFilter service, String search,
                       String companyId,
                       Consumer<List<ServiceItem>> onResult) {
        executor.submit(() -> {
            try {
                List<ServiceItem> results = serviceItemRepo.findWithFilters(
                        activity, time, service, search, companyId
                );
                Platform.runLater(() -> onResult.accept(results));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Starts a MongoDB change stream scoped to the given companyId.
     * Only change events whose fullDocument.companyId matches are delivered.
     */
    public void startChangeStream(String companyId,
                                  Consumer<ChangeStreamDocument<Document>> onChange) {
        changeStreamRunning = true;

        Thread thread = new Thread(() -> {
            try {
                MongoCollection<Document> collection =
                        mongoTemplate.getCollection(
                                mongoTemplate.getCollectionName(ServiceItem.class));

                // Filter the stream to this company only
                Bson companyFilter = Aggregates.match(
                        Filters.eq("fullDocument.companyId", companyId)
                );

                var cursor = collection.watch(List.of(companyFilter))
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
            try { currentCursor.close(); } catch (Exception ignored) {}
        }
    }

    public void restartChangeStream(String companyId,
                                    Consumer<ChangeStreamDocument<Document>> onChange) {
        stopChangeStream();
        startChangeStream(companyId, onChange);
    }
}