param(
    [string]$JavaHome = $env:JAVA_HOME,
    [ValidateSet('Unit', 'Build', 'Verify')]
    [string[]]$Phase = @('Unit', 'Build', 'Verify'),
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
if ($JavaHome) {
    $java = Join-Path $JavaHome 'bin/java.exe'
    if (!(Test-Path -LiteralPath $java)) { throw "Java was not found at $java." }
} else {
    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $JavaHome = Split-Path (Split-Path $java)
}
$javaSettings = & {
    # Java prints its version and settings to stderr even when it succeeds.
    $ErrorActionPreference = 'Continue'
    $settings = & $java -XshowSettings:properties -version 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) { throw "Could not read the Java version at $java." }
    $settings
}
if ($javaSettings -notmatch '(?m)^\s*java.specification.version = (17|21)\s*$') {
    throw 'Use JDK 17 or 21 for this Gradle wrapper.'
}

$savedJavaHome = $env:JAVA_HOME
$env:JAVA_HOME = $JavaHome
Push-Location (Join-Path $PSScriptRoot '..')
try {
    $gradleOptions = @(
        '--no-daemon'
        '--max-workers=1'
        '-Pkotlin.compiler.execution.strategy=in-process'
    )
    if ($Offline) { $gradleOptions += '--offline' }
    foreach ($step in $Phase) {
        $minimumGiB = if ($step -eq 'Build') { 2.2 } else { 2.5 }
        $heapMiB = if ($step -eq 'Build') { 768 } else { 1024 }
        $freeGiB = (Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1MB
        if ($freeGiB -lt $minimumGiB) {
            throw ('Only {0:N2} GiB RAM is available. Run {1} again when at least {2:N1} GiB is available.' -f $freeGiB, $step, $minimumGiB)
        }
        if (Get-Process -Name 'qemu-system-*', 'emulator' -ErrorAction SilentlyContinue) {
            throw 'Run this local check after the Android emulator has stopped.'
        }
        [string[]]$tasks = switch ($step) {
            'Unit' { 'testDebugUnitTest' }
            'Build' { 'assembleDebug' }
            'Verify' { 'assembleDebugAndroidTest'; 'lintDebug' }
        }
        $javaOptions = "-Dorg.gradle.jvmargs=-Xmx${heapMiB}m -XX:MaxMetaspaceSize=480m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8"
        & .\gradlew.bat @gradleOptions $javaOptions @tasks
        if ($LASTEXITCODE -ne 0) { throw "Gradle phase $step failed with exit code $LASTEXITCODE." }
    }
} finally {
    Pop-Location
    $env:JAVA_HOME = $savedJavaHome
}
