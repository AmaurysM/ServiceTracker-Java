package com.amaurysdelossantos.ServiceTracker;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServiceTrackerApplication {

    public static void main(String[] args) {
        System.setProperty("https.agent", "ServiceTracker/1.0 amaurydlsm@gmail.com");
        Application.launch(ServiceApplication.class, args);
    }

}
