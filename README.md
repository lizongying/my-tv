# 看电视 · KanDianshi

基于 [lizongying/my-tv](https://github.com/lizongying/my-tv) 的 Android 电视直播播放器分支，当前版本 1.9.0。保留遥控器操作界面，加入公开节目单、空闲维护、线路检测、可恢复的广告屏蔽和真实台标。

## 功能

- 内置央视、卫视及地方台公开节目单，支持 M3U、M3U8 和 TXT 导入、订阅更新与备用线路。
- 从公开电视台网页、播放器脚本、GET JSON 接口及 GitHub/GitLab/Jihulab 节目单目录发现候选；支持有上限的动态网页解析，实际媒体检查后加入。
- 停止播放后自动安排后台维护；观看优先，任务有时间、请求次数及存储上限。Android 待机、省电及强制停止会推迟任务。
- 视频画面巡检识别疑似无台标纯广告；普通电视台插播广告保留。两轮新证据确认后可自动屏蔽，并提供查看与解除入口。模型可能误判。
- 局域网临时扫码配置 API、密钥与订阅。入口默认关闭，随机配对令牌 10 分钟过期；密钥保存在设备中，不包含在源码和 APK 内。
- 结构化记忆和容量上限，避免保存聊天全文及截图文件。
- 内置 120 个原始台标图片，940 个频道名称/别名入口；自动补齐台标不调用 AI。下载缓存可选 2/5/10 MB。

![真实台标预览](screenshots/channel-logos.png)

模型推理使用用户自行配置的 HTTPS API，并非把模型权重放进 APK。节目源可用性取决于地区、运营商和来源状态。软件未实现登录、DRM、付费权限或运营商 IPTV 私网绕过；只处理公开可访问内容。

## 构建

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0；Gradle Wrapper 使用 8.6 并固定 SHA-256。

在本机创建未提交的 `local.properties` 指向 Android SDK，或设置 `ANDROID_HOME`。必要的网络代理请自行配置，本仓库不包含开发机器代理设置。

```sh
./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
```

Windows 使用 `gradlew.bat`。输出为 `app/build/outputs/apk/release/app-release-unsigned.apk`，自行发布时请使用自己的签名；本项目不公开交付版本的签名私钥。

实际应用代码目录为 `app/src/kandianshi/java`，上游 `app/src/main/java` 保留用于追溯，不参与本分支 Java/Kotlin 编译。

## 验证范围

1.9.0 交付版已有 253 项单元测试通过，Lint 0 错误。API 25 离线模拟器完成真实台标显示、后台播放交接、动态网页解析、节目单目录和签名版覆盖升级验证。本发布分支的检查记录见 `docs/publish-validation.json`。

Android 16 手机和电视实机尚未验证；不保证所有线路持续可播，也不承诺广告识别的全天准确性。后台图片巡检需要支持图片输入的模型服务，并受用户设置的每日额度约束。

## 来源与许可

上游基线为 `d65ac508eb56d454d565d7a53df35fa4d5403720`。上游来源和原始版权声明保留在 [原始 README](notices/my-tv-UPSTREAM-README.md)，本分支不替原作者重新授权。

新增代码、第三方代码、节目单和台标分别按 [许可说明](LICENSING.md) 处理。节目源地址及频道名称/标志属于对应提供者或权利人。第三方完整许可保存在 `notices/` 和应用资源中。

详细功能和使用边界见 [构建与版本说明](BUILD-README.txt)。
