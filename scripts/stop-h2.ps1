# Stop H2 server started by start-h2-and-app.ps1
# Usage: Run from project root in PowerShell
#   ./scripts\stop-h2.ps1

$pidFile = Join-Path $PSScriptRoot '..\h2-server.pid' | Resolve-Path -Relative
if (-not (Test-Path $pidFile)) {
    Write-Host "PID file not found. No server to stop (or started externally)."
    exit 0
}

$pid = Get-Content -Path $pidFile
try {
    Stop-Process -Id $pid -Force -ErrorAction Stop
    Remove-Item -Path $pidFile -ErrorAction SilentlyContinue
    Write-Host "Stopped H2 server (PID $pid)."
} catch {
    Write-Warning "Failed to stop process with PID $pid. It may have exited already. Removing PID file."
    Remove-Item -Path $pidFile -ErrorAction SilentlyContinue
}
