package me.duck.rePlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

public class Database {
    private Connection connection;

    public void init(File dataFolder) {
        try {
            if (!(dataFolder.exists())) dataFolder.mkdirs();

            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/database.db";
            connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS playersKeys(uuid TEXT PRIMARY KEY, pin TEXT)");
                statement.execute("CREATE TABLE IF NOT EXISTS homes(uuid TEXT PRIMARY KEY, homes TEXT)");
                statement.execute("CREATE TABLE IF NOT EXISTS bank(uuid TEXT PRIMARY KEY, money INTEGER)");
                statement.execute("CREATE TABLE IF NOT EXISTS playersData(uuid TEXT PRIMARY KEY, prefix TEXT, color TEXT)");
                statement.execute("CREATE TABLE IF NOT EXISTS otherData(nname TEXT PRIMARY KEY, vvalue TEXT)");
                statement.execute("CREATE TABLE IF NOT EXISTS playerPrefix(uuid TEXT PRIMARY KEY, prefix TEXT)");

            }
            String sql2 = "ALTER TABLE playersData ADD COLUMN gametimer INTEGER DEFAULT 0;";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql2);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public Connection getConnection() {
        return connection;
    }
}
