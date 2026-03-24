$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$logsDir = Join-Path $root "logs"
$skipBuild = $false

function Test-PortOpen {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMs = 2000
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $asyncResult = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $asyncResult.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }

        $client.EndConnect($asyncResult)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

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

function Start-RabbitMq {
    if (Test-PortOpen -HostName "localhost" -Port 5672) {
        Write-Host "RabbitMQ is already reachable on port 5672."
        return
    }

    $composeCommand = Get-DockerComposeCommand
    if ($null -eq $composeCommand) {
        throw "RabbitMQ is not reachable on port 5672 and Docker Compose was not found. Start RabbitMQ manually or install Docker Desktop."
    }

    Write-Host "Starting RabbitMQ with Docker Compose..."
    Invoke-DockerCompose -ComposeCommand $composeCommand -Arguments @("-f", (Join-Path $root "docker-compose.yml"), "up", "-d", "rabbitmq")
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortOpen -HostName "localhost" -Port 5672) {
            Write-Host "RabbitMQ is UP"
            return
        }
        Start-Sleep -Seconds 3
    }

    throw "Timed out waiting for RabbitMQ on localhost:5672"
}

function Ensure-LocalJwtKeys {
    $privateKey = [Environment]::GetEnvironmentVariable("JWT_PRIVATE_KEY", "Process")
    $publicKey = [Environment]::GetEnvironmentVariable("JWT_PUBLIC_KEY", "Process")

    $privateMissing = [string]::IsNullOrWhiteSpace($privateKey) -or $privateKey -match '^replace_with_'
    $publicMissing = [string]::IsNullOrWhiteSpace($publicKey) -or $publicKey -match '^replace_with_'

    if (-not $privateMissing -and -not $publicMissing) {
        return
    }

    Write-Host "JWT RSA keys are missing in .env. Generating temporary local-development keys for this run..."

    $tempDir = Join-Path $env:TEMP ("jwt-keygen-" + [guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    $javaFile = Join-Path $tempDir "GenerateJwtKeys.java"

    @'
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class GenerateJwtKeys {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        System.out.println(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
    }
}
'@ | Set-Content -Path $javaFile -Encoding ASCII

    Push-Location $tempDir
    try {
        & javac GenerateJwtKeys.java
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to compile JWT key generator."
        }

        $keys = & java GenerateJwtKeys
        if ($LASTEXITCODE -ne 0 -or $keys.Count -lt 2) {
            throw "Failed to generate JWT RSA keys."
        }

        [Environment]::SetEnvironmentVariable("JWT_PRIVATE_KEY", $keys[0], "Process")
        [Environment]::SetEnvironmentVariable("JWT_PUBLIC_KEY", $keys[1], "Process")
    } finally {
        Pop-Location
        Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if ($args -contains "--skip-build") {
    $skipBuild = $true
}

$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            return
        }

        $key = $parts[0].Trim()
        $value = $parts[1]
        if (-not [string]::IsNullOrWhiteSpace($key) -and -not [Environment]::GetEnvironmentVariable($key, "Process")) {
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set."
}

$mvnCmd = $null
if ($env:MAVEN_HOME) {
    $mvnCmd = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
} else {
    $mvn = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($null -eq $mvn) {
        throw "MAVEN_HOME is not set and mvn.cmd was not found on PATH."
    }
    $mvnCmd = $mvn.Source
}

if (-not (Test-Path $mvnCmd)) {
    throw "Maven command not found at $mvnCmd"
}

$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
if ($env:MAVEN_HOME) {
    $env:Path = "$($env:MAVEN_HOME)\bin;$env:Path"
}

Ensure-LocalJwtKeys

$requiredVars = @(
    "AUTH_DB_PASSWORD",
    "PRODUCT_DB_PASSWORD",
    "INVENTORY_DB_PASSWORD",
    "ORDER_DB_PASSWORD"
)

foreach ($requiredVar in $requiredVars) {
    $value = [Environment]::GetEnvironmentVariable($requiredVar, "Process")
    if (-not $value) {
        throw "Missing required environment variable: $requiredVar"
    }
    if ($value -match '^replace_with_') {
        throw "Environment variable $requiredVar is still using a placeholder value in .env. Replace it with the real secret/password first."
    }
}

New-Item -ItemType Directory -Path $logsDir -Force | Out-Null

Start-RabbitMq

if (-not $skipBuild) {
    Write-Host "Building all modules from the root aggregator pom..."
    & $mvnCmd -f (Join-Path $root "pom.xml") clean install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

$services = @(
    @{ Name = "config-server"; Module = "config-server"; Health = "http://localhost:8888/actuator/health" },
    @{ Name = "service-registry"; Module = "service-registry"; Health = "http://localhost:8761/actuator/health" },
    @{ Name = "auth-service"; Module = "auth-service"; Health = "http://localhost:8081/actuator/health" },
    @{ Name = "product-service"; Module = "product-service"; Health = "http://localhost:8082/actuator/health" },
    @{ Name = "inventory-service"; Module = "inventory-service"; Health = "http://localhost:8084/actuator/health" },
    @{ Name = "order-service"; Module = "order-service"; Health = "http://localhost:8083/actuator/health" },
    @{ Name = "api-gateway"; Module = "api-gateway"; Health = "http://localhost:8080/actuator/health" }
)

foreach ($service in $services) {
    $modulePath = Join-Path $root $service.Module
    $outLog = Join-Path $logsDir "$($service.Name).out.log"
    $errLog = Join-Path $logsDir "$($service.Name).err.log"

    Write-Host "Starting $($service.Name)..."
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "cd /d `"$modulePath`" && call `"$mvnCmd`" spring-boot:run 1>`"$outLog`" 2>`"$errLog`"" -WindowStyle Normal

    $deadline = (Get-Date).AddSeconds(120)
    $isUp = $false
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Uri $service.Health -TimeoutSec 5
            if ($response.status -eq "UP") {
                $isUp = $true
                break
            }
        } catch {
        }
        Start-Sleep -Seconds 3
    }

    if (-not $isUp) {
        throw "Timed out waiting for $($service.Name) health at $($service.Health)"
    }

    Write-Host "$($service.Name) is UP"
}

Write-Host ""
Write-Host "All services are running."
Write-Host "Logs directory: $logsDir"
