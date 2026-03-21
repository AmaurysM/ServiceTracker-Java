package com.amaurysdelossantos.ServiceTracker.controllers.Standard.Options;

import com.amaurysdelossantos.ServiceTracker.Services.AuthService;
import com.amaurysdelossantos.ServiceTracker.Services.MainService;
import com.amaurysdelossantos.ServiceTracker.models.User;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.MainView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserOptionsController {

    @FXML private Label  userNameLabel;
    @FXML private Label  userEmailLabel;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    @Autowired private AuthService authService;
    @Autowired private MainService mainService;

    @FXML
    public void initialize() {

        User user = authService.getCurrentUser();
        if (user != null) {
            String first    = user.getFirstname() != null ? user.getFirstname().trim() : "";
            String last     = user.getLastname()  != null ? user.getLastname().trim()  : "";
            String fullName = (first + " " + last).trim();
            if (fullName.isEmpty())
                fullName = user.getUsername() != null ? user.getUsername() : "—";

            userNameLabel.setText(fullName);
            userEmailLabel.setText(user.getEmail() != null ? user.getEmail() : "—");
        } else {
            userNameLabel.setText("—");
            userEmailLabel.setText("—");
        }

        configureSettingsButton(mainService.activeViewProperty().get());

        mainService.activeViewProperty().addListener((obs, oldView, newView) ->
                configureSettingsButton(newView));

        logoutBtn.setOnAction(e -> {
            authService.logout();
            mainService.activeViewProperty().setValue(MainView.AUTH);
        });
    }

    private void configureSettingsButton(MainView currentView) {
        if (currentView == null) {
            settingsBtn.setVisible(false);
            settingsBtn.setManaged(false);
            return;
        }

        switch (currentView) {
            case AUTH -> {
                settingsBtn.setVisible(false);
                settingsBtn.setManaged(false);
            }
            case SERVICES -> {
                settingsBtn.setVisible(true);
                settingsBtn.setManaged(true);
                settingsBtn.setText("Settings");
                settingsBtn.setOnAction(e ->
                        mainService.activeViewProperty().setValue(MainView.OPTIONS));
            }
            case OPTIONS -> {
                settingsBtn.setVisible(true);
                settingsBtn.setManaged(true);
                settingsBtn.setText("Back to App");
                settingsBtn.setOnAction(e ->
                        mainService.activeViewProperty().setValue(MainView.SERVICES));
            }
        }
    }
}