param(
  [string]$AndroidStudioHome = "C:\Program Files\Android\Android Studio",
  [string]$OutputDir = "C:\Github\ai-commit-message\outputs"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BuildDir = Join-Path $ProjectRoot "build"
$ClassesDir = Join-Path $BuildDir "classes"
$PackageDir = Join-Path $BuildDir "package\ai-commit-message-plugin"
$JarPath = Join-Path $PackageDir "lib\ai-commit-message-plugin.jar"
$ZipPath = Join-Path $OutputDir "ai-commit-message-plugin-0.3.6.zip"

Remove-Item -LiteralPath $BuildDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $ClassesDir, (Split-Path -Parent $JarPath), $OutputDir | Out-Null

$LibJars = @((Get-ChildItem -Path (Join-Path $AndroidStudioHome "lib") -Filter "*.jar").FullName)
$LibJars += @((Get-ChildItem -Path (Join-Path $AndroidStudioHome "plugins\vcs-git\lib") -Filter "*.jar").FullName)
$ClassPath = ($LibJars -join ";")
$Sources = Get-ChildItem -Path (Join-Path $ProjectRoot "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

$JavacArgs = Join-Path $BuildDir "javac.args"
function Quote-Arg([string]$Value) {
  '"' + $Value.Replace('\', '\\').Replace('"', '\"') + '"'
}

$ArgsLines = @(
  "-encoding", "UTF-8",
  "-source", "21",
  "-target", "21",
  "-classpath", (Quote-Arg $ClassPath),
  "-d", (Quote-Arg $ClassesDir)
)
$ArgsLines += ($Sources | ForEach-Object { Quote-Arg $_ })
[System.IO.File]::WriteAllLines($JavacArgs, $ArgsLines, [System.Text.UTF8Encoding]::new($false))

& javac "@$JavacArgs"
if ($LASTEXITCODE -ne 0) {
  throw "javac failed with exit code $LASTEXITCODE"
}

Copy-Item -Path (Join-Path $ProjectRoot "src\main\resources\META-INF") -Destination $ClassesDir -Recurse

Push-Location $ClassesDir
try {
  & jar cf $JarPath .
  if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
  }
} finally {
  Pop-Location
}

Remove-Item -LiteralPath $ZipPath -Force -ErrorAction SilentlyContinue
Push-Location (Join-Path $BuildDir "package")
try {
  & jar cMf $ZipPath "ai-commit-message-plugin"
  if ($LASTEXITCODE -ne 0) {
    throw "plugin zip packaging failed with exit code $LASTEXITCODE"
  }
} finally {
  Pop-Location
}

Write-Host "Built plugin ZIP: $ZipPath"







