package com.example.exam;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainController {

    @FXML
    private TextField txtNumber;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField txtEquipment;

    @FXML
    private TextField txtFaultType;

    @FXML
    private TextField txtDescription;

    @FXML
    private TextField txtClient;

    @FXML
    private ChoiceBox<String> statusChoiceBox;

    @FXML
    private TableView<Request> tableView;

    @FXML
    private TableColumn<Request, String> colNumber;

    @FXML
    private TableColumn<Request, LocalDate> colDate;

    @FXML
    private TableColumn<Request, String> colEquipmentType;

    @FXML
    private TableColumn<Request, String> colFaultType;

    @FXML
    private TableColumn<Request, String> colDescription;

    @FXML
    private TableColumn<Request, String> colClientFullName;

    @FXML
    private TableColumn<Request, Integer> colStatus;

    @FXML
    private TableColumn<Request, String> colAssignee;

    @FXML
    private TableColumn<Request, String> colComments;

    @FXML
    private TableColumn<Request, Integer> colPriority;

    @FXML
    private TableColumn<Request, LocalDate> colCompletionDate;

    @FXML
    private TextField searchField;

    @FXML
    public void initialize() {
        // Initialize choice box
        statusChoiceBox.setItems(FXCollections.observableArrayList("В ожидании", "В работе", "Выполнено"));

        // Bind columns with request properties
        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colEquipmentType.setCellValueFactory(new PropertyValueFactory<>("equipmentType"));
        colFaultType.setCellValueFactory(new PropertyValueFactory<>("faultType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colClientFullName.setCellValueFactory(new PropertyValueFactory<>("clientFullName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAssignee.setCellValueFactory(new PropertyValueFactory<>("assignee"));
        colComments.setCellValueFactory(new PropertyValueFactory<>("comments"));
        colPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        colCompletionDate.setCellValueFactory(new PropertyValueFactory<>("completionDate"));

        // Set items to table view
        refreshTable();
    }

    @FXML
    private void addRequest() {
        String number = txtNumber.getText();
        LocalDate date = datePicker.getValue();
        String equipment = txtEquipment.getText();
        String faultType = txtFaultType.getText();
        String description = txtDescription.getText();
        String client = txtClient.getText();
        String status = statusChoiceBox.getSelectionModel().getSelectedItem(); // С учетом того, что статусы начинаются с 1

        Request newRequest = new Request();
        newRequest.setNumber(number);
        newRequest.setDate(date);
        newRequest.setEquipmentType(equipment);
        newRequest.setFaultType(faultType);
        newRequest.setDescription(description);
        newRequest.setClientFullName(client);
        newRequest.setStatus(status);

        boolean success = DatabaseUtil.addRequest(newRequest);

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Успех", "Заявка успешно добавлена в базу данных.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось добавить заявку в базу данных.");
        }

        // Очищаем поля ввода
        clearInputFields();
    }

    @FXML
    private void editRequestHandler() {
        // Получаем выбранную заявку из таблицы
        Request selectedRequest = tableView.getSelectionModel().getSelectedItem();
        if (selectedRequest != null) {
            try {
                // Загружаем FXML файл окна редактирования
                FXMLLoader loader = new FXMLLoader(getClass().getResource("edit-view.fxml"));
                Parent root = loader.load();

                // Получаем контроллер окна редактирования и устанавливаем в него выбранную заявку
                EditRequestController controller = loader.getController();
                controller.initialize(selectedRequest);

                // Создаем новое окно и устанавливаем в него сцену с загруженным FXML
                Stage stage = new Stage();
                controller.setStage(stage);

                stage.setTitle("Редактирование заявки");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите заявку для редактирования.");
        }
    }

    @FXML
    private void deleteRequestHandler() {
        // Получаем выбранную заявку из таблицы
        Request selectedRequest = tableView.getSelectionModel().getSelectedItem();
        if (selectedRequest != null) {
            String requestNumber = selectedRequest.getNumber();

            // Удаляем заявку из базы данных
            boolean success = DatabaseUtil.deleteRequestByNumber(requestNumber);

            // Отображаем всплывающее окно с сообщением
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Успех", "Заявка №" + requestNumber + " успешно удалена.");
                refreshTable(); // Обновляем таблицу
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось удалить заявку №" + requestNumber + ".");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Выберите заявку для удаления.");
        }
    }


    @FXML
    private void searchRequestHandler() {
        // Получаем номер заявки из поля поиска
        String requestNumber = searchField.getText();

        // Поиск заявки в базе данных
        ObservableList<Request> requests = DatabaseUtil.findRequestByNumber(requestNumber);

        // Отображаем найденную заявку
        if (!requests.isEmpty()) {
            // Очищаем таблицу
            tableView.getItems().clear();

            // Добавляем найденные заявки в таблицу
            tableView.setItems(requests);
        } else {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Заявка с номером " + requestNumber + " не найдена.");
        }
    }


    @FXML
    private void calculateStatisticsHandler() {
        // Получаем данные из базы данных
        int completedRequestsCount = DatabaseUtil.getCompletedRequestsCount();
        long averageCompletionTime = DatabaseUtil.getAverageCompletionTime();
        Map<String, Integer> faultTypeStats = DatabaseUtil.getFaultTypeStats();

        // Отображаем статистику
        showStatisticsDialog(completedRequestsCount, averageCompletionTime, faultTypeStats);
    }

    private void showStatisticsDialog(int completedRequestsCount, long averageCompletionTime, Map<String, Integer> faultTypeStats) {
        // Создаем диалоговое окно
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Статистика");

        // Содержимое диалогового окна
        VBox dialogContent = new VBox();
        dialogContent.getChildren().addAll(
                new Label("Количество выполненных заявок: " + completedRequestsCount),
                new Label("Среднее время выполнения: " + averageCompletionTime + " дн."),
                createFaultTypeStatsTable(faultTypeStats)
        );

        dialog.getDialogPane().setContent(dialogContent);

        // Добавляем кнопку "Закрыть"
        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(event -> dialog.close());

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().lookupButton(ButtonType.OK);
        dialog.getDialogPane().getChildren().add(closeButton);

        // Обработка события закрытия
        dialog.setOnCloseRequest(event -> {
            System.out.println("Диалоговое окно закрыто");
        });

        // Отображение диалогового окна
        dialog.showAndWait();
    }

    private TableView<FaultTypeStat> createFaultTypeStatsTable(Map<String, Integer> faultTypeStats) {
        TableView<FaultTypeStat> tableView = new TableView<>();

        // Определяем столбцы
        TableColumn<FaultTypeStat, String> faultTypeColumn = new TableColumn<>("Тип неисправности");
        faultTypeColumn.setCellValueFactory(new PropertyValueFactory<>("faultType"));

        TableColumn<FaultTypeStat, Integer> countColumn = new TableColumn<>("Количество");
        countColumn.setCellValueFactory(new PropertyValueFactory<>("count"));

        // Добавляем столбцы в таблицу
        tableView.getColumns().addAll(faultTypeColumn, countColumn);

        // Заполняем таблицу данными
        ObservableList<FaultTypeStat> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : faultTypeStats.entrySet()) {
            data.add(new FaultTypeStat(entry.getKey(), entry.getValue()));
        }
        tableView.setItems(data);

        return tableView;
    }


    @FXML
    private void addCommentHandler() {
        Request selectedRequest = tableView.getSelectionModel().getSelectedItem();
        if (selectedRequest != null) {
        // Создание диалогового окна
            // Создание диалогового окна
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Добавить комментарий");

            // Создание DialogPane
            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Создание GridPane
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            // Добавление элементов
            Label nameLabel = new Label("Имя:");
            TextField nameField = new TextField();
            Label commentLabel = new Label("Комментарий:");
            TextArea commentField = new TextArea();

            grid.add(nameLabel, 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(commentLabel, 0, 1);
            grid.add(commentField, 1, 1);

            dialogPane.setContent(grid);

            // Отображение диалога
            dialog.showAndWait();

        if (dialog.getResult() == ButtonType.OK) {
            String name = nameField.getText();
            String comment = commentField.getText();

            // Добавление комментария в базу данных
            DatabaseUtil.addComment(name, comment, selectedRequest.getNumber());
        }
        } else {
            showAlert(Alert.AlertType.WARNING, "Предупреждение", "Выберите заявку для редактирования.");
        }
    }

    @FXML
    private void refreshTableHandler() {
        // Очищаем поля ввода
        clearInputFields();
        refreshTable();
    }

    private void refreshTable() {
        ObservableList<Request> requests = DatabaseUtil.fetchRequestsFromDatabase();
        tableView.setItems(requests);
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearInputFields() {
        txtNumber.clear();
        datePicker.setValue(null);
        txtEquipment.clear();
        txtFaultType.clear();
        txtDescription.clear();
        txtClient.clear();
        statusChoiceBox.setValue(null);
    }
}
