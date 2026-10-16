# status.ps1 - Inspect Microservices Cluster Health and Test HTTP Endpoints
$ns = "local-microservices"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "                 KUBERNETES POD STATUS                    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
kubectl get pods -n $ns -o wide

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "             KUBERNETES DEPLOYMENTS & REPLICAS            " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
kubectl get deployments -n $ns

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "                KUBERNETES SERVICES & PORTS               " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
kubectl get svc -n $ns

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "               KUBERNETES ENDPOINTSLICES                  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
kubectl get endpointslices -n $ns

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "               LOCAL HTTP SMOKE TESTS                     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$endpoints = @(
    @{ Name = "API Gateway Health";        Url = "http://localhost:8080/api/gateway/health" },
    @{ Name = "API Gateway Instance";      Url = "http://localhost:8080/api/instance" },
    @{ Name = "Product Service via GW";    Url = "http://localhost:8080/api/products" },
    @{ Name = "Order Service via GW";      Url = "http://localhost:8080/api/orders" },
    @{ Name = "Inventory Service via GW";  Url = "http://localhost:8080/api/inventory/1" },
    @{ Name = "Product NodePort Instance"; Url = "http://localhost:8081/api/instance" },
    @{ Name = "Order NodePort Instance";   Url = "http://localhost:8082/api/instance" },
    @{ Name = "Prometheus UI";             Url = "http://localhost:9090/-/ready" },
    @{ Name = "Grafana UI";                Url = "http://localhost:3000/api/health" }
)

foreach ($ep in $endpoints) {
    try {
        $resp = Invoke-WebRequest -Uri $ep.Url -TimeoutSec 5 -UseBasicParsing
        Write-Host ("[PASS] {0,-26} -> HTTP {1}" -f $ep.Name, $resp.StatusCode) -ForegroundColor Green
    } catch {
        Write-Host ("[FAIL] {0,-26} -> {1}" -f $ep.Name, $_.Exception.Message) -ForegroundColor Red
    }
}
