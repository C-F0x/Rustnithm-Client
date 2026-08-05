$ErrorActionPreference = "Stop"

Write-Host ">>> Stage 0: Resolving NDK linker..." -ForegroundColor Cyan

# Resolve the SDK directory from ../local.properties — the same source Gradle
# uses (sdk.dir), so moving the SDK no longer requires editing anything here.
$sdkDir = $null
$localPropsPath = Join-Path $PSScriptRoot "..\local.properties"
if (Test-Path $localPropsPath) {
    $props = ConvertFrom-StringData (Get-Content $localPropsPath -Raw)
    if ($props["sdk.dir"]) {
        # sdk.dir uses properties-file escaping: D\:\\DevHub\\AndroidSdk
        $sdkDir = $props["sdk.dir"].Replace('\\', '\').Replace('\:', ':')
    }
}

if (-not $sdkDir) {
    Write-Host ">>> Warning: sdk.dir not found in ../local.properties; falling back to .cargo/config.toml linker" -ForegroundColor Yellow
} else {
    # NDK version: $env:RUSTNITHM_NDK_VERSION overrides; else pinned 29.0.14206865; else newest installed
    $ndkVersion = if ($env:RUSTNITHM_NDK_VERSION) { $env:RUSTNITHM_NDK_VERSION } else { "29.0.14206865" }
    $ndkRoot = Join-Path $sdkDir "ndk\$ndkVersion"

    if (-not (Test-Path $ndkRoot)) {
        $latest = Get-ChildItem (Join-Path $sdkDir "ndk") -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | Select-Object -First 1
        if ($latest) {
            Write-Host ">>> NDK $ndkVersion not found, using newest installed: $($latest.Name)" -ForegroundColor Yellow
            $ndkRoot = $latest.FullName
        }
    }

    if (Test-Path $ndkRoot) {
        $linker = Join-Path $ndkRoot "toolchains\llvm\prebuilt\windows-x86_64\bin\aarch64-linux-android33-clang.cmd"
        if (Test-Path $linker) {
            $env:CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER = $linker
            Write-Host ">>> Linker: $linker" -ForegroundColor Gray
        } else {
            Write-Host ">>> Warning: linker not found at $linker; falling back to .cargo/config.toml" -ForegroundColor Yellow
        }
    } else {
        Write-Host ">>> Warning: no NDK under $sdkDir; falling back to .cargo/config.toml linker" -ForegroundColor Yellow
    }
}

Write-Host ">>> Stage 1: Building Rust Library..." -ForegroundColor Cyan

cargo build --target aarch64-linux-android --release

if ($LASTEXITCODE -eq 0) {
    Write-Host ">>> Stage 2: Copying Shared Library..." -ForegroundColor Cyan

    $source = Join-Path $PSScriptRoot "target\aarch64-linux-android\release\librustnithm.so"
    $destDir = Join-Path $PSScriptRoot "..\app\src\main\jniLibs\arm64-v8a"

    if (!(Test-Path $destDir)) {
        New-Item -ItemType Directory -Force -Path $destDir
    }

    Copy-Item -Path $source -Destination (Join-Path $destDir "librustnithm.so") -Force

    Write-Host ">>> Done: Build and Sync Successful." -ForegroundColor Green
} else {
    Write-Host ">>> Error: Build Failed." -ForegroundColor Red
    exit $LASTEXITCODE
}
