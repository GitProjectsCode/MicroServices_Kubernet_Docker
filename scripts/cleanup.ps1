# cleanup.ps1 - Tear Down Microservices or Entire Kind Cluster
param(
    [switch]$DeleteCluster
)

$clusterName = "local-ecommerce-cluster"
$ns = "local-microservices"
$projectRoot = Split-Path -Parent $PSScriptRoot
$binDir = Join-Path $projectRoot "bin"
$kindExe = Join-Path $binDir "kind.exe"
if (-not (Test-Path $kindExe)) { $kindExe = "kind" }

if ($DeleteCluster) {
    Write-Host ">>> Deleting entire Kind cluster '$clusterName'..." -ForegroundColor Yellow
    & $kindExe delete cluster --name $clusterName
    Write-Host "[OK] Kind cluster deleted." -ForegroundColor Green
} else {
    Write-Host ">>> Deleting Kubernetes namespace '$ns' and all associated resources..." -ForegroundColor Yellow
    kubectl delete namespace $ns --ignore-not-found=true
    Write-Host "[OK] Namespace '$ns' deleted." -ForegroundColor Green
    Write-Host "Tip: To delete the entire Kind cluster and free all container resources, run: .\scripts\cleanup.ps1 -DeleteCluster" -ForegroundColor Cyan
}
