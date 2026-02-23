package com.amaurysdelossantos.ServiceTracker.Helper;

import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemDeleteController;
import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemEditController;
import com.amaurysdelossantos.ServiceTracker.Controllers.ItemInteractions.ItemInfoController;
import com.amaurysdelossantos.ServiceTracker.models.ServiceItem;
import com.amaurysdelossantos.ServiceTracker.models.enums.ServiceType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class WindowHandler {
    static public void handleEdit(ServiceType serviceType, ServiceItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    WindowHandler.class.getResource("/components/ItemInteraction/item-edit-modal.fxml")
            );
            loader.setClassLoader(WindowHandler.class.getClassLoader());
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
            loader.setClassLoader(WindowHandler.class.getClassLoader());
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

    static public void handleToggleComplete() {
        //TODO: Complete Item
    }

    static public void handleDelete(ServiceItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    WindowHandler.class.getResource("/components/ItemInteraction/item-delete-modal.fxml")
            );
            loader.setClassLoader(WindowHandler.class.getClassLoader());
            Parent root = loader.load();
            ItemDeleteController controller = loader.getController();

            controller.setItem(item);
            controller.populate();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));

            stage.setTitle("Confirm Deletion");
            stage.initModality(Modality.APPLICATION_MODAL);
            // stage.initOwner(deleteButton.getScene().getWindow()); // Lock parent window
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
            System.out.println("loader: " + loader + WindowHandler.class.getResource("/components/ItemInteraction/item-info-modal.fxml"));
            loader.setClassLoader(WindowHandler.class.getClassLoader());
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
}