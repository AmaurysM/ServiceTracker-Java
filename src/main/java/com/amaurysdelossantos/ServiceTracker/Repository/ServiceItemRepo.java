package com.amaurysdelossantos.ServiceTracker.Repository;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServiceItemRepo extends MongoRepository<ServiceItem, String> {

    List<ServiceItem> findByDeletedAtIsNullAndCompletedAtIsNull();

    List<ServiceItem> findByDeletedAtIsNullAndCompletedAtIsNotNull();

    List<ServiceItem> findByDeletedAtIsNull();
//
//    List<ServiceItem> findWithFilters(ActivityFilter activity, TimeFilter time, ServiceFilter service, String search);

}
