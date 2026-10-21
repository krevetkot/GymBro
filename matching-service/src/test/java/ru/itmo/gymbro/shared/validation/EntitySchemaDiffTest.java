package ru.itmo.gymbro.shared.validation;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.gymbro.AbstractIntegrationTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EntitySchemaDiffTest extends AbstractIntegrationTest {

    private static final String ENTITIES_URL = "hibernate:spring:ru.itmo.gymbro"
            + "?dialect=org.hibernate.dialect.PostgreSQLDialect"
            + "&hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy";

    private static final Set<String> IGNORED_FIELDS = Set.of("order", "certainDataType", "defaultValue");

    private static final Map<String, String> TYPE_ALIASES = Map.of(
            "int2", "smallint",
            "int4", "integer",
            "int8", "bigint",
            "bool", "boolean",
            "float8", "double precision",
            "timestamptz", "timestamp with time zone",
            "timestamp with timezone", "timestamp with time zone");

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Таблицы и колонки сущностей совпадают со схемой после миграций Liquibase")
    void entitiesMatchMigratedSchema() throws Exception {
        assertThat(differences()).isEmpty();
    }

    private List<String> differences() throws Exception {
        DiffResult diff = compareEntitiesWithDatabase();
        List<String> differences = new ArrayList<>();
        for (DatabaseObject object : diff.getMissingObjects()) {
            differences.add(describe(object) + ": есть в сущностях, нет в БД");
        }
        for (DatabaseObject object : diff.getUnexpectedObjects()) {
            if (!isLiquibaseTable(object)) {
                differences.add(describe(object) + ": есть в БД, нет в сущностях");
            }
        }
        for (Map.Entry<DatabaseObject, ObjectDifferences> changed : diff.getChangedObjects().entrySet()) {
            for (Difference difference : changed.getValue().getDifferences()) {
                if (isMeaningful(difference)) {
                    differences.add(describe(changed.getKey()) + "." + difference.getField()
                            + ": в сущности " + normalize(difference.getReferenceValue())
                            + ", в БД " + normalize(difference.getComparedValue()));
                }
            }
        }
        return differences;
    }

    private DiffResult compareEntitiesWithDatabase() throws Exception {
        Database entities = DatabaseFactory.getInstance()
                .openDatabase(ENTITIES_URL, null, null, null, new ClassLoaderResourceAccessor());
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            CompareControl control = new CompareControl(new HashSet<>(Set.of(Table.class, Column.class)));
            return DiffGeneratorFactory.getInstance().compare(entities, database, control);
        } finally {
            entities.close();
        }
    }

    private static boolean isMeaningful(Difference difference) {
        if (IGNORED_FIELDS.contains(difference.getField())) {
            return false;
        }
        if ("type".equals(difference.getField())) {
            return !normalize(difference.getReferenceValue()).equals(normalize(difference.getComparedValue()));
        }
        return true;
    }

    private static String normalize(Object type) {
        String normalized = String.valueOf(type).toLowerCase(Locale.ROOT).replace(" byte", "");
        int bracket = normalized.indexOf('(');
        String name = bracket < 0 ? normalized : normalized.substring(0, bracket);
        String size = bracket < 0 ? "" : normalized.substring(bracket);
        name = TYPE_ALIASES.getOrDefault(name.trim(), name.trim());
        return name.startsWith("timestamp") ? name : name + size;
    }

    private static boolean isLiquibaseTable(DatabaseObject object) {
        String table = object instanceof Column column ? column.getRelation().getName() : object.getName();
        return table.toLowerCase(Locale.ROOT).startsWith("databasechangelog");
    }

    private static String describe(DatabaseObject object) {
        return object instanceof Column column
                ? column.getRelation().getName() + "." + column.getName()
                : object.getName();
    }
}
