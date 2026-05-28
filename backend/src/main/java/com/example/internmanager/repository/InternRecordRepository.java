package com.example.internmanager.repository;

import com.example.internmanager.config.AppProperties;
import com.example.internmanager.model.EmploymentStatus;
import com.example.internmanager.model.FormStatus;
import com.example.internmanager.model.InternRecord;
import com.example.internmanager.model.ResourceStatus;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InternRecordRepository {

    private static final String BOOTSTRAP_KEY = "storage_bootstrapped";
    private static final String LEGACY_TABLE_NAME = "intern_records_legacy_migration";
    private static final Set<String> REMOVED_COLUMNS = Set.of("phone", "id_number", "emergency_phone");
    private static final String DEFAULT_EMPLOYMENT_STATUS = EmploymentStatus.ACTIVE.getValue();

    private final JdbcTemplate jdbcTemplate;
    private final AppProperties appProperties;

    public InternRecordRepository(JdbcTemplate jdbcTemplate, AppProperties appProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void initialize() {
        createSchema();
        migrateLegacySchemaIfNeeded();
        createIndexes();
        bootstrapIfEmpty();
    }

    public synchronized List<InternRecord> findAll() {
        return jdbcTemplate.query("""
            SELECT id, name, grade, gender, school,
                   start_date, end_date, department, campus, employment_status, task_tracking, mentor, note,
                   status, access_status, network_status, updated_at
            FROM intern_records
            ORDER BY CASE WHEN employment_status = 'left' THEN 1 ELSE 0 END,
                     updated_at DESC, id DESC
            """, this::mapRow);
    }

    public synchronized Optional<InternRecord> findById(String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                SELECT id, name, grade, gender, school,
                       start_date, end_date, department, campus, employment_status, task_tracking, mentor, note,
                       status, access_status, network_status, updated_at
                FROM intern_records
                WHERE id = ?
                """, this::mapRow, id));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public synchronized void save(InternRecord record) {
        jdbcTemplate.update("""
            INSERT INTO intern_records (
                id, name, grade, gender, school,
                start_date, end_date, department, campus, employment_status, task_tracking, mentor, note,
                status, access_status, network_status, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                grade = excluded.grade,
                gender = excluded.gender,
                school = excluded.school,
                start_date = excluded.start_date,
                end_date = excluded.end_date,
                department = excluded.department,
                campus = excluded.campus,
                employment_status = excluded.employment_status,
                task_tracking = excluded.task_tracking,
                mentor = excluded.mentor,
                note = excluded.note,
                status = excluded.status,
                access_status = excluded.access_status,
                network_status = excluded.network_status,
                updated_at = excluded.updated_at
            """, (java.sql.PreparedStatement preparedStatement) -> bindRecord(preparedStatement, record));
    }

    public synchronized boolean deleteById(String id) {
        return jdbcTemplate.update("DELETE FROM intern_records WHERE id = ?", id) > 0;
    }

    public synchronized void deleteAll() {
        jdbcTemplate.update("DELETE FROM intern_records");
    }

    public synchronized long count() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intern_records", Long.class);
        return result == null ? 0L : result;
    }

    private void createSchema() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS intern_records (
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
                task_tracking TEXT,
                mentor TEXT NOT NULL,
                note TEXT,
                status TEXT NOT NULL,
                access_status TEXT NOT NULL,
                network_status TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS app_metadata (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """);
    }

    private void migrateLegacySchemaIfNeeded() {
        List<String> columns = jdbcTemplate.query(
            "PRAGMA table_info(intern_records)",
            (resultSet, rowNum) -> resultSet.getString("name")
        );

        boolean hasRemovedColumns = columns.stream().anyMatch(REMOVED_COLUMNS::contains);
        boolean hasEmploymentStatus = columns.contains("employment_status");
        boolean hasTaskTracking = columns.contains("task_tracking");

        if (!hasRemovedColumns && hasEmploymentStatus && hasTaskTracking) {
            return;
        }

        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_intern_records_updated_at");
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + LEGACY_TABLE_NAME);
        jdbcTemplate.execute("ALTER TABLE intern_records RENAME TO " + LEGACY_TABLE_NAME);

        createSchema();

        String employmentStatusExpression = hasEmploymentStatus
            ? "employment_status"
            : "'" + DEFAULT_EMPLOYMENT_STATUS + "'";
        String taskTrackingExpression = hasTaskTracking
            ? "task_tracking"
            : "NULL";

        jdbcTemplate.update("""
            INSERT INTO intern_records (
                id, name, grade, gender, school,
                start_date, end_date, department, campus, employment_status, task_tracking, mentor, note,
                status, access_status, network_status, updated_at
            )
            SELECT
                id, name, grade, gender, school,
                start_date, end_date, department, campus, %s, %s, mentor, note,
                status, access_status, network_status, updated_at
            FROM intern_records_legacy_migration
            """.formatted(employmentStatusExpression, taskTrackingExpression));
        jdbcTemplate.execute("DROP TABLE " + LEGACY_TABLE_NAME);
    }

    private void createIndexes() {
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_intern_records_updated_at
            ON intern_records(updated_at DESC, id DESC)
            """);
    }

    private void bootstrapIfEmpty() {
        if (count() > 0) {
            return;
        }

        if (isBootstrapComplete()) {
            return;
        }

        if (!importLegacyCsv()) {
            seedDemoRecords();
        }

        markBootstrapComplete();
    }

    private boolean importLegacyCsv() {
        Path csvPath = resolveLegacyCsvPath();

        if (Files.notExists(csvPath)) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            if (lines.size() <= 1) {
                return false;
            }

            int imported = 0;
            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);

                if (line == null || line.isBlank()) {
                    continue;
                }

                List<String> cells = parseCsvLine(line);

                try {
                    InternRecord record = mapCsvRow(cells);
                    if (record == null) {
                        continue;
                    }

                    save(record);
                    imported++;
                } catch (RuntimeException exception) {
                    // Skip malformed legacy rows and keep bootstrapping.
                }
            }

            return imported > 0;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to import legacy CSV storage", exception);
        }
    }

    private InternRecord mapCsvRow(List<String> cells) {
        if (cells.size() >= 18) {
            return new InternRecord(
                cells.get(0),
                cells.get(1),
                cells.get(4),
                cells.get(5),
                cells.get(7),
                LocalDate.parse(cells.get(8)),
                LocalDate.parse(cells.get(9)),
                cells.get(10),
                cells.get(11),
                EmploymentStatus.ACTIVE,
                null,
                cells.get(12),
                emptyToNull(cells.get(13)),
                FormStatus.fromValue(cells.get(14)),
                ResourceStatus.fromValue(cells.get(15)),
                ResourceStatus.fromValue(cells.get(16)),
                Instant.parse(cells.get(17))
            );
        }

        if (cells.size() >= 17) {
            return new InternRecord(
                cells.get(0),
                cells.get(1),
                cells.get(2),
                cells.get(3),
                cells.get(4),
                LocalDate.parse(cells.get(5)),
                LocalDate.parse(cells.get(6)),
                cells.get(7),
                cells.get(8),
                EmploymentStatus.fromValue(cells.get(9)),
                emptyToNull(cells.get(10)),
                cells.get(11),
                emptyToNull(cells.get(12)),
                FormStatus.fromValue(cells.get(13)),
                ResourceStatus.fromValue(cells.get(14)),
                ResourceStatus.fromValue(cells.get(15)),
                Instant.parse(cells.get(16))
            );
        }

        if (cells.size() >= 16) {
            return new InternRecord(
                cells.get(0),
                cells.get(1),
                cells.get(2),
                cells.get(3),
                cells.get(4),
                LocalDate.parse(cells.get(5)),
                LocalDate.parse(cells.get(6)),
                cells.get(7),
                cells.get(8),
                EmploymentStatus.fromValue(cells.get(9)),
                null,
                cells.get(10),
                emptyToNull(cells.get(11)),
                FormStatus.fromValue(cells.get(12)),
                ResourceStatus.fromValue(cells.get(13)),
                ResourceStatus.fromValue(cells.get(14)),
                Instant.parse(cells.get(15))
            );
        }

        if (cells.size() >= 15) {
            return new InternRecord(
                cells.get(0),
                cells.get(1),
                cells.get(2),
                cells.get(3),
                cells.get(4),
                LocalDate.parse(cells.get(5)),
                LocalDate.parse(cells.get(6)),
                cells.get(7),
                cells.get(8),
                EmploymentStatus.ACTIVE,
                null,
                cells.get(9),
                emptyToNull(cells.get(10)),
                FormStatus.fromValue(cells.get(11)),
                ResourceStatus.fromValue(cells.get(12)),
                ResourceStatus.fromValue(cells.get(13)),
                Instant.parse(cells.get(14))
            );
        }

        return null;
    }

    private void seedDemoRecords() {
        Instant now = Instant.now();

        save(new InternRecord(
            UUID.randomUUID().toString(),
            "林若溪",
            "大三",
            "女",
            "上海交通大学",
            LocalDate.parse("2026-06-01"),
            LocalDate.parse("2026-09-30"),
            "极光实验室",
            "中关村壹号",
            EmploymentStatus.ACTIVE,
            "本周确认入场时间，等待门禁开通。",
            "陈明",
            "下周一到岗，需要申请门禁。",
            FormStatus.PENDING,
            ResourceStatus.OPENED,
            ResourceStatus.UNOPENED,
            now
        ));

        save(new InternRecord(
            UUID.randomUUID().toString(),
            "周启航",
            "研一",
            "男",
            "浙江大学",
            LocalDate.parse("2026-05-20"),
            LocalDate.parse("2026-08-20"),
            "数据平台部",
            "环保园",
            EmploymentStatus.ACTIVE,
            "已完成设备登记，准备安排首次周会任务。",
            "王静",
            "已完成设备登记。",
            FormStatus.APPROVED,
            ResourceStatus.OPENED,
            ResourceStatus.OPENED,
            now.minusSeconds(3600)
        ));
    }

    private InternRecord mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new InternRecord(
            resultSet.getString("id"),
            resultSet.getString("name"),
            resultSet.getString("grade"),
            resultSet.getString("gender"),
            resultSet.getString("school"),
            LocalDate.parse(resultSet.getString("start_date")),
            LocalDate.parse(resultSet.getString("end_date")),
            resultSet.getString("department"),
            resultSet.getString("campus"),
            EmploymentStatus.fromValue(resultSet.getString("employment_status")),
            emptyToNull(resultSet.getString("task_tracking")),
            resultSet.getString("mentor"),
            emptyToNull(resultSet.getString("note")),
            FormStatus.fromValue(resultSet.getString("status")),
            ResourceStatus.fromValue(resultSet.getString("access_status")),
            ResourceStatus.fromValue(resultSet.getString("network_status")),
            Instant.ofEpochMilli(resultSet.getLong("updated_at"))
        );
    }

    private void bindRecord(java.sql.PreparedStatement preparedStatement, InternRecord record) throws SQLException {
        int index = 1;
        preparedStatement.setString(index++, record.id());
        preparedStatement.setString(index++, record.name());
        preparedStatement.setString(index++, record.grade());
        preparedStatement.setString(index++, record.gender());
        preparedStatement.setString(index++, record.school());
        preparedStatement.setString(index++, record.startDate().toString());
        preparedStatement.setString(index++, record.endDate().toString());
        preparedStatement.setString(index++, record.department());
        preparedStatement.setString(index++, record.campus());
        preparedStatement.setString(index++, record.employmentStatus().getValue());
        preparedStatement.setString(index++, record.taskTracking());
        preparedStatement.setString(index++, record.mentor());
        preparedStatement.setString(index++, record.note());
        preparedStatement.setString(index++, record.status().getValue());
        preparedStatement.setString(index++, record.accessStatus().getValue());
        preparedStatement.setString(index++, record.networkStatus().getValue());
        preparedStatement.setLong(index, record.updatedAt().toEpochMilli());
    }

    private Path resolveLegacyCsvPath() {
        return Paths.get(appProperties.getStorage().getLegacyCsvPath()).toAbsolutePath().normalize();
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);

            if (inQuotes) {
                if (ch == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        result.add(current.toString());
        return result;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isBootstrapComplete() {
        try {
            String value = jdbcTemplate.queryForObject(
                "SELECT value FROM app_metadata WHERE key = ?",
                String.class,
                BOOTSTRAP_KEY
            );
            return Boolean.parseBoolean(value);
        } catch (EmptyResultDataAccessException exception) {
            return false;
        }
    }

    private void markBootstrapComplete() {
        jdbcTemplate.update("""
            INSERT INTO app_metadata (key, value)
            VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """, BOOTSTRAP_KEY, Boolean.TRUE.toString());
    }
}
