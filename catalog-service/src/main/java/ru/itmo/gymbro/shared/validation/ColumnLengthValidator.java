package ru.itmo.gymbro.shared.validation;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class ColumnLengthValidator implements SmartInitializingSingleton {

    private final R2dbcMappingContext mappingContext;
    private final DatabaseClient database;

    ColumnLengthValidator(R2dbcMappingContext mappingContext, DatabaseClient database) {
        this.mappingContext = mappingContext;
        this.database = database;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> mismatches = mismatchesFor(expectedColumns());
        if (!mismatches.isEmpty()) {
            throw new IllegalStateException("Длины колонок в БД не совпадают с сущностями:\n  - "
                    + String.join("\n  - ", mismatches));
        }
    }

    List<ExpectedColumn> expectedColumns() {
        List<ExpectedColumn> columns = new ArrayList<>();
        for (RelationalPersistentEntity<?> entity : mappingContext.getPersistentEntities()) {
            for (RelationalPersistentProperty property : entity) {
                Size size = property.findAnnotation(Size.class);
                if (size != null && property.getType() == String.class) {
                    columns.add(new ExpectedColumn(entity.getTableName().getReference(),
                            property.getColumnName().getReference(), size.max()));
                }
            }
        }
        return columns;
    }

    List<String> mismatchesFor(Collection<ExpectedColumn> expected) {
        Map<String, Integer> actualLengths = database.sql("""
                        SELECT table_name, column_name, character_maximum_length
                        FROM information_schema.columns
                        WHERE table_schema = current_schema() AND data_type = 'character varying'
                        """)
                .map(row -> Map.entry(row.get("table_name", String.class) + "." + row.get("column_name", String.class),
                        row.get("character_maximum_length", Integer.class)))
                .all()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .block();
        List<String> mismatches = new ArrayList<>();
        for (ExpectedColumn column : expected) {
            Integer actualLength = actualLengths.get(column.table + "." + column.name);
            if (actualLength != null && actualLength != column.length) {
                mismatches.add(column.table + "." + column.name + ": в сущности длина " + column.length
                        + ", в БД varchar(" + actualLength + ")");
            }
        }
        return mismatches;
    }

    @Getter(AccessLevel.PACKAGE)
    static final class ExpectedColumn {

        private final String table;
        private final String name;
        private final long length;

        ExpectedColumn(String table, String name, long length) {
            this.table = table;
            this.name = name;
            this.length = length;
        }
    }
}
