param(
    [string]$DeployEnv = "staging",
    [int]$AppPort = 8085,
    [int]$NginxPort = 8090,
    [string]$ContainerImage = "",
    [string]$ContainerName = "",
    [string]$EvidencePath = "target\docker-deployment-evidence.json"
)

$ErrorActionPreference = "Stop"
$UseDocker = -not [string]::IsNullOrWhiteSpace($ContainerImage)

function Invoke-Docker {
    param([string[]]$DockerArgs)

    & docker @DockerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker $($DockerArgs -join ' ') failed with exit code $LASTEXITCODE"
    }
}

function Start-PortalContainer {
    param([string]$ImageReference)

    $runArgs = @(
        "run", "--detach",
        "--name", $ContainerName,
        "--restart", "unless-stopped",
        "--publish", "127.0.0.1:${AppPort}:8085",
        "--env", "PORT=8085",
        "--label", "com.municipal-portal.environment=$DeployEnv",
        "--label", "org.opencontainers.image.ref=$ImageReference",
        $ImageReference
    )
    Invoke-Docker -DockerArgs $runArgs
}

function Restore-PreviousContainer {
    param([string]$ImageReference)

    & docker rm --force $ContainerName 2>$null | Out-Null
    if (-not [string]::IsNullOrWhiteSpace($ImageReference)) {
        Write-Warning "[Deploy] Restoring previous container image: $ImageReference"
        try {
            Start-PortalContainer -ImageReference $ImageReference | Out-Null
        } catch {
            Write-Warning "[Deploy] Automatic container rollback failed: $_"
        }
    }
}

if ($DeployEnv -notin @("staging", "production", "development")) {
    throw "Unsupported deployment environment: $DeployEnv"
}
if ($AppPort -lt 1 -or $AppPort -gt 65535 -or $NginxPort -lt 1 -or $NginxPort -gt 65535) {
    throw "Application and Nginx ports must be between 1 and 65535."
}
if ($UseDocker) {
    if ($ContainerName -eq "") {
        $ContainerName = "municipal-portal-$DeployEnv"
    }
    if ($ContainerName -notmatch '^municipal-portal-(staging|production|development)$') {
        throw "Invalid container name: $ContainerName"
    }
    if ($ContainerImage -notmatch '^[a-z0-9]+([._-][a-z0-9]+)*(?:/[a-z0-9]+([._-][a-z0-9]+)*)+:[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$') {
        throw "Container image must be a tagged Docker Hub image reference: $ContainerImage"
    }
}

Write-Host "================================================="
Write-Host " Deploying Municipal Complaint Portal"
Write-Host " Environment : $DeployEnv"
Write-Host " App Port    : $AppPort"
Write-Host " Nginx Port  : $NginxPort"
if ($UseDocker) {
    Write-Host " Docker Image: $ContainerImage"
    Write-Host " Container   : $ContainerName"
}
Write-Host "================================================="

$WorkspaceDir = (Get-Location).Path
$DeployDir = Join-Path $WorkspaceDir "deploy\$DeployEnv"
$ArtifactSource = Join-Path $WorkspaceDir "target\municipal-complaint-portal-1.0.0-SNAPSHOT.jar"
$ArtifactDest = Join-Path $DeployDir "municipal-complaint-portal.jar"
$NginxConf = Join-Path $DeployDir "nginx.conf"
$NginxHome = "C:\Users\Student\scoop\apps\nginx\current"

if (-not $UseDocker -and -not (Test-Path $ArtifactSource)) {
    throw "Build artifact not found: $ArtifactSource"
}
if ($UseDocker) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI is required for container deployment."
    }
    $pullArgs = @("pull", $ContainerImage)
    Invoke-Docker -DockerArgs $pullArgs

    $previousImage = & docker inspect --format '{{.Config.Image}}' $ContainerName 2>$null
    $containerExists = $LASTEXITCODE -eq 0
    if ($containerExists) {
        $previousImage = ($previousImage | Out-String).Trim()
        Write-Host "[Deploy] Existing container image recorded for rollback: $previousImage"
    } else {
        $previousImage = ""
    }
}

# Ensure deploy directory exists
if (-not (Test-Path $DeployDir)) {
    New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null
}

if (-not $UseDocker) {
    # Preserve the original non-container deployment path for earlier pipeline tasks.
    Copy-Item -Path $ArtifactSource -Destination $ArtifactDest -Force
    Write-Host "[Deploy] Packaged artifact deployed to: $ArtifactDest"
}

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

# Stop previous app process only for the legacy, non-container deployment.
if (-not $UseDocker) {
    $existingApp = Get-NetTCPConnection -LocalPort $AppPort -ErrorAction SilentlyContinue
    if ($existingApp) {
        Write-Host "[Deploy] Stopping existing process on port $AppPort..."
        foreach ($pidToKill in ($existingApp.OwningProcess | Select-Object -Unique)) {
            Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 2
    }
}

# Start the new container or the legacy packaged-JAR process, then verify it.
$appReady = $false
$appHealthResponse = ""
try {
    if ($UseDocker) {
        if ($containerExists) {
            $removeArgs = @("rm", "--force", $ContainerName)
            Invoke-Docker -DockerArgs $removeArgs
            for ($attempt = 0; $attempt -lt 10; $attempt++) {
                if (-not (Get-NetTCPConnection -LocalPort $AppPort -ErrorAction SilentlyContinue)) {
                    break
                }
                Start-Sleep -Seconds 1
            }
        }
        $existingApp = Get-NetTCPConnection -LocalPort $AppPort -ErrorAction SilentlyContinue
        if ($existingApp) {
            foreach ($processId in ($existingApp.OwningProcess | Select-Object -Unique)) {
                $owner = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
                if ($owner -and $owner.Name -match '^java(w)?\.exe$' -and
                    $owner.CommandLine -match 'municipal-complaint-portal.*\.jar') {
                    Write-Host "[Deploy] Stopping the previous native portal process (PID $processId)..."
                    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                } else {
                    $ownerName = if ($owner) { $owner.Name } else { "unknown process" }
                    throw "Port $AppPort is already in use by $ownerName (PID $processId); refusing to stop an unrelated service."
                }
            }
            Start-Sleep -Seconds 2
        }
        Write-Host "[Deploy] Starting fresh Docker container on port $AppPort..."
        Start-PortalContainer -ImageReference $ContainerImage | Out-Null
    } else {
        Write-Host "[Deploy] Starting application on port $AppPort..."
        $javaExe = "C:\Program Files\Java\jdk-17.0.11\bin\java.exe"
        if (-not (Test-Path $javaExe)) {
            $javaExe = (Get-Command java).Source
        }

        $appOutLog = Join-Path $DeployDir "app.stdout.log"
        $appErrLog = Join-Path $DeployDir "app.stderr.log"
        Start-Process -FilePath $javaExe -ArgumentList "-DPORT=$AppPort", "-jar", "`"$ArtifactDest`"" -RedirectStandardOutput $appOutLog -RedirectStandardError $appErrLog -WindowStyle Hidden
    }

    Write-Host "[Deploy] Waiting for application to initialize on port $AppPort..."
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 1
        try {
            $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$AppPort/api/health" -Method Get -UseBasicParsing -TimeoutSec 2
            if ($resp.StatusCode -eq 200) {
                $appReady = $true
                $appHealthResponse = $resp.Content
                Write-Host "[Deploy] Application healthy on port $AppPort - $appHealthResponse"
                break
            }
        } catch {}
    }

    if (-not $appReady) {
        throw "Application failed to become healthy on port $AppPort"
    }
} catch {
    if ($UseDocker) {
        Restore-PreviousContainer -ImageReference $previousImage
    }
    throw
}

# Keep the existing proxy online until the replacement backend passes health checks.
$existingNginx = Get-Process nginx -ErrorAction SilentlyContinue
if ($existingNginx) {
    Write-Host "[Deploy] Stopping existing Nginx processes..."
    Stop-Process -Name nginx -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# Start Nginx reverse proxy
Write-Host "[Deploy] Starting Nginx on port $NginxPort..."
$nginxExe = Join-Path $NginxHome "nginx.exe"
Start-Process -FilePath $nginxExe -ArgumentList "-p `"$NginxHome`"", "-c `"$NginxConf`"" -WindowStyle Hidden
Start-Sleep -Seconds 2

# Verify deployment through Nginx
Write-Host "[Deploy] Verifying deployment through Nginx reverse proxy on port $NginxPort..."
$nginxReady = $false
$proxyHealthResponse = ""
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    try {
        $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$NginxPort/api/health" -Method Get -UseBasicParsing -TimeoutSec 2
        if ($resp.StatusCode -eq 200) {
            $nginxReady = $true
            $proxyHealthResponse = $resp.Content
            Write-Host "[Deploy] Nginx reverse-proxy verification SUCCESSFUL!"
            Write-Host "[Deploy] Response: $proxyHealthResponse"
            break
        }
    } catch {}
}

if (-not $nginxReady) {
    throw "Nginx failed to proxy to application on port $NginxPort"
}

if ($UseDocker) {
    $imageDetails = & docker image inspect --format '{{.Id}}|{{json .RepoDigests}}' $ContainerImage
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect deployed image $ContainerImage"
    }
    $imageParts = ($imageDetails | Out-String).Trim() -split '\|', 2
    $containerId = (& docker inspect --format '{{.Id}}' $ContainerName).Trim()
    $containerStatus = (& docker inspect --format '{{.State.Status}}' $ContainerName).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect deployed container $ContainerName"
    }
    if ($containerStatus -ne "running") {
        throw "Deployed container $ContainerName is not running (status: $containerStatus)"
    }

    $evidenceDirectory = Split-Path -Parent $EvidencePath
    if ($evidenceDirectory -and -not (Test-Path $evidenceDirectory)) {
        New-Item -ItemType Directory -Path $evidenceDirectory -Force | Out-Null
    }
    $evidence = [ordered]@{
        capturedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
        environment = $DeployEnv
        image = $ContainerImage
        imageId = $imageParts[0]
        registryDigests = $imageParts[1]
        containerName = $ContainerName
        containerId = $containerId
        containerStatus = $containerStatus
        hostPort = $AppPort
        healthEndpoint = "http://127.0.0.1:$AppPort/api/health"
        healthResponse = $appHealthResponse
        nginxEndpoint = "http://127.0.0.1:$NginxPort/api/health"
        nginxHealthResponse = $proxyHealthResponse
    }
    $evidenceJson = $evidence | ConvertTo-Json -Depth 4
    [System.IO.File]::WriteAllText(
        $EvidencePath,
        $evidenceJson,
        (New-Object System.Text.UTF8Encoding($false))
    )
    Write-Host "[Deploy] Registry image digest: $($imageParts[1])"
    Write-Host "[Deploy] Deployment evidence written to: $EvidencePath"
}

Write-Host "================================================="
Write-Host " Deployment Complete!"
Write-Host " Deployed Application URL: http://localhost:$NginxPort"
Write-Host " Direct Backend Port     : http://localhost:$AppPort"
Write-Host " Environment             : $DeployEnv"
Write-Host " Status                  : HEALTHY (200 OK)"
Write-Host "================================================="
