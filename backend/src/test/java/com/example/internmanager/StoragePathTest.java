package com.example.internmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.internmanager.config.AppProperties;
import com.example.internmanager.config.SqliteConfig;
import com.example.internmanager.model.EmploymentStatus;
import com.example.internmanager.model.FormStatus;
import com.example.internmanager.model.InternRecord;
import com.example.internmanager.model.ResourceStatus;
import com.example.internmanager.repository.InternRecordRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class StoragePathTest {

    @TempDir
    Path tempDir;

    @Test
    void sqliteDataSourceWorksForSpecialCharacterPaths() throws Exception {
        Path dbPath = tempDir.resolve("db #dir").resolve("intern manager's.db");
        AppProperties properties = appProperties(dbPath, tempDir.resolve("legacy.csv"));

        DataSource dataSource = new SqliteConfig().dataSource(properties);

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:sqlite:"));
        }

        assertTrue(Files.exists(dbPath.getParent()));
        assertTrue(Files.exists(dbPath));
    }

    @Test
    void repositoryImportsLegacyCsvFromSpecialCharacterPaths() throws Exception {
        Path dbPath = tempDir.resolve("db #dir").resolve("intern manager's.db");
        Path csvPath = tempDir.resolve("legacy data & import").resolve("intern-records.csv");
        Files.createDirectories(csvPath.getParent());

        String id = UUID.randomUUID().toString();
        String csv = String.join(
            "\n",
            "id,name,grade,gender,school,startDate,endDate,department,campus,employmentStatus,taskTracking,mentor,note,status,accessStatus,networkStatus,updatedAt",
            String.join(
                ",",
                List.of(
                    id,
                    "Alice",
                    "G3",
                    "F",
                    "School A",
                    "2026-05-01",
                    "2026-08-01",
                    "Dept A",
                    "Campus A",
                    "left",
                    "Kickoff done",
                    "Mentor A",
                    "Has note",
                    "pending",
                    "opened",
                    "opened",
                    Instant.parse("2026-05-26T00:00:00Z").toString()
                )
            ),
            ""
        );
        Files.writeString(csvPath, csv, StandardCharsets.UTF_8);

        AppProperties properties = appProperties(dbPath, csvPath);
        InternRecordRepository repository = new InternRecordRepository(
            new JdbcTemplate(new SqliteConfig().dataSource(properties)),
            properties
        );

        repository.initialize();

        assertTrue(Files.exists(dbPath));
        assertEquals(1L, repository.count());

        InternRecord record = repository.findById(id).orElseThrow();
        assertEquals("Alice", record.name());
        assertEquals("G3", record.grade());
        assertEquals("F", record.gender());
        assertEquals("School A", record.school());
        assertEquals(LocalDate.parse("2026-05-01"), record.startDate());
        assertEquals(LocalDate.parse("2026-08-01"), record.endDate());
        assertEquals(EmploymentStatus.LEFT, record.employmentStatus());
        assertEquals("Kickoff done", record.taskTracking());
        assertEquals("Has note", record.note());
        assertEquals(FormStatus.PENDING, record.status());
        assertEquals(ResourceStatus.OPENED, record.accessStatus());
        assertEquals(ResourceStatus.OPENED, record.networkStatus());
    }

    @Test
    void repositoryMigratesExistingDatabaseWithoutEmploymentStatusColumn() throws Exception {
        Path dbPath = tempDir.resolve("db #dir").resolve("intern manager's.db");
        AppProperties properties = appProperties(dbPath, tempDir.resolve("legacy.csv"));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SqliteConfig().dataSource(properties));
        jdbcTemplate.execute("""
            CREATE TABLE intern_records (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                grade TEXT NOT NULL,
                gender TEXT NOT NULL,
                school TEXT NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT NOT NULL,
                department TEXT NOT NULL,
                campus TEXT NOT NULL,
                mentor TEXT NOT NULL,
                note TEXT,
                status TEXT NOT NULL,
                access_status TEXT NOT NULL,
                network_status TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """);
        jdbcTemplate.update("""
            INSERT INTO intern_records (
                id, name, grade, gender, school, start_date, end_date,
                department, campus, mentor, note, status, access_status,
                network_status, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            "legacy-1",
            "Alice",
            "G3",
            "F",
            "School A",
            "2026-05-01",
            "2026-08-01",
            "Dept A",
            "Campus A",
            "Mentor A",
            "Note",
            "pending",
            "opened",
            "opened",
            Instant.parse("2026-05-26T00:00:00Z").toEpochMilli()
        );

        InternRecordRepository repository = new InternRecordRepository(jdbcTemplate, properties);
        repository.initialize();

        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(intern_records)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );

        assertEquals(1L, repository.count());
        assertTrue(columns.contains("employment_status"));
        assertTrue(columns.contains("task_tracking"));
        assertFalse(columns.contains("phone"));
        assertFalse(columns.contains("id_number"));
        assertFalse(columns.contains("emergency_phone"));

        InternRecord record = repository.findById("legacy-1").orElseThrow();
        assertEquals("Alice", record.name());
        assertEquals("G3", record.grade());
        assertEquals("School A", record.school());
        assertEquals(EmploymentStatus.ACTIVE, record.employmentStatus());
        assertNull(record.taskTracking());
    }

    @Test
    void repositoryMigratesExistingDatabaseWithoutTaskTrackingColumn() throws Exception {
        Path dbPath = tempDir.resolve("db #dir").resolve("intern manager's.db");
        AppProperties properties = appProperties(dbPath, tempDir.resolve("legacy.csv"));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SqliteConfig().dataSource(properties));
        jdbcTemplate.execute("""
            CREATE TABLE intern_records (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                grade TEXT NOT NULL,
                gender TEXT NOT NULL,
                school TEXT NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT NOT NULL,
                department TEXT NOT NULL,
                campus TEXT NOT NULL,
                employment_status TEXT NOT NULL,
                mentor TEXT NOT NULL,
                note TEXT,
                status TEXT NOT NULL,
                access_status TEXT NOT NULL,
                network_status TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """);
        jdbcTemplate.update("""
            INSERT INTO intern_records (
                id, name, grade, gender, school, start_date, end_date,
                department, campus, employment_status, mentor, note, status,
                access_status, network_status, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            "legacy-2",
            "Bob",
            "G2",
            "M",
            "School B",
            "2026-06-01",
            "2026-08-31",
            "Dept B",
            "Campus B",
            "left",
            "Mentor B",
            "Note B",
            "approved",
            "opened",
            "disabled",
            Instant.parse("2026-05-27T00:00:00Z").toEpochMilli()
        );

        InternRecordRepository repository = new InternRecordRepository(jdbcTemplate, properties);
        repository.initialize();

        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(intern_records)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );

        assertTrue(columns.contains("employment_status"));
        assertTrue(columns.contains("task_tracking"));

        InternRecord record = repository.findById("legacy-2").orElseThrow();
        assertEquals(EmploymentStatus.LEFT, record.employmentStatus());
        assertNull(record.taskTracking());
        assertEquals(ResourceStatus.DISABLED, record.networkStatus());
    }

    private static AppProperties appProperties(Path dbPath, Path csvPath) {
        AppProperties properties = new AppProperties();
        properties.getStorage().setDbPath(dbPath.toString());
        properties.getStorage().setLegacyCsvPath(csvPath.toString());
        return properties;
    }
}
