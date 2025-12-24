# 🏥 Hospital Core: Performance Lab & Modular Monolith

This project is a **Learning Sandbox** designed to shift from a "feature developer" to a **"systems engineer"** mindset. Instead of just writing code, this laboratory focuses on intentionally engineering architectural flaws—specifically **Missing Indexes** and **N+1 Selects**—to observe their impact using professional observability tools.

## 🚀 The Mission
1. **Establish a Baseline:** Measure a "Naive" implementation.
2. **Observe Failure:** Use Grafana, Prometheus, and JFR to find the "mathematical proof" of bottlenecks.
3. **Targeted Optimization:** Apply B-Tree indexes and Batch Processing.
4. **Quantify Impact:** Analyze the **Throughput Paradox** (why faster code can cause 5,700% more memory churn).

---

## 🛠 Tech Stack
- **Framework:** Spring Boot 4.0 (Spring Framework 7.0)
- **Database:** PostgreSQL 15
- **Identity:** Keycloak 22 (OAuth2/JWT)
- **Observability:** Prometheus, Grafana, Postgres Exporter
- **Profiling:** Java Flight Recorder (JFR)
- **Load Generation:** Python 3 + Requests

---

## 📦 Infrastructure Setup

### 1. Launch the Stack
Ensure you have Docker and Docker Compose installed. From the project root, run:
```bash
docker-compose up -d --build
```
*Wait ~1 minute for the data seeder to insert 100,000 patients and 50,000 appointments.*

### 2. Keycloak Configuration (Manual Step)
1. Access UI: [http://localhost:8180/auth/admin/](http://localhost:8180/auth/admin/) (admin/admin).
2. **Create Realm:** Name it `hospital-realm`.
3. **Create Client:**
    - ID: `hospital-app`.
    - Client Authentication: `OFF`.
    - Authentication Flow: Standard Flow `ON`, Direct Access Grants `ON`.
    - Valid Redirect URIs: `*`.
    - Web Origins: `*`.
4. **Create User:**
    - Username: `testuser`.
    - Credentials: Set password to `password123`.
    - **Important:** Toggle `Temporary` to `OFF`.

### 3. Grafana Dashboard Setup
1. Access UI: [http://localhost:3000](http://localhost:3000) (admin/admin).
2. **Add Data Source:** Select **Prometheus**. URL: `http://prometheus:9090`. Click **Save & Test**.
3. **Import Dashboards:**
    - Click `+` -> `Import`.
    - Enter ID **`11378`** (JVM Micrometer).
    - Enter ID **`9628`** (PostgreSQL Database).

---

## 🔬 Performance Testing Scenarios

### 🧪 Scenario 1: The "Naive" State (No Index + N+1 Loop)
**Setup:**
- **DB:** Ensure index is deleted:
  `docker exec -it hospital-db psql -U user -d hospital_db -c "DROP INDEX IF EXISTS idx_patient_email;"`
- **Code:** Ensure `AppointmentService` uses the loop-based `findById` logic (comment out batch logic).

**Run Load Test:**
```bash
python3 load_gen.py
```

**Capture Diagnostic:**
```bash
# Capture a 60-second JFR trace
docker exec hospital-core jcmd 1 JFR.start duration=60s filename=/tmp/naive.jfr
docker cp hospital-core:/tmp/naive.jfr ./naive.jfr
```
*Observation: High latency (~30s), pool starvation, and "Seq Scan" in Postgres.*

---

### 🧪 Scenario 2: The Paradox (Index Added + N+1 Loop)
**Setup:**
- **Code:** Add `@Table(indexes = @Index(columnList = "email"))` to `Patient.java`.
- **Rebuild:** `docker-compose up -d --build hospital-core`.

**Run Load Test & Capture Diagnostic:**
```bash
python3 load_gen.py
docker exec hospital-core jcmd 1 JFR.start duration=60s filename=/tmp/index_only.jfr
docker cp hospital-core:/tmp/index_only.jfr ./index_only.jfr
```
*Observation: Search latency drops, but **Memory Allocation Churn** spikes from ~3GB to ~30GB as the app finally runs fast enough to expose the N+1 object churn.*

---

### 🧪 Scenario 3: Engineered State (Index + Batching)
**Setup:**
- **Code:** Refactor `AppointmentService` to use `findAllByIdsMap` (batch fetch).
- **Rebuild:** `docker-compose up -d --build hospital-core`.

**Run Final Test:**
```bash
python3 load_gen.py
docker exec hospital-core jcmd 1 JFR.start duration=60s filename=/tmp/optimized.jfr
docker cp hospital-core:/tmp/optimized.jfr ./optimized.jfr
```
*Observation: 0 errors, Throughput increases by 6,000%+, and Latency < 200ms.*

---

## 🔍 Diagnostics & Troubleshooting

### JVM Thread Dump
To see where threads are stuck during N+1:
```bash
docker exec hospital-core jcmd 1 Thread.print > thread_dump.txt
```

### PostgreSQL Execution Plan
To prove the missing index is causing a Sequential Scan:
```bash
docker exec -it hospital-db psql -U user -d hospital_db -c "EXPLAIN ANALYZE SELECT * FROM patients WHERE email = 'test@hospital.com';"
```

### Reset Database Weight
If you need to wipe and re-seed the 150k records:
```bash
docker-compose down -v
docker-compose up -d
```

---

## 📈 Learning Outcomes
- **Throughput Paradox:** Learned that cumulative memory allocation can increase when code is optimized because the system is performing more work per second.
- **Connection Pool Starvation:** Observed how a single slow endpoint can exhaust the HikariCP pool, causing unrelated fast endpoints to fail (401/500 errors).
- **Security Short-Circuit:** Identified how expiring JWT tokens can lead to "false health" in metrics where system load drops to zero because the security filter is rejecting all traffic.

---
*Created as part of a Systems Engineering Deep Dive.*