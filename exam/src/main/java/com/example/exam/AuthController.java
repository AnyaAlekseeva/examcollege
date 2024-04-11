package com.example.exam;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AuthController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    void login(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (DatabaseUtil.checkCredentials(username, password)) {
            // Успешная авторизация
            showAlert(AlertType.INFORMATION, "Login Successful", "Welcome, " + username + "!");
            loadMainView();
        } else {
            // Неверные учетные данные
            showAlert(AlertType.ERROR, "Login Failed", "Invalid credentials!");
        }

        // После проверки очистите поля или выполните другие действия
        usernameField.clear();
        passwordField.clear();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadMainView() {
        try {
            // Загружаем главный экран
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Parent root = loader.load();

            // Показываем главный экран на сцене
            Scene scene = new Scene(root);
            Stage stage = (Stage) usernameField.getScene().getWindow(); // Получаем текущее окно
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Обработка ошибки загрузки главного экрана
        }
    }
}
