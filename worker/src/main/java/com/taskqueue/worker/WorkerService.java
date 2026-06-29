package com.taskqueue.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.common.Metrics;
import com.taskqueue.common.Task;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class WorkerService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String QUEUE_NAME = "task_queue";

    private final AtomicInteger jobsDone = new AtomicInteger(0);
    private final AtomicInteger jobsFailed = new AtomicInteger(0);

    @PostConstruct
    public void startWorker() {
        // Run in a separate thread so it doesn't block the application startup
        new Thread(this::runLoop).start();
    }

    private void runLoop() {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");

        // try-with-resources use korle connection auto-close hoy, crash kore na
        try (Jedis jedis = new Jedis(redisHost, 6379)) {
            log.info("Worker thread started using BRPOP. Waiting for tasks on host: {}", redisHost);

            while (true) {
                try {
                    List<String> tasks = jedis.brpop(0, QUEUE_NAME);
                    if (tasks != null && tasks.size() == 2) {
                        String taskJson = tasks.get(1);
                        Task task = objectMapper.readValue(taskJson, Task.class);

                        try {
                            // 1. Attempt to process the task
                            processTask(task);
                            jobsDone.incrementAndGet();
                        } catch (Exception e) {
                            // 2. If processing crashes, route to Retry or DLQ
                            log.error("Task failed: {}", task.getType(), e);
                            handleFailedTask(jedis, task, taskJson);
                            jobsFailed.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    log.error("Critical error connecting to Redis or parsing JSON: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Fatal error connecting to Redis in Worker thread", e);
        }
    }

    private void handleFailedTask(Jedis jedis, Task task, String originalJson) {
        try {
            int retriesLeft = task.getRetries();

            if(retriesLeft > 0) {
                // Decrement retries and push back to the main queue
                task.setRetries(retriesLeft - 1);
                String updatedTaskJson = objectMapper.writeValueAsString(task);

                log.warn("Retrying task: {}. Retries left: {}", task.getType(), task.getRetries());
                jedis.lpush(QUEUE_NAME, updatedTaskJson);
            } else {
                log.error("Task exhausted all retries. Moving to DLQ: {}", task.getType());
                jedis.lpush("dead_letter_queue", originalJson);
            }
        } catch (Exception e) {
            log.error("Failed to route task to DLQ", e);
        }
    }

    private void processTask(Task task) {
        log.info("Processing task: {}", task.getType());
        // Author-er switch-case logic (Better for variety)
        switch (task.getType()) {
            case "send_email":
                // Extract the email address as a String
                String emailTo = String.valueOf(task.getPayload().get("to"));
                log.info("Sending email to {}", emailTo);

                // If the email address contains the word "fail", simulate a crash!
//                if(emailTo.contains("fail")) {
//                    throw new RuntimeException("503 Service Unavailable. API Connection Dropped for: " + emailTo);
//                }

                log.info("SUCESS: Email successfully sent to {}", emailTo);
                break;
            case "resize_image":
                log.info("Resizing image...");
                break;
            default:
                log.warn("Unsupported task: " + task.getType());
        }
    }

    public Metrics getMetrics() {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");

        try (Jedis jedis = new Jedis(redisHost, 6379)) {
            long totalJobsInQueue = jedis.llen(QUEUE_NAME);
            return new Metrics(totalJobsInQueue, jobsDone.get(), jobsFailed.get());
        } catch (Exception e) {
            log.error("Failed to fetch queue metrics from Redis", e);
            return new Metrics(0, jobsDone.get(), jobsFailed.get());
        }
    }
}