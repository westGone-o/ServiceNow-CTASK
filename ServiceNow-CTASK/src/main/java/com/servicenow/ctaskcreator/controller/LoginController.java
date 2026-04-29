package com.servicenow.ctaskcreator.controller;

import com.servicenow.ctaskcreator.model.AppSettings;
import com.servicenow.ctaskcreator.service.SettingsManager;
import com.servicenow.ctaskcreator.service.ServiceNowClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private CheckBox rememberMeCheckBox;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private ProgressIndicator progressIndicator;
    
    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
        statusLabel.setText("");
        
        // Load saved settings
        AppSettings settings = SettingsManager.loadSettings();
        if (settings.getSnowId() != null && !settings.getSnowId().isEmpty()) {
            usernameField.setText(settings.getSnowId());
            rememberMeCheckBox.setSelected(true);
        }
        
        if (settings.getSnowPassword() != null && !settings.getSnowPassword().isEmpty()) {
            passwordField.setText(settings.getSnowPassword());
        }
        
        // Login with Enter key
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both ID and Password.");
            return;
        }

        setLoading(true);
        statusLabel.setText("Signing in...");

        new Thread(() -> {
            try {
                // ServiceNow connection and authentication test
                ServiceNowClient client = new ServiceNowClient(username, password);
                client.validateConnection();

                // Save settings if Remember Me is checked
                if (rememberMeCheckBox.isSelected()) {
                    AppSettings settings = SettingsManager.loadSettings();
                    settings.setSnowId(username);
                    settings.setSnowPassword(password);
                    SettingsManager.saveSettings(settings);
                }

                Platform.runLater(() -> {
                    try {
                        // Switch to Main screen
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
                        Parent root = loader.load();

                        // Pass ServiceNowClient to MainController
                        MainController mainController = loader.getController();
                        mainController.setServiceNowClient(client);
                        
                        Scene scene = new Scene(root, 850, 820);
                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        stage.setTitle("ServiceNow CTASK Creator");
                        stage.setScene(scene);
                        stage.setResizable(true);
                        
                        System.out.println("Login successful for user: " + username);
                    } catch (Exception e) {
                        System.err.println("Failed to load main screen: " + e.getMessage());
                        showError("An error occurred while loading the main screen.");
                        setLoading(false);
                    }
                });

            } catch (Exception e) {
                System.err.println("Login failed: " + e.getMessage());
                Platform.runLater(() -> {
                    showError("Login failed: " + e.getMessage());
                    setLoading(false);
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        rememberMeCheckBox.setDisable(loading);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
    }
}
