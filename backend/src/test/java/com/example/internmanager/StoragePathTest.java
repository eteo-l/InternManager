package com.example.internmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.internmanager.config.AppProperties;
import com.example.internmanager.config.SqliteConfig;
import com.example.internmanager.model.FormStatus;
import com.example.internmanager.model.InternRecord;
import com.example.internmanager.model.ResourceStatus;
import com.example.internmanager.repository.InternRecordRepository;
import com.example.internmanager.service.SensitiveFieldCryptoService;
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

        SensitiveFieldCryptoService crypto = new SensitiveFieldCryptoService();
        crypto.initialize();

        String id = UUID.randomUUID().toString();
        String csv = String.join(
            "\n",
            "id,name,phone,idNumber,grade,gender,emergencyPhone,school,startDate,endDate,department,campus,mentor,note,status,accessStatus,networkStatus,updatedAt",
            String.join(
                ",",
                List.of(
                    id,
                    "Alice",
                    crypto.encrypt("13800138000"),
                    crypto.encrypt("110101200001010000"),
                    "G3",
                    "F",
                    crypto.encrypt("13900139000"),
                    "School A",
                    "2026-05-01",
                    "2026-08-01",
                    "Dept A",
                    "Campus A",
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
            properties,
            crypto
        );

        repository.initialize();

        assertTrue(Files.exists(dbPath));
        assertEquals(1L, repository.count());

        InternRecord record = repository.findById(id).orElseThrow();
        assertEquals("Alice", record.name());
        assertEquals("13800138000", record.phone());
        assertEquals("110101200001010000", record.idNumber());
        assertEquals("13900139000", record.emergencyPhone());
        assertEquals(LocalDate.parse("2026-05-01"), record.startDate());
        assertEquals(LocalDate.parse("2026-08-01"), record.endDate());
        assertEquals("Has note", record.note());
        assertEquals(FormStatus.PENDING, record.status());
        assertEquals(ResourceStatus.OPENED, record.accessStatus());
        assertEquals(ResourceStatus.OPENED, record.networkStatus());
    }

    private static AppProperties appProperties(Path dbPath, Path csvPath) {
        AppProperties properties = new AppProperties();
        properties.getStorage().setDbPath(dbPath.toString());
        properties.getStorage().setLegacyCsvPath(csvPath.toString());
        return properties;
    }
}
