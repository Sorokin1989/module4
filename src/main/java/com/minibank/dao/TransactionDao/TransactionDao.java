package com.minibank.dao.TransactionDao;

import com.minibank.config.DatabaseConnection;
import com.minibank.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionDao {

    public void save(Long fromAccountId, Long toAccountId, BigDecimal amount,Connection connection) throws SQLException {
        String logSql = "INSERT INTO transactions(account_id,amount,type) VALUES (?,?,?)";

        try(PreparedStatement statement=connection.prepareStatement(logSql)){

            statement.setLong(1, fromAccountId);
            statement.setBigDecimal(2, amount.negate());
            statement.setString(3, "TRANSFER_OUT");
            statement.executeUpdate();

            statement.setLong(1, toAccountId);
            statement.setBigDecimal(2, amount);
            statement.setString(3, "TRANSFER_IN");
            statement.executeUpdate();
        }
    }
}
