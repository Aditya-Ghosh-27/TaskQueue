package com.taskqueue.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerApplication {
    public static void main(String[] args) {
        // Eita Tomcat start korbe, kintu code-ta ekhane 'stuck' hoye thakbe na
        SpringApplication.run(WorkerApplication.class, args);
    }
}