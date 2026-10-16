# setup.ps1 - Initialize Local Kubernetes Cluster with Kind
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$binDir = Join-Path $projectRoot "bin"
$kindExe = Join-Path $binDir "kind.exe"

# If kind is not in bin, fallback to PATH
if (-not (Test-Path $kindExe)) {
    $kindCmd = Get-Command "kind" -ErrorAction SilentlyContinue
    if ($kindCmd) {
        $kindExe = $kindCmd.Source
    } else {
        Write-Error "kind executable not found in $binDir or system PATH. Please run stage 1 inspection."
    }
}

Write-Host ">>> Using kind: $kindExe" -ForegroundColor Cyan

# Verify Docker daemon is running
Write-Host ">>> Verifying Docker daemon..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "[OK] Docker is running." -ForegroundColor Green
} catch {
    Write-Error "Docker daemon is not running! Please start Docker Desktop and retry."
}

# Check if kind cluster already exists
$clusterName = "local-ecommerce-cluster"
$existingClusters = & $kindExe get clusters
if ($existingClusters -contains $clusterName) {
    Write-Host "[INFO] Kind cluster '$clusterName' already exists." -ForegroundColor Green
} else {
    Write-Host ">>> Creating kind cluster '$clusterName' with port mappings..." -ForegroundColor Yellow
    $kindConfig = Join-Path $projectRoot "k8s\kind-config.yaml"
    & $kindExe create cluster --config $kindConfig --name $clusterName
    Write-Host "[OK] Kind cluster created successfully." -ForegroundColor Green
}

# Verify kubectl context
kubectl cluster-info --context "kind-$clusterName"
Write-Host ">>> Cluster setup complete! Current context set to kind-$clusterName." -ForegroundColor Cyan
