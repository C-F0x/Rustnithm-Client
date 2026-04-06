$OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

$signJsonPath = "../sign.json"
$versionPropPath = "./version.properties"

if (Test-Path $versionPropPath) {
    $props = ConvertFrom-StringData (Get-Content $versionPropPath -Raw)
    $currMajor = $props.ver_Major
    $currMinor = $props.ver_Minor
} else {
    $currMajor = "0"
    $currMinor = "0"
}

Write-Host "--- Rustnithm Build Tool ---" -ForegroundColor Cyan

$newMajor = Read-Host "Enter new ver_Major (Current: $currMajor, press Enter to keep)"
if (-not $newMajor) { $newMajor = $currMajor }
if ($newMajor -notmatch '^[0-9]$') { Write-Host "Error: Major must be a single digit (0-9)"; exit 1 }

$newMinor = Read-Host "Enter new ver_Minor (Current: $currMinor, press Enter to keep)"
if (-not $newMinor) { $newMinor = $currMinor }
if ($newMinor -notmatch '^[0-9]$') { Write-Host "Error: Minor must be a single digit (0-9)"; exit 1 }

"ver_Major=$newMajor`nver_Minor=$newMinor" | Out-File -FilePath $versionPropPath -Encoding ascii

if (-not (Test-Path $signJsonPath)) {
    Write-Host "Error: External signing file not found at $signJsonPath" -ForegroundColor Red
    exit 1
}
$sign = Get-Content $signJsonPath | ConvertFrom-Json
$dateDotTag = Get-Date -Format "yy.MM"

function Get-Masked ([string]$str) {
    if ([string]::IsNullOrWhiteSpace($str)) { return "N/A" }
    $leaf = Split-Path $str -Leaf
    if ($leaf.Length -le 1) { return "*" }
    return $leaf.Substring(0, 1) + ("*" * ($leaf.Length - 1))
}

Clear-Host
Write-Host "================ BUILD PREVIEW ================" -ForegroundColor Yellow
Write-Host "Build Type    : Release"
Write-Host "ABI Filter    : arm64-v8a"
Write-Host "Target Version: $dateDotTag.$newMajor$newMinor"
Write-Host "-----------------------------------------------"
Write-Host "Keystore File : $(Get-Masked $sign.storeFile)"
Write-Host "Store Password: $(Get-Masked $sign.storePassword)"
Write-Host "Key Alias     : $(Get-Masked $sign.keyAlias)"
Write-Host "Key Password  : $(Get-Masked $sign.keyPassword)"
Write-Host "==============================================="
Write-Host ""
Pause -Message "Verify the info above and press [Enter] to start building..."

Write-Host "Starting Gradle build process..." -ForegroundColor Cyan

$jsonDir = Split-Path (Resolve-Path $signJsonPath) -Parent
$jksPath = Join-Path $jsonDir $sign.storeFile

.\gradlew.bat assembleRelease `
    "-PverMajor=$newMajor" `
    "-PverMinor=$newMinor" `
    "-PksFile=$jksPath" `
    "-PksPwd=$($sign.storePassword)" `
    "-Palias=$($sign.keyAlias)" `
    "-PkeyPwd=$($sign.keyPassword)"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild Successful!" -ForegroundColor Green

    $apkDir = Join-Path $PSScriptRoot "app/build/outputs/apk/release"

    if (Test-Path $apkDir) {
        Write-Host "Opening output directory: $apkDir" -ForegroundColor Gray
        explorer $apkDir
    } else {
        Write-Host "Warning: Standard path not found, searching for APK..." -ForegroundColor Yellow
        $altPath = Join-Path $PSScriptRoot "app/build/outputs/apk"
        if (Test-Path $altPath) { explorer $altPath }
    }
}