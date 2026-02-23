package com.amaurysdelossantos.ServiceTracker.Services;

import com.amaurysdelossantos.ServiceTracker.Repository.ServiceItemRepo;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceItemService {

    @Autowired
    private ServiceItemRepo serviceItemRepo;

    public void saveService(ServiceItem item) {
        serviceItemRepo.save(item);
    }

    public ServiceItem getById(String id) {
        return serviceItemRepo.findById(id).orElse(null);
    }
}
