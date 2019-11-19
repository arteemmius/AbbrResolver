package ru.textanalysis.abbrresolver.realization;

import ru.textanalysis.abbrresolver.beans.Item;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManager implements Closeable {

    private static DBManager singleInstance;
    private static Connection conn;
    
    private DBManager() {
        try {
            Class.forName("org.sqlite.JDBC").newInstance();
            conn = DriverManager.getConnection("jdbc:sqlite:" + "../webapps/dict/abbreviation.db");
        } catch (Exception e) {
            System.out.println("Ошибка подключения: " + e.getMessage());
        }
    }

    public synchronized static DBManager getInstance() {
        if (singleInstance == null) {
            singleInstance = new DBManager();
        }
        return singleInstance;
    }

    public static Connection getConnection() {
        try {
            if (conn == null) {
                Class.forName("org.sqlite.JDBC").newInstance();
                conn = DriverManager.getConnection("jdbc:sqlite:" + "../webapps/dict/abbreviation.db");
            }
        } catch (Exception e) {
            System.out.println("Ошибка подключения: " + e.getMessage());
        }
        return conn;
    }

    @Override
    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
            singleInstance = null;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void addItem(Item item) throws Exception {

        Integer shortFormId = null;

        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM shortForm WHERE value = ? AND type = ?")) {
            stmt.setString(1, item.getWord().trim());
            if (item.getType() != null) {
                stmt.setInt(2, item.getType());
            }
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    shortFormId = resultSet.getInt(1);
                }
            }
        }

        if (shortFormId == null) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO shortForm ('value', 'type') VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, item.getWord().trim());
                if (item.getType() != null) {
                    stmt.setInt(2, item.getType());
                }
                stmt.executeUpdate();
                shortFormId = stmt.getGeneratedKeys().getInt(1);
            }
        }

        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO longForm ('shortFormId', 'definition', 'description') VALUES (?, ?, ?)")) {
            stmt.setInt(1, shortFormId);
            stmt.setString(2, item.getDefinition().trim());
            stmt.setString(3, item.getDescription() != null ? item.getDescription().trim() : item.getDescription());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<String> findAbbrLongForms(String abbr) throws Exception {
        List<String> values = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT longForm.definition FROM shortForm INNER JOIN longForm ON longForm.shortFormId = shortForm.id WHERE shortForm.value = ?")) {
            stmt.setString(1, abbr);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
        }
        return values;
    }
    
    public List<String> findAbbrLongFormsWithMainWord(String abbr, String topic) throws Exception {
        List<String> values = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT longForm.definition FROM shortForm INNER JOIN longForm ON longForm.shortFormId = shortForm.id WHERE shortForm.value = '" + abbr + "' and longForm.description LIKE '%" + topic + "%'")) {
            //stmt.setString(1, abbr);
            //stmt.setString(2, topic);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
        }
        return values;
    }  

    public List<String> findAbbrInfo(String abbr) throws Exception {
        List<String> values = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM longForm INNER JOIN shortForm ON longForm.shortFormId = shortForm.id WHERE shortForm.value = ?")) {
            stmt.setString(1, abbr);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    values.add(abbr);
                    values.add(resultSet.getString(3));
                    if (resultSet.getString(4) == null){
                        values.add("-");
                    } 
                }
            }
        }
        return values;
    }

    public void dropAllTables() throws Exception {

        List<String> tableNames = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM main.sqlite_master WHERE type = 'table'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tableNames.add(rs.getString(1));
                }
            }
        }

        int d = tableNames.indexOf("sqlite_sequence");
        if (d >= 0) {
            tableNames.remove(d);
        }

        for (String tableName : tableNames) {
            try (PreparedStatement stmt = conn.prepareStatement(String.format("DROP TABLE %s", tableName))) {
                stmt.execute();
            }
        }
    }

    public void executeScript(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("windows-1251")))) {

            String curLine;
            while ((curLine = reader.readLine()) != null) {
                if ("/".equals(curLine)) {
                    stmt.execute(sb.toString());
                    sb = new StringBuilder();
                } else {
                    sb.append(curLine);
                }
            }
        }
        is.close();
    }
}
