param(
    [string]$DeployEnv = "staging",
    [int]$AppPort = 8085,
    [int]$NginxPort = 8090
)

$ErrorActionPreference = "Stop"

Write-Host "================================================="
Write-Host " Deploying Municipal Complaint Portal"
Write-Host " Environment : $DeployEnv"
Write-Host " App Port    : $AppPort"
Write-Host " Nginx Port  : $NginxPort"
Write-Host "================================================="

$WorkspaceDir = (Get-Location).Path
$DeployDir = Join-Path $WorkspaceDir "deploy\$DeployEnv"
$ArtifactSource = Join-Path $WorkspaceDir "target\municipal-complaint-portal-1.0.0-SNAPSHOT.jar"
$ArtifactDest = Join-Path $DeployDir "municipal-complaint-portal.jar"
$NginxConf = Join-Path $DeployDir "nginx.conf"
$NginxHome = "C:\Users\Student\scoop\apps\nginx\current"

if (-not (Test-Path $ArtifactSource)) {
    throw "Build artifact not found: $ArtifactSource"
}

# Ensure deploy directory exists
if (-not (Test-Path $DeployDir)) {
    New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
}

# Copy artifact to deploy directory
Copy-Item -Path $ArtifactSource -Destination $ArtifactDest -Force
Write-Host "[Deploy] Packaged artifact deployed to: $ArtifactDest"

# Generate Nginx configuration for this environment
$NginxHomeForward = $NginxHome.Replace('\', '/')
$NginxConfigContent = @"
worker_processes  1;
events {
    worker_connections  1024;
}
http {
    include       '$NginxHomeForward/conf/mime.types';
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       $NginxPort;
        listen       [::]:$NginxPort;
        server_name  localhost;

        location / {
            proxy_pass http://127.0.0.1:$AppPort;
            proxy_set_header Host `$host;
            proxy_set_header X-Real-IP `$remote_addr;
            proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto `$scheme;
        }

        error_page 500 502 503 504 /50x.html;
        location = /50x.html {
            root '$NginxHomeForward/html';
        }
    }
}
"@

[System.IO.File]::WriteAllText($NginxConf, $NginxConfigContent, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "[Deploy] Nginx configuration generated at: $NginxConf"

# Stop previous app process on AppPort if running
$existingApp = Get-NetTCPConnection -LocalPort $AppPort -ErrorAction SilentlyContinue
if ($existingApp) {
    Write-Host "[Deploy] Stopping existing process on port $AppPort..."
    foreach ($pidToKill in ($existingApp.OwningProcess | Select-Object -Unique)) {
        Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 2
}

# Stop previous Nginx if running
$existingNginx = Get-Process nginx -ErrorAction SilentlyContinue
if ($existingNginx) {
    Write-Host "[Deploy] Stopping existing Nginx processes..."
    Stop-Process -Name nginx -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# Start application in background
Write-Host "[Deploy] Starting application on port $AppPort..."
$javaExe = "C:\Program Files\Java\jdk-17.0.11\bin\java.exe"
if (-not (Test-Path $javaExe)) {
    $javaExe = (Get-Command java).Source
}

$appOutLog = Join-Path $DeployDir "app.stdout.log"
$appErrLog = Join-Path $DeployDir "app.stderr.log"
Start-Process -FilePath $javaExe -ArgumentList "-DPORT=$AppPort", "-jar", "`"$ArtifactDest`"" -RedirectStandardOutput $appOutLog -RedirectStandardError $appErrLog -WindowStyle Hidden

# Wait for application to be ready
Write-Host "[Deploy] Waiting for application to initialize on port $AppPort..."
$appReady = $false
for ($i = 0; $i -lt 15; $i++) {
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$AppPort/api/health" -Method Get -UseBasicParsing -TimeoutSec 2
        if ($resp.StatusCode -eq 200) {
            $appReady = $true
            Write-Host "[Deploy] Application healthy on port $AppPort - $($resp.Content)"
            break
        }
    } catch {}
}

if (-not $appReady) {
    throw "Application failed to become healthy on port $AppPort"
}

# Start Nginx reverse proxy
Write-Host "[Deploy] Starting Nginx on port $NginxPort..."
$nginxExe = Join-Path $NginxHome "nginx.exe"
Start-Process -FilePath $nginxExe -ArgumentList "-p `"$NginxHome`"", "-c `"$NginxConf`"" -WindowStyle Hidden
Start-Sleep -Seconds 2

# Verify deployment through Nginx
Write-Host "[Deploy] Verifying deployment through Nginx reverse proxy on port $NginxPort..."
$nginxReady = $false
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$NginxPort/api/health" -Method Get -UseBasicParsing -TimeoutSec 2
        if ($resp.StatusCode -eq 200) {
            $nginxReady = $true
            Write-Host "[Deploy] Nginx reverse-proxy verification SUCCESSFUL!"
            Write-Host "[Deploy] Response: $($resp.Content)"
            break
        }
    } catch {}
}

if (-not $nginxReady) {
    throw "Nginx failed to proxy to application on port $NginxPort"
}

Write-Host "================================================="
Write-Host " Deployment Complete!"
Write-Host " Deployed Application URL: http://localhost:$NginxPort"
Write-Host " Direct Backend Port     : http://localhost:$AppPort"
Write-Host " Environment             : $DeployEnv"
Write-Host " Status                  : HEALTHY (200 OK)"
Write-Host "================================================="
