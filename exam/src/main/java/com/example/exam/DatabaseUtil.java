package com.example.exam;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseUtil {
    private static final String URL = "jdbc:postgresql://localhost:5432/db1";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "26071938";

    public static boolean checkCredentials(String username, String password) {
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String query = "SELECT COUNT(*) FROM Users WHERE username = ? AND password = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        int count = resultSet.getInt(1);
                        return count == 1; // Возвращаем true, если найден один пользователь с указанным именем пользователя и паролем
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean addRequest(Request request) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            connection.setAutoCommit(false); // Отключить автоподтверждение

            // Получить ID типа оборудования и клиента
            int equipmentTypeId = getEquipmentTypeId(request.getEquipmentType());
            if (equipmentTypeId == -1) {
                System.err.println("Тип оборудования не найден.");
                return false;
            }
            int clientId = getClientId(request.getClientFullName());
            if (clientId == -1) {
                System.err.println("Клиент не найден.");
                return false;
            }

            String requestQuery = "INSERT INTO Requests (requestNumber, requestDate, equipmentTypeId, faultType, problemDescription, clientId) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement requestStatement = connection.prepareStatement(requestQuery, Statement.RETURN_GENERATED_KEYS)) {
                requestStatement.setString(1, request.getNumber());
                requestStatement.setDate(2, java.sql.Date.valueOf(request.getDate()));
                requestStatement.setInt(3, equipmentTypeId);
                requestStatement.setString(4, request.getFaultType());
                requestStatement.setString(5, request.getDescription());
                requestStatement.setInt(6, clientId);
                int rowsInserted = requestStatement.executeUpdate();

                if (rowsInserted > 0) {
                    // Получить сгенерированный ID заявки
                    ResultSet generatedKeys = requestStatement.getGeneratedKeys();
                    int requestId = -1;
                    if (generatedKeys.next()) {
                        requestId = generatedKeys.getInt(1);
                    }

                    // Добавить запись о статусе заявки
                    String statusQuery = "INSERT INTO RequestStatus (requestId, statusId) VALUES (?, ?)";
                    try (PreparedStatement statusStatement = connection.prepareStatement(statusQuery)) {
                        int statusId = getStatusIdByName(request.getStatus(), connection);
                        if (statusId != -1) {
                            statusStatement.setInt(1, requestId);
                            statusStatement.setInt(2, statusId);
                            int statusRowsInserted = statusStatement.executeUpdate();
                            if (statusRowsInserted == 0) {
                                throw new SQLException("Не удалось добавить статус заявки");
                            }
                        } else {
                            System.err.println("Статус не найден: " + request.getStatus());
                        }
                    }

                    // Если исполнитель не пустой, добавить запись о нем
                    if (request.getAssignee() != null && !request.getAssignee().isEmpty()) {
                        String assigneeQuery = "INSERT INTO RequestAssignee (requestId, assigneeId) VALUES (?, ?)";
                        try (PreparedStatement assigneeStatement = connection.prepareStatement(assigneeQuery)) {
                            int assigneeId = getAssigneeId(request.getAssignee(), connection);
                            if (assigneeId != -1) {
                                assigneeStatement.setInt(1, requestId);
                                assigneeStatement.setInt(2, assigneeId);
                                int assigneeRowsInserted = assigneeStatement.executeUpdate();
                                if (assigneeRowsInserted == 0) {
                                    throw new SQLException("Не удалось добавить исполнителя");
                                }
                            } else {
                                System.err.println("Исполнитель не найден.");
                            }
                        }
                    }

                    connection.commit(); // Подтвердить транзакцию, если все успешно
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (connection != null) {
                try {
                    connection.rollback(); // Откатить транзакцию в случае ошибки
                } catch (SQLException rollbackException) {
                    rollbackException.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close(); // Закрыть соединение
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


    private static int getStatusIdByName(String statusName, Connection connection) throws SQLException {
        String query = "SELECT statusId FROM Status WHERE statusName = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, statusName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("statusId");
                }
            }
        }
        // Если статус не найден, возвращаем -1
        return -1;
    }

    private static int getEquipmentTypeId(String equipmentType) {
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String query = "SELECT typeId FROM EquipmentTypes WHERE equipmentType = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, equipmentType);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("typeId");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static int getClientId(String clientFullName) {
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String query = "SELECT clientId FROM Clients WHERE fullName = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, clientFullName);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("clientId");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static ObservableList<Request> fetchRequestsFromDatabase() {
        ObservableList<Request> requests = FXCollections.observableArrayList();

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            // Основной запрос для получения основных данных о заявках
            String mainQuery = "SELECT " +
                    "Requests.requestId, " +
                    "Requests.requestNumber, " +
                    "Requests.requestDate, " +
                    "EquipmentTypes.equipmentType, " +
                    "Requests.faultType, " +
                    "Requests.problemDescription, " +
                    "Requests.priority, " +
                    "Requests.completionDate, " +
                    "Clients.fullName " +
                    "FROM " +
                    "Requests " +
                    "INNER JOIN " +
                    "EquipmentTypes ON Requests.equipmentTypeId = EquipmentTypes.typeId " +
                    "INNER JOIN " +
                    "Clients ON Requests.clientId = Clients.clientId";

            try (PreparedStatement mainStatement = connection.prepareStatement(mainQuery)) {
                try (ResultSet mainResultSet = mainStatement.executeQuery()) {
                    while (mainResultSet.next()) {
                        Request request = new Request();
                        request.setNumber(mainResultSet.getString("requestNumber"));
                        request.setDate(mainResultSet.getDate("requestDate").toLocalDate());
                        request.setEquipmentType(mainResultSet.getString("equipmentType"));
                        request.setFaultType(mainResultSet.getString("faultType"));
                        request.setDescription(mainResultSet.getString("problemDescription"));
//                        request.setPriority(mainResultSet.getInt("priority"));
//                        request.setDate(mainResultSet.getDate("completionDate").toLocalDate());
                        request.setClientFullName(mainResultSet.getString("fullName"));

                        // Запрос для получения последнего статуса для текущей заявки
                        String statusQuery = "SELECT * FROM RequestStatus " +
                                "INNER JOIN Status ON RequestStatus.statusId = Status.statusId " +
                                "WHERE requestId = ? ORDER BY RequestStatus.statusId DESC LIMIT 1";
                        try (PreparedStatement statusStatement = connection.prepareStatement(statusQuery)) {
                            statusStatement.setInt(1, mainResultSet.getInt("requestId"));
                            try (ResultSet statusResultSet = statusStatement.executeQuery()) {
                                if (statusResultSet.next()) {
                                    request.setStatus(statusResultSet.getString("statusName"));
                                } else {
                                    request.setStatus(""); // Если статус не найден, установите пустую строку или другое значение по умолчанию
                                }
                            }
                        }

                        // Запрос для получения последнего исполнителя для текущей заявки
                        String assigneeQuery = "SELECT * FROM RequestAssignee " +
                                "INNER JOIN Assignees ON RequestAssignee.assigneeId = Assignees.assigneeId " +
                                "WHERE requestId = ? ORDER BY RequestAssignee.assigneeId DESC LIMIT 1";
                        try (PreparedStatement assigneeStatement = connection.prepareStatement(assigneeQuery)) {
                            assigneeStatement.setInt(1, mainResultSet.getInt("requestId"));
                            try (ResultSet assigneeResultSet = assigneeStatement.executeQuery()) {
                                if (assigneeResultSet.next()) {
                                    request.setAssignee(assigneeResultSet.getString("assigneeName"));
                                } else {
                                    request.setAssignee(""); // Если исполнитель не найден, установите пустую строку или другое значение по умолчанию
                                }
                            }
                        }
                        String commentQuery = "SELECT * FROM Comments WHERE requestId = ?";
                        try (PreparedStatement commentStatement = connection.prepareStatement(commentQuery)) {
                            commentStatement.setInt(1, mainResultSet.getInt("requestId"));
                            try (ResultSet commentResultSet = commentStatement.executeQuery()) {
                                List<Comment> comments = new ArrayList<>();
                                while (commentResultSet.next()) {
                                    Comment comment = new Comment();
                                    comment.setName(commentResultSet.getString("commenterName"));
                                    comment.setText(commentResultSet.getString("commentText"));
                                    comments.add(comment);
                                }
                                String commentText = String.join(", ", comments.stream()
                                        .map(comment -> comment.getName() + ": " + comment.getText())
                                        .collect(Collectors.toList()));

                                request.setComments(commentText);
                            }
                        }
                        requests.add(request);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }


    public static boolean updateRequest(Request request) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            connection.setAutoCommit(false); // Отключить автоподтверждение

            String query = "UPDATE Requests SET problemDescription = ? WHERE requestNumber = ? RETURNING requestId";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, request.getDescription());
                preparedStatement.setString(2, request.getNumber());
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int requestId = resultSet.getInt("requestId");

                    // Добавить запись о статусе заявки
                    String statusQuery = "INSERT INTO RequestStatus (requestId, statusId) VALUES (?, ?)";
                    try (PreparedStatement statusStatement = connection.prepareStatement(statusQuery)) {
                        int statusId = getStatusIdByName(request.getStatus(), connection);
                        if (statusId != -1) {
                            statusStatement.setInt(1, requestId);
                            statusStatement.setInt(2, statusId);
                            int statusRowsInserted = statusStatement.executeUpdate();

                            // Добавить запись о назначенном исполнителе
                            if (request.getAssignee() != null && !request.getAssignee().isEmpty()) {
                                String assigneeQuery = "INSERT INTO RequestAssignee (requestId, assigneeId) VALUES (?, ?)";
                                try (PreparedStatement assigneeStatement = connection.prepareStatement(assigneeQuery)) {
                                    int assigneeId = getAssigneeId(request.getAssignee(), connection);
                                    if (assigneeId != -1) {
                                        assigneeStatement.setInt(1, requestId);
                                        assigneeStatement.setInt(2, assigneeId);
                                        int assigneeRowsInserted = assigneeStatement.executeUpdate();

                                        if (statusRowsInserted > 0 && assigneeRowsInserted > 0) {
                                            connection.commit(); // Подтвердить транзакцию
                                            return true;
                                        }
                                    } else {
                                        System.err.println("Исполнитель не найден.");
                                    }
                                }
                            } else {
                                if (statusRowsInserted > 0) {
                                    connection.commit(); // Подтвердить транзакцию
                                    return true;
                                }
                            }
                        } else {
                            System.err.println("Статус не найден: " + request.getStatus());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (connection != null) {
                try {
                    connection.rollback(); // Откатить транзакцию
                } catch (SQLException rollbackException) {
                    rollbackException.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close(); // Закрыть соединение
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


    private static int getAssigneeId(String assigneeName, Connection connection) {
        try {
            String query = "SELECT assigneeId FROM Assignees WHERE assigneeName = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, assigneeName);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("assigneeId");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Если не удалось найти ID исполнителя, возвращаем -1
    }

    public static boolean deleteRequestByNumber(String requestNumber) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            connection.setAutoCommit(false); // Отключить автоподтверждение

            // Получаем ID заявки по номеру
            String sqlGetRequestId = "SELECT requestId FROM Requests WHERE requestNumber = ?";
            try (PreparedStatement statement = connection.prepareStatement(sqlGetRequestId)) {
                statement.setString(1, requestNumber);
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return false;
                }
                int requestId = resultSet.getInt(1);

                // Удаление комментариев
                String sqlDeleteComments = "DELETE FROM Comments WHERE requestId = ?";
                try (PreparedStatement statement2 = connection.prepareStatement(sqlDeleteComments)) {
                    statement2.setInt(1, requestId);
                    statement2.executeUpdate();
                }

                // Удаление истории изменения статуса
                String sqlDeleteStatusTracking = "DELETE FROM RequestStatus WHERE requestId = ?";
                try (PreparedStatement statement2 = connection.prepareStatement(sqlDeleteStatusTracking)) {
                    statement2.setInt(1, requestId);
                    statement2.executeUpdate();
                }

                // Удаление связи с исполнителями
                String sqlDeleteAssignees = "DELETE FROM RequestAssignee WHERE requestId = ?";
                try (PreparedStatement statement2 = connection.prepareStatement(sqlDeleteAssignees)) {
                    statement2.setInt(1, requestId);
                    statement2.executeUpdate();
                }

                // Удаление самой заявки
                String sqlDeleteRequest = "DELETE FROM Requests WHERE requestId = ?";
                try (PreparedStatement statement2 = connection.prepareStatement(sqlDeleteRequest)) {
                    statement2.setInt(1, requestId);
                    statement2.executeUpdate();
                }

                connection.commit(); // Коммитим транзакцию
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (connection != null) {
                try {
                    connection.rollback(); // Откатить транзакцию
                } catch (SQLException rollbackException) {
                    rollbackException.printStackTrace();
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close(); // Закрыть соединение
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static ObservableList<Request> findRequestByNumber(String requestNumber) {
        ObservableList<Request> requests = FXCollections.observableArrayList();

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            // Основной запрос для получения основных данных о заявках
            String mainQuery = "SELECT " +
                    "Requests.requestId, " +
                    "Requests.requestNumber, " +
                    "Requests.requestDate, " +
                    "EquipmentTypes.equipmentType, " +
                    "Requests.faultType, " +
                    "Requests.problemDescription, " +
                    "Clients.fullName " +
                    "FROM " +
                    "Requests " +
                    "INNER JOIN " +
                    "EquipmentTypes ON Requests.equipmentTypeId = EquipmentTypes.typeId " +
                    "INNER JOIN " +
                    "Clients ON Requests.clientId = Clients.clientId " +
                    "WHERE Requests.requestNumber = ?";

            try (PreparedStatement mainStatement = connection.prepareStatement(mainQuery)) {
                mainStatement.setString(1, requestNumber);
                try (ResultSet mainResultSet = mainStatement.executeQuery()) {
                    while (mainResultSet.next()) {
                        Request request = new Request();
                        request.setNumber(mainResultSet.getString("requestNumber"));
                        request.setDate(mainResultSet.getDate("requestDate").toLocalDate());
                        request.setEquipmentType(mainResultSet.getString("equipmentType"));
                        request.setFaultType(mainResultSet.getString("faultType"));
                        request.setDescription(mainResultSet.getString("problemDescription"));
                        request.setClientFullName(mainResultSet.getString("fullName"));

                        // Запрос для получения последнего статуса для текущей заявки
                        String statusQuery = "SELECT * FROM RequestStatus " +
                                "INNER JOIN Status ON RequestStatus.statusId = Status.statusId " +
                                "WHERE requestId = ? ORDER BY RequestStatus.statusId DESC LIMIT 1";
                        try (PreparedStatement statusStatement = connection.prepareStatement(statusQuery)) {
                            statusStatement.setInt(1, mainResultSet.getInt("requestId"));
                            try (ResultSet statusResultSet = statusStatement.executeQuery()) {
                                if (statusResultSet.next()) {
                                    request.setStatus(statusResultSet.getString("statusName"));
                                } else {
                                    request.setStatus(""); // Если статус не найден, установите пустую строку или другое значение по умолчанию
                                }
                            }
                        }

                        // Запрос для получения последнего исполнителя для текущей заявки
                        String assigneeQuery = "SELECT * FROM RequestAssignee " +
                                "INNER JOIN Assignees ON RequestAssignee.assigneeId = Assignees.assigneeId " +
                                "WHERE requestId = ? ORDER BY RequestAssignee.assigneeId DESC LIMIT 1";
                        try (PreparedStatement assigneeStatement = connection.prepareStatement(assigneeQuery)) {
                            assigneeStatement.setInt(1, mainResultSet.getInt("requestId"));
                            try (ResultSet assigneeResultSet = assigneeStatement.executeQuery()) {
                                if (assigneeResultSet.next()) {
                                    request.setAssignee(assigneeResultSet.getString("assigneeName"));
                                } else {
                                    request.setAssignee(""); // Если исполнитель не найден, установите пустую строку или другое значение по умолчанию
                                }
                            }
                        }
                        // Запрос для получения комментариев для текущей заявки
                        String commentQuery = "SELECT * FROM Comments WHERE requestId = ?";
                        try (PreparedStatement commentStatement = connection.prepareStatement(commentQuery)) {
                            commentStatement.setInt(1, mainResultSet.getInt("requestId"));
                            try (ResultSet commentResultSet = commentStatement.executeQuery()) {
                                List<Comment> comments = new ArrayList<>();
                                while (commentResultSet.next()) {
                                    Comment comment = new Comment();
                                    comment.setName(commentResultSet.getString("commenterName"));
                                    comment.setText(commentResultSet.getString("commentText"));
                                    comments.add(comment);
                                }
                                String commentText = String.join(", ", comments.stream()
                                        .map(comment -> comment.getName() + ": " + comment.getText())
                                        .collect(Collectors.toList()));

                                request.setComments(commentText);
                            }
                        }
                        requests.add(request);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }

    public static int getCompletedRequestsCount() {
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String query = """
                        SELECT COUNT(*) AS total
                                           FROM Requests r
                                           INNER JOIN RequestStatus rs ON r.requestId = rs.requestId
                                           INNER JOIN Status s ON rs.statusId = s.statusId
                                           WHERE s.statusName = 'Выполнено';
                    """;
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("total");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }


    public static long getAverageCompletionTime() {
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String query = "SELECT AVG(completionDate - requests.requestdate) AS average_time FROM Requests";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getLong("average_time");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static Map<String, Integer> getFaultTypeStats() {
        Map<String, Integer> faultTypeStats = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String query = "SELECT faultType, COUNT(*) AS total FROM Requests GROUP BY faultType";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        faultTypeStats.put(resultSet.getString("faultType"), resultSet.getInt("total"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return faultTypeStats;
    }

    public static void addComment(String name, String comment, String requestNumber) {
        Connection connection = null;
        PreparedStatement selectStatement = null;
        PreparedStatement insertStatement = null;
        int requestId = 0;

        try {
            // Connect to database
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

            // Get request ID by request number (separate PreparedStatement)
            String selectSql = "SELECT requestId FROM Requests WHERE requestNumber = ?";
            selectStatement = connection.prepareStatement(selectSql);
            selectStatement.setString(1, requestNumber);
            ResultSet resultSet = selectStatement.executeQuery();

            // Check if request exists
            if (resultSet.next()) {
                requestId = resultSet.getInt("requestId");
            } else {
                System.err.println("Request with number " + requestNumber + " not found!");
                return;
            }

            // Add comment using obtained request ID (separate PreparedStatement)
            String insertSql = "INSERT INTO comments (commenterName, commentText, requestId) VALUES (?, ?, ?)";
            insertStatement = connection.prepareStatement(insertSql);
            insertStatement.setString(1, name);
            insertStatement.setString(2, comment);
            insertStatement.setInt(3, requestId);

            insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close resources (close selectStatement if used)
            try {
                if (selectStatement != null) {
                    selectStatement.close();
                }
                if (insertStatement != null) {
                    insertStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

}


