package com.code.database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static Connection connection;

    private DBConnection() {
    }

    public static Connection getConnection() {

        try {

            if (
                    connection == null ||
                            connection.isClosed()
            ) {

                connection =
                        DriverManager.getConnection(
                                DBConfig.getUrl(),
                                DBConfig.getUsername(),
                                DBConfig.getPassword()
                        );
            }

            return connection;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Database connection failed",
                    e
            );
        }
    }
}