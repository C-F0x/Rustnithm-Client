Write-Host ">>> Stage 1: Building Rust Library..." -ForegroundColor Cyan
cargo build --target aarch64-linux-android --release

if ($LASTEXITCODE -eq 0) {
    Write-Host ">>> Stage 2: Copying Shared Library..." -ForegroundColor Cyan
    
    $source = "target\aarch64-linux-android\release\librustnithm.so"
    $destDir = "..\app\src\main\jniLibs\arm64-v8a"
    
    if (!(Test-Path $destDir)) {
        New-Item -ItemType Directory -Force -Path $destDir
    }
    
    Copy-Item -Path $source -Destination "$destDir\librustnithm.so" -Force
    
    Write-Host ">>> Done: Build and Sync Successful." -ForegroundColor Green
} else {
    Write-Host ">>> Error: Build Failed." -ForegroundColor Red
    exit $LASTEXITCODE
}