$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Get-DockerComposeCommand {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($null -ne $docker) {
        try {
            & $docker.Source compose version *> $null
            if ($LASTEXITCODE -eq 0) {
                return @($docker.Source, "compose")
            }
        } catch {
        }
    }

    $dockerCompose = Get-Command docker-compose -ErrorAction SilentlyContinue
    if ($null -ne $dockerCompose) {
        return @($dockerCompose.Source)
    }

    return $null
}

function Invoke-DockerCompose {
    param(
        [string[]]$ComposeCommand,
        [string[]]$Arguments
    )

    if ($ComposeCommand.Length -gt 1) {
        & $ComposeCommand[0] $ComposeCommand[1..($ComposeCommand.Length - 1)] @Arguments
        return
    }

    & $ComposeCommand[0] @Arguments
}

function Stop-ServiceProcess {
    param(
        [string]$ServiceName,
        [string]$ModuleName
    )

    $modulePath = Join-Path $root $ModuleName
    $pathRegex = [regex]::Escape($modulePath)
    $processes = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match $pathRegex }

    if (-not $processes) {
        Write-Host "$ServiceName is not running"
        return
    }

    Write-Host "Stopping $ServiceName..."
    foreach ($process in $processes) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

Stop-ServiceProcess -ServiceName "api-gateway" -ModuleName "api-gateway"
Stop-ServiceProcess -ServiceName "order-service" -ModuleName "order-service"
Stop-ServiceProcess -ServiceName "inventory-service" -ModuleName "inventory-service"
Stop-ServiceProcess -ServiceName "product-service" -ModuleName "product-service"
Stop-ServiceProcess -ServiceName "auth-service" -ModuleName "auth-service"
Stop-ServiceProcess -ServiceName "service-registry" -ModuleName "service-registry"
Stop-ServiceProcess -ServiceName "config-server" -ModuleName "config-server"

$composeCommand = Get-DockerComposeCommand
if ($null -ne $composeCommand) {
    Write-Host "Stopping RabbitMQ..."
    Invoke-DockerCompose -ComposeCommand $composeCommand -Arguments @("-f", (Join-Path $root "docker-compose.yml"), "stop", "rabbitmq") *> $null
}

Write-Host ""
Write-Host "Stop requests sent."
