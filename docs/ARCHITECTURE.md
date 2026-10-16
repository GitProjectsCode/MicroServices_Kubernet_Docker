# Architecture & Network Design Guide

## 1. High-Level Architecture Overview

This project implements a realistic microservices e-commerce application deployed on a local Kubernetes cluster, instrumented with Spring Boot Actuator, Micrometer, Prometheus, and Grafana OSS.

```
                              [ Windows Host Machine / Client ]
                                   |           |            |
                                   | :8080     | :3000      | :9090
                                   v           v            v
                      +-------------------+ +----------+ +------------+
                      | Kind Port Mapping | | Grafana  | | Prometheus |
                      |   (:30080)        | | (:30030) | |  (:30090)   |
                      +-------------------+ +----------+ +------------+
                                   |                         | Scrapes
                                   v                         | Metrics
                      +--------------------------+           |
                      |  Spring Cloud API Gateway|           |
                      |   (api-gateway:8080)     |-----------+
                      +--------------------------+           |
                         |            |          |           |
        /api/products/** |            |          |           |
                         v            |          |           |
     +---------------------------+    |          |           |
     |   Product Service (x2)    |----+          |           |
     | [pod-1]        [pod-2]    |               |           |
     +---------------------------+               |           |
                         ^                       |           |
         Internal HTTP   |       /api/orders/**  |           |
          Validation     +---------------+       v           |
                                         | +---------------+ |
                                         | | Order Service | |
                                         | |     (x2)      |-+
                                         | +---------------+ |
                                         |         |         |
                                         |         | Stock   |
                                         |         | Check   |
                                         |         v         |
                        /api/inventory/**|   +-------------+ |
                                         +-->|  Inventory  |-+
                                             |   Service   |
                                             +-------------+
                                                    |
                              +---------------------+
                              |
                              v  (via host.docker.internal:3306)
             ========================================================
             [ Local MySQL 8.0 on Windows Host (user: root, pwd: root) ]
             - product_db   - order_db   - inventory_db
             ========================================================
```

---

## 2. Component Breakdown

### A. API Gateway (`services/api-gateway`, Port: 8080)
- **Role**: Single entry point into the Kubernetes cluster.
- **Routing**: Non-blocking reactive routing via Spring Cloud Gateway.
  - `/api/products/**` -> `http://product-service:8081`
  - `/api/orders/**` -> `http://order-service:8082`
  - `/api/inventory/**` -> `http://inventory-service:8083`
- **Observability**:
  - `CorrelationIdFilter`: Generates or propagates `X-Correlation-Id` across all requests.
  - Logs method, URI, status, and duration for every request.
  - Exposes `/api/instance` returning its own pod hostname and runtime instance ID.

### B. Product Service (`services/product-service`, Port: 8081)
- **Role**: Manages the product catalog, descriptions, and pricing.
- **Replicas**: 2 active pods for load balancing demonstration.
- **Database**: MySQL `product_db` (or shared `ecommerce_db`).
- **Resilience Simulation**: `GET /api/products/slow?delayMs=3000` allows simulating slow downstream responses and testing gateway response timeouts.

### C. Order Service (`services/order-service`, Port: 8082)
- **Role**: Handles customer purchase workflows.
- **Replicas**: 2 active pods.
- **Inter-Service Orchestration**:
  - When `POST /api/orders` is called:
    1. Validates product existence and price via `product-service` using Kubernetes DNS (`http://product-service:8081`).
    2. Checks real-time stock via `inventory-service` (`http://inventory-service:8083`).
    3. Deducts stock in `inventory-service`.
    4. Persists the confirmed order in MySQL `order_db`.

### D. Inventory Service (`services/inventory-service`, Port: 8083)
- **Role**: Tracks warehouse stock levels and reserves items during order placement.
- **Database**: MySQL `inventory_db`.

---

## 3. Kubernetes Routing & Load Balancing Comparison

It is crucial to understand that different load-balancing mechanisms operate at distinct layers of the OSI model:

| Mechanism | Layer | Algorithm / Behavior | Where Applied |
| :--- | :--- | :--- | :--- |
| **Kubernetes Service (ClusterIP)** | Layer 4 (TCP/UDP) | Random / Round-Robin via `iptables` or IPVS kernel rules. Connection-level distribution. | Pod-to-Pod and Gateway-to-Service communication. |
| **Ingress / API Gateway** | Layer 7 (HTTP) | Request-level routing (Path, Host, Headers). Can inspect HTTP payload, cookies, and tokens. | External client traffic entering the cluster. |
| **Client-Side Load Balancing** | Layer 7 (Application) | Round-robin, Weighted response time, or Least Connections executed inside the calling client code (e.g. Spring Cloud LoadBalancer / gRPC). | Client library making outbound HTTP/RPC calls. |
| **Horizontal Pod Autoscaler (HPA)** | Control Plane | Metrics-based autoscaling (CPU/Memory % or custom metrics). Dynamically changes replica count. | Pod controller adjusting replica count over time. |

### Why Repeated Requests Don't Distribute 50/50 Evenly
When you click **Send** multiple times in Postman or a browser:
1. **HTTP Keep-Alive & TCP Connection Reuse**: The client maintains an open TCP connection to the gateway or service. If subsequent requests reuse the same TCP socket, iptables keeps routing packets to the same pod.
2. **Layer 4 vs Layer 7**: Standard Kubernetes ClusterIP operates at Layer 4 (TCP packet routing). It does not inspect individual HTTP requests within a reused persistent TCP connection.
3. **Observation Tip**: In k6 or Postman, sending requests from distinct connections or setting `Connection: close` forces fresh TCP handshakes, revealing the Layer 4 load distribution across all replica pods.
