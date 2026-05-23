$ErrorActionPreference = "Stop"

$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$jdkBin = "C:\Program Files\Java\jdk-24\bin"
$javafxSdk = "C:\javafx-sdk-24.0.1"

$build = Join-Path $repo "build"
$classes = Join-Path $build "classes"
$input = Join-Path $build "jpackage-input"
$dist = Join-Path $build "dist"
$sources = Join-Path $build "sources.txt"

if (Test-Path $build) {
    Remove-Item -LiteralPath $build -Recurse -Force
}

New-Item -ItemType Directory -Path $classes, $input, $dist | Out-Null

Get-ChildItem -Path (Join-Path $repo "src"), (Join-Path $repo "UTIL") -Recurse -Filter "*.java" |
    ForEach-Object { $_.FullName.Substring($repo.Length + 1).Replace("\", "/") } |
    Set-Content -Path $sources -Encoding ASCII

Push-Location $repo
try {
    & (Join-Path $jdkBin "javac.exe") -encoding UTF-8 `
        -cp "lib\mysql-connector-java-9.3.0.jar;$javafxSdk\lib\*" `
        -d $classes `
        "@$sources"

    New-Item -ItemType Directory -Path (Join-Path $classes "erp\view") -Force | Out-Null
    Copy-Item -Path "src\erp\view\*" -Destination (Join-Path $classes "erp\view") -Recurse -Force

    & (Join-Path $jdkBin "jar.exe") --create `
        --file (Join-Path $input "ERP-2.0.jar") `
        --main-class erp.application.Launcher `
        -C $classes .

    Copy-Item -Path "lib\mysql-connector-java-9.3.0.jar" -Destination $input -Force
    Copy-Item -Path "$javafxSdk\lib\*.jar" -Destination $input -Force
    Copy-Item -Path "$javafxSdk\bin\*.dll" -Destination $input -Force

    & (Join-Path $jdkBin "jpackage.exe") --type app-image `
        --dest $dist `
        --name "ERP 2.0" `
        --input $input `
        --main-jar ERP-2.0.jar `
        --main-class erp.application.Launcher `
        --app-version 2.0.0 `
        --vendor "Victor Hugo" `
        --java-options '-Djava.library.path=$APPDIR'

    Compress-Archive -Path (Join-Path $dist "ERP 2.0") `
        -DestinationPath (Join-Path $dist "ERP-2.0-portable.zip") `
        -Force
} finally {
    Pop-Location
}

Write-Host "Portatil gerado em: $dist"
