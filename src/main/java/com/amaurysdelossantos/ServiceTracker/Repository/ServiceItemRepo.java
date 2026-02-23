package com.amaurysdelossantos.ServiceTracker.Repository;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServiceItemRepo extends MongoRepository<ServiceItem, String>, ServiceItemRepoCustom {
    List<ServiceItem> findByDeletedAtIsNullAndCompletedAtIsNull();
    List<ServiceItem> findByDeletedAtIsNullAndCompletedAtIsNotNull();
    List<ServiceItem> findByDeletedAtIsNull();
}