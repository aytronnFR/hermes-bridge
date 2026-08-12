param(
  [string]$OutputPath = "hermes-bridge-alexa.zip"
)

$ErrorActionPreference = "Stop"

$sourceDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$outputFile = Join-Path $sourceDirectory $OutputPath
$stagingDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("hermes-bridge-lambda-" + [guid]::NewGuid())

try {
  New-Item -ItemType Directory -Path $stagingDirectory | Out-Null
  Copy-Item (Join-Path $sourceDirectory "index.js") $stagingDirectory
  Copy-Item (Join-Path $sourceDirectory "package.json") $stagingDirectory
  Copy-Item (Join-Path $sourceDirectory "package-lock.json") $stagingDirectory

  npm ci --omit=dev --prefix $stagingDirectory
  if ($LASTEXITCODE -ne 0) {
    throw "npm ci failed while preparing the Lambda package."
  }

  if (Test-Path -LiteralPath $outputFile) {
    Remove-Item -LiteralPath $outputFile -Force
  }
  Compress-Archive -Path (Join-Path $stagingDirectory "*") -DestinationPath $outputFile -CompressionLevel Optimal
  Write-Output "Created $outputFile"
}
finally {
  if (Test-Path -LiteralPath $stagingDirectory) {
    Remove-Item -LiteralPath $stagingDirectory -Recurse -Force
  }
}
