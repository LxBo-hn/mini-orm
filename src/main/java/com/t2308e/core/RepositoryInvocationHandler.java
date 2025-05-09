package com.t2308e.core;

import com.t2308e.annotations.MyColumn;
import com.t2308e.config.DataSourceConfig;
import com.t2308e.exception.MiniOrmException;
import com.t2308e.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RepositoryInvocationHandler<T, ID> implements InvocationHandler {

    private final DataSourceConfig dataSourceConfig;
    private final Class<T> entityClass;
    private final Class<ID> idClass; // Not directly used in SQL but good for type safety
    private final String tableName;
    private final Field idField;
    private final String idColumnName;

    @SuppressWarnings("unchecked")
    public RepositoryInvocationHandler(DataSourceConfig dataSourceConfig, Class<?> repositoryInterface) {
        this.dataSourceConfig = dataSourceConfig;

        ParameterizedType genericInterface = (ParameterizedType) repositoryInterface.getGenericInterfaces()[0];
        this.entityClass = (Class<T>) genericInterface.getActualTypeArguments()[0];
        this.idClass = (Class<ID>) genericInterface.getActualTypeArguments()[1];

        this.tableName = ReflectionUtil.getTableName(entityClass);
        this.idField = ReflectionUtil.getIdField(entityClass);
        this.idColumnName = ReflectionUtil.getColumnName(this.idField);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if ("save".equals(methodName)) {
            return save((T) args[0]);
        } else if ("findById".equals(methodName)) {
            return findById((ID) args[0]);
        } else if ("findAll".equals(methodName)) {
            return findAll();
        } else if ("deleteById".equals(methodName)) {
            deleteById((ID) args[0]);
            return null; // void method
        } else if ("count".equals(methodName)) {
            return count();
        }
        // Handle Object methods like toString, hashCode, equals
        if (method.getDeclaringClass().equals(Object.class)) {
            return method.invoke(this, args);
        }

        throw new MiniOrmException("Unsupported method: " + methodName);
    }

    private T save(T entity) throws SQLException, IllegalAccessException {
        ID idValue = (ID) idField.get(entity);
        Map<String, Field> columnFields = ReflectionUtil.getColumnFields(entityClass);

        if (idValue == null || (idValue instanceof Number && ((Number) idValue).longValue() == 0L)) { // Assuming 0 means new for numeric IDs
            // INSERT
            String columns = String.join(", ", columnFields.keySet());
            String placeholders = columnFields.keySet().stream().map(c -> "?").collect(Collectors.joining(", "));
            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);

            System.out.println("Executing SQL: " + sql);

            try (Connection conn = dataSourceConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                int i = 1;
                for (Field field : columnFields.values()) {
                    stmt.setObject(i++, field.get(entity));
                }
                stmt.executeUpdate();

                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    // Set the generated ID back to the entity object
                    Object generatedId = generatedKeys.getObject(1);
                    // Convert to appropriate type if necessary, e.g., Long for auto-increment
                    if (idField.getType() == Long.class || idField.getType() == long.class) {
                        idField.set(entity, ((Number)generatedId).longValue());
                    } else if (idField.getType() == Integer.class || idField.getType() == int.class) {
                        idField.set(entity, ((Number)generatedId).intValue());
                    } else {
                        idField.set(entity, generatedId);
                    }
                }
                return entity;
            }
        } else {
            // UPDATE
            String setClauses = columnFields.keySet().stream()
                    .map(colName -> colName + " = ?")
                    .collect(Collectors.joining(", "));
            String sql = String.format("UPDATE %s SET %s WHERE %s = ?", tableName, setClauses, idColumnName);
            System.out.println("Executing SQL: " + sql);

            try (Connection conn = dataSourceConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                int i = 1;
                for (Field field : columnFields.values()) {
                    stmt.setObject(i++, field.get(entity));
                }
                stmt.setObject(i, idValue); // Set ID for WHERE clause
                stmt.executeUpdate();
                return entity;
            }
        }
    }

    private Optional<T> findById(ID id) throws SQLException {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumnName);
        System.out.println("Executing SQL: " + sql);

        try (Connection conn = dataSourceConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToEntity(rs));
            }
        }
        return Optional.empty();
    }

    private List<T> findAll() throws SQLException {
        List<T> results = new ArrayList<>();
        String sql = String.format("SELECT * FROM %s", tableName);
        System.out.println("Executing SQL: " + sql);

        try (Connection conn = dataSourceConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRowToEntity(rs));
            }
        }
        return results;
    }

    private void deleteById(ID id) throws SQLException {
        String sql = String.format("DELETE FROM %s WHERE %s = ?", tableName, idColumnName);
        System.out.println("Executing SQL: " + sql);

        try (Connection conn = dataSourceConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        }
    }

    private long count() throws SQLException {
        String sql = String.format("SELECT COUNT(*) FROM %s", tableName);
        System.out.println("Executing SQL: " + sql);

        try (Connection conn = dataSourceConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    private T mapRowToEntity(ResultSet rs) throws SQLException {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            // Map ID field
            Object idValue = rs.getObject(idColumnName);
            idField.set(entity, convertToFieldType(idValue, idField.getType()));


            // Map other column fields
            Map<String, Field> columnFields = ReflectionUtil.getColumnFields(entityClass);
            for (Map.Entry<String, Field> entry : columnFields.entrySet()) {
                String columnName = entry.getKey();
                Field field = entry.getValue();
                Object value = rs.getObject(columnName);
                field.set(entity, convertToFieldType(value, field.getType()));
            }
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new MiniOrmException("Failed to instantiate or map entity " + entityClass.getSimpleName(), e);
        }
    }

    private Object convertToFieldType(Object dbValue, Class<?> fieldType) {
        if (dbValue == null) {
            return null;
        }
        // Basic conversion, can be expanded
        if (fieldType == Long.class || fieldType == long.class) {
            return ((Number) dbValue).longValue();
        }
        if (fieldType == Integer.class || fieldType == int.class) {
            return ((Number) dbValue).intValue();
        }
        if (fieldType == Double.class || fieldType == double.class) {
            return ((Number) dbValue).doubleValue();
        }
        if (fieldType == Float.class || fieldType == float.class) {
            return ((Number) dbValue).floatValue();
        }
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            if (dbValue instanceof Number) {
                return ((Number) dbValue).intValue() != 0;
            }
            return (Boolean) dbValue;
        }
        // For String, Date, etc., JDBC driver usually handles it well with getObject()
        return dbValue;
    }
}
