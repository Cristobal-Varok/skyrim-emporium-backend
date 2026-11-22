$ErrorActionPreference = "Stop"

$secret = [Environment]::GetEnvironmentVariable("JWT_SECRET", "User")
if ([string]::IsNullOrWhiteSpace($secret)) {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }

    $secret = [Convert]::ToBase64String($bytes)
    [Environment]::SetEnvironmentVariable("JWT_SECRET", $secret, "User")
}

$env:JWT_SECRET = $secret
Set-Location $PSScriptRoot
& .\mvnw.cmd spring-boot:run
exit $LASTEXITCODE
