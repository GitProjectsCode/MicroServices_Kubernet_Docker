# Docker Configuration & Containerization Guide

This directory contains container assets and optional Docker Compose orchestration for running the microservice stack directly under Docker Desktop without Kubernetes.

## Microservice Container Images

| Service | Port | Image Tag | Base Image | Size |
| :--- | :--- | :--- | :--- | :--- |
| **api-gateway** | 8080 | `api-gateway:1.0.0` | `eclipse-temurin:21-jre-alpine` | ~144 MB |
| **product-service** | 8081 | `product-service:1.0.0` | `eclipse-temurin:21-jre-alpine` | ~168 MB |
| **order-service** | 8082 | `order-service:1.0.0` | `eclipse-temurin:21-jre-alpine` | ~168 MB |
| **inventory-service** | 8083 | `inventory-service:1.0.0` | `eclipse-temurin:21-jre-alpine` | ~168 MB |

## Security Hardening
All Dockerfiles adhere to enterprise container standards:
- Run as non-root user `appuser:appgroup` (UID 10001).
- Container-aware JVM memory management: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`.
- Fast entropy seed: `-Djava.security.egd=file:/dev/./urandom`.
- Slim Alpine base minimizing CVE attack surface.
