param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Tasks)
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) {
    foreach ($candidate in @('D:\code\Android\Studio\jbr', 'C:\Program Files\Java\jdk-21.0.12')) {
        if (Test-Path -LiteralPath "$candidate\bin\java.exe") { $env:JAVA_HOME = $candidate; break }
    }
}
if (-not $Tasks) { $Tasks = @(':core:test', ':app:testDebugUnitTest', ':app:assembleDebug', ':app:lintDebug') }
& "$PSScriptRoot\gradlew.bat" @Tasks --console=plain
exit $LASTEXITCODE
