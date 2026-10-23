param(
    [ValidateSet('Status','Install','InstallReleaseSmoke','OpenSmokeIcs','OpenCultureDay','Launch','Stop','Screenshot','Resize','ResetSize','FontScale','PullHoroscopeQa','DumpUI','Tap','Swipe','Back','Logcat')][string]$Action = 'Status',
    [string]$Value,
    [string]$SecondValue
)
$ErrorActionPreference = 'Stop'
$sdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { 'D:\code\Android\Sdk' }
$adbPath = Join-Path $sdkPath 'platform-tools\adb.exe'
$deviceArgs = @('-s', 'emulator-5554')
switch ($Action) {
    'Status' { & $adbPath devices; & $adbPath @deviceArgs shell getprop sys.boot_completed }
    'Install' { & $adbPath @deviceArgs install -r "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk" }
    'InstallReleaseSmoke' { & $adbPath @deviceArgs install -r "$PSScriptRoot\artifacts\qa\release-smoke.apk" }
    'OpenSmokeIcs' { & $adbPath @deviceArgs shell am start -n vn.lichviet.app/.MainActivity -a android.intent.action.VIEW -d content://vn.lichviet.app.fileprovider/ics/release-smoke.ics -t text/calendar }
    'OpenCultureDay' { & $adbPath @deviceArgs shell am start -n vn.lichviet.app/.MainActivity --es selected_date 2026-11-24 }
    'Launch' { & $adbPath @deviceArgs shell am start -n vn.lichviet.app/.MainActivity }
    'Stop' { & $adbPath @deviceArgs shell am force-stop vn.lichviet.app }
    'Screenshot' {
        New-Item -ItemType Directory -Force -Path "$PSScriptRoot\artifacts" | Out-Null
        & $adbPath @deviceArgs shell screencap -p /sdcard/lichviet-screen.png
        $screenshotName = if ($Value) { [System.IO.Path]::GetFileName($Value) } else { 'phone.png' }
        & $adbPath @deviceArgs pull /sdcard/lichviet-screen.png "$PSScriptRoot\artifacts\$screenshotName"
    }
    'Resize' {
        if ($Value -notmatch '^\d{3,4}x\d{3,4}$' -or $SecondValue -notmatch '^\d{2,3}$') { throw 'Supply size and density, e.g. 1600x1000 160' }
        & $adbPath @deviceArgs shell wm size $Value
        & $adbPath @deviceArgs shell wm density $SecondValue
    }
    'ResetSize' { & $adbPath @deviceArgs shell wm size reset; & $adbPath @deviceArgs shell wm density reset }
    'FontScale' {
        if ($Value -notin @('1.0','2.0')) { throw 'Use font scale 1.0 or 2.0' }
        & $adbPath @deviceArgs shell settings put system font_scale $Value
    }
    'PullHoroscopeQa' { & $adbPath @deviceArgs pull /sdcard/Android/data/vn.lichviet.app/files/horoscope-qa "$PSScriptRoot\artifacts" }
    'DumpUI' { & $adbPath @deviceArgs shell uiautomator dump /sdcard/lichviet-ui.xml; & $adbPath @deviceArgs shell cat /sdcard/lichviet-ui.xml }
    'Tap' {
        if ($Value -notmatch '^\d{1,4}$' -or $SecondValue -notmatch '^\d{1,4}$') { throw 'Supply x y coordinates' }
        & $adbPath @deviceArgs shell input tap $Value $SecondValue
    }
    'Swipe' {
        if ($Value -notmatch '^\d{1,4},\d{1,4},\d{1,4},\d{1,4}$') { throw 'Supply x1,y1,x2,y2 coordinates' }
        $swipePoints = $Value.Split(',')
        & $adbPath @deviceArgs shell input swipe @swipePoints 400
    }
    'Back' { & $adbPath @deviceArgs shell input keyevent 4 }
    'Logcat' { & $adbPath @deviceArgs logcat -d -t 150 'AndroidRuntime:E' '*:S' }
}
exit $LASTEXITCODE
