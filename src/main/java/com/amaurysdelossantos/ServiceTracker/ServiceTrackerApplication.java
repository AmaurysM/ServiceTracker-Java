package com.amaurysdelossantos.ServiceTracker;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServiceTrackerApplication {

	public static void main(String[] args) {

        Application.launch(ServiceApplication.class, args);
	}

}
