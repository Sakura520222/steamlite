param(
    [ValidateRange(1,65535)][int]$ProxyPort = 7890,
    [string]$Serial = '127.0.0.1:16384',
    [string]$AdbPath = 'adb',
    [switch]$Disable
)
$ErrorActionPreference = 'Stop'
if($AdbPath -eq 'adb') {
    $workspaceAdb = Join-Path $PSScriptRoot '../../../work/toolchain/android-sdk/platform-tools/adb.exe'
    if(Test-Path -LiteralPath $workspaceAdb) { $AdbPath = (Resolve-Path -LiteralPath $workspaceAdb).Path }
}
$proxyAddress = "127.0.0.1:$ProxyPort"
if($Disable) {
    $currentProxy = (& $AdbPath -s $Serial shell settings get global http_proxy).Trim()
    if($currentProxy -eq $proxyAddress) {
        & $AdbPath -s $Serial shell settings delete global http_proxy
        if($LASTEXITCODE -ne 0) { throw '无法清除模拟器代理' }
    } else { Write-Output '模拟器代理已不同于本脚本地址，保留当前设置。' }
    & $AdbPath -s $Serial reverse --remove "tcp:$ProxyPort"
    Write-Output '已移除本脚本的 ADB 转发。'
} else {
    & $AdbPath connect $Serial
    if($LASTEXITCODE -ne 0) { throw '无法连接模拟器 ADB' }
    & $AdbPath -s $Serial reverse "tcp:$ProxyPort" "tcp:$ProxyPort"
    if($LASTEXITCODE -ne 0) { throw '无法建立 ADB 端口转发' }
    & $AdbPath -s $Serial shell settings put global http_proxy $proxyAddress
    if($LASTEXITCODE -ne 0) { throw '无法设置模拟器代理' }
    Write-Output "模拟器使用电脑代理端口 $ProxyPort。无需更改电脑代理配置或开放局域网监听。"
}
