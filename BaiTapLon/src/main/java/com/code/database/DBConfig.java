package com.code.database;

import java.io.InputStream;
import java.util.Properties;

public class DBConfig {

    private static final Properties properties = new Properties();

    static {

        try (
                InputStream input =
                        DBConfig.class
                                .getClassLoader()
                                .getResourceAsStream("db.properties")
        ) {

            properties.load(input);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot load db.properties",
                    e
            );
        }
    }

    public static String getUrl() {
        return properties.getProperty("db.url");
    }

    public static String getUsername() {
        return properties.getProperty("db.username");
    }

    public static String getPassword() {
        return properties.getProperty("db.password");
    }
}