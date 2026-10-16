# Postman API Testing & Verification Guide

## 1. Importing the Collection & Environment

1. Open **Postman Desktop**.
2. Click **Import** in the top-left corner.
3. Drag and drop or browse to the following two files in this repository:
   - `postman/Local-Kubernetes-Lab.postman_collection.json`
   - `postman/Local-Kubernetes-Lab.postman_environment.json`
4. In the top-right environment selector in Postman, choose **`Local-Kubernetes-Lab-Environment`**.

---

## 2. Environment Variables Overview

The environment includes the following pre-configured variables:

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `gatewayUrl` | `http://localhost:8080` | URL for the Spring Cloud API Gateway entrypoint |
| `baseUrl` | `http://localhost:8080` | Alias for gateway URL |
| `productId` | `1` | Dynamically updated when listing or creating products |
| `orderId` | `1` | Dynamically updated when creating orders |

---

## 3. Step-by-Step API Validation Flow

Execute the requests in the collection sequentially:

### Folder 1: Gateway & System Health
1. **Gateway Health Check** (`GET {{gatewayUrl}}/api/gateway/health`):
   - Verifies the gateway is UP and returns its pod hostname and timestamp.
   - Tests verify `X-Correlation-Id` is returned in the response headers.
2. **Gateway Actuator Prometheus Metrics** (`GET {{gatewayUrl}}/actuator/prometheus`):
   - Verifies that the gateway exports Micrometer Prometheus metrics.

### Folder 2: Product Service
1. **Product Service Health** (`GET {{gatewayUrl}}/api/products/health`):
   - Checks Product Service availability through the gateway.
2. **List All Products** (`GET {{gatewayUrl}}/api/products`):
   - Returns initial seeded catalog items from MySQL (Laptop, Headphones, Chair, Keyboard, Monitor).
   - Test script automatically saves the first product ID into the `productId` variable.
3. **Get Product by ID** (`GET {{gatewayUrl}}/api/products/{{productId}}`):
   - Retrieves product details by ID.
4. **Create New Product** (`POST {{gatewayUrl}}/api/products`):
   - Inserts a new product into the database and updates `productId`.
5. **Simulate Slow Downstream Product** (`GET {{gatewayUrl}}/api/products/slow?delayMs=2000`):
   - Simulates a 2000ms delay to verify gateway response handling under load.

### Folder 3: Order Service (Inter-Service Orchestration)
1. **Order Service Health** (`GET {{gatewayUrl}}/api/orders/health`)
2. **List All Orders** (`GET {{gatewayUrl}}/api/orders`):
   - Shows past placed orders stored in MySQL.
3. **Create Order** (`POST {{gatewayUrl}}/api/orders`):
   - Body: `{"productId": 1, "quantity": 2}`
   - **What happens behind the scenes**:
     1. Gateway forwards request to `order-service`.
     2. `order-service` calls `product-service` via Kubernetes DNS to fetch product details and price.
     3. `order-service` calls `inventory-service` to check available stock and deduct 2 units.
     4. `order-service` saves the order into MySQL with status `CONFIRMED`.
4. **Get Order by ID** (`GET {{gatewayUrl}}/api/orders/{{orderId}}`)

### Folder 4: Inventory Service
1. **Inventory Health Check** (`GET {{gatewayUrl}}/api/inventory/health`)
2. **Get Inventory by Product ID** (`GET {{gatewayUrl}}/api/inventory/1`):
   - Verifies available stock count and warehouse location.
3. **Update Inventory Stock** (`PUT {{gatewayUrl}}/api/inventory/1?quantity=25`):
   - Adds 25 units to product 1's stock level.

---

## 4. Observing Pod Replica Distribution in Postman

Open the Postman Console (**Alt + Ctrl + C** or click "Console" in the bottom toolbar).

Select the request:
**`5. Load Balancing Experiments -> Product Service Instance Info`**
(`GET {{gatewayUrl}}/api/instance`)

Click **Send** repeatedly (10–20 times):
In the console log, you will observe the response details:
```
>>> [Product Replica Pod]: product-service-65fd84b9c8-ab12x InstanceId: 98f4b123 Time: 2026-10-16T15:05:01Z
>>> [Product Replica Pod]: product-service-65fd84b9c8-cd34y InstanceId: e1129b87 Time: 2026-10-16T15:05:03Z
```

### Why Repeated Requests May Stick to One Pod (Connection Reuse)
If you see the same pod name on several consecutive clicks:
- Postman uses **HTTP/1.1 Persistent Connections (Keep-Alive)** by default.
- As long as the TCP socket between Postman and the gateway / node remains open, all HTTP requests flow across the same established TCP pipe.
- To force immediate round-robin pod switching on every click, add this header in Postman:
  - **Key**: `Connection`
  - **Value**: `close`
- Disabling keep-alive forces a new TCP handshake for each request, prompting Kubernetes iptables to re-evaluate and balance traffic across replicas.
