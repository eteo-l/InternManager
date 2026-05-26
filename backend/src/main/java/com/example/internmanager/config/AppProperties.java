package com.example.internmanager.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Storage storage = new Storage();
    private final Cors cors = new Cors();

    public Cors getCors() {
        return cors;
    }

    public Storage getStorage() {
        return storage;
    }

    public static class Cors {

        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Storage {

        private String dbPath;
        private String legacyCsvPath;

        public String getDbPath() {
            return dbPath;
        }

        public void setDbPath(String dbPath) {
            this.dbPath = dbPath;
        }

        public String getLegacyCsvPath() {
            return legacyCsvPath;
        }

        public void setLegacyCsvPath(String legacyCsvPath) {
            this.legacyCsvPath = legacyCsvPath;
        }
    }
}
