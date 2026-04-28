# TaskQueue

A robust, asynchronous distributed job queue system built with **Spring Boot**, **Redis**, and **Maven**. 
This project demonstrates the decoupling of **task production and consumption** using a **Producer-Consumer architecture**.

## 🚀 High Level Overview

![TaskQueue](TaskQueue.png)

The system consists of three main components:

* **Producer (Port 8080):** A Spring Boot REST API that receives tasks and pushes them into a Redis queue.
* **Redis (Port 6379):** Acts as the message broker/orchestrator, storing the task queue.
* **Worker (Port 8081):** A background processor that pulls tasks from Redis using the `BRPOP`(Blocking Pop) strategy and executes them. It also provides a monitoring endpoint.

## 🛠 Tech Stack

* **Java 21**
* **Spring Boot 3.x**
* **Redis** (via Docker)
* **Jedis** (Redis Client)
* **Maven** (Multi-module structure)
* **Lombok**

## 🏗 Project Structure

* `common/`: Contains shared models like `Task` and `Metrics`.
* `producer/`: The API gateway for enqueuing jobs.
* `worker/`: The consumer that processes jobs and serves monitoring data.

## ⚙️ Key Features

* **Efficient Task Pulling:** Uses `BRPOP` to eliminate CPU wastage by waiting for tasks instead of constant polling.
* **Asynchronous Processing:** Worker processes tasks in a dedicated background thread, keeping the web server responsive.
* **Monitoring Dashboard:** A `/metrics` endpoint on the Worker to track live queue size, completed jobs, and failures.
* **Decoupled Architecture:** Producer and Worker can be scaled independently.

## 🚦 How to Run the Project

### 1. Start Redis
Ensure you have Docker installed and run:
```bash

docker run --name redis -p 6379:6379 -d redis
```

### 2. Build the project

From the root directory run
```bash

mvn clean install
```

### 3. Start the Producer

Run the ProducerApplication from the producer module. It will start on PORT 8080.

### 4. Start the Worker

Run the WorkerApplication from the worker module. It will start on PORT 8081.

### 5. Send Tasks

Use curl command to send tasks from the terminal

```bash

for i in {1..5}; do
  curl -s -X POST http://$(hostname).local:8080/enqueue \
  -H "Content-Type: application/json" \
  -d "{\"type\": \"send_email\", \"retries\": 3, \"payload\": {\"to\": \"user$i@example.com\", \"subject\": \"Load Test $i\"}}"
  echo "Task $i Sent!"
done
```

### 6. Monitor Progress

Open your browser and visit: http://localhost:8081/metrics

### 📊 Metrics Explained

- total_jobs_in_queue: Number of tasks currently waiting in Redis.
- jobs_done: Successfully processed tasks.
- jobs_failed: Tasks that encountered errors during execution.

### ➕ Additional Features

**Efficient Resource Management (Blocking I/O)**:

The project implements the BRPOP (Blocking Pop) command instead of traditional
polling. This means the Worker thread "sleeps" and consumes zero CPU cycles while the
queue is empty, instantly waking up only when a new task arrives.

**Multi-Threaded Monitoring (Non-Blocking)**:

By using a dedicated background thread for task processing, the Worker's Tomcat server
(Port 8081) remains free and responsive. You can check your /metrics
dashboard even while the Worker is busy processing a long-running task.

Created by Aditya