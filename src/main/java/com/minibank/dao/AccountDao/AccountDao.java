package com.minibank.dao.AccountDao;

import com.minibank.config.DatabaseConnection;
import com.minibank.model.Account;

import java.math.BigDecimal;
import java.sql.*;

public class AccountDao {

    public void withdraw(Connection connection, Long fromAccountId, BigDecimal amount) throws SQLException {
    String withdrawSql = "UPDATE accounts SET balance = balance - ? WHERE id = ? AND balance >= ?";

    try(PreparedStatement statement=connection.prepareStatement(withdrawSql)
    ) {
        statement.setBigDecimal(1, amount);
        statement.setLong(2, fromAccountId);
        statement.setBigDecimal(3, amount);
        int update = statement.executeUpdate();

        if (update == 0) {
            throw new SQLException("Недостаточно средств или счет не найден:" + fromAccountId);
        }
    }
    }

    public void deposit(Connection connection,Long toAccountId, BigDecimal amount) throws SQLException {
        String depositSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";

        try(PreparedStatement statement=connection.prepareStatement(depositSql)) {
            statement.setBigDecimal(1, amount);
            statement.setLong(2, toAccountId);
            statement.executeUpdate();
        }
    }





    public Account createAccount(Long clientId, String accountNumber) {

        String sql = "INSERT INTO accounts (client_id,account_number,balance) values (?,?,0)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {
            statement.setLong(1, clientId);
            statement.setString(2, accountNumber);
            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    long accountId = resultSet.getLong(1);
                    System.out.println("Счет создан! " + accountId + " " + accountNumber);
                    return new Account(accountId, clientId, accountNumber, BigDecimal.ZERO);
                } else {
                    throw new SQLException("Id счета не найден!");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания счета " + e);
        }
    }

    public Account findById(Long accountId) {

        String sql = "SELECT id,client_id,account_number,balance FROM accounts WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    long clientId = resultSet.getLong("client_id");
                    String accountNumber = resultSet.getString("account_number");
                    BigDecimal balance = resultSet.getBigDecimal("balance");
                    return new Account(id, clientId, accountNumber, balance);
                }
            }
            throw new IllegalArgumentException("Аккаунт по id не найден! " + accountId);

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении счета! " + accountId, e);
        }
    }
}
