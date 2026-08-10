param(
    [string]$BaseUrl = "http://localhost:8080"
)

$payload = @{
    text = "Bonjour Hermes"
    deviceId = "smoke-device"
    sessionId = "smoke-session"
    requestId = "smoke-request"
} | ConvertTo-Json

$health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get
if ($health.status -ne "UP") {
    throw "Bridge health check failed: $($health | ConvertTo-Json -Compress)"
}

$response = Invoke-RestMethod `
    -Uri "$BaseUrl/v1/channels/alexa/turn" `
    -Method Post `
    -ContentType "application/json" `
    -Body $payload

if ($response.text -ne "Bien reçu chef") {
    throw "Unexpected bridge response: $($response | ConvertTo-Json -Compress)"
}

Write-Output "Hermes Bridge smoke test passed: $($response.text)"

