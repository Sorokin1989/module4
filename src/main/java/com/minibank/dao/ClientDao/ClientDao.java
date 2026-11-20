package com.minibank.dao.ClientDao;

import com.minibank.config.DatabaseConnection;
import com.minibank.model.Client;

import java.sql.*;

public class ClientDao {


    public Client save(String name, String email) {
        String sql = "INSERT INTO clients (name,email) values (?,?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.executeUpdate();


            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    long id = resultSet.getLong(1);
                    System.out.println("Клиент создан! " + id + " " + name + " " + email);

                    return new Client(id, name, email);

                } else {
                    throw new SQLException("Не удалось найти клиента по id!");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания клиента! " + e);
        }

    }

    public Client findById(Long clientId) {

        String sql = "SELECT id,name,email FROM clients WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String name = resultSet.getString("name");
                    String email = resultSet.getString("email");
                    return new Client(id, name, email);
                }
            }
            throw new IllegalArgumentException("Клиент с таким id не найден " + clientId);

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения клиента! " + clientId, e);
        }
    }
}
