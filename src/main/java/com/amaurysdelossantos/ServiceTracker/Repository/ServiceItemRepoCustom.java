package com.amaurysdelossantos.ServiceTracker.Repository;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;

import java.util.List;

public interface ServiceItemRepoCustom {
    List<ServiceItem> findWithFilters(ActivityFilter activity, TimeFilter time, ServiceFilter service, String search);

}
