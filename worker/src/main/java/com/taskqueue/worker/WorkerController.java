package com.taskqueue.worker;

import com.taskqueue.common.Metrics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkerController {

    private final WorkerService workerService;

    // Dependency Injection
    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Metrics> getMetrics() {

        return ResponseEntity.ok(workerService.getMetrics());
    }
}