# 构建与验证

## 环境

- JDK 17
- Android SDK Platform 35、Build Tools 35.0.0
- Gradle 8.9（使用仓库 Wrapper）
- 最低 Android API 26；目标 API 34

```sh
./gradlew testDebugUnitTest assembleDebug
./gradlew assembleRelease
```

未配置签名时，Release 构建为未签名产物，不能直接安装；直接安装请使用 GitHub Release 附件中的已签名 APK。Debug 构建使用本机调试证书。

## 发布签名

发布者通过环境变量提供签名资料，不将密钥提交到仓库：

- `STEAMLITE_KEYSTORE`：签名文件路径
- `STEAMLITE_KEYSTORE_PASSWORD`：签名库密码
- `STEAMLITE_KEY_ALIAS`：别名（默认 androiddebugkey）
- `STEAMLITE_KEY_PASSWORD`：私钥密码

1.0 沿用此前测试安装版证书，保证同包名覆盖升级。后续发布必须使用同一私钥；请自行构建者勿用不同证书覆盖官方附件，避免签名不一致。

## 设备验证

JVM 测试包含图片解析、价格限流、消息标识、未读合并、好友菜单互斥及认证方式选择等回归。

设备测试入口：

```sh
./gradlew assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e artworkOnly true org.steamlite.test/org.steamlite.ImageNetworkCheck
```

公开图片检查使用 Steam 公共表情与贴纸，不发送聊天消息。消息、图片上传和语音互通测试应由测试者在已同意测试的会话中手动执行。
