# Start H2 TCP server (if not present) and then optionally start the Spring Boot app
# Usage: Run from project root in PowerShell (Windows PowerShell 5.1)
#   ./scripts\start-h2-and-app.ps1            (starts H2 and the app)
#   ./scripts\start-h2-and-app.ps1 -StartApp:$false   (starts only H2)

param(
    [int]$H2Port = 9092,
    [string]$H2Version = '2.2.224',
    [string]$BaseDir = './data',
    [switch]$StartApp = $true
)

# Resolve paths
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
$libDir = Join-Path $projectRoot 'lib'
if (-not (Test-Path -Path $libDir)) { New-Item -ItemType Directory -Path $libDir | Out-Null }

$h2Jar = Join-Path $libDir "h2-$H2Version.jar"
if (-not (Test-Path -Path $h2Jar)) {
    Write-Host "Downloading H2 $H2Version..."
    $url = "https://repo1.maven.org/maven2/com/h2database/h2/$H2Version/h2-$H2Version.jar"
    try {
        Invoke-WebRequest -Uri $url -OutFile $h2Jar -UseBasicParsing -ErrorAction Stop
        Write-Host "Downloaded $h2Jar"
    } catch {
        Write-Error "Failed to download H2 jar from $url. Please download manually and place it at $h2Jar"
        exit 1
    }
} else {
    Write-Host "Found H2 jar: $h2Jar"
}

# Function to test TCP port
function Test-Port($host, $port) {
    try {
        $result = Test-NetConnection -ComputerName $host -Port $port -WarningAction SilentlyContinue
        return $result.TcpTestSucceeded
    } catch {
        return $false
    }
}

$host = 'localhost'
$pidFile = Join-Path $projectRoot 'h2-server.pid'

if (-not (Test-Port $host $H2Port)) {
    Write-Host "Starting H2 TCP server on port $H2Port..."
    $argList = @('-cp', $h2Jar, 'org.h2.tools.Server', '-tcp', '-tcpPort', [string]$H2Port, '-tcpAllowOthers', '-baseDir', $BaseDir)
    $proc = Start-Process -FilePath 'java' -ArgumentList $argList -PassThru -WindowStyle Hidden
    # Save PID for stop script
    Set-Content -Path $pidFile -Value $proc.Id
    Write-Host "H2 server started (PID $($proc.Id)). Waiting for port to be available..."
    $tries = 30
    while ($tries -gt 0) {
        Start-Sleep -Seconds 1
        if (Test-Port $host $H2Port) { break }
        $tries--
    }
    if ($tries -le 0) {
        Write-Error "H2 server did not start within expected time. Check logs or start the server manually. PID: $($proc.Id)"
        exit 1
    }
    Write-Host "H2 server is listening on $host:$H2Port"
} else {
    Write-Host "H2 TCP server already listening on $host:$H2Port"
}

if ($StartApp) {
    # Start the Spring Boot application using Maven
    Write-Host "Starting Spring Boot application (mvn -DskipTests spring-boot:run)..."
    # Use Start-Process to run mvn in this shell window so logs show up; do not detach
    $mvnArgs = @('-DskipTests', 'spring-boot:run')
    Start-Process -FilePath 'mvn' -ArgumentList $mvnArgs -NoNewWindow -Wait
    Write-Host "Spring Boot process has exited."
} else {
    Write-Host "Skipping starting the Spring Boot app (StartApp=false)."
}

Write-Host "Done. To stop the H2 server started by this script, run ./scripts/stop-h2.ps1"
