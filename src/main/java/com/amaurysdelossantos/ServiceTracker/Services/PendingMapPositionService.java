package com.amaurysdelossantos.ServiceTracker.Services;

import org.springframework.stereotype.Service;

@Service
public class PendingMapPositionService {

    private Double lat = null;
    private Double lon = null;

    public void set(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public boolean isPresent() {
        return lat != null && lon != null;
    }


    public double[] consumeIfPresent() {
        if (!isPresent()) return null;
        double[] result = { lat, lon };
        clear();
        return result;
    }

    public void clear() {
        lat = null;
        lon = null;
    }
}