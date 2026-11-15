package com.minibank.service;

import com.minibank.connection.DatabaseConnection;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * MiniBank — "MVP", который наспех собрал предыдущий разработчик:
 * вся логика в одном классе, все данные — в HashMap, персистентности
 * нет вообще (перезапуск процесса — и всё пропало).
 * <p>
 * ПРОБЛЕМА (найдёшь и исправишь, когда дойдёт очередь — модуль 7.1,
 * когда появится @Transactional): в transfer() проверка существования
 * счёта-получателя происходит ПОСЛЕ списания денег со счёта-
 * отправителя. Если счёт-получатель не существует, деньги уже спишутся
 * с первого счёта и "исчезнут" — их некуда вернуть, потому что метод
 * прерывается исключением до зачисления.
 * <p>
 * Эта проблема не решается на HashMap "в лоб" без искусственных
 * костылей (можно вручную откатывать баланс в catch, но это не
 * настоящее решение) — она по-настоящему решается только когда
 * появится транзакционность (сначала в БД — ACID, модуль 4, а затем
 * в коде — @Transactional, модуль 7.1). До этого момента баг просто
 * существует и это нормально: то, что появится в модуле 7.1, — как
 * раз тот инструмент, который проблему полностью закрывает.
 */
public class BankService {


    public Client registerClient(String name, String email) {

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

    public Account openAccount(Long clientId, String accountNumber) {
        Client client = getClient(clientId);

        if (client == null) {
            throw new IllegalArgumentException("Клиент не найден! " + clientId);
        }

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

    public Account getAccount(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Id некорректный!");
        }

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

    public Client getClient(Long clientId) {

        if (clientId == null || clientId <= 0) {
            throw new IllegalArgumentException("Некорректный id клиента " + clientId);
        }

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

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {

        validate(fromAccountId, toAccountId, amount);

        String withdrawSql = "UPDATE accounts SET balance = balance - ? WHERE id = ? AND balance >= ?";
        String depositSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
        String logSql = "INSERT INTO transactions(account_id,amount,type) VALUES (?,?,?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(withdrawSql);
                 PreparedStatement statement2 = connection.prepareStatement(depositSql);
                 PreparedStatement statement3 = connection.prepareStatement(logSql)
            ) {
                statement.setBigDecimal(1, amount);
                statement.setLong(2, fromAccountId);
                statement.setBigDecimal(3, amount);
                int update = statement.executeUpdate();

                if (update == 0) {
                    throw new SQLException("Недостаточно средств или счет не найден:" + fromAccountId);
                }


                statement2.setBigDecimal(1, amount);
                statement2.setLong(2, toAccountId);
                statement2.executeUpdate();

                statement3.setLong(1, fromAccountId);
                statement3.setBigDecimal(2, amount.negate());
                statement3.setString(3, "TRANSFER_OUT");
                statement3.executeUpdate();

                statement3.setLong(1, toAccountId);
                statement3.setBigDecimal(2, amount);
                statement3.setString(3, "TRANSFER_IN");
                statement3.executeUpdate();

                connection.commit();
                System.out.println("Перевод выполнен!");
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw new RuntimeException("Ошибка при переводе! ", e);
            }

        } catch (SQLException e) {

            throw new RuntimeException("Ошибка подключения к БД или начала транзакции! ", e);

        }
    }
    private void validate(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной!");
        }
        if (fromAccountId == null || fromAccountId <= 0) {
            throw new IllegalArgumentException("Некорректный id отправителя! " + fromAccountId);
        }
        if (toAccountId == null || toAccountId <= 0) {
            throw new IllegalArgumentException("Некорректный id получателя " + toAccountId);
        }

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Нельзя переводить на один и тот же счет!");
        }
    }


}
