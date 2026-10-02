package ru.itmo.gymbro.shared.validation;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "spring.jpa.hibernate.ddl-auto", havingValue = "validate")
class ColumnLengthValidator implements InitializingBean {

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    ColumnLengthValidator(EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        List<String> mismatches = mismatchesFor(expectedColumns());
        if (!mismatches.isEmpty()) {
            throw new IllegalStateException("Длины колонок в БД не совпадают с сущностями:\n  - "
                    + String.join("\n  - ", mismatches));
        }
    }

    List<ExpectedColumn> expectedColumns() {
        List<ExpectedColumn> columns = new ArrayList<>();
        entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getMappingMetamodel()
                .forEachEntityDescriptor(entity -> entity.forEachSelectable((index, selectable) -> {
                    Long length = selectable.getLength();
                    Class<?> javaType = selectable.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass();
                    if (length != null && javaType == String.class) {
                        columns.add(new ExpectedColumn(
                                selectable.getContainingTableExpression(),
                                selectable.getSelectionExpression(),
                                length));
                    }
                }));
        return columns;
    }

    List<String> mismatchesFor(Collection<ExpectedColumn> expected) throws SQLException {
        List<String> mismatches = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String schema = connection.getSchema();
            for (ExpectedColumn column : expected) {
                try (ResultSet rows = metaData.getColumns(null, schema, column.table, column.name)) {
                    if (!rows.next() || !"varchar".equals(rows.getString("TYPE_NAME").toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    int actualLength = rows.getInt("COLUMN_SIZE");
                    if (actualLength != column.length) {
                        mismatches.add(column.table + "." + column.name + ": в сущности длина " + column.length
                                + ", в БД varchar(" + actualLength + ")");
                    }
                }
            }
        }
        return mismatches;
    }

    static final class ExpectedColumn {

        private final String table;
        private final String name;
        private final long length;

        ExpectedColumn(String table, String name, long length) {
            this.table = table;
            this.name = name;
            this.length = length;
        }

        String getTable() {
            return table;
        }

        String getName() {
            return name;
        }

        long getLength() {
            return length;
        }
    }
}
