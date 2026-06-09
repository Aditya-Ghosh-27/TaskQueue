package com.taskqueue.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.common.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

@Slf4j
@RestController
public class ProducerController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String QUEUE_NAME = "task_queue";

    @PostMapping("/enqueue")
    public ResponseEntity<String> enqueueTask(@RequestBody Task task) {
        // Using a try-with-resources block ensures Jedis is thread-safe
        // and automatically closes the connection when finished.

        // 1. Dynamic Host Resolution for Docker
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        try (Jedis jedis = new Jedis(redisHost, 6379)) {

            String taskJson = objectMapper.writeValueAsString(task);
            jedis.lpush(QUEUE_NAME, taskJson);

            log.info("Successfully pushed task to queue: {}", task.getType());
            return ResponseEntity.ok("Task enqueued successfully: " + task.getType());

        } catch (JsonProcessingException e) {
            log.error("Serialization failed for task: {}", task, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to serialize task");
        } catch (Exception e) {
            log.error("Failed to connect to Redis at {}:6379", redisHost, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Redis connection failed");
        }
    }
}