// src/main/java/com/example/chartvisualizerbackend/service/DynamicConnectionService.java
package com.example.chartvisualizerbackend.service;

import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

@Service
public class DynamicConnectionService {

    // Veritabanı bağlantısı kurar ve verilen SQL sorgusunu çalıştırır.
    // Sorgudan dönen sütun başlıklarını ve verileri Map listesi olarak döndürür.
    public Map<String, Object> executeDynamicQuery(
            String host, String dbName, String user, String password, String sqlQuery) throws SQLException {

        String url = String.format("jdbc:postgresql://%s:5432/%s", host, dbName);
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        Map<String, Object> response = new HashMap<>();
        // List<String> columns = new ArrayList<>(); // Sadece isimler yerine Map olarak tutacağız
        List<Map<String, String>> columnsMetadata = new ArrayList<>(); // Sütun adı ve tipi için
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sqlQuery);

            // Sütun bilgilerini al (adı ve tipi)
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                Map<String, String> col = new HashMap<>();
                col.put("name", metaData.getColumnName(i));
                col.put("type", metaData.getColumnTypeName(i)); // PostgreSQL tipi (örn: varchar, int4, numeric)
                columnsMetadata.add(col);
                // columns.add(metaData.getColumnName(i)); // Artık buna gerek kalmadı
            }

            // Verileri oku
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                // columns listesi yerine columnsMetadata'dan isimleri alıyoruz
                for (Map<String, String> colMeta : columnsMetadata) {
                    row.put(colMeta.get("name"), resultSet.getObject(colMeta.get("name")));
                }
                data.add(row);
            }

            response.put("columns", columnsMetadata); // Sütun başlıkları ve tipleri
            response.put("data", data);       // Veri satırları

        } finally {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        }
        return response;
    }

    // Veritabanındaki View ve Fonksiyonların isimlerini listeler.
    // Sadece tablo döndüren fonksiyonları da dahil ederiz.
    public List<String> getDatabaseObjects(String host, String dbName, String user, String password) throws SQLException {
        String url = String.format("jdbc:postgresql://%s:5432/%s", host, dbName);
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        List<String> objects = new ArrayList<>();

        try {
            connection = DriverManager.getConnection(url, user, password);
            statement = connection.createStatement();

            // SQL sorgusu ile View ve Fonksiyon isimlerini al
            // PostgreSQL'de stored procedure'ler genellikle function olarak implemente edilir.
            // Bu sorgu View'ları ve veri döndüren fonksiyonları (tablo değerli fonksiyonlar da dahil) listeler.
            String sql = "SELECT routine_name FROM information_schema.routines " +
                    "WHERE routine_schema = 'public' AND routine_type = 'FUNCTION' " +
                    "UNION ALL " +
                    "SELECT table_name FROM information_schema.views " +
                    "WHERE table_schema = 'public';";

            resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                objects.add(resultSet.getString("routine_name")); // veya table_name
            }

        } finally {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        }
        return objects;
    }

    // Seçilen veri objesinin (View/Function) parametrelerini bulur
    public List<Map<String, String>> getFunctionParameters(String host, String dbName, String user, String password, String functionName) throws SQLException {
        String url = String.format("jdbc:postgresql://%s:5432/%s", host, dbName);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Map<String, String>> parameters = new ArrayList<>();

        try {
            connection = DriverManager.getConnection(url, user, password);

            // PostgreSQL'de fonksiyon parametrelerini almak için information_schema.parameters kullanılır.
            // Sadece IN (giriş) modundaki parametreleri filtreliyoruz.
            String sql = "SELECT p.parameter_name, p.data_type, p.ordinal_position " +
                    "FROM information_schema.parameters p " +
                    "WHERE p.specific_name = (SELECT specific_name FROM information_schema.routines WHERE routine_name = ? AND routine_schema = 'public') " +
                    "AND p.parameter_mode = 'IN' " + // <-- BURAYI EKLEDİK
                    "ORDER BY p.ordinal_position;";

            statement = connection.prepareStatement(sql);
            statement.setString(1, functionName);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Map<String, String> param = new HashMap<>();
                param.put("name", resultSet.getString("parameter_name"));
                param.put("type", resultSet.getString("data_type"));
                // position'ı şu anlık kullanmıyoruz, ancak isteğe bağlı olarak ekleyebilirsiniz.
                // param.put("position", String.valueOf(resultSet.getInt("ordinal_position")));
                parameters.add(param);
            }
        } finally {
            if (resultSet != null) resultSet.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        }
        return parameters;
    }

}