# Environment Setup & Prerequisites Guide

## 1. Prerequisites Checklist

Ensure the following tools are installed on your Windows machine:

| Component | Minimum Version | Verified in Lab | Purpose |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10 / 11 64-bit | Windows 10 Home Single Language | Host operating system |
| **Java Development Kit** | Java 17 or 21 LTS | Java 21.0.7 LTS | Compiling and running Spring Boot apps |
| **Apache Maven** | 3.8+ | 3.9.8 | Building Java multi-module project |
| **Docker Desktop** | 20.10+ | 29.4.3 (Linux Engine) | Running local containers and Kind node |
| **Kubectl CLI** | 1.28+ | v1.34.1 | Interacting with Kubernetes cluster |
| **Kind CLI** | 0.20+ | v0.27.0 (included in `bin/`) | Running local Kubernetes in Docker |
| **k6 Load Generator** | 0.45+ | v0.56.0 (included in `bin/`) | Performance & replica load testing |
| **MySQL Server** | 8.0+ | MySQL 8.0 (localhost:3306) | Backing relational database |
| **Postman** | Latest Desktop | Any modern release | Manual REST API validation |

---

## 2. Resource Trade-offs & Memory Budget

Running a multi-replica microservices cluster with a full observability stack locally requires mindful memory planning:

- **Estimated RAM Breakdown**:
  - Windows Host Baseline: ~3.5 GB
  - Docker Desktop VM: ~4.0 GB
  - Spring Boot Services (6 pods total: 1 Gateway + 2 Products + 2 Orders + 1 Inventory): ~2.5 GB (configured with `MaxRAMPercentage=75.0` and 256Mi requests)
  - Prometheus OSS: ~300 MB
  - Grafana OSS: ~200 MB
  - Local MySQL 8.0: ~400 MB
  - **Total Recommended Host RAM**: **8 GB Minimum**, **16 GB Recommended**.

---

## 3. Fast-Track PowerShell Setup Sequence

Open PowerShell in the project root directory (`d:\Developing\MicroServices_Kubernet_Docker`) and run:

### Step 1: Initialize Local Kubernetes Cluster
```powershell
.\scripts\setup.ps1
```
*Creates the single-node Kind cluster `local-ecommerce-cluster` with port mappings for Gateway (8080), Product (8081), Order (8082), Inventory (8083), Grafana (3000), and Prometheus (9090).*

### Step 2: Build and Package Microservices & Docker Images
```powershell
.\scripts\build.ps1
```
*Compiles the Java 21 Spring Boot fat JARs using Maven, builds hardened Alpine-based Docker images, and side-loads them directly into the Kind cluster.*

### Step 3: Deploy All Resources to Kubernetes
```powershell
.\scripts\deploy.ps1
```
*Applies the namespace, configs, secrets, Prometheus, Grafana, and deploys the microservices, waiting until all pods reach `1/1 Running` state.*

### Step 4: Verify Status and Test Endpoints
```powershell
.\scripts\status.ps1
```
*Displays active pods, deployments, services, endpoints, and runs immediate HTTP smoke tests against all endpoints.*

### Step 5: Clean Up When Finished
```powershell
# Delete only Kubernetes workloads and namespace:
.\scripts\cleanup.ps1

# Or delete the entire Kind cluster to reclaim all RAM and disk:
.\scripts\cleanup.ps1 -DeleteCluster
```
