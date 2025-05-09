package com.t2308e.util;

import com.t2308e.annotations.MyColumn;
import com.t2308e.annotations.MyEntity;
import com.t2308e.annotations.MyId;
import com.t2308e.annotations.MyTransient;
import com.t2308e.exception.MiniOrmException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReflectionUtil {

    public static String getTableName(Class<?> entityClass) {
        MyEntity entityAnnotation = entityClass.getAnnotation(MyEntity.class);
        if (entityAnnotation == null) {
            throw new MiniOrmException("Class " + entityClass.getSimpleName() + " is not annotated with @MyEntity");
        }
        String tableName = entityAnnotation.tableName();
        return tableName.isEmpty() ? entityClass.getSimpleName().toLowerCase() + "s" : tableName; // Simple pluralization or use as is
    }

    public static Field getIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(MyId.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new MiniOrmException("No @MyId field found in entity " + entityClass.getSimpleName());
    }

    public static String getColumnName(Field field) {
        MyColumn columnAnnotation = field.getAnnotation(MyColumn.class);
        if (columnAnnotation != null && !columnAnnotation.name().isEmpty()) {
            return columnAnnotation.name();
        }
        return field.getName(); // Default to field name
    }

    public static Map<String, Field> getColumnFields(Class<?> entityClass) {
        Map<String, Field> columnFields = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(MyTransient.class) || field.isAnnotationPresent(MyId.class)) {
                continue; // Skip transient fields and ID field (handled separately)
            }
            field.setAccessible(true);
            columnFields.put(getColumnName(field), field);
        }
        return columnFields;
    }

    public static List<Field> getAllPersistableFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        Field idField = getIdField(entityClass);
        fields.add(idField); // Add ID field first

        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(MyTransient.class) && !field.isAnnotationPresent(MyId.class)) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }
}
