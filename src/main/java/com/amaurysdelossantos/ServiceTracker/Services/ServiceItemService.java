package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Repository.ServiceItemRepo;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceItemService {

    @Autowired
    private ServiceItemRepo serviceItemRepo;

//    public StringProperty query = new SimpleStringProperty("");
//
//    public StringProperty searchTextProperty() {
//        return query;
//    }
//
//    @Getter
//    private final ObservableList<ServiceItem> items =
//            FXCollections.observableArrayList();
//
//
//    public void loadInitialData() {
//        items.setAll(serviceItemRepo.findByDeletedAtIsNull());
//    }
//
//    public List<ServiceItem> getActiveItems() {
//        return serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNull();
//    }
//
//    public List<ServiceItem> getCompletedItems() {
//        return serviceItemRepo.findByDeletedAtIsNullAndCompletedAtIsNotNull();
//    }

    public ServiceItem saveService(ServiceItem item) {
        return serviceItemRepo.save(item);
    }

    public ServiceItem getById(String id) {
        return serviceItemRepo.findById(id).orElse(null);
    }
}
