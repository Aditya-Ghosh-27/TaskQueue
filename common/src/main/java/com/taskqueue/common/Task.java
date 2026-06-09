package com.taskqueue.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @NotBlank(message = "Task type cannot be empty")
    @JsonProperty("type")
    private String type;

    @NotNull(message = "Payload cannot be null")
    @JsonProperty("payload")
    private Map<String, Object> payload;

    @Min(value = 0, message = "Retries cannot be negative")
    @JsonProperty("retries")
    private int retries;
}