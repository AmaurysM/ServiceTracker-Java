package com.amaurysdelossantos.ServiceTracker.controllers.auth;

import com.amaurysdelossantos.ServiceTracker.Services.AuthService;
import com.amaurysdelossantos.ServiceTracker.Services.MainService;
import com.amaurysdelossantos.ServiceTracker.controllers.auth.component.RoadLinesCanvas;
import com.amaurysdelossantos.ServiceTracker.models.enums.views.MainView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Scope("prototype")
public class AuthController {

    // ─── Root ──────────────────────────────────────────────────
    @FXML private StackPane rootStack;
    @FXML private Pane canvasHolder;

    // ─── Login panel controls ──────────────────────────────────
    @FXML private VBox loginPane;
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button loginButton;
    @FXML private Label loginErrorLabel;
    @FXML private Label loginLoadingLabel;

    // ─── Register panel controls ───────────────────────────────
    @FXML private VBox registerPane;
    @FXML private TextField regFirstnameField;
    @FXML private TextField regLastnameField;
    @FXML private TextField regEmailField;
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label registerErrorLabel;
    @FXML private Label registerSuccessLabel;
    @FXML private Label registerLoadingLabel;

    @Autowired private AuthService authService;
    @Autowired private MainService mainService;

    private RoadLinesCanvas roadCanvas;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "auth-thread");
        t.setDaemon(true);
        return t;
    });

    @FXML
    public void initialize() {
        // ── Inject animated road-lines canvas ──────────────────
        roadCanvas = new RoadLinesCanvas();

        // Bind canvas size to the holder pane so it fills the whole screen
        canvasHolder.widthProperty().addListener((obs, old, w) ->
                roadCanvas.setWidth(w.doubleValue()));
        canvasHolder.heightProperty().addListener((obs, old, h) ->
                roadCanvas.setHeight(h.doubleValue()));

        // Also bind canvasHolder itself to root size
        rootStack.widthProperty().addListener((obs, old, w) ->
                canvasHolder.setPrefWidth(w.doubleValue()));
        rootStack.heightProperty().addListener((obs, old, h) ->
                canvasHolder.setPrefHeight(h.doubleValue()));

        canvasHolder.getChildren().add(roadCanvas);

        // ── Initial form state ──────────────────────────────────
        showLogin();
        hideAll();

        // Enter key support
        loginPasswordField.setOnAction(e -> handleLogin());
        loginEmailField.setOnAction(e -> loginPasswordField.requestFocus());
        regConfirmPasswordField.setOnAction(e -> handleRegister());
    }

    private void hideAll() {
        loginErrorLabel.setVisible(false);
        loginLoadingLabel.setVisible(false);
        registerErrorLabel.setVisible(false);
        registerSuccessLabel.setVisible(false);
        registerLoadingLabel.setVisible(false);
    }

    // ─── Navigation ────────────────────────────────────────────

    @FXML
    public void showLogin() {
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        registerPane.setVisible(false);
        registerPane.setManaged(false);
        clearLoginErrors();
    }

    @FXML
    public void showRegister() {
        registerPane.setVisible(true);
        registerPane.setManaged(true);
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        clearRegisterErrors();
    }

    // ─── Login Logic ───────────────────────────────────────────

    @FXML
    public void handleLogin() {
        String email = loginEmailField.getText().trim();
        String password = loginPasswordField.getText();

        clearLoginErrors();

        if (email.isEmpty() || password.isEmpty()) {
            showLoginError("Please fill in all fields.");
            return;
        }
        if (!isValidEmail(email)) {
            showLoginError("Please enter a valid email address.");
            return;
        }

        setLoginLoading(true);

        executor.submit(() -> {
            AuthService.LoginResult result = authService.login(email, password);
            Platform.runLater(() -> {
                setLoginLoading(false);
                if (result.success()) {
                    roadCanvas.stopAnimation();
                    mainService.activeViewProperty().setValue(MainView.SERVICES);
                } else {
                    showLoginError(result.message());
                }
            });
        });
    }

    // ─── Register Logic ────────────────────────────────────────

    @FXML
    public void handleRegister() {
        String firstname = regFirstnameField.getText().trim();
        String lastname  = regLastnameField.getText().trim();
        String email     = regEmailField.getText().trim();
        String username  = regUsernameField.getText().trim();
        String password  = regPasswordField.getText();
        String confirm   = regConfirmPasswordField.getText();

        clearRegisterErrors();

        if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty()
                || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showRegisterError("Please fill in all fields.");
            return;
        }
        if (!isValidEmail(email)) {
            showRegisterError("Please enter a valid email address.");
            return;
        }
        if (password.length() < 8) {
            showRegisterError("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showRegisterError("Passwords do not match.");
            return;
        }

        setRegisterLoading(true);

        executor.submit(() -> {
            AuthService.RegisterResult result =
                    authService.register(firstname, lastname, email, username, password);
            Platform.runLater(() -> {
                setRegisterLoading(false);
                if (result.success()) {
                    showRegisterSuccess(result.message() + " Please log in.");
                    clearRegisterForm();
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(this::showLogin);
                    }).start();
                } else {
                    showRegisterError(result.message());
                }
            });
        });
    }

    // ─── Helpers ───────────────────────────────────────────────

    private void showLoginError(String msg) {
        loginErrorLabel.setText(msg);
        loginErrorLabel.setVisible(true);
    }

    private void clearLoginErrors() {
        loginErrorLabel.setVisible(false);
    }

    private void setLoginLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginLoadingLabel.setVisible(loading);
    }

    private void showRegisterError(String msg) {
        registerErrorLabel.setText(msg);
        registerErrorLabel.setVisible(true);
        registerSuccessLabel.setVisible(false);
    }

    private void showRegisterSuccess(String msg) {
        registerSuccessLabel.setText(msg);
        registerSuccessLabel.setVisible(true);
        registerErrorLabel.setVisible(false);
    }

    private void clearRegisterErrors() {
        registerErrorLabel.setVisible(false);
        registerSuccessLabel.setVisible(false);
    }

    private void setRegisterLoading(boolean loading) {
        registerButton.setDisable(loading);
        registerLoadingLabel.setVisible(loading);
    }

    private void clearRegisterForm() {
        regFirstnameField.clear();
        regLastnameField.clear();
        regEmailField.clear();
        regUsernameField.clear();
        regPasswordField.clear();
        regConfirmPasswordField.clear();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }
}