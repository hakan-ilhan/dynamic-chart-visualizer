// src/main/java/com/example/chartvisualizerbackend/controller/ChartController.java
package com.example.chartvisualizerbackend.controller;

import com.example.chartvisualizerbackend.service.DynamicConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/charts")
public class ChartController {

    @Autowired
    private DynamicConnectionService dynamicConnectionService;

    @PostMapping("/data")
    public ResponseEntity<?> getChartData(@RequestBody Map<String, Object> requestBody) {
        try {
            String host = (String) requestBody.get("host");
            String dbName = (String) requestBody.get("dbName");
            String user = (String) requestBody.get("user");
            String password = (String) requestBody.get("password");
            String objectName = (String) requestBody.get("objectName");
            List<Map<String, Object>> params = (List<Map<String, Object>>) requestBody.get("parameters");

            String sqlQuery;
            if (params != null && !params.isEmpty()) {
                StringBuilder paramString = new StringBuilder();
                for (int i = 0; i < params.size(); i++) {
                    Map<String, Object> param = params.get(i);
                    String paramValue = String.valueOf(param.get("value"));
                    String paramType = String.valueOf(param.get("type")).toLowerCase(); // Tipi küçük harfe çevir

                    // Parametre tipine göre değeri formatla
                    // PostgreSQL veri tiplerini daha kapsamlı ele alalım
                    if (paramType.contains("char") || paramType.contains("text") || paramType.contains("date") || paramType.contains("time") || paramType.contains("uuid")) {
                        // String türündeki değerleri tek tırnak içine al ve SQL enjeksiyonuna karşı kaçış karakteri kullan
                        paramString.append("'").append(paramValue.replace("'", "''")).append("'");
                    } else if (paramType.contains("int") || paramType.contains("numeric") || paramType.contains("decimal") || paramType.contains("float") || paramType.contains("double")) {
                        // Sayısal değerleri doğrudan kullan
                        paramString.append(paramValue);
                    } else {
                        // Bilinmeyen tipler için varsayılan olarak string muamelesi yap
                        paramString.append("'").append(paramValue.replace("'", "''")).append("'");
                    }

                    if (i < params.size() - 1) {
                        paramString.append(", ");
                    }
                }
                sqlQuery = String.format("SELECT * FROM %s(%s)", objectName, paramString.toString());
            } else {
                sqlQuery = String.format("SELECT * FROM %s", objectName);
            }

            Map<String, Object> result = dynamicConnectionService.executeDynamicQuery(host, dbName, user, password, sqlQuery);
            return ResponseEntity.ok(result);

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Veritabanı hatası: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Beklenmedik bir hata oluştu: " + e.getMessage());
        }
    }

    // ... (diğer metodlar: getDatabaseObjects, getObjectParameters aynı kalır)
    @PostMapping("/objects")
    public ResponseEntity<?> getDatabaseObjects(@RequestBody Map<String, String> connectionInfo) {
        try {
            String host = connectionInfo.get("host");
            String dbName = connectionInfo.get("dbName");
            String user = connectionInfo.get("user");
            String password = connectionInfo.get("password");

            List<String> objects = dynamicConnectionService.getDatabaseObjects(host, dbName, user, password);
            return ResponseEntity.ok(objects);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Veritabanı bağlantı hatası: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Beklenmedik bir hata oluştu: " + e.getMessage());
        }
    }

    @PostMapping("/object-parameters")
    public ResponseEntity<?> getObjectParameters(@RequestBody Map<String, String> requestBody) {
        try {
            String host = requestBody.get("host");
            String dbName = requestBody.get("dbName");
            String user = requestBody.get("user");
            String password = requestBody.get("password");
            String objectName = requestBody.get("objectName");

            List<Map<String, String>> parameters = dynamicConnectionService.getFunctionParameters(host, dbName, user, password, objectName);
            return ResponseEntity.ok(parameters);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Veritabanı hatası: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Beklenmedik bir hata oluştu: " + e.getMessage());
        }
    }
}