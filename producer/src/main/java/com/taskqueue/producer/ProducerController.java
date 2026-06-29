package com.taskqueue.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.common.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.List;

@Slf4j
@RestController
public class ProducerController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String QUEUE_NAME = "task_queue";
    private static final String DLQ_NAME = "dead_letter_queue";

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

    // Management endpoint to view the DLQ
    @GetMapping("/dlq")
    public ResponseEntity<List<String>> viewDeadLetterQueue() {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");

        try (Jedis jedis  = new Jedis(redisHost, 6379)) {
            List<String> deadLetters = jedis.lrange(DLQ_NAME, 0, -1);

            log.info("Fetched {} poisoned tasks from the Dead Letter Queue", deadLetters.size());
            return ResponseEntity.ok(deadLetters);
        } catch (Exception e) {
            log.error("Failed to connect to Redis to fetch DLQ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    // Management endpoint to replay the Dead Letter Queue
    @PostMapping("/dlq/replay")
    public ResponseEntity<String> replayDeadLetterQueue() {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int replayedCount = 0;

        try (Jedis jedis = new Jedis(redisHost, 6379)) {
            // 1. Fetch all poisoned tasks
            List<String> deadLetters = jedis.lrange(DLQ_NAME, 0, -1);

            if (deadLetters.isEmpty()) {
                return ResponseEntity.ok("DLQ is empty. Nothing to replay!");
            }

            // 2. Loop through them, reset retries, and push back to the main queue
            for (String taskJson : deadLetters) {
                Task task = objectMapper.readValue(taskJson, Task.class);
                task.setRetries(3); // Reset the safety net

                String resetTaskJson = objectMapper.writeValueAsString(task);
                jedis.lpush(QUEUE_NAME, resetTaskJson);
                replayedCount++;
            }

            // 3. Clear the DLQ since we moved everything back
            jedis.del(DLQ_NAME);

            log.info("Successfully replayed {} tasks from the DLQ", replayedCount);
            return ResponseEntity.ok("Replayed " + replayedCount + " tasks back to the main queue.");
        } catch (Exception e) {
            log.error("Failed to replay DLQ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to replay tasks");
        }
    }
}