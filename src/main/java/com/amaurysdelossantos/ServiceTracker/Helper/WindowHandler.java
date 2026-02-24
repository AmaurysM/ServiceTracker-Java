package com.amaurysdelossantos.ServiceTracker.Helper;

import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemDeleteController;
import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemEditController;
import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemInfoController;
import com.amaurysdelossantos.ServiceTracker.Services.ServiceItemService;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class WindowHandler {

    private static ApplicationContext applicationContext;

    private static ServiceItemService serviceItemService;

    @Autowired
    public WindowHandler(ApplicationContext applicationContext, ServiceItemService serviceItemService) {
        WindowHandler.applicationContext = applicationContext;
        WindowHandler.serviceItemService = serviceItemService;
    }

    static public void handleEdit(ServiceType serviceType, ServiceItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    WindowHandler.class.getResource("/components/ItemInteraction/item-edit-modal.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            ItemEditController controller = loader.getController();

            controller.setItem(item);
            controller.setInitialService(serviceType);
            controller.populate();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("Edit: " + item.getTail());
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.show();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static public void handleEdit(ServiceItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    WindowHandler.class.getResource("/components/ItemInteraction/item-edit-modal.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            ItemEditController controller = loader.getController();

            controller.setItem(item);
            controller.populate();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("Edit: " + item.getTail());
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.show();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static public void handleToggleComplete(ServiceItem item) {

        if (item.getCompletedAt() == null) {
            item.setCompletedAt(Instant.now());
        } else {
            item.setCompletedAt(null);
        }

        Thread t = new Thread(() -> {
            try {
                serviceItemService.saveService(item);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    static public void handleToggleComplete(ServiceItem item, Runnable onComplete) {

        if (item.getCompletedAt() == null) {
            item.setCompletedAt(Instant.now());
        } else {
            item.setCompletedAt(null);
        }

        Thread t = new Thread(() -> {
            try {
                serviceItemService.saveService(item);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (onComplete != null) {
                    Platform.runLater(onComplete);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    static public void handleDelete(ServiceItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    WindowHandler.class.getResource("/components/ItemInteraction/item-delete-modal.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            ItemDeleteController controller = loader.getController();

            controller.setItem(item);
            controller.populate();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("Confirm Deletion");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.show();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static public void handleInfo(ServiceItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    WindowHandler.class.getResource("/components/ItemInteraction/item-info-modal.fxml")
            );

            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            ItemInfoController controller = loader.getController();

            controller.setItem(item);
            controller.populate();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("Info: " + item.getTail());
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.show();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static public void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    WindowHandler.class.getResource("/components/ItemInteraction/add-service-item.fxml")
            );

            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("ADD SERVICE ITEM");
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.show();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}