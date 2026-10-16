# Grafana OSS Observability & Dashboard Guide

## 1. Accessing Grafana Locally

Once deployed, Grafana OSS is accessible on your Windows host:

- **Direct URL**: **`http://localhost:3000`** (mapped directly from NodePort `30030` via Kind cluster config)
- **Alternative (Port-Forward)**:
  ```powershell
  kubectl port-forward svc/grafana 3000:3000 -n local-microservices
  ```
- **Authentication**:
  - **Anonymous Access**: Enabled by default with Viewer/Admin role for friction-free local development.
  - **Admin Credentials**: Username: `admin` | Password: `admin`
- **Prometheus Direct Access**:
  - **URL**: **`http://localhost:9090`** (or `kubectl port-forward svc/prometheus 9090:9090 -n local-microservices`)

---

## 2. Automatic Provisioning

You do not need to manually configure Prometheus data sources or import dashboards. The setup automatically provisions:
1. **Datasource**: Configured to query the internal Prometheus service at `http://prometheus:9090`.
2. **Dashboard**: Pre-loaded in the folder **`Microservices`** titled:
   **`Spring Boot Microservices & Kubernetes Observability`** (UID: `ecommerce-microservices-lab`).

---

## 3. Explaining the 12 Dashboard Panels

| # | Panel Name | Visualization | PromQL Query | What It Measures |
| :- | :--- | :--- | :--- | :--- |
| **1** | **Total HTTP Requests** | Stat Counter | `sum(http_server_requests_seconds_count)` | Cumulative number of HTTP requests processed by all services since startup. |
| **2** | **Requests Per Second (RPS)** | Time Series | `sum(rate(http_server_requests_seconds_count[1m]))` | Real-time global request throughput across the microservice cluster. |
| **3** | **Request Rate by Service** | Time Series | `sum by (application) (rate(http_server_requests_seconds_count[1m]))` | Breakdown of incoming traffic per microservice (`api-gateway`, `product-service`, `order-service`, `inventory-service`). |
| **4** | **HTTP Status Code Distribution** | Stacked Area | `sum by (status) (rate(http_server_requests_seconds_count[1m]))` | Request volume classified by HTTP response codes (200, 201, 400, 404, 500, 504). |
| **5** | **4xx and 5xx Error Rates (%)** | Gauge Meter | `(sum(rate(http_server_requests_seconds_count{status=~"4..\|5.."}[1m])) or vector(0)) / (sum(rate(http_server_requests_seconds_count[1m])) > 0 or vector(1)) * 100` | Percentage of requests resulting in client (4xx) or server (5xx) errors. Alerts turn yellow at 1% and red at 5%. |
| **6** | **Request Latency Percentiles (p50, p95, p99)** | Time Series | `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[1m])))` | Tail latency in seconds. Shows median response time (p50), 95th percentile, and 99th percentile slowest requests. |
| **7** | **JVM Heap Memory Usage** | Time Series | `sum by (application) (jvm_memory_used_bytes{area="heap"})` vs `jvm_memory_max_bytes` | Real-time Java heap allocation compared against the JVM's maximum heap limit. |
| **8** | **JVM CPU Usage (%)** | Time Series | `avg by (application) (process_cpu_usage) * 100` | CPU consumption percentage attributed directly to the Java process. |
| **9** | **Process Uptime** | Stat Counter | `avg(process_uptime_seconds)` | Elapsed time since each microservice JVM was launched. |
| **10**| **Active Replicas / Restarts** | Stat Counter | `count(count by (instance) (process_uptime_seconds))` | Total number of distinct active pod replicas currently scraping metrics. |
| **11**| **Memory Usage by Instance** | Time Series | `sum by (instance) (jvm_memory_used_bytes{area="heap"})` | Individual heap memory consumption per pod instance, allowing detection of memory leaks in a specific replica. |
| **12**| **Request Distribution by Service Instance** | Time Series | `sum by (application, instance, kubernetes_pod_name) (rate(http_server_requests_seconds_count[1m]))` | **Core Load Balancing Panel**: Visualizes the proportion of requests handled by each pod replica in real time. |

---

## 4. Troubleshooting Missing Data

If panels show "No data":
1. **Verify Prometheus Targets**:
   Open `http://localhost:9090/targets` in your browser. All jobs (`api-gateway`, `product-service`, etc.) should show state **`UP`**.
2. **Generate Traffic**:
   Prometheus metrics with `rate(...[1m])` require active traffic within the last minute. Run the k6 load test or send requests in Postman to generate data points.
3. **Verify Time Range**:
   Ensure Grafana's top-right time picker is set to **Last 15 minutes** or **Last 5 minutes** with a refresh interval of **5s**.
