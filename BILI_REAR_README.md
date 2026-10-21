# MRSS B站背屏网页实验版

基于 AntiOblivionis/MiRearScreenSwitcher，基础提交
`7e0ae1a35aea58c1f94019cf3cb54681adb54b9d`，2026-10-21 修改。
保留上游 GPL-3.0 许可、作者署名和原有功能。不是 B站官方客户端。

## 已实现的代码

- 主界面右上角视频图标：B站背屏（网页实验版）。
- 独立 Android WebView，直接用 ActivityOptions 在 display 1 启动，不移动 B站原生任务。
  沿用上游的小米背屏声明；找不到背屏或系统拒绝时显示错误，不静默改为主屏。
- 顶栏：返回、首页、搜索（关键词 / BV号 / HTTPS B站链接）、刷新、退出。
- B站视频锚点在当前窗口打开，使用真实网页历史返回；保留分P参数。
- 原生两行页面标题，点标题查看全文。视频页面优先使用实际标题元素或 og:title。
- 视频卡片标题允许换行；仅在卡片缺少文本时，使用网页提供的 title / 图片 alt 补全。
- 隐藏适配规则覆盖的评论节点，包括动态插入的评论；不隐藏弹幕。
- 视频全屏仍保留顶栏，返回优先退出全屏，再返回列表。
- 无 Android JavaScript 桥；禁止文件访问、混合内容和任意外部协议跳转。
- 退出/进入后台暂停视频，释放 WebView；不修改全局 DPI，不修改 B站安装包。

## 使用与构建

本包为源码，不含 APK。当前执行环境缺少 Flutter / Android SDK，未完成 Android 编译和小米实机测试。

本地：安装满足 pubspec 的 Flutter stable、Java 17、Android SDK，执行：

```sh
flutter pub get
flutter build apk --debug --target-platform android-arm64
```

APK 路径：`build/app/outputs/flutter-apk/app-debug.apk`。

或将源码放入自己的 GitHub 仓库，在 Actions 手动运行
**Build Bili Rear Experimental APK**，完成后下载
**MRSS-Bili-Rear-experimental-debug** 构建产物。此流程只上传构建产物，不发布 Release。
本次未替用户创建仓库、推送代码或运行远程 Actions。

原项目使用自己的签名，本地/Actions 调试签名通常不能覆盖原 APK。
请先备份原应用配置；不要把“签名冲突”误认为代码编译失败。

安装后打开 MRSS → 右上角视频图标 → 翻到背屏。
此入口不需要通过“切换至背屏”快捷开关投放 B站 App。
如果小米系统拒绝第三方 Activity 进入背屏，需要针对系统版本进一步适配。

## 测试与边界

`tests/bili-rear.cjs` 使用模拟网页验证页面适配器；不是 B站线上验证。

```sh
npm install --no-save playwright
npx playwright install chromium --only-shell
node tests/bili-rear.cjs
```

必须在小米17 Pro上补测：
1. 点击入口是否在背屏启动，返回/搜索按钮可否触摸。
2. 搜索→视频A→返回→视频B，重复10次；全屏后重复。
3. 长标题、竖屏视频、分P、旋转、锁屏恢复、网络断开和刷新。
4. 评论是否隐藏，动态加载后是否重新出现。
5. 当前 B站网页是否允许所需视频播放，账号登录是否正常。

网站可能强制打开 App、调整 DOM、限制网页播放或触发验证。
这些限制不能由 MRSS 保证消除；未添加绕过登录、付费或风控的逻辑。
仅支持已识别的普通视频链接，不保证直播、番剧、全部深链。
网页升级后可能需更新 `android/app/src/main/assets/bili-rear.js` 的选择器。
不能把本实验版宣称为“已修复原生 B站点击问题”；它是绕开原生任务的替代入口。

### 本次实际验证记录

- JavaScript 语法检查通过。
- AndroidManifest XML 解析通过，git diff 空白检查通过。
- `node tests/video-url.cjs` 的7项真实 URL 归一化/域名边界断言通过。
- Playwright 场景脚本已提供，但浏览器组件下载失败，因此未执行通过。
- 没有完成 Java/Flutter 编译、线上 B站验证或背屏设备验证；没有生成 APK。
