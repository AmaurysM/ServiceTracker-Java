package com.amaurysdelossantos.ServiceTracker.Repository;

import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ActivityFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceFilter;
import com.amaurysdelossantos.ServiceTracker.models.enums.TimeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class ServiceItemRepoCustomImpl implements ServiceItemRepoCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ServiceItem> findWithFilters(ActivityFilter activity, TimeFilter time, ServiceFilter service, String search) {
        List<Criteria> all = new ArrayList<>();

        all.add(Criteria.where("deletedAt").isNull());

        if (activity != null) {
            switch (activity) {
                case ACTIVE -> all.add(Criteria.where("completedAt").isNull());
                case DONE   -> all.add(Criteria.where("completedAt").ne(null));
            }
        }

        if (time != null) {
            ZonedDateTime now = ZonedDateTime.now();
            Instant start = switch (time) {
                case TODAY -> now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
                case WEEK  -> now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay(now.getZone()).toInstant();
                case MONTH -> now.toLocalDate().withDayOfMonth(1).atStartOfDay(now.getZone()).toInstant();
            };
            Criteria timeCriteria = Criteria.where("createdAt").gte(start);
            if (time == TimeFilter.TODAY) {
                Instant end = now.toLocalDate().atStartOfDay(now.getZone()).plusDays(1).toInstant();
                timeCriteria = timeCriteria.lt(end);
            }
            all.add(timeCriteria);
        }

        if (service != null && service != ServiceFilter.ALL) {
            String fieldName = switch (service) {
                case FUEL               -> "fuel";
                case GPU                -> "gpu";
                case LAVATORY           -> "lavatory";
                case POTABLE_WATER      -> "potableWater";
                case CATERING           -> "catering";
                case WINDSHIELD_CLEANING -> "windshieldCleaning";
                case OIL_SERVICE        -> "oilService";
                default -> null;
            };
            if (fieldName != null) {
                all.add(Criteria.where(fieldName).ne(null));
            }
        }

        if (search != null && !search.isBlank()) {
            all.add(new Criteria().orOperator(
                    Criteria.where("tail").regex(search, "i"),
                    Criteria.where("description").regex(search, "i")
            ));
        }

        Criteria criteria = new Criteria().andOperator(all.toArray(new Criteria[0]));
        return mongoTemplate.find(new Query(criteria), ServiceItem.class);
    }
}