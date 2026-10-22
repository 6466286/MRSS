# B站 260 DPI 参考布局（实验）

打开已登录的原版 B站，通过控制中心快捷开关投放到背屏。回到 MRSS，在「B站参考布局 · 260 DPI（实验）」点击「应用参考布局」。

参考图为 1536×972，右侧内容从 x=503 开始。本版按同一比例计算实际背屏窗口范围，DPI 为 260。请保持参考图的横向方向；本版不自动旋转屏幕。窗口由系统管理，不进行触摸坐标注入、不改 B站账号数据。

需要 Shizuku 运行并授权。通过 WindowContainerTransaction 尝试设置原版 B站任务窗口。仅对背屏上的 tv.danmaku.bili 任务生效。读取系统任务边界与窗口模式验证，失败尝试还原；不能保证所有 HyperOS 版本支持。

还原按钮恢复保存的窗口范围；退出投放移回主屏前也会还原。若 Shizuku 服务重启后记录丢失，还原按钮请求将背屏 B站恢复全屏。DPI 保持 260，可在现有 DPI 设置中自行更改。

请真机验证：搜索页、我的页、视频页可点击；旋转或播放器全屏后布局是否保持。后两种场景可能覆盖窗口布局，需要重新应用。本版不承诺左侧一定纯黑，也不修改 B站自身控件尺寸与排版。

依据 Android AOSP：
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/window/WindowOrganizer.java
- https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/window/WindowContainerTransaction.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/app/IActivityTaskManager.aidl
