package com.taskqueue.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Metrics {

    @JsonProperty("total_jobs_in_queue")
    private long totalJobsInQueue;

    @JsonProperty("jobs_done")
    private int jobsDone;

    @JsonProperty("jobs_failed")
    private int jobsFailed;
}