# Local Java Spring Boot, Kubernetes Load Balancing & Grafana Monitoring Lab

A complete, production-grade, beginner-friendly local development and observability lab running on Windows using **Java 21 LTS**, **Spring Boot 3.3.4**, **Docker**, **Kubernetes (Kind)**, **Prometheus OSS**, **Grafana OSS**, and **Postman**.

---

## 1. Architecture Overview

```
                                [ Windows Host ]
                         (Browser / Postman / k6 Tests)
                                |       |       |
                                |:8080  |:3000  |:9090
                                v       v       v
                     +---------------------------------------+
                     | Kind Cluster (local-ecommerce-cluster)|
                     |  Port Mappings: 8080, 8081, 8082, 8083|
                     +---------------------------------------+
                                        |
                 +----------------------+----------------------+
                 |                                             |
                 v                                             v
      +---------------------+                       +---------------------+
      |   Prometheus OSS    |                       |     Grafana OSS     |
      |   (Port 9090)       |                       |     (Port 3000)     |
      +---------------------+                       +---------------------+
                 |                                             |
                 | Scrapes metrics every 5s                    | Reads Datasource
                 +---------------------------------------------+
                 |
                 v
      +--------------------------+
      |  Spring Cloud API Gateway|
      |  (Port 8080 / NodePort)  |
      +--------------------------+
            |             |             |
            v             v             v
      +------------+ +------------+ +---------------+
      |  Product   | |   Order    | |   Inventory   |
      |  Service   | |  Service   | |    Service    |
      | (2 Pods)   | |  (2 Pods)  | |    (1 Pod)    |
      | Port: 8081 | | Port: 8082 | |  Port: 8083   |
      +------------+ +------------+ +---------------+
            |             |                 |
            |             +-----------------+
            |             Inter-Service HTTP
            v
      =============================================================
      [ Local MySQL 8.0 on Windows Host (user: root, pwd: root) ]
      - product_db   - order_db   - inventory_db
      =============================================================
```

---

## 2. Directory Structure

```
local-kubernetes-lab/
├── bin/                       # Local standalone executables (kind.exe, k6.exe)
├── services/
│   ├── api-gateway/           # Spring Cloud Gateway (Port 8080)
│   ├── product-service/       # Product Catalog Service (Port 8081, 2 Replicas)
│   ├── order-service/         # Order Orchestration Service (Port 8082, 2 Replicas)
│   └── inventory-service/     # Inventory Warehouse Service (Port 8083, 1 Replica)
├── k8s/
│   ├── namespace.yaml         # local-microservices namespace
│   ├── kind-config.yaml       # Kind cluster port mapping definition
│   ├── configmaps/            # App environment variables & service URLs
│   ├── secrets/               # Database credentials
│   ├── deployments/           # Kubernetes Deployments with health probes & limits
│   ├── services/              # Kubernetes Services (ClusterIP & NodePort)
│   ├── ingress/               # NGINX Ingress configuration
│   └── monitoring/            # Prometheus & Grafana Kubernetes manifests
├── monitoring/
│   ├── prometheus/            # Prometheus scrape configuration
│   └── grafana/
│       ├── provisioning/      # Automated datasources & dashboard providers
│       └── dashboards/        # 12-Panel JSON Observability Dashboard
├── postman/
│   ├── Local-Kubernetes-Lab.postman_collection.json
│   └── Local-Kubernetes-Lab.postman_environment.json
├── load-tests/
│   └── k6/
│       └── load-test.js       # Configurable k6 virtual user load test
├── scripts/
│   ├── setup.ps1              # Spins up local Kind cluster with port mappings
│   ├── build.ps1              # Compiles Maven apps, builds Docker images, loads into Kind
│   ├── deploy.ps1             # Deploys manifests and waits for rollout
│   ├── status.ps1             # Checks pods, services, and runs HTTP smoke tests
│   └── cleanup.ps1            # Tears down workloads or entire cluster
└── docs/
    ├── SETUP.md               # Prerequisites and hardware sizing
    ├── ARCHITECTURE.md        # Network topology & load balancing mechanics
    ├── POSTMAN_TESTING.md     # Step-by-step Postman collection guide
    ├── GRAFANA_GUIDE.md       # Observability dashboard panel explanations
    └── LOAD_BALANCING_LAB.md  # 6 reproducible experiment walkthroughs
```

---

## 3. Quick Start (Windows PowerShell)

Run all commands from PowerShell in the project root:

### Step 1: Initialize Local Cluster
```powershell
.\scripts\setup.ps1
```

### Step 2: Build & Load Microservices
```powershell
.\scripts\build.ps1
```

### Step 3: Deploy Workloads & Monitoring
```powershell
.\scripts\deploy.ps1
```

### Step 4: Verify Status
```powershell
.\scripts\status.ps1
```

---

## 4. Endpoints & Direct Access

| Service | Port | Endpoint URL | Description |
| :--- | :--- | :--- | :--- |
| **API Gateway** | `8080` | `http://localhost:8080/api/gateway/health` | Central API Gateway entrypoint |
| **Gateway Instance** | `8080` | `http://localhost:8080/api/instance` | Gateway pod metadata |
| **Product Service** | `8080` / `8081` | `http://localhost:8080/api/products` | Product catalog (via GW or direct) |
| **Product Instance** | `8081` | `http://localhost:8081/api/instance` | Product replica pod load balancing |
| **Order Service** | `8080` / `8082` | `http://localhost:8080/api/orders` | Order placement & listing |
| **Inventory Service**| `8080` / `8083` | `http://localhost:8080/api/inventory/1`| Stock check and adjustments |
| **Grafana OSS** | `3000` | `http://localhost:3000` | Pre-provisioned dashboards |
| **Prometheus OSS** | `9090` | `http://localhost:9090` | Metrics targets and PromQL queries |

---

## 5. Grafana Dashboard Panels

Access Grafana at **`http://localhost:3000`** (Credentials: `admin` / `admin` or Anonymous Viewer).
The pre-provisioned dashboard **`Spring Boot Microservices & Kubernetes Observability`** includes:
1. **Total HTTP Requests** (Stat)
2. **Requests Per Second (RPS)** (Time Series)
3. **Request Rate by Service** (Time Series)
4. **HTTP Status-Code Distribution** (Stacked Area)
5. **4xx and 5xx Error Rates** (Gauge)
6. **Request Latency Percentiles (p50, p95, p99)** (Time Series)
7. **JVM Heap Memory Usage** (Time Series)
8. **JVM CPU Usage (%)** (Time Series)
9. **Process Uptime** (Stat)
10. **Active Replicas / Restarts** (Stat)
11. **Memory Usage by Instance** (Time Series)
12. **Request Distribution by Service Instance** (Time Series showing per-pod balancing)

---

## 6. Running Load Balancing Experiments

### Experiment 1: Scaling Replicas
```powershell
# Scale Product Service to 3 replicas
kubectl scale deployment product-service --replicas=3 -n local-microservices
kubectl rollout status deployment/product-service -n local-microservices

# Observe pod distribution across 30 requests
1..30 | ForEach-Object {
    (Invoke-RestMethod -Uri "http://localhost:8081/api/instance" -Headers @{"Connection"="close"}).hostname
} | Group-Object | Select-Object Count, Name
```

### Experiment 2: Continuous Load Testing with k6
```powershell
# Run 5 virtual users for 30 seconds
.\bin\k6.exe run load-tests\k6\load-test.js
```

### Experiment 3: Pod Failure Simulation
```powershell
# Delete an active product pod during traffic
$pod = (kubectl get pods -l app=product-service -n local-microservices -o jsonpath='{.items[0].metadata.name}')
kubectl delete pod $pod -n local-microservices
```

---

## 7. Troubleshooting Common Issues

### 1. Pods in `ImagePullBackOff` / `ErrImagePull`
- **Cause**: Kubernetes cannot find the container image in the local Kind node.
- **Fix**: Re-load the images into Kind:
  ```powershell
  .\bin\kind.exe load docker-image api-gateway:1.0.0 --name local-ecommerce-cluster
  .\bin\kind.exe load docker-image product-service:1.0.0 --name local-ecommerce-cluster
  .\bin\kind.exe load docker-image order-service:1.0.0 --name local-ecommerce-cluster
  .\bin\kind.exe load docker-image inventory-service:1.0.0 --name local-ecommerce-cluster
  ```

### 2. Pods in `CrashLoopBackOff`
- **Cause**: Application startup error (e.g. database unreachable).
- **Diagnostic Command**:
  ```powershell
  kubectl logs -l app=product-service -n local-microservices --tail=100
  ```
- **Fix**: Verify your local MySQL service `MySQL80` is running and reachable on `host.docker.internal:3306`.

### 3. Readiness Probe Failing (`0/1 Running`)
- **Diagnostic Command**:
  ```powershell
  kubectl describe pod -l app=product-service -n local-microservices
  ```
- **Check**: Verify `/actuator/health/readiness` returns HTTP 200. Probes give the container 25 seconds initial delay to start before marking it unready.

### 4. Prometheus Targets Showing `DOWN`
- Open `http://localhost:9090/targets` in your browser.
- Check if the target URL resolves inside the cluster (e.g., `product-service:8081`). Ensure the Service name and port match `prometheus.yml`.

### 5. Grafana Dashboard Displays "No Data"
- Ensure you have executed a few requests via Postman or `k6` to populate `http_server_requests_seconds_count`.
- Confirm Grafana's time range is set to **Last 5 minutes** or **Last 15 minutes**.
