# 🏥 Hospital Core: The Performance Trap Laboratory

This project is a personal sandbox built to help me bridge the gap between being a feature developer and an engineer who actually understands how code behaves under pressure. I intentionally built a "Naive Modular Monolith" filled with common mistakes—missing indexes and N+1 select problems—to see them fail in real-time.

By using observability tools, I stopped guessing why things were slow and started looking at the actual proof.

## 🚀 The Mission
*   **Establish a Baseline:** Measure exactly how bad a naive implementation performs under load.
*   **Observe the Crash:** Use Grafana and JFR dumps to find the "smoking gun" behind bottlenecks.
*   **Fix and Quantify:** Apply senior-level optimizations like B-Tree indexes and batching to measure the real impact.
*   **The Throughput Paradox:** Understand why fixing a DB index can actually make your memory usage look "worse" on paper while throughput explodes.

## 🛠 Tech Stack
*   **Framework:** Spring Boot 4.0 (Spring Framework 7.0)
*   **Database:** PostgreSQL 15
*   **Security:** Keycloak 22 (OAuth2/JWT)
*   **Observability:** Prometheus, Grafana, Postgres Exporter
*   **Profiling:** Java Flight Recorder (JFR)
*   **Load Testing:** Python 3 + Requests

## 📦 Infrastructure Setup

### 1. Launch the Stack
Run this from your root directory to pull the images and build the Java app:
```bash
docker-compose up -d --build
```
The data seeder will take about a minute to inject 100,000 patients and 50,000 appointments. You can check the progress with `docker logs -f hospital-core`.

### 2. Keycloak Configuration
You need to do this manually once to set up the security "traps":
1. Open [http://localhost:8180/auth/admin/](http://localhost:8180/auth/admin/) (admin/admin).
2. Create a Realm named `hospital-realm`.
3. Create a Client named `hospital-app`.
    *   Client Authentication: OFF.
    *   Standard Flow: ON.
    *   Direct Access Grants: ON.
    *   Redirect URIs & Web Origins: `*`.
4. Create a User `testuser`.
    *   Set password to `password123`.
    *   Toggle `Temporary` to OFF.

### 3. Grafana Dashboard Setup
1. Open [http://localhost:3000](http://localhost:3000) (admin/admin).
2. Add a Data Source for **Prometheus**. URL: `http://prometheus:9090`. Save and test.
3. Import Dashboard ID **11378** for the JVM metrics.
4. Import Dashboard ID **9628** for the PostgreSQL metrics.

## 🔬 Performance Testing Scenarios

### Scenario 1: The Naive State (No Index + N+1 Loop)
This is the baseline where everything is broken.
*   **DB Setup:** Drop the index if you added it: `docker exec -it hospital-db psql -U user -d hospital_db -c "DROP INDEX IF EXISTS idx_patient_email;"`
*   **Code Setup:** Use the loop-based `findById` logic in `AppointmentService`.

Run the test:
```bash
python3 load_gen.py
```
Check the autopsy:
```bash
docker exec hospital-core jcmd 1 JFR.start duration=60s filename=/tmp/naive.jfr
docker cp hospital-core:/tmp/naive.jfr ./naive.jfr
```
*You will see search latency spike to 500ms+ and the appointment retrieval basically timeout because it's doing 50,001 queries per request.*

### Scenario 2: The Paradox (Index Added + N+1 Loop)
This is where I realized that making the DB faster exposes the application's memory issues.
*   **Code Setup:** Add the `@Index` on the email column in `Patient.java`. Keep the N+1 loop active.
*   **Rebuild:** `docker-compose up -d --build hospital-core`

Run the test and observe IntelliJ Profiler:
*Memory allocations will jump from 3GB to 30GB+ because the system is finally fast enough to create millions of short-lived Hibernate objects in a loop. The bottleneck moved from the DB to the JVM Heap.*

### Scenario 3: Engineered State (Index + Batch Fetching)
The "Correct" way to do things while keeping domains separated.
*   **Code Setup:** Refactor `AppointmentService` to use a `Map` based batch fetch.
*   **Rebuild:** `docker-compose up -d --build hospital-core`

*The errors drop to zero. Throughput increases by 6,000%+. Latency falls below 200ms. The system is finally efficient.*

## 🔍 Tools for Investigation

### JVM Thread Dump
To see exactly where the threads are stuck waiting for the DB:
```bash
docker exec hospital-core jcmd 1 Thread.print > thread_dump.txt
```

### PostgreSQL X-Ray
To prove the database is doing a Sequential Scan instead of using your index:
```bash
docker exec -it hospital-db psql -U user -d hospital_db -c "EXPLAIN ANALYZE SELECT * FROM patients WHERE email = 'test@hospital.com';"
```

### Reset Data
If you want to wipe the 150k rows and start fresh:
```bash
docker-compose down -v
docker-compose up -d
```

## 📈 Learning Outcomes
*   **The Throughput Paradox:** I discovered that high memory allocation isn't always a leak; sometimes it's just proof that your code is finally running at full speed.
*   **Pool Starvation:** I watched the HikariPool hit 10 connections and die because one slow endpoint (N+1) stole all the resources from the fast ones.
*   **Security Short-Circuits:** I learned that load tests can "fake" a healthy system if your tokens expire—401 errors use zero CPU, making the charts look green when the system is actually useless.