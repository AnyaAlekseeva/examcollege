package com.example.exam;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class EditRequestController {

    @FXML
    private Label lblNumber;

    @FXML
    private Label lblDate;

    @FXML
    private Label lblEquipmentType;

    @FXML
    private Label lblFaultType;

    @FXML
    private Label lblClientFullName;

    @FXML
    private TextArea txtDescription;

    @FXML
    private ChoiceBox<String> statusChoiceBox;

    @FXML
    private TextArea txtAssignee;

    @FXML
    private Label lblComments;

    @FXML
    private Label lblPriority;

    @FXML
    private Label lblCompletionDate;

    private Request currentRequest;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void initialize(Request request) {
        if (request != null) {
            currentRequest = request;

            // Загрузить данные заявки и установить их в соответствующие Label
            lblNumber.setText(request.getNumber() != null ? request.getNumber() : "");
            lblDate.setText(request.getDate() != null ? request.getDate().toString() : "");
            lblEquipmentType.setText(request.getEquipmentType() != null ? request.getEquipmentType() : "");
            lblFaultType.setText(request.getFaultType() != null ? request.getFaultType() : "");
            lblClientFullName.setText(request.getClientFullName() != null ? request.getClientFullName() : "");
            lblComments.setText(request.getComments() != null ? request.getComments() : "");
            lblPriority.setText(request.getPriority() != null ? String.valueOf(request.getPriority()) : "");
            lblCompletionDate.setText(request.getCompletionDate() != null ? String.valueOf(request.getCompletionDate()) : "");

            // Установить значения в соответствующие TextArea
            txtAssignee.setText(request.getAssignee() != null ? request.getAssignee() : "");
            txtDescription.setText(request.getDescription() != null ? request.getDescription() : "");
            statusChoiceBox.setValue(request.getStatus() != null ? request.getStatus() : ""); // Установить выбранное значение в ChoiceBox
        } else {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Заявка не выбрана");
            cancel();
        }
    }


    @FXML
    private void saveChanges() {
        // Обновляем информацию о заявке с помощью DatabaseUtil
        currentRequest.setDescription(txtDescription.getText()); // Обновляем описание проблемы
        currentRequest.setStatus(statusChoiceBox.getSelectionModel().getSelectedItem()); // Обновляем статус исполнителя
        currentRequest.setAssignee(txtAssignee.getText());

        boolean success = DatabaseUtil.updateRequest(currentRequest);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Успех", "Информация о заявке успешно обновлена.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось обновить информацию о заявке.");
        }

        stage.close();
    }

    @FXML
    private void cancel() {
        // Обработчик отмены редактирования
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
