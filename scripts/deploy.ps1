# deploy.ps1 - Deploy All Kubernetes Resources and Monitoring Stack
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$k8sDir = Join-Path $projectRoot "k8s"
$ns = "local-microservices"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 1: Applying Namespace, Configs, and Secrets        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

kubectl apply -f (Join-Path $k8sDir "namespace.yaml")
kubectl apply -f (Join-Path $k8sDir "configmaps\app-config.yaml")
kubectl apply -f (Join-Path $k8sDir "secrets\app-secrets.yaml")

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 2: Deploying Grafana Dashboards ConfigMap           " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$dashboardJson = Join-Path $projectRoot "monitoring\grafana\dashboards\microservices-dashboard.json"
kubectl create configmap grafana-dashboard-data `
    --from-file="microservices-dashboard.json=$dashboardJson" `
    -n $ns `
    --dry-run=client -o yaml | kubectl apply -f -

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 3: Deploying Prometheus and Grafana Monitoring      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

kubectl apply -f (Join-Path $k8sDir "monitoring\prometheus.yaml")
kubectl apply -f (Join-Path $k8sDir "monitoring\grafana.yaml")

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 4: Deploying Microservices Applications & Services " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

kubectl apply -f (Join-Path $k8sDir "deployments\")
kubectl apply -f (Join-Path $k8sDir "services\")

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 5: Waiting for Pods and Deployments Rollout        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$deployments = @("api-gateway", "product-service", "order-service", "inventory-service", "prometheus", "grafana")
foreach ($dep in $deployments) {
    Write-Host ">>> Waiting for deployment/$dep to be ready..." -ForegroundColor Yellow
    kubectl rollout status deployment/$dep -n $ns --timeout=180s
}

Write-Host "[OK] All microservices and observability components are Ready!" -ForegroundColor Green
