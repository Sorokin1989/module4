package com.minibank.service;

import com.minibank.config.DatabaseConnection;
import com.minibank.dao.AccountDao.AccountDao;
import com.minibank.dao.ClientDao.ClientDao;
import com.minibank.dao.TransactionDao.TransactionDao;
import com.minibank.model.Account;
import com.minibank.model.Client;
import com.minibank.model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private final ClientDao clientDao;
    private final AccountDao accountDao;
    private final TransactionDao transactionDao;

    public BankService(ClientDao clientDao, AccountDao accountDao, TransactionDao transactionDao) {
        this.clientDao = clientDao;
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
    }

    public Client registerClient(String name, String email) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не должно быть пустым!");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email не должен быть пустым!");
        }

        return clientDao.save(name, email);
    }


    public Account openAccount(Long clientId, String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Номер счёта не может быть пустым!");
        }
        getClient(clientId);

        return accountDao.createAccount(clientId, accountNumber);

    }

    public Account getAccount(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Id некорректный!");
        }
        return accountDao.findById(accountId);

    }

    public Client getClient(Long clientId) {

        if (clientId == null || clientId <= 0) {
            throw new IllegalArgumentException("Некорректный id клиента " + clientId);
        }
        return clientDao.findById(clientId);

    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {

        validateTransferParams(fromAccountId, toAccountId, amount);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                accountDao.withdraw(connection, fromAccountId, amount);
                accountDao.deposit(connection, toAccountId, amount);
                transactionDao.save(fromAccountId, toAccountId, amount, connection);
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
            }
        catch (SQLException e) {
            throw new RuntimeException("Ошибка подключения к БД или начала транзакции! ", e);
        }
    }


    public List<Transaction> getTransactions(Long accountId, LocalDateTime from, LocalDateTime to) {

        validateRequest(accountId, from, to);


        String transactionSql = "SELECT id, account_id, amount, type, transaction_date \n" +
                "FROM transactions \n" +
                "WHERE account_id = ? AND transaction_date BETWEEN ? AND ?\n" +
                "ORDER BY transaction_date DESC";

        List<Transaction> transactions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(transactionSql)) {
            statement.setLong(1, accountId);
            statement.setTimestamp(2, Timestamp.valueOf(from));
            statement.setTimestamp(3, Timestamp.valueOf(to));

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    long accId = resultSet.getLong("account_id");
                    BigDecimal amount = resultSet.getBigDecimal("amount");
                    String type = resultSet.getString("type");
                    LocalDateTime transactionDate = resultSet.getTimestamp("transaction_date").toLocalDateTime();

                    transactions.add(new Transaction(id, accId, amount, type, transactionDate));
                }
            }


        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении транзакций ", e);
        }

        return transactions;

    }

    private void validateRequest(Long accountId, LocalDateTime from, LocalDateTime to) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("Некорректный id " + accountId);
        }

        if (from == null || to == null) {
            throw new IllegalArgumentException("Некорректная дата!");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Начальная дата не может быть позже конечной!");
        }
    }


    private void validateTransferParams(Long fromAccountId, Long toAccountId, BigDecimal amount) {
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
