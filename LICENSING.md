# 许可范围

本仓库是 lizongying/my-tv 的派生分支，保留上游文件及版权说明，不将上游未明确授权的部分改为新的许可。

- 上游文件及界面衍生内容：保留 `notices/my-tv-UPSTREAM-README.md` 中的原始声明。基线未提供可验证的独立 LICENSE 文件；不得因本分支公开发布就推定上游授予其他用途。
- 本分支新增的独立代码与测试（`app/src/kandianshi/java` 中的独立维护模块、`app/src/test`、`app/src/androidTest`）及新增构建文档：在不涉及上游和第三方权利的范围内使用 GPL-3.0，完整文本为 `notices/fanmingming-live-GPL-3.0.txt`。
- 真实台标 PNG 与台标目录：来自 fanmingming/live，按 GPL-3.0 保留来源及许可。频道名称和标志的权利仍属于对应电视台，见 `notices/channel-logos-NOTICE.txt`。
- 第三方依赖和节目单：适用 `notices/` 内各项目自己的许可及来源说明；软件公开不代表提供者授予流媒体内容的再分发权。

本文件不授予任何第三方商标、节目内容或上游原作者未授权部分的额外权利。


## JLayer 音频回退

使用 javazoom:jlayer:1.0.1 的解码器子集（按原始源码允许的后续版本选用 LGPL-2.1）。解码器类和资源逐字节保持原样，仅移除未使用的桌面播放器与转换器，生成脚本在 scripts/build_jlayer_subset.py。许可证随 APK 放在 assets/licenses/jlayer-LGPL-2.1.txt；完整对应源码位于 notices/jlayer-1.0.1-sources.jar，来源及校验和见 notices/jlayer-origin.md。开源工程包含可替换的原始JAR与完整构建代码，可修改或替换该库后重建。只调用纯Java解码器，不使用JavaSound设备，也不引入原生.so。
