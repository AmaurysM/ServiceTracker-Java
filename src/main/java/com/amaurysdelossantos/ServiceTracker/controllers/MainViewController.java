package com.amaurysdelossantos.ServiceTracker.controllers;

import com.amaurysdelossantos.ServiceTracker.Services.AuthService;
import com.amaurysdelossantos.ServiceTracker.Services.MainService;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceTrackerService;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.MainView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MainViewController {

    @FXML public AnchorPane containerAnchorPane;

    @Autowired private ApplicationContext    applicationContext;
    @Autowired private MainService           mainService;
    @Autowired private ServiceTrackerService serviceTrackerService;
    @Autowired private AuthService           authService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "session-restore-thread");
        t.setDaemon(true);
        return t;
    });

    @FXML
    public void initialize() {
        mainService.activeViewProperty().addListener((e, oldItem, newItem) ->
                onMainViewChange(newItem));

        // ── Attempt silent session restore on a background thread ──
        // We show nothing (blank) while checking, then navigate to either
        // SERVICES (restored) or AUTH (no valid session) on the FX thread.
        executor.submit(() -> {
            boolean restored = authService.restoreSession();
            Platform.runLater(() ->
                    mainService.activeViewProperty().setValue(
                            restored ? MainView.SERVICES : MainView.AUTH
                    )
            );
        });
    }

    private void onMainViewChange(MainView newView) {
        try {
            switch (newView) {
                case AUTH    -> loadView("/components/auth/auth-view.fxml");
                case OPTIONS -> loadView("/components/settings/settings-view.fxml");
                case SERVICES -> {
                    serviceTrackerService.activeViewProperty().setValue(null);
                    loadView("/service-tracker.fxml");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + newView, e);
        }
    }

    private void loadView(String fxmlPath) throws IOException {
        containerAnchorPane.getChildren().clear();
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(applicationContext::getBean);
        Node child = loader.load();
        AnchorPane.setTopAnchor(child, 0.0);
        AnchorPane.setBottomAnchor(child, 0.0);
        AnchorPane.setLeftAnchor(child, 0.0);
        AnchorPane.setRightAnchor(child, 0.0);
        containerAnchorPane.getChildren().add(child);
    }
}