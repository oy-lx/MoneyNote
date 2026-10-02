# 轻记账 MoneyNote

一款面向安卓手机的本地记账应用。**纯离线、无账号、无广告**，所有数据只保存在手机本地的 SQLite 数据库中。

采用当前主流的现代 Android 技术栈：**Kotlin + Jetpack Compose + Room + MVVM**。

---

## 下载安装

不想自己编译的话，直接到 [Releases](https://github.com/oy-lx/MoneyNote/releases/latest) 下载 **`app-debug.apk`** 安装即可。

- 安装包使用 **debug 签名**（口令是公开的 `android` / `androiddebugkey`），仅供试用体验；正式发布请自行配置 release 签名。
- 最低支持 **Android 8.0**（`minSdk 26`），目标版本 **Android 14**（`targetSdk 34`）。
- 本 APK 的 SHA-256：`B98DAD8A1ADA083AAE70492AB58FB498233E03D5C9257EB29EA93757DD248F13`

---

## 功能

### 1. 明细（首页）
- 按月查看流水，一键切换上/下月，点击月份标题可快速回到本月
- 顶部汇总卡实时显示 **本月结余 / 收入 / 支出**，设置了预算时额外显示预算进度条
- 流水按天分组，标题显示「今天 / 昨天 / 前天 / 周几」及当天收支小计
- 点击任意一条流水即可进入编辑；右下角悬浮按钮快速记账

### 2. 记账
- 支出 / 收入 分段切换
- 金额输入带实时校验（最多两位小数、上限九位整数），非法输入即时标红
- 分类以「图标 + 名称」胶囊形式平铺，emoji 图标无需额外图片资源
- Material 3 日期选择器，可补记任意一天的账
- 支持 60 字备注；编辑态支持删除

### 3. 统计
- 环形图展示**支出 / 收入构成**，圆心显示当期总额
- 分类排行：占比进度条 + 金额 + 百分比
- **每日趋势柱状图**，自动高亮当月支出最高的一天
- 日均支出、记账天数、单日峰值等派生指标
- 收入 / 支出可一键切换视图

### 4. 预算
- 月度**总预算**：环形进度显示剩余额度与完成百分比，超支变红
- **分类预算**：为「餐饮」「购物」等易超支分类单独设上限，独立进度条
- 日均可用额度（按当月剩余天数折算）
- 未设预算的分类支出单独列出，避免预算口径漏算
- 预算按月独立保存，切换月份互不影响

### 5. 我的
- **分类管理**：新增 / 改名 / 换图标 / 删除，支出与收入分类分开维护
  - 已被流水引用的分类执行**归档**而非物理删除，历史统计口径不会被破坏
- **导出 CSV**：通过系统文件选择器导出全部流水，写入 UTF-8 BOM，Excel / WPS 直接打开不乱码
- **意见反馈**：选好类型、写下内容，一键唤起系统邮件应用，收件人 / 主题 / 正文已预填，确认后发送
  - 可选择性附带应用版本、系统版本、设备型号与记录条数，方便定位问题
  - 走的是系统邮件应用（`ACTION_SENDTO` + `mailto:`），**App 自身不联网、不申请任何权限**；设备上没有邮件客户端时可复制邮箱地址或整段反馈内容
- **运行日志**：查看 / 复制 / 导出应用运行日志，未捕获的崩溃会自动连堆栈一起记下来
  - 日志文件超过 512 KB 自动轮转，最多保留 3 个历史文件，不会无限增长
  - 出于隐私考虑**只记录 id、类型、条数，绝不写入金额与备注内容**——因为日志是可以导出发出去的
- **清空数据**：仅清除流水与预算，保留分类
- 内置 15 个支出分类、7 个收入分类

---

## 技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| Kotlin | 2.0.21 | 语言 |
| Jetpack Compose | BOM 2024.09.03 | 声明式 UI |
| Material 3 | 1.3.0 | 设计体系 |
| Room | 2.6.1 | 本地数据库（KSP 生成代码） |
| Navigation Compose | 2.8.4 | 页面导航 |
| Lifecycle / ViewModel | 2.8.7 | MVVM + StateFlow |
| Coroutines | 1.8.1 | 异步与 Flow 数据流 |
| Android Gradle Plugin | 8.7.3 | 构建 |
| Gradle | 8.9 | 构建 |
| JDK | 17+（实测 21） | 编译 |
| compileSdk / targetSdk | 34 | |
| minSdk | 26（Android 8.0） | 直接使用 `java.time`，无需脱糖 |

**刻意没有引入**的依赖，以及原因：

- **Hilt / Dagger**：项目只有一个数据源和一个 Repository，手写轻量依赖注入（`MoneyNoteApp` + `AppViewModelFactory`）更直观，也省掉注解处理器的编译开销。
- **第三方图表库（MPAndroidChart 等）**：环形图、柱状图、进度环全部用 Compose `Canvas` 手绘（`ui/components/Charts.kt`），视觉可控、增加零依赖，也避开 JitPack 仓库的可用性风险。
- **material-icons-extended**：内置图标包体积很大。分类图标直接存 emoji 字符串，排版上反而更直观。

---

## 目录结构

```
MoneyNote/
├── settings.gradle.kts            # 仓库配置（国内镜像优先，官方源兜底）
├── build.gradle.kts               # 插件版本声明
├── gradle.properties
├── keystore/debug.keystore        # 调试签名（口令公开，见下文说明）
├── design/                        # 图标设计稿（不参与编译，仅供生成图标资源）
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/moneynote/
        │   │   ├── MoneyNoteApp.kt            # Application，持有 DB 与 Repository
        │   │   ├── MainActivity.kt
        │   │   ├── data/
        │   │   │   ├── model/TxnType.kt
        │   │   │   ├── local/
        │   │   │   │   ├── entity/            # Txn / Category / Budget
        │   │   │   │   ├── dao/               # 三个 DAO（Flow 响应式查询）
        │   │   │   │   ├── Converters.kt
        │   │   │   │   ├── AppDatabase.kt     # 建库 + 首次写入默认分类
        │   │   │   │   └── SeedData.kt
        │   │   │   └── repository/LedgerRepository.kt   # 唯一数据入口
        │   │   ├── ui/
        │   │   │   ├── LedgerApp.kt           # NavHost + 底部导航
        │   │   │   ├── AppViewModelFactory.kt
        │   │   │   ├── navigation/Routes.kt
        │   │   │   ├── components/            # 图表 + 通用组件
        │   │   │   ├── home/ stats/ budget/ edit/ category/ settings/
        │   │   │   ├── feedback/              # 意见反馈
        │   │   │   ├── logs/                  # 运行日志查看 / 导出
        │   │   │   └── theme/
        │   │   ├── log/                       # 日志内核（存储 / 格式化 / 崩溃捕获）
        │   │   └── util/                      # 金额 / 日期格式化、设备信息采集
        │   └── res/                           # 主题、自适应图标（WebP + 生成的 mipmap）
        └── test/java/com/moneynote/           # 60 个 JVM 单元测试
```

---

## 构建与运行

### 前置条件
- JDK 17 或更高
- Android SDK，已安装 `platforms;android-34` 与 `build-tools;34.0.0`（或更高）
- 在 `local.properties` 中指定 SDK 路径：

```properties
sdk.dir=C\:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk
```

### 命令行构建

```bash
# Debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# 安装到已连接的设备 / 模拟器
./gradlew :app:installDebug

# 单元测试
./gradlew :app:testDebugUnitTest

# 静态检查报告 → app/build/reports/lint-results-debug.html
./gradlew :app:lintDebug
```

Windows 下把 `./gradlew` 换成 `gradlew.bat`。

也可以用 Android Studio 直接 `File → Open` 打开 `MoneyNote` 目录，等待 Gradle Sync 完成后点运行。

> **关于 Gradle 分发包地址**
> `gradle/wrapper/gradle-wrapper.properties` 里的 `distributionUrl` 指向腾讯云镜像：
> `https://mirrors.cloud.tencent.com/gradle/gradle-8.9-bin.zip`。
> 这是因为在部分国内网络下 `services.gradle.org` 与 `downloads.gradle-dn.com` 会连接超时。
> 如果你的网络能直连官方源，把这一行换成
> `https://services.gradle.org/distributions/gradle-8.9-bin.zip` 即可。
>
> 同理，`settings.gradle.kts` 中把阿里云镜像排在 `google()` / `mavenCentral()` 之前，
> 也是为了让依赖下载在国内网络下更稳定；镜像未命中时会自动回落到官方源。

### 关于 `keystore/debug.keystore`

Android Gradle Plugin 默认在 `~/.android/debug.keystore` 生成调试签名。在 CI 或受限沙箱中该路径往往不可写，会导致 `validateSigningDebug` 任务失败。

因此本工程内置了一份调试密钥库，`app/build.gradle.kts` 在检测到它时优先使用：

```kotlin
val projectDebugKeystore = rootProject.file("keystore/debug.keystore")
val hasProjectDebugKeystore = projectDebugKeystore.exists()
```

这是**调试专用**密钥，口令为公开的 `android` / `androiddebugkey`，不包含任何机密信息，发布正式版时请务必换成自己的 release 签名。

---

## 架构与设计要点

### 数据模型

```
categories ──┐
             │ categoryId (SET NULL)
             └──< txns >          budgets
                  amountCents       yearMonth
                  type              categoryId  (0 = 当月总预算)
                  dateMillis        amountCents
                  note
```

三个关键设计决策：

1. **金额一律以「分」为单位存 `Long`**，彻底规避浮点累加误差。解析与格式化集中在 `util/Formatters.kt`，并有完整单元测试覆盖边界（空串、多余小数位、超长整数、千分位逗号）。
2. **日期存本地零点时间戳**。查询某月时用 `[月初 00:00, 次月 01 日 00:00)` 左闭右开区间，跨时区/夏令时不会漏记录。注意 Material 3 `DatePicker` 返回的是 UTC 零点毫秒，与业务时间戳不是一回事，两者在 `util/DateUtils.kt` 中分开处理。
3. **预算的「总预算」用 `categoryId = 0` 哨兵值而非 `NULL`**。SQLite 的唯一索引不把多个 `NULL` 视为重复，用 `NULL` 会让 `(yearMonth, categoryId)` 唯一约束失效。

### 分层

```
UI (Compose Screen)
   ↓ 只读 StateFlow<UiState>，只调用 ViewModel 的意图方法
ViewModel
   ↓ 组合多个 Flow，把实体聚合成 UiState
LedgerRepository
   ↓ 唯一数据入口，封装 DAO 与业务规则（如分类归档）
Room DAO / AppDatabase
```

- **单向数据流**：Room 的 `Flow` 查询 → ViewModel 用 `combine` / `flatMapLatest` 聚合成 `UiState` → `stateIn` 暴露给 UI。数据库变更自动驱动界面刷新，无需手动通知。
- **UI 层不接触 DAO**，也不持有 Room 实体以外的业务逻辑。
- **依赖注入**由 `MoneyNoteApp`（持有单例 `AppDatabase` 与 `LedgerRepository`）配合 `appViewModelFactory` 完成。

### 应用图标

图标用的是设计稿 `design/记账AppLogo-方案2-干净版.png`（2048×2048，暖橙渐变 + 主体标识），
由 `_toolchain/make_logo_icons.py` 转成自适应图标资源：

| 层 | 资源 | 内容 |
|---|---|---|
| background | `values/colors.xml` → `ic_launcher_background` | 取自设计稿边缘的均色 `#FFD296`，仅作兜底 |
| foreground | `mipmap-*/ic_launcher_foreground.webp` | 设计稿满幅，108dp 画布 |

**设计稿本身就是按自适应图标画布画的**：实测主体落在 x 17.6%~82.4%、y 18.2%~84.2%，
几乎正好是 108dp 画布的中心 72dp 安全区（16.7%~83.3%），四周 16.7% 是留给启动器裁切的出血区。
所以这里**整幅满幅使用**，不缩放也不重新居中——任何额外的缩边都会破坏设计稿本来的留白比例。

几个转换时踩到的点：

1. **源文件其实是 JPEG**（扩展名是 `.png`，但 `Image.open().format` 返回 `JPEG`），所以它没有 alpha 通道、也不能靠透明度做圆角。
2. **传统图标不能铺满整个方形**。最初把 `mipmap-*/ic_launcher.*` 直接存成满幅方块，Lint 报了 5 个 `IconLauncherShape`（"Launcher icons should not fill every pixel of their square region"）。现在按圆角遮罩写入 alpha 通道。
3. **遮罩要写 alpha 通道，不能用 `Image.paste(img, (0,0), mask)`**。后者会把未覆盖处的 RGB 也一起插值，圆角的抗锯齿边缘会和透明黑混出深色描边。
4. **没有提供 `<monochrome>`**。设计稿是一张柔和的全幅位图，主体没有可单色化的清晰轮廓：用 R-G 通道阈值 + 连通域提取出的轮廓占自身包围盒 60%、填满可见区 44%，本质上是一整块实心形状，作为 monochrome 只会让 Android 13+ 的主题图标退化成一个纯色色块，比不给主题图标更糟。因此显式不提供该图层，并在 `mipmap-anydpi/ic_launcher.xml` 里用 `tools:ignore="MonochromeLauncherIcon"` 抑制对应检查（注释里写了原因）。若之后有简化的单色标识，补一行 `<monochrome>` 即可。
5. **`android:roundIcon` 已从清单移除**。自 API 26 起启动器都直接对自适应图标套遮罩，`roundIcon` 只对传统圆形图标有意义，minSdk 26 下是多余引用。

**图标资源用 WebP 而不是 PNG。** 源图是 JPEG，压缩噪点存成 PNG 后极难压缩：五档密度的前景 + 传统图标用 PNG 要 **543 KB**，换 WebP（q92）后只要 **50 KB**，省下 482 KB——否则光是换个 logo 就会让 APK 明显变大。minSdk 26 对 WebP（含有 alpha 的有损 WebP）是完全支持的。

---

## 测试

`app/src/test/` 下有 60 个 JVM 单元测试，全部通过：

| 测试类 | 用例数 | 覆盖内容 |
|---|---|---|
| `FormattersTest` | 16 | 金额解析（空串、非法字符、超两位小数、超九位整数、千分位、边界值）、格式化、格式化↔解析互逆 |
| `DateUtilsTest` | 4 | 本地时间戳与 DatePicker UTC 时间戳的往返转换、月末与闰年边界 |
| `SeedAndModelTest` | 6 | 内置分类数量/命名唯一性/排序连续性/字段完整性、预算哨兵值语义、收支方向语义 |
| `FeedbackComposerTest` | 9 | 反馈邮件主题与正文拼装、空白裁剪、未填联系方式占位、环境信息开关的包含与剔除 |
| `LogFormatterTest` | 10 | 日志行格式、级别标记、时间戳、堆栈缩进、超长堆栈截断与省略提示 |
| `LogStoreTest` | 9 | 日志追加/读取/尾部截取/清空、超限轮转与历史文件上限、末尾无换行时的读取 |
| `LogExporterTest` | 6 | 导出文档头部与环境信息、空日志占位、字节数单位换算、负值兜底 |

> 说明：本机没有安装 Android 系统镜像，也没有可用的 AVD 或真机，因此**未做设备上的运行时 UI 验证**。UI 部分的正确性目前只覆盖到「编译通过 + Lint 无 error + APK 签名与清单校验通过」。要端到端验证界面，请把 APK 装到手机上，或用 Android Studio 创建 AVD 后运行。

---

## 参考的开源项目

本项目的功能取舍与分层方式参考了 GitHub 上几个成熟的开源记账应用（按 Star 数排序）：

| 项目 | Star | 许可 | 借鉴点 |
|---|---|---|---|
| [mtotschnig/MyExpenses](https://github.com/mtotschnig/MyExpenses) | 1.2k | GPL-3.0 | 老牌开源记账应用。参考了它的分类/账户模型与「按天分组流水」的信息层级 |
| [Spikeysanju/Expenso](https://github.com/Spikeysanju/Expenso) | 1.1k | Apache-2.0 | MVVM + Room + StateFlow 的教科书式组织方式，本项目的分层与之接近 |
| [nominalista/expenses](https://github.com/nominalista/expenses) | 403 | Apache-2.0 | 预算追踪的交互设计，尤其是「总预算 + 分类预算」的双层结构 |
| [wisnukurniawan/Compose-Expense](https://github.com/wisnukurniawan/Compose-Expense) | 184 | Apache-2.0 | 纯 Compose + Room 的工程组织，单 Activity + Navigation Compose 的写法 |
| [jxareas/Xpensor](https://github.com/jxareas/Xpensor) | 44 | MIT | Clean Architecture 的分层思路（本项目按其精神做了简化） |
| [furqanullah717/expense-tracker-android](https://github.com/furqanullah717/expense-tracker-android) | 42 | — | 图表与统计页的呈现方式 |
| [rishavchanda/Buckoid-Android-App](https://github.com/rishavchanda/Buckoid-Android-App) | 38 | — | 预算超支提醒的视觉表达 |
| [KenAli77/ExpenseTracker](https://github.com/KenAli77/ExpenseTracker) | 27 | — | 极简 UI 与配色参考 |

本项目为**独立实现**，未复制上述项目的代码。

---

## 已知限制与后续可做

当前版本未包含（按价值排序）：

1. **多账户 / 多账本** —— 目前只有一本总账，没有「现金 / 银行卡 / 支付宝」的概念。
2. **周期账单** —— 房租、订阅这类固定支出需要手动重复录入。
3. **搜索与筛选** —— 流水多了以后只能按月翻。
4. **应用锁** —— 没有密码或生物识别保护。
5. **云同步 / 备份还原** —— 目前只能导出 CSV，不能导回。建议后续加 JSON 备份 + 恢复。
6. **小组件与快捷记账通知栏** —— 记账入口目前只有 App 内。
7. **聚合逻辑的单元测试** —— 目前统计/预算的聚合写在 ViewModel 私有方法里，可提取为纯函数以便测试（当前这部分的验证依赖人工审查）。

---

## 许可协议

本项目采用 [MIT 许可证](LICENSE)，你可以自由使用、修改、分发，包括用于商业用途，
只需保留原始版权声明。

### 关于第三方内容

- 本项目的代码为**独立实现**，未复制上文列出的任何参考项目的代码。
- 反馈邮件地址、应用图标（`design/`）属于本项目自有内容。

