package com.example.internmanager.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceStatus {
    UNOPENED("unopened"),
    OPENED("opened"),
    DISABLED("disabled");

    private final String value;

    ResourceStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ResourceStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Unknown resource status: null");
        }

        if ("pending".equalsIgnoreCase(value)) {
            return UNOPENED;
        }

        if ("rejected".equalsIgnoreCase(value)) {
            return DISABLED;
        }

        for (ResourceStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown resource status: " + value);
    }
}
