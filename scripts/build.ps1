# build.ps1 - Compile Maven Microservices, Build Docker Images, and Load into Kind
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$binDir = Join-Path $projectRoot "bin"
$kindExe = Join-Path $binDir "kind.exe"
if (-not (Test-Path $kindExe)) { $kindExe = "kind" }
$clusterName = "local-ecommerce-cluster"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 1: Compiling and Packaging Maven Microservices     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Push-Location $projectRoot
try {
    mvn clean package -DskipTests
    Write-Host "[OK] Maven packages built successfully." -ForegroundColor Green
} finally {
    Pop-Location
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 2: Building Microservice Container Images          " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$services = @("api-gateway", "product-service", "order-service", "inventory-service")
foreach ($svc in $services) {
    $svcPath = Join-Path $projectRoot "services\$svc"
    $tag = "$($svc):1.0.0"
    Write-Host ">>> Building Docker image: $tag..." -ForegroundColor Yellow
    docker build -t $tag $svcPath
    Write-Host "[OK] Built $tag" -ForegroundColor Green
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Step 3: Loading Container Images into Kind Cluster      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

foreach ($svc in $services) {
    $tag = "$($svc):1.0.0"
    Write-Host ">>> Loading image $tag into kind cluster '$clusterName'..." -ForegroundColor Yellow
    & $kindExe load docker-image $tag --name $clusterName
    Write-Host "[OK] Loaded $tag into Kind." -ForegroundColor Green
}

Write-Host ">>> All images successfully built and loaded into Kind cluster!" -ForegroundColor Cyan
