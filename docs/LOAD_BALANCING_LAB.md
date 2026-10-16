# Load Balancing & Fault Tolerance Lab Experiments

This document provides step-by-step instructions for running the 6 reproducible Kubernetes load-balancing, scaling, and fault tolerance experiments.

---

## Experiment 1: Single Replica Baseline

### Objective
Observe routing behavior when only one instance of `product-service` is active.

### Execution
1. Scale Product Service to 1 replica:
   ```powershell
   kubectl scale deployment product-service --replicas=1 -n local-microservices
   kubectl rollout status deployment/product-service -n local-microservices
   ```
2. Verify active pod:
   ```powershell
   kubectl get pods -l app=product-service -n local-microservices
   ```
3. Send 30 requests to the instance endpoint:
   ```powershell
   1..30 | ForEach-Object {
       (Invoke-RestMethod -Uri "http://localhost:8081/api/instance" -Headers @{ "Connection" = "close" }).hostname
   } | Group-Object | Select-Object Count, Name
   ```

### Expected Result
100% of requests are handled by the single pod replica (Count = 30 for that exact pod name).

---

## Experiment 2: Scaling to Three Replicas

### Objective
Observe how Kubernetes Service L4 load balancing distributes requests across multiple pods.

### Execution
1. Scale Product Service to 3 replicas:
   ```powershell
   kubectl scale deployment product-service --replicas=3 -n local-microservices
   kubectl rollout status deployment/product-service -n local-microservices
   ```
2. List the 3 active pods:
   ```powershell
   kubectl get pods -l app=product-service -n local-microservices
   ```
3. Send 60 requests with fresh connections (`Connection: close`):
   ```powershell
   1..60 | ForEach-Object {
       (Invoke-RestMethod -Uri "http://localhost:8081/api/instance" -Headers @{ "Connection" = "close" }).hostname
   } | Group-Object | Select-Object Count, Name
   ```

### Expected Result
Requests are distributed across all 3 distinct pod hostnames (e.g., approximately ~20 requests per pod).

---

## Experiment 3: Continuous Traffic & Performance with k6

### Objective
Measure throughput, latency percentiles, and replica distribution under sustained load.

### Execution
Run the bundled k6 load test (5 virtual users for 30 seconds):
```powershell
.\bin\k6.exe run load-tests\k6\load-test.js
```

Or configure custom load via environment variables:
```powershell
# Run with 10 Virtual Users for 45 seconds
$env:VUS = "10"
$env:DURATION = "45s"
.\bin\k6.exe run load-tests\k6\load-test.js
```

### Metrics Recorded
- **Requests Per Second (RPS)**
- **Average Latency (ms)**
- **p95 Latency (ms)**
- **Error Percentage (%)**
- **Distinct Pod Instances Observed**

---

## Experiment 4: Pod Failure & Self-Healing Recovery

### Objective
Demonstrate Kubernetes pod failure detection, automatic replacement, and endpoint slice updates.

### Execution
1. In one PowerShell terminal, start continuous traffic:
   ```powershell
   $env:VUS = "5"
   $env:DURATION = "60s"
   .\bin\k6.exe run load-tests\k6\load-test.js
   ```
2. In a second PowerShell terminal, identify and delete one active product pod:
   ```powershell
   $podToDelete = (kubectl get pods -l app=product-service -n local-microservices -o jsonpath='{.items[0].metadata.name}')
   Write-Host "Killing pod: $podToDelete"
   kubectl delete pod $podToDelete -n local-microservices
   ```
3. Watch the Kubernetes ReplicaSet launch an immediate replacement pod:
   ```powershell
   kubectl get pods -l app=product-service -n local-microservices -w
   ```

### Observations
- **Readiness Probe Impact**: The terminating pod is promptly removed from the `EndpointSlice` so traffic stops routing to it. The new pod is created, goes through container initialization, passes its readiness probe (`/actuator/health/readiness`), and then starts receiving requests.
- **k6 Results**: Observe whether any 502/503 errors occurred during the transition and how quickly recovery occurred.

---

## Experiment 5: Resource Limits & Horizontal Pod Autoscaling

### Understanding CPU Requests & Limits
In `k8s/deployments/product-service-deployment.yaml`:
- `requests.cpu: 100m` (guaranteed CPU slice: 0.1 core)
- `limits.cpu: 500m` (maximum CPU throttle threshold: 0.5 core)

### Autoscaling Requirements
Horizontal Pod Autoscaler (HPA) requires **Metrics Server** to be installed in the cluster.
1. Check if metrics-server is available:
   ```powershell
   kubectl top pods -n local-microservices
   ```
2. If metrics-server is not installed, it can be applied to Kind using:
   ```powershell
   kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
   # Note: For Kind, metrics-server requires --kubelet-insecure-tls argument
   ```
3. Apply an HPA rule:
   ```powershell
   kubectl autoscale deployment product-service --cpu-percent=50 --min=2 --max=6 -n local-microservices
   ```
*Important Note*: Autoscaling occurs based on measured CPU consumption against requests, not merely raw RPS count. If microservices are I/O bound or lightweight, CPU may stay below 50% unless intensive load is applied.

---

## Experiment 6: Downstream Latency, Timeouts & Gateway Recovery

### Objective
Test Gateway resilience, timeout handling, and failure propagation when a downstream service becomes slow.

### Execution
1. Call the test simulation endpoint with a 3000ms delay:
   ```powershell
   Measure-Command {
       Invoke-RestMethod -Uri "http://localhost:8080/api/products/slow?delayMs=3000"
   }
   ```
2. Call with a 7000ms delay (exceeding the Gateway's configured 5s response timeout in `application.yml`):
   ```powershell
   try {
       Invoke-RestMethod -Uri "http://localhost:8080/api/products/slow?delayMs=7000"
   } catch {
       Write-Host "Gateway error caught: $($_.Exception.Message)" -ForegroundColor Red
   }
   ```

### Observations
- When downstream latency exceeds 5 seconds, the Gateway returns `504 Gateway Timeout`.
- Check Gateway logs to observe timeout handling:
  ```powershell
  kubectl logs -l app=api-gateway -n local-microservices --tail=50
  ```
- Normal traffic to `/api/products` continues serving without downtime.
