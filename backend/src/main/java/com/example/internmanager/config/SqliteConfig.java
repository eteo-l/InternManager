package com.example.internmanager.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

@Configuration
public class SqliteConfig {

    @Bean
    public DataSource dataSource(AppProperties appProperties) {
        Path dbPath = Paths.get(appProperties.getStorage().getDbPath()).toAbsolutePath().normalize();
        Path parent = dbPath.getParent();

        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to create SQLite storage directory", exception);
            }
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbPath.toString().replace('\\', '/'));
        return dataSource;
    }
}
