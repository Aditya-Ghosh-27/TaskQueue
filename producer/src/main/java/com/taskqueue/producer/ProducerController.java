package com.taskqueue.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskqueue.common.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

@RestController
public class ProducerController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String QUEUE_NAME = "work_queue";

    @PostMapping("/enqueue")
    public ResponseEntity<String> enqueueTask(@RequestBody Task task) {
        // Using a try-with-resources block ensures Jedis is thread-safe
        // and automatically closes the connection when finished.
        try (Jedis jedis = new Jedis("localhost", 6379)) {

            String taskJson = objectMapper.writeValueAsString(task);
            jedis.lpush(QUEUE_NAME, taskJson);

            System.out.println("Pushed task to queue: " + task.getType());
            return ResponseEntity.ok("Task enqueued successfully: " + task.getType());

        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to serialize task");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Redis connection failed");
        }
    }
}