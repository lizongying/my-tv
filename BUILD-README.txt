KanDianshi / 看电视 1.9.0
============================

Upstream: https://github.com/lizongying/my-tv
Upstream commit: d65ac508eb56d454d565d7a53df35fa4d5403720

This customized project builds the preserved my-tv TV UI from app/src/kandianshi/java.
The upstream native authentication pipeline is replaced by public M3U playlist loading.
Application ID: com.kandianshi.tv
Minimum Android version: 5.0 / API 21.

Build requirements:
- JDK 17
- Android SDK platform 35, build-tools 35.0.0
- Gradle 8.6 (wrapper SHA-256 pinned)
- Configure your own local.properties sdk.dir.
- If your network needs an HTTP proxy, configure it locally; no machine proxy is bundled.

Commands (Windows):
gradlew.bat :app:testDebugUnitTest :app:assembleRelease :app:lintRelease

The release task produces an unsigned APK; sign it with your own key to publish your own build.
The original delivery signing key is kept outside this repository and is not published.
Private signing keys and passwords are intentionally omitted from this source ZIP.

Read notices/SOURCE-NOTICES.txt for stream origins, update dates and tested limits.
The generated stream inventory is distributed alongside the APK.

Version 1.1.0 adds local source maintenance, playback health ranking, a bounded
Chat Completions agent, and temporary LAN-only QR configuration.
No API key is bundled. Users enter their own provider key through TV pairing.
AI logic runs on the TV and calls the selected HTTPS model provider; no custom
cloud backend is required. Android WorkManager may defer jobs while asleep.
The pairing server binds one private IPv4 LAN address, requires a random token,
expires after 10 minutes, and closes when the activity stops or saving completes.
Use a trusted home network: the local pairing web page uses HTTP.
Public pages, player scripts, GET-based JSON APIs and bounded dynamic WebView parsing are supported. Login, IPTV private networks, DRM and paywall bypass are not implemented.
Official DeepSeek discovery uses native web search via the Anthropic-compatible
Messages endpoint with the existing API key. Only structured search-tool results enter
the crawl frontier; generated answer URLs are not trusted. Other providers retain
search-result ID selection. New AI sources are parsed and media-sampled before acceptance.
Daily source-discovery request cap: 12 including
connection tests; version 1.3 adds a separate 10/50/200-call vision quota. A failed update retains the last valid cache.

Dependencies added: androidx.work:work-runtime-ktx:2.9.1 (Apache-2.0),
org.nanohttpd:nanohttpd:2.3.1 (BSD-3-Clause), com.google.zxing:core:3.3.3
(Apache-2.0); org.json:json:20240303 is used for host-side tests only.
Corresponding license texts are retained in notices and the app assets.

Version 1.2.0 adds resumable, three-round source scanning and recoverable removal
after three spaced failures. Failures require working media-network evidence.
Unrecognized or encrypted media remains unverified rather than being removed.
The player verifies a rendered video frame before reporting playback success;
20 seconds without a first frame triggers a fallback route. Cleanup preserves
the current route when it is still in the updated catalogue.
The maintenance screen provides scan/stop, manual black-screen removal and
per-route/all-route restore. This feature makes no model requests.

1.3.0：台标与纯广告画面巡检
-------------------------
- VisionActivity 使用独立前台静音 ExoPlayer 与 TextureView，每线路取三帧，间隔 10 秒；不读取配置窗口。
- VisionClient 发送三张 JPEG data URL 到用户配置的 HTTPS Chat Completions API；官方 DeepSeek 使用 deepseek-flash。
- VisionPolicy 严格校验三个不同图号、台标类别、广告布尔值和置信度；任意台标保留，三帧全无台标且明确广告才标为疑似。
- 疑似默认仅进入待确认列表，用户确认后走可恢复手动移除，不受 HTTP 成功或解码回调自动恢复。
- VisionStore 原子保存全列表/单线路独立进度及最多 2000 个结果；每日图片请求 10/50/200 次，与来源发现的每日 3 次分开。
- API 调用失败计次；API/截图失败保留线路；离开前台暂停，截图仅驻内存。
- 尚无电视实机图像采集与真实视觉模型调用联调；不承诺识别准确率或全日内容判断。

- 合成图片接口测试：HTTP 200，三张带 CCTV1 文字台标的广告图，共 1180 token；生产 VisionPolicy 解析为 tv 并保留。未验证电视真实截图与完整视觉巡检。


1.4.0：有容量上限的本地记忆
---------------------------
- MemoryPolicy / MemoryStore：普通记忆默认 1 MiB / 7 天，用户可选 1/2/5 MiB 与 1/7/30/90 天。
- 只保存结构化识别摘要和失败来源冷却；不保存聊天全文、截图或模型对话。
- UTF-8 序列化字节数决定容量，过期与最旧条目先淘汰；最多 4000 个普通条目。
- 已确认纯广告单独保存 SHA-256 完整线路指纹与简短脱敏标签，最多 2 MiB / 5000 条，满时拒绝新增而不自动忘记。
- 指纹保留查询参数，防止同一路径不同频道被误合并；地址或参数变化后需要重新判断。
- 在播放器与扫描候选入口过滤永久广告，包括重新下载订阅后；普通无效线路恢复不解除广告名单。
- 视觉缓存：正常电视 / 用户保留按所选天数；疑似最多 24 小时，无法判断 / 无可靠截图最多 6 小时。
- 复用结果不会刷新原始判定时间，也不占用图片 API 额度；仅在有新任务时调用图片接口。
- 失败网页 / 订阅来源冷却 1/6/24/72/168 小时，成功后重置；冷却记录也受普通记忆保留时间约束。
- 自动迁移 1.3 已确认广告和有效识别结果。广告保存先于普通记录清理。
- 全列表与单线路计划单份最多 384 KiB，进度文件最多 1 MiB。记忆容量不包括节目源缓存或 APK 本体。
- MemoryActivity 可查看占用、设置容量/天数、分页解除广告，或清理普通记忆与巡检进度。
- 清理普通记忆保留永久广告、密钥、每日用量和限额；维护/巡检运行时拒绝清理。
- 记忆文件读写与列表整理放在后台处理，界面不存取截图缓存。没有新付费 API 调用。

1.5.0：默认自动广告巡检
----------------------
看电视 1.5.0：默认自动广告巡检

覆盖安装新版，保留原密钥和记忆。入口：
设置 → AI 维护 / 手机扫码配置 → 自动广告巡检（默认开启）。

一、默认行为
自动巡检、两轮确认后自动屏蔽、仅使用不计费网络均默认开启。
需要先配置支持图片的 API，并启用 AI 与自动维护。安装包不含密钥。
已有配置覆盖升级后无需点击“开始巡检”；配置保存、源维护完成或退出播放后会自动安排任务。
普通观看优先；开始播放或进入手动画面巡检时，后台停止取样并让出解码资源。
没有观看时，后台用内部静音播放器取样，读取节目视频，不获取电视整个屏幕，也不申请屏幕录制权限。
后台不显示巡检窗口，不保存截图文件。

二、屏蔽规则
每轮每条线路读取三张真实视频画面，间隔 10 秒，三图合成一次模型请求。
任意一帧有电视台台标就保留，包括央视和地方台的正常插播广告。
三帧均无台标、均为明确广告、每帧模型置信度至少 0.93，才记为疑似。
没有台标本身不算广告；新闻、电影、赛事和普通节目不会仅因无台标就删除。
同一条线路必须在不同自动任务中、相隔至少 30 分钟，再获一次新的疑似结果，才永久屏蔽。
缓存结果不能算第二票。看不清、无法解码、黑屏或相同画面保留线路，不自动按广告删除。
误判仍可能发生。可在该页切换“仅检测”，也可关闭自动巡检。
在记忆管理中查看屏蔽原因或解除；手动保留会清除待复核记录。
同频道其他线路不受影响，线路地址或查询参数变化后需要重新识别。

三、运行时机与进度
后台默认要求已连接且不计流量的网络，一般是家庭 Wi-Fi 或有线网络。
可手动切换允许其他网络。定时补查采用原维护间隔（默认 6 小时，可选 12 / 24）。
配置保存或退出播放后安排约 2 分钟后的空闲任务；首次定时任务延迟约 5 分钟。
每批最多 3 次新的图片请求或 40 条缓存/线路检查，限制任务时长，保存每条已完成结果。
还有待检查或待复核线路时，安排约 30 分钟后的下一批；第二轮复核优先。
自动计划独立保存，不覆盖手动单条/全列表计划，最多 192 KB / 2000 条路线；读取当前列表时每频道最多 64 条。
自动与手动视觉巡检互斥，避免重复请求。
Android 省电、休眠、强制停止和网络条件会推迟后台任务，无法保证指定分钟准时启动。
关机时不执行；强制停止后需要重新打开 APP。设备长期持续观看可能长期没有空闲取样机会。

四、额度与记忆
自动和手动图片巡检共用每日 10 次额度，可在自动巡检页改为 50 / 200 次。
一次请求三张图。两轮确认至少需要两次新的请求；API 失败也占本地额度。
用完后停止图片请求，等待下一日自动任务。按 UTC 日期重置（北京时间 08:00）。
图片 API 报错会停止本批并冷却 6 小时，避免持续失败扣费；重新保存配置可解除该冷却。
节目画面发给你配置的模型服务，费用按服务商规则计算。正在发送的一次请求即使取消也可能计费。
普通结果按记忆设置复用，默认 1 MB / 7 天，可选容量与保留时间。
待复核证据最多 7 天，且受普通记忆容量/天数限制；被清理后重新从第一轮开始。
已确认纯广告单独永久记住，最多 2 MB / 5000 条，容量满时不自动遗忘旧名单。
清理普通记忆不解除永久广告，也不删除密钥或每日用量。
截图只在内存短暂处理，不保存聊天全文或整张图片。

五、验证范围
本版有针对两轮新证据、间隔、缓存隔离、记忆淘汰、完整 URL 指纹和播放优先的自动测试。
具体构建和视频取样测试结果见“交付报告.txt”和“验证结果.json”。
本轮开发没有新增付费模型请求。未完成真实电视节目取样到模型的端到端准确率验收。
模型判断反映当时的取样，不能证明线路全天内容；可随时解除误屏蔽。

Offline Android smoke test:
gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.kandianshi.tv.test/com.lizongying.mytv.SamplerInstrumentation
The smoke test generates its own local moving MP4; it makes no API or media-network requests.

Software-emulator fallback for the offline smoke test:
Some old x86 emulator system images crash their platform AVC encoder.
The test accepts an already-created cache/sampler-original-test.mp4 in the target app.
Generate an original grayscale moving H.264 baseline clip on the host with FFmpeg:
ffmpeg -f lavfi -i testsrc2=size=320x192:rate=15:duration=5 -vf hue=s=0,drawbox=x=0:y=0:w=32:h=32:color=white:t=fill -c:v libx264 -profile:v baseline -pix_fmt yuv420p -an sampler-original-test.mp4
Copy the file with adb push to /data/local/tmp/kan-tv-original.mp4, then run:\nadb shell run-as com.kandianshi.tv cp /data/local/tmp/kan-tv-original.mp4 cache/sampler-original-test.mp4\nVerify it byte-for-byte using adb exec-out run-as com.kandianshi.tv cat cache/sampler-original-test.mp4 before instrumentation.\nDirect stdin transfer was truncated in this environment and must not be used without byte verification.
The test checks an intact bright top-left corner, three JPEGs, cancellation and subsequent capture.
No original clip, FFmpeg binary, API key or test fixture is bundled in the release APK.
Temporary test tools and emulator data were removed after verification; see the delivery report.
\nFinal local result: default scheduling and cancellation passed. MPEG-4 offscreen capture passed, including three JPEGs, corner preservation, cancellation and a subsequent capture. H.264 decoding failed on this Android 7.1 x86 software emulator after byte-verified file copy. The emulator also showed a system-process ANR dialog, so its settings screenshot is not a completed UI verification. Android 16 / real-TV playback and real TV frames sent to the model remain unverified.\n
1.6.0：播放与后台找源自动衔接
---------------------------
- 所有找源工具、队列和索引内置 APP，不需要安装额外服务，也不要求第二个搜索密钥。
- 官方 DeepSeek 地址自动接入 https://api.deepseek.com/anthropic/v1/messages 原生 web_search。
- 沿用原 API 地址、模型与密钥，不会把密钥写入 APK；第三方 Chat Completions 配置保留原筛选方式。
- 只读取 web_search_tool_result 中真实 web_search_result 地址；模型正文编造的 URL 不入库。
- 每次请求声明最多两次搜索；服务商额外摘要调用及费用以服务商实际执行为准。
- 自动维护开启时，PlayerFragment.onStop 停止视频后立即持久化空闲维护任务。
- 返回观看时取消空闲任务、让出解码资源；后台每次访问前检查观看状态。
- 已发出的模型请求可能继续完成并计费，其真实结果先保存再暂停后续查找。
- 周期补查继续使用既有 6/12/24 小时间隔。到期或有未完成进度才执行；不会每次退出重复扣费。
- 手动“立即找源与更新”可在距上次搜索五分钟后重新搜索，不再固定限制一天一次。
- 12 个央视 / 卫视 / 地区主题轮换，每次携带已知来源提示，优先发现其他网站。
- 公开网页、Markdown 相对链接、相关外站直播页面均可跟进；GitHub 仓库与 blob 页面直接读取原始文件。
- 队列、来源归属和访问摘要原子保存，单文件最多 256 KiB；最多 128 个待访问页、256 个访问指纹。
- 找到的订阅文件优先读取；每批最多读取 20 页，整个维护批次约 4.5 分钟。
- 未完成页面约 15 分钟后空闲继续；返回观看取消补查，下一次退出重新安排。
- 镜像节目单按频道与完整播放地址集合去重，忽略列表顺序和重复行，保留签名 / 频道查询参数。
- 来源缓存最多 6 MiB；来源发现缓存保留最多 64 条同频道线路，健康排序后才限制播放候选。
- 日志分别记录候选页、订阅更新、新增订阅、重复节目单、净增线路、媒体抽检；已有订阅不再标成新订阅。
- 普通记忆清理同时清除找源进度；不会清除永久广告、API Key 或每日额度。
- 找源 / API 连接测试共用每日 12 次客户端请求上限，与图片巡检每日额度分开。
- 源接收仍是媒体抽样验证，不是所有线路完整实播验收；不能保证所有节目均可播放。
- Android 系统可能延迟后台任务；系统“强制停止”后须重新打开 APP，应用不能绕过。
- 文档依据：
  https://api-docs.deepseek.com/quick_start/agent_integrations/claude_code/
  https://api-docs.deepseek.com/guides/anthropic_api/
- 本版验证情况以交付报告和验证结果为准，不将模拟器结果等同小米 Android 16 实测。

1.6.0 最终验证（2026-10-10）：
- 171 项单元测试全部通过，0 失败 / 错误 / 跳过；Android Lint 0 错误，138 项既有警告。
- DeepSeek 原生搜索 HTTP 200，留存响应包含 10 个真实网页结果与 1 次 server web search。
- 本轮共发起 2 次接口测试请求；第一份响应打印因字符编码失败，没有保存其完整用量。
- Android 7.1 / API 25 离线模拟器：空闲找源任务唯一性、观看时取消、周期任务保留、关闭后取消全部任务、进度恢复均通过。
- 实际 MainActivity / PlayerFragment 退出后释放播放状态并持久化找源任务，通过。
- 已签名正式版在模拟器安装 1.5.0，再覆盖升级 1.6.0，通过；旧 Windows adb 使用英文下载名。
- 未在小米 15 Pro / Android 16、真实电视验证；未完成 APP 内真实节目源到模型的端到端巡检。
- 临时模拟器数据与调试测试 APK 已删除；APK / 源码 ZIP 不含 API Key 或签名私钥。

1.7.0 自主直播发现
-----------------
- 无需搜索引擎或 AI Key：六个电视台入口（央视、山东、广东、江苏、浙江、湖北）默认进入队列，并跟随相关频道链接扩展。用户也可添加其他公开入口，不限制频道城市；这不是全网扫描，也不保证覆盖全国频道。
- 从 HTML、脚本、JSON 及播放器的公开 GET 请求中提取地址；浏览器组件解析动态页面，含无 .m3u8 后缀的 HLS。保留频道标签；目录歧义不默认归给第一个台。
- 每轮最多 20 页、3 个动态页面（每页 18 秒 / 40 请求 / 6 MiB）、6 条直接直播验证，最大深度 3；队列/历史 256 KiB。后续空闲批次继续。
- WebView 无 JS 原生桥，禁文件/内容访问、下载、权限及弹窗；受控 HTTP 传输仅转发 Accept/User-Agent/Referer/Origin，不转发 Cookie、Authorization 或 AI Key。禁止视频分片和播放器视频渲染，抓取到的 HLS 地址交独立验证器。
- 新发现 HLS：读取主/媒体清单、检查最近分片并最多观察两轮更新；已结束/点播清单暂不收录。加密、特殊字节范围、过长分片、没有更新或身份不明暂不收录，保留旧有效频道。不保证台标识别或所有媒体能实播。
- 过期动态地址重新从频道页解析；同一页同一频道的新地址替换旧缓存。已确认纯广告沿用永久记忆，后续画面巡检另行运行。
- 已识别的无后缀 HLS 在播放器及前后台巡检中显式指定格式；格式索引最多 256 KiB，只保留当前频道。
- 先自主爬取，再可选 AI 补充找入口；不调用模型解析每页，不增加模型权重。WorkManager 和 Android 省电策略仍可能推迟后台任务。
- 部分旧电视没有可用 WebView、网站需要 POST/登录/本地存储、平台防爬或改变播放器时，动态解析可能失败；静态爬取和原有订阅保留。

1.7.0 最终验证（2026-10-11）：
- 204 项单元测试通过；包括动态链接提取、频道归属、满队列媒体优先、HLS 更新、点播/停更排除及观看优先。
- Android API 25 专用离线模拟器实际执行 JavaScript → GET 接口 → 无后缀 HLS 请求；频道链接、40 请求上限、内网地址阻止、播放 MIME 与让出观看均通过。
- 既有后台任务、前后台衔接测试通过；正式签名版 1.6.0 安装后覆盖升级 1.7.0 通过。
- 主机使用同一 Kotlin 爬虫解析了央视直播页、山东台首页、山东卫视直播页的实时 HTTP 200 响应；这只验证真实网页入口和链接提取，不代表全部台或真实网站动态播放器都已成功播放。
- 未在小米 15 Pro / Android 16、真实电视或运营商专网实测；模型广告识别准确率没有在本轮重新评估。
- 本轮没有调用付费模型接口。测试网页及模拟器数据、调试 APK 已清理，只保留新版交付包和可复用构建环境。

1.8.0（2026-10-11）：
- 新增公开仓库目录发现（GitHub / GitLab / 极狐），分块游标避免大目录只取前几个，目录深度受限、同级任务轮换域名，节目单优先。用户示例源和目录 HTTP200，真实 TXT 与目录由同一 Kotlin 实现解析。
- 频道列表及节目提示共用图标加载器：缺图、下载失败、透明/单色空白图片自动显示本地名称图标；失败 URL 的哈希记忆 24 小时，原图恢复后优先显示。
- AI 设计使用既有加密密钥和 Chat Completions，输出受限 JSON 配色/装饰，由 Android Canvas 绘制。生成图标不是官方台标；不写进视频，不改变广告巡检的截图或判断。
- 停止观看后自动分批设计，每批 8 个，每日另限 4 次请求；失败计入额度，24 小时退避。关闭 AI 仍可使用离线名称图标。
- 只持久化设计参数及失败 URL 哈希，不保存生成图片、网页或聊天记录。手机扫码页可关闭设计并设置 64/128/256 KB 缓存，默认 128 KB，条目最多 500。
- 235 项单元测试通过，Lint 0 错误；API25 离线模拟器检查本地绘制、缺图/空白/失败回退、坏链接记忆、复用视图、参数持久化、缓存及调度，既有后台衔接/动态爬虫回归通过，签名版 1.7.0 覆盖升级 1.8.0 通过。
- 没有在真实 Android16 手机或付费 DeepSeek 接口上重新实测，本轮付费模型请求为零。
- 已研究 Bitmagnet 队列及去重实现、BEP5/51。DHT 的 infohash 发现不能直接用于普通 M3U8；本版没有加入独立 BT 搜索或下载。

1.9.0（2026-10-12）：
- 改用真实 PNG 台标，停止运行 AI 配色/文字绘制。旧的文字设计缓存不参与展示，不会覆盖本版图片。
- 内置 fanmingming/live 台标库的 120 个常用央视、卫视及部分地方台原图，附 GPL-3.0 与来源说明；940 个明确名称/别名对应图片入口。图案保持下载原样，标志属于对应电视台，未宣称这些文件均来自电视台官网。
- 本地缓存或内置原图直接显示。图标区域采用中灰底，透明白色台标也可辨认。其他频道按明确名称查找公开图标，再尝试节目单原图。CCTV5 与 CCTV5+、CCTV4 与 CCTV4K、美洲与欧洲分开匹配；未知频道用备用电视图案。
- 修复 SourceTools.encode 丢失 tvg-logo，保存和重读节目单保留原台标地址。图片为空白或下载失败会记忆 24 小时并尝试后续入口；列表复用视图有绑定标识防止旧请求串图。
- 空闲后台每批 12 个、最多 24 次图片请求，不需要 AI Key；前台播放优先。缓存图片每周检查更新；手机扫码页设置 2/5/10 MB 图片缓存，默认 5 MB，超额清理较旧下载图片，内置图片不受影响。台标索引元数据上限 128 KB / 500 条，不保存聊天。
- 台标仅在节目 UI 显示，不覆盖播放画面、不修改广告巡检截图。
- 253 项单元测试通过，Lint 0 错误。API25 离线模拟器验证真实 PNG 显示与同图比对、图片缓存、缓存容量、无密钥调度、坏图回退与复用，原有后台衔接和爬虫回归通过，签名版 1.8 -> 1.9 升级通过。
- 所有内置 PNG 实际联网下载并校验，安装包内图片与源码一致。模拟器处于离线状态，新增远程图片下载流程未在真实 Android16 或电视上联网实测，本轮付费模型请求为零。
