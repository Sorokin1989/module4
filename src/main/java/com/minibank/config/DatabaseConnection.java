package com.minibank.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public static final String URL = "jdbc:postgresql://localhost:5432/bank_db";
    public static final String USER = "admin";
    public static final String PASSWORD = "admin";

    private DatabaseConnection() {}

    public static Connection getConnection() {
        try {
           return DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подключения к БД " + e);
        }
    }


}
