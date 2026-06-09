package com.taskqueue.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.common.Metrics;
import com.taskqueue.common.Task;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkerService {

    private final Jedis jedis = new Jedis("localhost", 6379);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String QUEUE_NAME = "work_queue";

    private final AtomicInteger jobsDone = new AtomicInteger(0);
    private final AtomicInteger jobsFailed = new AtomicInteger(0);

    @PostConstruct
    public void startWorker() {
        // Run in a separate thread so it doesn't block the application startup
        new Thread(this::runLoop).start();
    }

    private void runLoop() {
        // try-with-resources use korle connection auto-close hoy, crash kore na
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            System.out.println("Worker thread started using BRPOP. Waiting for tasks...");
            while (true) {
                try {
                    List<String> tasks = jedis.brpop(0, QUEUE_NAME);
                    if (tasks != null && tasks.size() == 2) {
                        String taskJson = tasks.get(1);
                        Task task = objectMapper.readValue(taskJson, Task.class);

                        processTask(task);
                        jobsDone.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error processing task: " + e.getMessage());
                    jobsFailed.incrementAndGet();
                }
            }
        }
    }

    private void processTask(Task task) {
        System.out.println("Processing task: " + task.getType());
        // Author-er switch-case logic (Better for variety)
        switch (task.getType()) {
            case "send_email":
                System.out.println("Sending email to " + task.getPayload().get("to"));
                break;
            case "resize_image":
                System.out.println("Resizing image...");
                break;
            default:
                System.out.println("Unsupported task: " + task.getType());
        }
    }

    public Metrics getMetrics() {
        long totalJobsInQueue = jedis.llen(QUEUE_NAME);
        return new Metrics(totalJobsInQueue, jobsDone.get(), jobsFailed.get());
    }
}