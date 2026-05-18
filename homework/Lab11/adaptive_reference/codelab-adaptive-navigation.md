# Android Compose 大屏自适应导航 

---

## 第一部分：这节课要解决什么问题

### 1.1 从一个现实问题出发

想象你用手机打开一个邮件应用，底部有四个标签：收件箱、已发送、草稿、垃圾邮件。你的拇指可以轻松够到屏幕底部，点击标签切换邮箱。这个交互非常自然。

现在你拿起一台平板电脑，打开同一个应用。平板屏幕有 11 英寸，底部导航栏还在那里，但它被横向拉伸到了整个屏幕宽度。更大的问题是：平板通常平放在桌上使用，或者双手握持，你的拇指根本够不到屏幕底部中央的按钮。

**这就是这节课要解决的问题**：同一套代码，在不同尺寸的设备上，自动切换为最适合该设备的导航组件。

### 1.2 解决方案预览

| 屏幕宽度 | 设备形态 | 使用的导航组件 |
|---------|---------|--------------|
| 0 – 599 dp | 手机 | 底部导航栏（NavigationBar） |
| 600 – 839 dp | 折叠屏、小平板 | 侧边导航栏（NavigationRail） |
| 840 dp 以上 | 大平板 | 永久抽屉（PermanentNavigationDrawer） |

效果是：写一套代码，应用自动检测屏幕宽度，选择对应的导航组件。业务逻辑（邮件数据、标签切换）完全不变，只换"导航外壳"。

### 1.3 学习目标

完成这节课后，你应该能：
- 解释什么是 `WindowSizeClass`，以及 Compact / Medium / Expanded 三个类别的划分依据
- 在代码中根据屏幕宽度动态切换导航组件
- 理解"状态驱动导航"（不用 NavHost 实现屏幕切换）
- 使用 `BackHandler` 处理自定义返回事件

### 1.4 起始代码

```bash
git clone https://github.com/google-developer-training/basic-android-kotlin-compose-training-reply-app.git
cd basic-android-kotlin-compose-training-reply-app
git checkout starter   # 起点：只有底部导航栏
git checkout nav-update  # 终点：三种自适应导航
```

---

## 第二部分：读懂项目结构

在动手改代码之前，先搞清楚项目里每个文件是干什么的，以及它们之间的关系。

### 2.1 整体目录结构

```
app/src/main/java/com/example/reply/
│
├── MainActivity.kt                       ← Android 程序入口
│
├── data/                                 ← 数据层（这节课全程不改这里）
│   ├── Email.kt                          ← 一封邮件的数据结构
│   ├── Account.kt                        ← 发件人/收件人的数据结构
│   ├── MailboxType.kt                    ← 邮箱类型枚举
│   └── local/
│       ├── LocalAccountsDataProvider.kt  ← 模拟联系人数据（单例）
│       └── LocalEmailsDataProvider.kt    ← 模拟邮件数据（单例，12 封邮件）
│
└── ui/                                   ← 界面层（本节课主要改这里）
    ├── ReplyUiState.kt                   ← 界面状态数据类
    ├── ReplyViewModel.kt                 ← 状态管理中心
    ├── ReplyApp.kt                       ← 顶层 Composable，接收窗口大小
    ├── ReplyHomeScreen.kt                ← 决定用哪种导航组件（核心改动文件）
    ├── ReplyHomeContent.kt               ← 邮件列表 UI 组件
    ├── ReplyDetailsScreen.kt             ← 邮件详情页
    └── utils/
        └── WindowStateUtils.kt           ← 新建：导航类型枚举
```

**分层的意义**：数据层和界面层分开，改导航逻辑不需要动任何数据代码。这节课的所有改动都在 `ui/` 目录里。

### 2.2 数据层：三个核心文件

#### Email.kt — 一封邮件长什么样

```kotlin
data class Email(
    val id: Long,
    val sender: Account,                            // 发件人（Account 类型）
    val recipients: List<Account> = emptyList(),    // 收件人列表
    @StringRes val subject: Int = -1,               // 主题（字符串资源 ID）
    @StringRes val body: Int = -1,                  // 正文（字符串资源 ID）
    var mailbox: MailboxType = MailboxType.Inbox,   // 所属邮箱类型
    @StringRes var createdAt: Int = -1              // 发送时间（字符串资源 ID）
)
```

**注意 `@StringRes` 注解**：`subject`、`body`、`createdAt` 的类型是 `Int` 而不是 `String`。这是因为这是一个教学项目，把文本内容放在 `strings.xml` 资源文件中，`subject = R.string.email_0_subject` 存储的是资源 ID（一个整数）。在 UI 层通过 `stringResource(email.subject)` 获取真正的文字内容。实际项目中这些字段通常是 `String` 类型，来自服务器接口。

#### MailboxType.kt — 邮箱类型枚举

```kotlin
enum class MailboxType {
    Inbox,   // 收件箱
    Drafts,  // 草稿箱
    Sent,    // 已发送
    Spam     // 垃圾邮件
}
```

这四个值对应应用底部的四个标签。整个项目里，从数据分组到导航状态更新，都会用到这个枚举。

#### LocalEmailsDataProvider.kt — 模拟数据

```kotlin
object LocalEmailsDataProvider {
    val allEmails = listOf(
        Email(id = 0L, sender = ..., mailbox = MailboxType.Inbox, ...),
        Email(id = 1L, ...),
        // ... 共 12 封邮件
        Email(id = 3L, mailbox = MailboxType.Sent, ...),   // 少数邮件属于其他邮箱
        Email(id = 9L, mailbox = MailboxType.Drafts, ...),
        Email(id = 11L, mailbox = MailboxType.Spam, ...)
    )
}
```

大部分邮件默认属于 `Inbox`。只有几封邮件显式标注了其他邮箱。ViewModel 初始化时用 `.groupBy { it.mailbox }` 把这 12 封邮件按类型分成 4 组，每组对应一个标签页。

### 2.3 界面层：理解状态是如何流动的

在 Compose 应用中，UI 是状态的函数：**状态变了，UI 自动更新**。所以理解"状态"是理解整个应用的关键。

#### ReplyUiState.kt — 应用所有 UI 状态

```kotlin
data class ReplyUiState(
    val mailboxes: Map<MailboxType, List<Email>> = emptyMap(),
    val currentMailbox: MailboxType = MailboxType.Inbox,
    val currentSelectedEmail: Email = LocalEmailsDataProvider.defaultEmail,
    val isShowingHomepage: Boolean = true
) {
    val currentMailboxEmails: List<Email> by lazy {
        mailboxes[currentMailbox]!!
    }
}
```

逐字段解释：

| 字段 | 含义 | 默认值 |
|------|------|--------|
| `mailboxes` | 按邮箱类型分好组的邮件。key 是四种邮箱，value 是各自的邮件列表 | 空 Map |
| `currentMailbox` | 用户当前看的是哪个标签页 | Inbox |
| `currentSelectedEmail` | 用户当前选中（正在查看）的邮件 | 默认空邮件 |
| `isShowingHomepage` | **最重要的开关**：`true` 显示列表页，`false` 显示详情页 | `true` |

`currentMailboxEmails` 是一个计算属性，用 `by lazy` 延迟计算：第一次访问时从 `mailboxes[currentMailbox]` 取出当前邮箱的邮件列表。UI 层直接用这个属性，不需要自己过滤。

#### ReplyViewModel.kt — 唯一能修改状态的地方

```kotlin
class ReplyViewModel : ViewModel() {

    // 背板模式：私有的可变版本，公开的只读版本
    private val _uiState = MutableStateFlow(ReplyUiState())
    val uiState: StateFlow<ReplyUiState> = _uiState

    init {
        initializeUIState()
    }
    // ...
}
```

**为什么要"背板模式"（`_uiState` + `uiState`）？**

`_` 前缀的私有版本是可变的（`MutableStateFlow`），只有 ViewModel 自己能改。公开暴露的 `uiState` 是只读的（`StateFlow`），UI 层只能观察不能修改。这样保证了状态的修改逻辑全部集中在 ViewModel 里，不会散落到各个 UI 组件中。

ViewModel 提供四个方法：

**① 初始化**
```kotlin
private fun initializeUIState() {
    val mailboxes: Map<MailboxType, List<Email>> =
        LocalEmailsDataProvider.allEmails.groupBy { it.mailbox }  // 12 封邮件分成 4 组
    _uiState.value = ReplyUiState(
        mailboxes = mailboxes,
        currentSelectedEmail = mailboxes[MailboxType.Inbox]?.get(0)
            ?: LocalEmailsDataProvider.defaultEmail  // 默认选中 Inbox 第一封
    )
}
```

**② 切换标签页**
```kotlin
fun updateCurrentMailbox(mailboxType: MailboxType) {
    _uiState.update {
        it.copy(currentMailbox = mailboxType)  // 只改这一个字段，其他保持不变
    }
}
```

**③ 打开邮件详情**
```kotlin
fun updateDetailsScreenStates(email: Email) {
    _uiState.update {
        it.copy(
            currentSelectedEmail = email,
            isShowingHomepage = false    // ← 这里把"开关"拨到详情页
        )
    }
}
```

**④ 返回列表页**
```kotlin
fun resetHomeScreenStates() {
    _uiState.update {
        it.copy(
            currentSelectedEmail = it.mailboxes[it.currentMailbox]?.get(0)
                ?: LocalEmailsDataProvider.defaultEmail,
            isShowingHomepage = true     // ← 把"开关"拨回列表页
        )
    }
}
```

**`_uiState.update { it.copy(...) }` 的含义**：`update` 是原子操作（线程安全），`it` 代表当前状态，`.copy(...)` 创建只改了指定字段的新状态对象。`data class` 的 `copy()` 方法会保持没有指定的字段不变。

---

## 第三部分：检测屏幕宽度

### 3.1 什么是断点（Breakpoint）

不同屏幕尺寸需要不同的布局，布局发生变化的临界宽度就叫**断点**。Material Design 定义了针对 Android 设备的断点：

```
  0 dp ─────────── 599 dp ─────────── 839 dp ─────────── ∞
  │                  │                   │
  │   Compact        │   Medium          │   Expanded
  │   手机竖屏        │   折叠屏/小平板   │   大平板横屏
```

| 范围 | 类别名 | 典型设备 |
|------|-------|---------|
| 0 – 599 dp | Compact | 手机（竖屏或横屏） |
| 600 – 839 dp | Medium | 可折叠设备展开、小型平板 |
| 840 dp 以上 | Expanded | 大型平板、桌面窗口 |

**单位是 dp（密度无关像素），不是 px**。600 dp 在大多数平板上约等于 10 英寸设备竖屏时的宽度。

### 3.2 WindowSizeClass API

Jetpack 提供了 `WindowSizeClass` API，封装了上面的断点判断。你不需要手动写 `if (widthDp > 600)`，只需调用一个函数就得到三种类别之一。

**第一步：添加依赖**

在模块的 `build.gradle.kts` 中：

```kotlin
dependencies {
    implementation("androidx.compose.material3:material3-window-size-class")
}
```

添加后点击 **Sync Now**。注意这是独立的库，不包含在 `material3` 基础库里。

**第二步：在 MainActivity 中检测**

```kotlin
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ReplyTheme {
                val layoutDirection = LocalLayoutDirection.current
                Surface(
                    modifier = Modifier.padding(
                        start = WindowInsets.safeDrawing.asPaddingValues()
                            .calculateStartPadding(layoutDirection),
                        end = WindowInsets.safeDrawing.asPaddingValues()
                            .calculateEndPadding(layoutDirection)
                    )
                ) {
                    val windowSize = calculateWindowSizeClass(this)   // ← 核心
                    ReplyApp(windowSize = windowSize.widthSizeClass)  // ← 传给应用
                }
            }
        }
    }
}
```

逐行说明重点：

- **`enableEdgeToEdge()`**：让应用内容延伸到系统状态栏和导航栏下方（透明系统栏）。Android 15 之后推荐调用。

- **`WindowInsets.safeDrawing.asPaddingValues()`**：获取系统"安全区域"的内边距——刘海屏、状态栏、系统导航栏等占据的空间。

- **`calculateStartPadding(layoutDirection)`**：根据布局方向（LTR 或 RTL）计算正确的边距。在阿拉伯语（RTL）环境中，"start"在右边。这保证了国际化场景下的正确布局。

- **`calculateWindowSizeClass(this)`**：`this` 是 Activity。这个函数从 Activity 获取窗口宽度的 dp 值，与断点阈值比较，返回 `WindowSizeClass` 对象。返回值有 `widthSizeClass` 和 `heightSizeClass` 两个字段，这里只取宽度。

- **`@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)`**：因为 `WindowSizeClass` API 目前还在实验阶段（Alpha），必须加这个注解表示"我知道这个 API 可能会变，我选择使用它"。IDE 会提示你添加。

### 3.3 Preview 函数中如何处理

Preview 没有真实的 Activity，无法调用 `calculateWindowSizeClass()`，所以要硬编码传入对应的类别：

```kotlin
@Preview(showBackground = true, widthDp = 400)
@Composable
fun ReplyAppCompactPreview() {
    ReplyTheme {
        Surface {
            ReplyApp(windowSize = WindowWidthSizeClass.Compact)  // 直接传枚举值
        }
    }
}
```

---

## 第四部分：新建导航类型枚举

在 `ui/utils/` 目录下新建 `WindowStateUtils.kt`（这是本 Codelab 新建的文件，起始代码中没有）：

```kotlin
package com.example.reply.ui.utils

enum class ReplyNavigationType {
    BOTTOM_NAVIGATION,            // 底部导航栏 → Compact 屏幕（手机）
    NAVIGATION_RAIL,              // 侧边导航栏 → Medium 屏幕（折叠屏/小平板）
    PERMANENT_NAVIGATION_DRAWER   // 永久抽屉   → Expanded 屏幕（大平板）
}
```

**为什么要单独建一个文件，而不是直接把枚举放在 `ReplyApp.kt` 里？**

这个枚举会被 `ReplyApp`、`ReplyHomeScreen`、`ReplyAppContent` 三个文件共同使用。如果放在 `ReplyApp.kt` 里，另外两个文件就会对 `ReplyApp.kt` 产生依赖，这是不合理的依赖方向（UI 组件不应该依赖上层组件的文件）。放在 `utils/` 包里，三个文件都依赖 `utils`，而 `utils` 不依赖任何人，依赖方向是向下的。

---

## 第五部分：核心改造 — ReplyApp.kt

`ReplyApp` 是连接 `MainActivity` 和 `ReplyHomeScreen` 的中间层，负责两件事：
1. 把窗口宽度类别映射为导航类型
2. 创建回调函数，把 UI 事件连接到 ViewModel

```kotlin
@Composable
fun ReplyApp(
    windowSize: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReplyViewModel = viewModel()
    val replyUiState = viewModel.uiState.collectAsState().value

    // 步骤 1：窗口宽度 → 导航类型
    val navigationType: ReplyNavigationType = when (windowSize) {
        WindowWidthSizeClass.Compact  -> ReplyNavigationType.BOTTOM_NAVIGATION
        WindowWidthSizeClass.Medium   -> ReplyNavigationType.NAVIGATION_RAIL
        WindowWidthSizeClass.Expanded -> ReplyNavigationType.PERMANENT_NAVIGATION_DRAWER
        else                          -> ReplyNavigationType.BOTTOM_NAVIGATION
    }

    // 步骤 2：传给下层，同时传入三个事件回调
    ReplyHomeScreen(
        navigationType = navigationType,
        replyUiState = replyUiState,
        onTabPressed = { mailboxType ->
            viewModel.updateCurrentMailbox(mailboxType)
            viewModel.resetHomeScreenStates()
        },
        onEmailCardPressed = { email ->
            viewModel.updateDetailsScreenStates(email = email)
        },
        onDetailScreenBackPressed = {
            viewModel.resetHomeScreenStates()
        },
        modifier = modifier
    )
}
```

**`viewModel()` 的工作原理**

`viewModel()` 是 `androidx.lifecycle.viewmodel.compose` 提供的函数，在 Composable 中获取或创建 ViewModel 实例。关键特性：
- ViewModel 的生命周期绑定到最近的 Activity/Fragment，不是绑定到这个 Composable
- 即使 Compose 发生重组（Recomposition），`viewModel()` 返回的始终是**同一个实例**
- 屏幕旋转导致 Activity 重建时，ViewModel 不会被销毁，数据不丢失

**`collectAsState().value` 的工作原理**

`viewModel.uiState.collectAsState()` 把 `StateFlow` 转成 Compose 可观察的 `State<ReplyUiState>`，`.value` 取出当前值。每次 `_uiState` 发出新值，`ReplyApp` 就会重组，新的状态流向所有子组件。

**三个回调函数的完整数据流示例**

以用户点击"Sent"标签为例：
```
用户点击 Sent 标签
  → onTabPressed(MailboxType.Sent) 被调用
  → viewModel.updateCurrentMailbox(MailboxType.Sent)   currentMailbox 变为 Sent
  → viewModel.resetHomeScreenStates()                  isShowingHomepage 变为 true
  → _uiState 发出新值
  → replyUiState 更新 → Compose 重组
  → 列表显示 Sent 邮箱的邮件，界面回到列表页
```

---

## 第六部分：状态驱动的屏幕切换

### 6.1 两种导航方式的对比

在学习 Cupcake 应用时，你用过 `NavHost` + `NavController` 来跳转屏幕。Reply 应用用了一种完全不同的方式。

| | NavHost 方式（Cupcake） | 状态驱动方式（Reply） |
|--|----------------------|---------------------|
| 核心机制 | 路线字符串 + NavController | ViewModel 布尔字段 + if/else |
| 返回堆栈 | 框架自动管理 | 需要手动实现 |
| 代码量 | 需定义路线和 NavGraph | 只需一个布尔值 |
| 适用场景 | 多个独立页面的导航流 | 简单的主/详情两级切换 |
| 深层链接 | 原生支持 | 需自行实现 |

Reply 应用只有两级——列表页和详情页。用状态驱动更简洁，而且在自适应布局中（大屏上两页可以同时显示），用状态驱动更容易控制。如果页面超过三级，建议用 NavHost。

### 6.2 用 isShowingHomepage 切换屏幕

在 `ReplyHomeScreen.kt` 的 `ReplyHomeScreen` 函数中添加切换逻辑：

```kotlin
@Composable
fun ReplyHomeScreen(
    navigationType: ReplyNavigationType,
    replyUiState: ReplyUiState,
    onTabPressed: (MailboxType) -> Unit,
    onEmailCardPressed: (Email) -> Unit,
    onDetailScreenBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 构建四个标签的数据（图标 + 文字）
    val navigationItemContentList = listOf(
        NavigationItemContent(MailboxType.Inbox,  Icons.Default.Inbox,   "Inbox"),
        NavigationItemContent(MailboxType.Sent,   Icons.AutoMirrored.Filled.Send, "Sent"),
        NavigationItemContent(MailboxType.Drafts, Icons.Default.Drafts,  "Drafts"),
        NavigationItemContent(MailboxType.Spam,   Icons.Default.Report,  "Spam")
    )

    // 大屏：永久抽屉始终包裹内容区，不论当前是列表还是详情
    if (navigationType == ReplyNavigationType.PERMANENT_NAVIGATION_DRAWER
        && replyUiState.isShowingHomepage) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet {
                    NavigationDrawerContent(
                        selectedDestination = replyUiState.currentMailbox,
                        onTabPressed = onTabPressed,
                        navigationItemContentList = navigationItemContentList
                    )
                }
            }
        ) {
            ReplyAppContent(
                navigationType = navigationType,
                replyUiState = replyUiState,
                onTabPressed = onTabPressed,
                onEmailCardPressed = onEmailCardPressed,
                navigationItemContentList = navigationItemContentList,
                modifier = modifier
            )
        }
    } else {
        // 小/中屏：根据 isShowingHomepage 决定显示哪个屏幕
        if (replyUiState.isShowingHomepage) {
            ReplyAppContent(
                navigationType = navigationType,
                replyUiState = replyUiState,
                onTabPressed = onTabPressed,
                onEmailCardPressed = onEmailCardPressed,
                navigationItemContentList = navigationItemContentList,
                modifier = modifier
            )
        } else {
            ReplyDetailsScreen(
                replyUiState = replyUiState,
                onBackPressed = onDetailScreenBackPressed,
                modifier = modifier
            )
        }
    }
}
```

**切换的本质**：Compose 是声明式的，你不需要"命令"UI 跳转到哪里，只需要修改状态，UI 根据状态重新计算应该显示什么。

```
用户点击邮件
  → onEmailCardPressed(email) 触发
  → viewModel.updateDetailsScreenStates(email)
  → _uiState 内部：isShowingHomepage 变为 false
  → StateFlow 发出新值 → Compose 检测到变化 → 触发重组
  → ReplyHomeScreen 里的 if/else 重新判断
  → isShowingHomepage == false → 走 else 分支 → 渲染 ReplyDetailsScreen
```

### 6.3 添加自定义返回处理

使用状态驱动导航的代价是：没有免费的返回堆栈。当用户在详情页按系统返回键时，系统默认行为是退出应用（finish Activity），而不是回到列表。必须用 `BackHandler` 拦截这个事件。

在 `ReplyDetailsScreen.kt` 中，在函数体**最顶部**添加：

```kotlin
import androidx.activity.compose.BackHandler

@Composable
fun ReplyDetailsScreen(
    replyUiState: ReplyUiState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {        // 拦截系统返回键（包括手势返回）
        onBackPressed() // → viewModel.resetHomeScreenStates() → isShowingHomepage = true
    }

    Box(modifier = modifier) {
        LazyColumn(...) {
            item {
                ReplyDetailsScreenTopBar(...)
                ReplyEmailDetailsCard(...)
            }
        }
    }
}
```

**`BackHandler` 必须在 `ReplyDetailsScreen` 内部的原因**：

Compose 的 `BackHandler` 只在它所在的 Composable 处于组合树（Composition）中时才会生效。详情页通过 if/else 条件渲染——当 `isShowingHomepage = true` 时，`ReplyDetailsScreen` 根本不在组合树里，那时候 `BackHandler` 不应该生效（否则在列表页按返回键也会触发）。把 `BackHandler` 放在 `ReplyDetailsScreen` 内部，确保了：它存在 ↔ 详情页显示，完全同步。

---

## 第七部分：三种导航组件的实现

### 7.1 共享的标签数据

三种导航组件都需要显示相同的四个标签。为了避免重复定义，用一个私有数据类 + 列表：

```kotlin
private data class NavigationItemContent(
    val mailboxType: MailboxType,  // 关联的邮箱类型（点击时用来更新状态）
    val icon: ImageVector,         // Material 图标
    val text: String               // 标签文字
)

// 在 ReplyHomeScreen 中定义一次，三种组件共用
val navigationItemContentList = listOf(
    NavigationItemContent(MailboxType.Inbox,  Icons.Default.Inbox,   "Inbox"),
    NavigationItemContent(MailboxType.Sent,   Icons.AutoMirrored.Filled.Send, "Sent"),
    NavigationItemContent(MailboxType.Drafts, Icons.Default.Drafts,  "Drafts"),
    NavigationItemContent(MailboxType.Spam,   Icons.Default.Report,  "Spam")
)
```

### 7.2 底部导航栏（Compact 屏幕）

```kotlin
@Composable
private fun ReplyBottomNavigationBar(
    currentTab: MailboxType,
    onTabPressed: (MailboxType) -> Unit,
    navigationItemContentList: List<NavigationItemContent>,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        for (navItem in navigationItemContentList) {
            NavigationBarItem(
                selected = currentTab == navItem.mailboxType,
                onClick = { onTabPressed(navItem.mailboxType) },
                icon = { Icon(imageVector = navItem.icon, contentDescription = navItem.text) },
                label = { Text(text = navItem.text) }
            )
        }
    }
}
```

**用普通 `for` 循环而不是 `LazyColumn { items(...) }`**：因为四个标签是固定数量，永远不会动态增减，不需要懒加载的优化。直接 `for` 循环最简单清晰。

### 7.3 侧边导航栏（Medium 屏幕）

```kotlin
@Composable
private fun ReplyNavigationRail(
    currentTab: MailboxType,
    onTabPressed: (MailboxType) -> Unit,
    navigationItemContentList: List<NavigationItemContent>,
    modifier: Modifier = Modifier
) {
    NavigationRail(modifier = modifier) {
        for (navItem in navigationItemContentList) {
            NavigationRailItem(
                selected = currentTab == navItem.mailboxType,
                onClick = { onTabPressed(navItem.mailboxType) },
                icon = { Icon(imageVector = navItem.icon, contentDescription = navItem.text) }
            )
        }
    }
}
```

`NavigationRail` 和 `NavigationBar` 的 API 几乎一样，只是显示方向不同（一个横向，一个纵向）。

### 7.4 永久抽屉导航（Expanded 屏幕）

```kotlin
@Composable
private fun NavigationDrawerContent(
    selectedDestination: MailboxType,
    onTabPressed: (MailboxType) -> Unit,
    navigationItemContentList: List<NavigationItemContent>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        // 顶部 Logo 区域
        NavigationDrawerHeader(modifier = Modifier.fillMaxWidth().padding(...))

        // 四个导航项
        for (navItem in navigationItemContentList) {
            NavigationDrawerItem(
                selected = selectedDestination == navItem.mailboxType,
                label = { Text(navItem.text) },
                icon = { Icon(navItem.icon, contentDescription = navItem.text) },
                onClick = { onTabPressed(navItem.mailboxType) }
            )
        }
    }
}
```

永久抽屉（`PermanentNavigationDrawer`）和普通抽屉（`ModalNavigationDrawer`）的区别：永久抽屉始终可见，不需要手势打开，适合屏幕空间充足的大屏设备。

### 7.5 三种组件的横向对比

| 组件 | Material3 API | 布局位置 | 适用屏幕 |
|------|--------------|---------|---------|
| `ReplyBottomNavigationBar` | `NavigationBar` + `NavigationBarItem` | 屏幕底部（Row 内 Column 底部） | Compact |
| `ReplyNavigationRail` | `NavigationRail` + `NavigationRailItem` | 屏幕左侧（Row 左侧） | Medium |
| `NavigationDrawerContent` | `PermanentDrawerSheet` + `NavigationDrawerItem` | 左侧固定抽屉 | Expanded |

### 7.6 ReplyAppContent：把导航组件和内容区组装在一起

```kotlin
@Composable
private fun ReplyAppContent(
    navigationType: ReplyNavigationType,
    replyUiState: ReplyUiState,
    onTabPressed: (MailboxType) -> Unit,
    onEmailCardPressed: (Email) -> Unit,
    navigationItemContentList: List<NavigationItemContent>,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {

        // 侧边导航栏（Medium 模式时显示，带进入/退出动画）
        AnimatedVisibility(visible = navigationType == ReplyNavigationType.NAVIGATION_RAIL) {
            ReplyNavigationRail(
                currentTab = replyUiState.currentMailbox,
                onTabPressed = onTabPressed,
                navigationItemContentList = navigationItemContentList
            )
        }

        // 内容区（邮件列表 + 可能的底部导航栏）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.inverseOnSurface)
        ) {
            ReplyListOnlyContent(
                replyUiState = replyUiState,
                onEmailCardPressed = onEmailCardPressed,
                modifier = Modifier.weight(1f)
            )

            // 底部导航栏（Compact 模式时显示，带进入/退出动画）
            AnimatedVisibility(visible = navigationType == ReplyNavigationType.BOTTOM_NAVIGATION) {
                ReplyBottomNavigationBar(
                    currentTab = replyUiState.currentMailbox,
                    onTabPressed = onTabPressed,
                    navigationItemContentList = navigationItemContentList
                )
            }
        }
    }
}
```

**`AnimatedVisibility` 的作用**：当 `visible` 参数变化时，内部组件会以动画方式进入或退出，而不是直接消失/出现。这让屏幕方向变化（比如折叠屏展开）时的导航切换有渐变效果，而不是生硬的瞬间替换。

**布局结构图**：

```
Row（fillMaxSize）
│
├── [AnimatedVisibility: NavigationRail]   ← Medium 时可见
│    显示在 Row 左侧
│
└── Column（weight(1f)，占剩余宽度）
     │
     ├── ReplyListOnlyContent（weight(1f)）  ← 邮件列表，占 Column 剩余高度
     │
     └── [AnimatedVisibility: BottomNav]    ← Compact 时可见
          显示在 Column 底部
```

---

## 第八部分：测试 — 使用可调整大小的模拟器

### 8.1 创建可调整大小的模拟器

传统方法是为手机、折叠屏、平板各创建一个 AVD，但切换很麻烦。Android Studio 提供了**可调整大小的模拟器（Resizable Emulator）**，一台模拟器可以在手机、折叠屏、平板模式间切换。

创建步骤：
1. Android Studio → **Tools > Device Manager > +**
2. 选择 **Phone** 类别 → 选 **Resizable (Experimental)**
3. 选 API 34 或更高
4. 完成创建

运行后，点击模拟器顶部的模式切换按钮，可以在 Phone / Foldable / Tablet / Desktop 之间切换。切换时应用不重启，可以实时观察布局变化。

### 8.2 验证三种导航组件

| 模拟器模式 | 预期导航组件 |
|----------|------------|
| Phone（竖屏） | 底部导航栏（NavigationBar） |
| Tablet（横屏） | 永久抽屉（PermanentNavigationDrawer） |
| Foldable 展开 | 侧边导航栏（NavigationRail） |

---

## 第九部分：总结与完整数据流

### 9.1 本节课的完整改动

| 文件 | 改了什么 |
|------|---------|
| `build.gradle.kts` | 添加 `material3-window-size-class` 依赖 |
| `MainActivity.kt` | 调用 `calculateWindowSizeClass()`，把结果传给 `ReplyApp` |
| `ui/utils/WindowStateUtils.kt` | **新建**：定义 `ReplyNavigationType` 枚举 |
| `ui/ReplyApp.kt` | 添加 `windowSize` 参数，`when` 语句映射为 `navigationType`，传给 `ReplyHomeScreen` |
| `ui/ReplyHomeScreen.kt` | 根据 `navigationType` 和 `isShowingHomepage` 选择渲染的导航组件和屏幕 |
| `ui/ReplyDetailsScreen.kt` | 添加 `BackHandler` 拦截返回键 |
| `data/` 目录 | **没有改动** |

### 9.2 完整数据流图

```
┌─────────────────────────────────────────────────────────┐
│  MainActivity                                           │
│  calculateWindowSizeClass(this)                         │
│       ↓ WindowWidthSizeClass (Compact/Medium/Expanded)  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  ReplyApp                                               │
│  when(windowSize) → ReplyNavigationType                 │
│  viewModel.uiState.collectAsState() → ReplyUiState      │
│       ↓ navigationType + replyUiState + 3个回调          │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  ReplyHomeScreen                                        │
│  if(PERMANENT_DRAWER && isShowingHomepage)              │
│    → PermanentNavigationDrawer { ReplyAppContent }      │
│  else if(isShowingHomepage)                             │
│    → ReplyAppContent（内含 Rail 或 BottomNav）           │
│  else                                                   │
│    → ReplyDetailsScreen（全屏详情）                      │
└─────────────────────────────────────────────────────────┘

用户交互 → 回调 → ViewModel 修改状态 → StateFlow 发出新值 → 重组
```

### 9.3 关键概念总结

| 概念 | 核心要点 |
|------|---------|
| WindowSizeClass | 把屏幕宽度分为 Compact/Medium/Expanded 三类，替代手动写 dp 判断 |
| 状态驱动导航 | 用 ViewModel 的布尔字段 + if/else 切换屏幕，不用 NavHost |
| BackHandler | 在 Composable 内拦截系统返回键；必须放在详情页 Composable 内部 |
| AnimatedVisibility | 让组件的出现/消失有动画，适合导航模式切换 |
| 背板模式 | `_uiState`（私有可变）+ `uiState`（公开只读），保护状态不被外部随意修改 |
