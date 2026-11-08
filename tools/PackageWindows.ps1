[CmdletBinding()]
param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$OutputDirectory = 'dist',
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not $JdkHome) { throw 'Set JAVA_HOME or pass -JdkHome (Windows x64 JDK 17 with jpackage/jlink).' }
$JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
foreach ($tool in @('java.exe','jpackage.exe','jlink.exe')) {
    if (-not (Test-Path -LiteralPath (Join-Path $JdkHome "bin/$tool"))) { throw "Required tool missing: $tool. No tools are installed automatically." }
}
$release = Get-Content -LiteralPath (Join-Path $JdkHome 'release') -Raw
if ($release -notmatch 'OS_ARCH="(x86_64|amd64)"' -or $release -notmatch 'OS_NAME="Windows"') {
    throw 'A Windows x64 JDK is required.'
}
$version = ((Get-Content (Join-Path $repoRoot 'gradle.properties') | Where-Object { $_ -match '^projectVersion=' }) -split '=',2)[1].Trim()
$outputRoot = if ([IO.Path]::IsPathRooted($OutputDirectory)) { [IO.Path]::GetFullPath($OutputDirectory) } else { [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDirectory)) }
$image = Join-Path $outputRoot 'CampusRunner'
$zip = Join-Path $outputRoot "CampusRunner-Windows-v$version.zip"
if ((Test-Path -LiteralPath $image) -or (Test-Path -LiteralPath $zip)) {
    throw 'Output already exists. Use a fresh -OutputDirectory; existing packages are never deleted automatically.'
}
$stage = Join-Path $repoRoot ("build/windows-package/" + [Guid]::NewGuid().ToString('N'))
$inputDirectory = Join-Path $stage 'input'
New-Item -ItemType Directory -Force -Path $inputDirectory,$outputRoot | Out-Null
$oldJava = $env:JAVA_HOME
Push-Location $repoRoot
try {
    $env:JAVA_HOME = $JdkHome
    if (-not $SkipBuild) {
        & .\gradlew.bat -I tools/ExportRuntimeLicenses.gradle build lwjgl3:jar :lwjgl3:exportRuntimeLicenses
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
    }
    $jar = Join-Path $repoRoot "lwjgl3/build/libs/Project2-$version.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw 'Executable JAR not found. Run .\gradlew.bat build lwjgl3:jar first.' }
    Copy-Item -LiteralPath $jar -Destination (Join-Path $inputDirectory 'CampusRunner.jar')
    # Explicit modules match local jdeps output, including LWJGL Unsafe and StartupHelper management APIs.
    # Keep legal notices and java.exe for independent bundled-runtime verification.
    $runtime = Join-Path $stage 'runtime'
    & (Join-Path $JdkHome 'bin/jlink.exe') --add-modules java.base,java.desktop,java.management,jdk.unsupported --strip-debug --no-header-files --no-man-pages --output $runtime
    if ($LASTEXITCODE -ne 0) { throw 'jlink failed.' }
    & (Join-Path $JdkHome 'bin/jpackage.exe') --type app-image --name CampusRunner --app-version $version --vendor 'Campus Runner' --description '8 semesters to graduation' --input $inputDirectory --main-jar CampusRunner.jar --main-class io.jbnu.test.lwjgl3.Lwjgl3Launcher --runtime-image $runtime --dest $outputRoot --java-options '-Dfile.encoding=UTF-8'
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }
    $licenses = Join-Path $image 'licenses'
    New-Item -ItemType Directory -Path $licenses | Out-Null
    Copy-Item -Path (Join-Path $repoRoot 'packaging/licenses/*') -Destination $licenses
    Copy-Item -LiteralPath (Join-Path $repoRoot 'packaging/THIRD_PARTY_NOTICES.md') -Destination (Join-Path $licenses 'THIRD_PARTY_NOTICES.md')
    foreach ($notice in @('FONT_PROVENANCE.md','IMAGE_PROVENANCE.md','ASSET_INVENTORY.csv','DEPENDENCY_INVENTORY.json','NATIVE_INVENTORY.json','LGPL_REPLACEMENT.md')) {
        Copy-Item -LiteralPath (Join-Path $repoRoot "packaging/$notice") -Destination (Join-Path $licenses $notice)
    }
    Copy-Item -LiteralPath (Join-Path $repoRoot 'packaging/sources') -Destination (Join-Path $licenses 'sources') -Recurse
    Copy-Item -LiteralPath (Join-Path $repoRoot 'docs/CODE_LICENSE_POLICY.md') -Destination (Join-Path $licenses 'SOURCE_CODE_POLICY.md')
    Copy-Item -LiteralPath (Join-Path $repoRoot 'assets/fonts/title-ko-OFL.txt'),(Join-Path $repoRoot 'assets/fonts/vacation-ko-OFL.txt'),(Join-Path $repoRoot 'assets/fonts/ending-ko-OFL.txt') -Destination $licenses
    Copy-Item -LiteralPath (Join-Path $JdkHome 'LICENSE') -Destination (Join-Path $licenses 'JAVA-RUNTIME-LICENSE.txt')
    Copy-Item -LiteralPath (Join-Path $repoRoot 'packaging/WINDOWS_README.txt') -Destination (Join-Path $image 'START-HERE.txt')
    # Whitelist: app-image only. No repository/test/docs/build trees enter the ZIP.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::CreateFromDirectory($image,$zip,[IO.Compression.CompressionLevel]::Optimal,$true)
    $hash = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText("$zip.sha256", "$hash  $([IO.Path]::GetFileName($zip))`n", [Text.Encoding]::ASCII)
    Write-Output "EXE: $(Join-Path $image 'CampusRunner.exe')"
    Write-Output "ZIP: $zip"
    Write-Output "SHA256: $hash"
    Write-Output 'Internal validation candidate: source policy recorded; native-source and image-record checks remain. See THIRD_PARTY_NOTICES.md.'
} finally {
    $env:JAVA_HOME = $oldJava
    Pop-Location
}
