# Android Compose 大屏自适应内容布局 

---

## 第一部分：这节课要解决什么问题

### 1.1 从上节课的遗留问题说起

上节课（自适应导航）解决了导航栏的问题：手机用底部栏，折叠屏用侧边栏，大平板用永久抽屉。但如果你现在在平板上运行应用，会发现另一个问题：

**内容区还是只显示邮件列表。** 整个平板屏幕有 11 英寸，右半边全是空白。用户点击一封邮件，还是要全屏跳转到详情页，然后按返回键回来，再点下一封……

这和 iPad 上的邮件应用体验差太多了。iPad 邮件应用左边是邮件列表，右边是邮件内容，两个区域同时可见，用户点击左边的邮件，右边内容立即更新，完全不需要页面跳转。

**这就是这节课要解决的问题**：在大屏上，让邮件列表和邮件详情并排显示。

### 1.2 解决方案预览

```
手机（Compact）:                       平板（Expanded）:
┌──────────────────────┐       ┌────────────┬───────────────────────┐
│  [←] [收] [发] [草]  │       │ 永久抽屉   │  [收] [发] [草] [垃]  │
├──────────────────────┤       ├────────────┼───────────────────────┤
│ 邮件列表（全屏）      │  →   │ 邮件列表   │  邮件详情（同屏）      │
│ ···                  │       │ ···        │  发件人: Ali           │
│ ···                  │       │ ···        │  主题: ···             │
│ ···                  │       │ ···        │  正文: ···             │
├──────────────────────┤       └────────────┴───────────────────────┘
│ [收件箱][发][草][垃]  │
└──────────────────────┘
    LIST_ONLY 模式                      LIST_AND_DETAIL 模式
```

这种"左列表 + 右详情"的布局，在 Material Design 中叫做**列表-详情视图（List-Detail）**，是官方推荐的三种大屏规范布局之一。

### 1.3 学习目标

完成这节课后，你应该能：
- 解释什么是 Material Design 规范布局，以及列表-详情视图的使用场景
- 用 `ReplyContentType` 枚举区分"仅列表"和"列表+详情"两种内容模式
- 理解 `isFullScreen` 参数的设计思路，以及为什么同一个 Composable 需要两种显示模式
- 为自适应应用编写多屏幕尺寸的 Compose Preview
- 理解配置更改（屏幕旋转）测试的原理，以及 `StateRestorationTester` 的使用方式
- 用自定义注解对不同屏幕尺寸的测试进行分组管理

### 1.4 起始代码

```bash
git checkout nav-update  # 起点：自适应导航已完成，内容区待适配
git checkout main        # 终点：两层自适应全部完成
```

起始代码就是上节课的完成状态。窗口大小检测、`ReplyNavigationType` 枚举、三种导航组件的切换逻辑都已经有了，本节课在这个基础上继续。

---

## 第二部分：多屏幕尺寸 Preview

### 2.1 为什么需要多个 Preview

Compose 的 `@Preview` 注解是开发时的"即时预览"工具，不需要运行模拟器就能看效果。对于自适应应用，最佳实践是**为每种屏幕尺寸都建一个 Preview**，这样每次改代码都能立刻看到三种屏幕上的效果。

好处有两个：
1. **开发效率高**：改一行代码，三个 Preview 同时更新，不需要切换三个模拟器
2. **充当文档**：其他开发者打开这个文件，看到三个 Preview 函数，就知道这个应用支持哪些屏幕尺寸

### 2.2 添加三个 Preview

在 `MainActivity.kt` 末尾添加（起始代码里已有 Compact 的，再补 Medium 和 Expanded）：

```kotlin
// Compact 预览（手机）
@Preview(showBackground = true, widthDp = 400)
@Composable
fun ReplyAppCompactPreview() {
    ReplyTheme {
        Surface {
            ReplyApp(windowSize = WindowWidthSizeClass.Compact)
        }
    }
}

// Medium 预览（折叠屏 / 小平板）
@Preview(showBackground = true, widthDp = 700)
@Composable
fun ReplyAppMediumPreview() {
    ReplyTheme {
        Surface {
            ReplyApp(windowSize = WindowWidthSizeClass.Medium)
        }
    }
}

// Expanded 预览（大平板）
@Preview(showBackground = true, widthDp = 1000)
@Composable
fun ReplyAppExpandedPreview() {
    ReplyTheme {
        Surface {
            ReplyApp(windowSize = WindowWidthSizeClass.Expanded)
        }
    }
}
```

### 2.3 一个容易犯的错误

`widthDp` 参数和 `windowSize` 参数**必须同时设置，而且要匹配**。

`widthDp = 700` 只是控制预览窗口的显示宽度，让你能在 IDE 里看到 700 dp 宽的效果图。但 `ReplyApp` 内部的 `when(windowSize)` 判断，用的是你手动传入的 `windowSize` 参数——Preview 没有真实的 Activity，无法调用 `calculateWindowSizeClass()`。

```kotlin
// ❌ 错误写法：widthDp 是 700，但没传 windowSize，默认会用某个值，效果不对
@Preview(widthDp = 700)
fun WrongPreview() {
    ReplyApp() // windowSize 参数缺失或默认值不对
}

// ✅ 正确写法：widthDp 和 windowSize 都要设置，且要对应
@Preview(widthDp = 700)
fun CorrectPreview() {
    ReplyApp(windowSize = WindowWidthSizeClass.Medium)
}
```

`widthDp` 的选值：落在对应范围中间就行，不需要精确到边界值：
- Compact：0–599 dp → 用 400
- Medium：600–839 dp → 用 700
- Expanded：840 dp 以上 → 用 1000

---

## 第三部分：实现自适应内容布局

### 3.1 Material Design 的三种规范布局

在正式写代码之前，先了解 Google 定义的三种大屏规范布局（Canonical Layouts）。这些是经过大量用户研究验证的布局模式，可以直接用作设计和开发的起点：

| 规范布局 | 结构 | 典型应用场景 |
|---------|------|------------|
| **列表-详情视图**（List-Detail） | 左侧列表 + 右侧详情 | 邮件客户端、文件管理器、设置页面 |
| **支持面板**（Supporting Pane） | 主内容区 + 侧边辅助面板 | 文档编辑 + 评论/批注侧栏 |
| **信息流**（Feed） | 多列卡片流式布局 | 新闻、社交媒体、图片库 |

Reply 应用是典型的邮件客户端，选用**列表-详情视图**。

### 3.2 新建内容类型枚举

在 `ui/utils/WindowStateUtils.kt` 中，在已有的 `ReplyNavigationType` 下方添加：

```kotlin
enum class ReplyNavigationType {
    BOTTOM_NAVIGATION,
    NAVIGATION_RAIL,
    PERMANENT_NAVIGATION_DRAWER
}

// 新增：内容布局类型
enum class ReplyContentType {
    LIST_ONLY,       // 仅显示列表，点击邮件后全屏跳转详情页（手机/中屏）
    LIST_AND_DETAIL  // 列表与详情并排显示（大屏）
}
```

为什么只有两种，而不像 `ReplyNavigationType` 那样三种？

因为内容布局是二选一的问题：空间足够就并排，不够就单列。Compact 和 Medium 都不足以并排（列表和详情各自至少需要 300+ dp，加起来超过 840 dp），只有 Expanded 才够。没有第三种有意义的状态。

### 3.3 在 ReplyApp 中同时决定两件事

`ReplyApp.kt` 里的 `when(windowSize)` 现在同时决定**导航类型**和**内容类型**：

```kotlin
@Composable
fun ReplyApp(
    windowSize: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val viewModel: ReplyViewModel = viewModel()
    val replyUiState = viewModel.uiState.collectAsState().value

    val navigationType: ReplyNavigationType
    val contentType: ReplyContentType    // ← 新增

    when (windowSize) {
        WindowWidthSizeClass.Compact -> {
            navigationType = ReplyNavigationType.BOTTOM_NAVIGATION
            contentType    = ReplyContentType.LIST_ONLY
        }
        WindowWidthSizeClass.Medium -> {
            navigationType = ReplyNavigationType.NAVIGATION_RAIL
            contentType    = ReplyContentType.LIST_ONLY    // ← 中屏宽度不够并排
        }
        WindowWidthSizeClass.Expanded -> {
            navigationType = ReplyNavigationType.PERMANENT_NAVIGATION_DRAWER
            contentType    = ReplyContentType.LIST_AND_DETAIL
        }
        else -> {
            navigationType = ReplyNavigationType.BOTTOM_NAVIGATION
            contentType    = ReplyContentType.LIST_ONLY
        }
    }

    ReplyHomeScreen(
        navigationType = navigationType,
        contentType = contentType,          // ← 新增：传给下层
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

**两个决策是独立的**，用一张表看得更清楚：

| windowSize | navigationType | contentType | 直观含义 |
|-----------|---------------|-------------|---------|
| Compact | BOTTOM_NAVIGATION | LIST_ONLY | 手机：底部栏 + 单列列表 |
| Medium | NAVIGATION_RAIL | LIST_ONLY | 中屏：侧边栏 + 单列列表（宽度还不够并排） |
| Expanded | PERMANENT_DRAWER | LIST_AND_DETAIL | 大屏：永久抽屉 + 列表详情并排 |

**重要理解**：Medium 屏幕用了 NavigationRail（侧边导航栏），但内容仍然是 `LIST_ONLY`。这是 Google 经过用户研究得出的结论——600–839 dp 的宽度，扣掉侧边栏的宽度（约 80 dp），剩下约 520–760 dp 的内容区，同时放下可读的邮件列表和邮件详情会太挤。只有 Expanded（840 dp 以上）才有足够的空间做并排布局。

### 3.4 contentType 沿层级向下传递

`contentType` 需要经过三个层级才到达真正使用它的地方：

```
ReplyApp
  └─ ReplyHomeScreen(contentType = contentType)
       └─ ReplyAppContent(contentType = contentType)
            └─ if (contentType == LIST_AND_DETAIL) → ReplyListAndDetailContent
               else → ReplyListOnlyContent
```

**ReplyHomeScreen.kt** 函数签名添加 `contentType` 参数：

```kotlin
@Composable
fun ReplyHomeScreen(
    navigationType: ReplyNavigationType,
    contentType: ReplyContentType,        // ← 新增
    replyUiState: ReplyUiState,
    onTabPressed: (MailboxType) -> Unit,
    onEmailCardPressed: (Email) -> Unit,
    onDetailScreenBackPressed: () -> Unit,
    modifier: Modifier = Modifier
)
```

同时，`ReplyHomeScreen` 内部调用 `ReplyAppContent` 的两处地方，都要加上 `contentType = contentType`。

**ReplyHomeScreen.kt** 中 `ReplyAppContent` 函数签名也添加：

```kotlin
@Composable
private fun ReplyAppContent(
    navigationType: ReplyNavigationType,
    contentType: ReplyContentType,        // ← 新增
    replyUiState: ReplyUiState,
    onTabPressed: (MailboxType) -> Unit,
    onEmailCardPressed: (Email) -> Unit,
    navigationItemContentList: List<NavigationItemContent>,
    modifier: Modifier = Modifier
)
```

### 3.5 ReplyAppContent 内部：根据 contentType 切换布局

这是本节课最核心的改动。`ReplyAppContent` 的 Column 内部，根据 `contentType` 选择显示哪种内容组件：

```kotlin
Column(
    modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.inverseOnSurface)
) {
    if (contentType == ReplyContentType.LIST_AND_DETAIL) {
        // 大屏：列表和详情并排
        ReplyListAndDetailContent(
            replyUiState = replyUiState,
            onEmailCardPressed = onEmailCardPressed,
            modifier = Modifier.weight(1f)
        )
    } else {
        // 手机/中屏：仅列表
        ReplyListOnlyContent(
            replyUiState = replyUiState,
            onEmailCardPressed = onEmailCardPressed,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = dimensionResource(R.dimen.email_list_only_horizontal_padding)
                )
        )
    }

    // 底部导航栏（只在 Compact 模式显示）
    AnimatedVisibility(visible = navigationType == ReplyNavigationType.BOTTOM_NAVIGATION) {
        ReplyBottomNavigationBar(
            currentTab = replyUiState.currentMailbox,
            onTabPressed = onTabPressed,
            navigationItemContentList = navigationItemContentList
        )
    }
}
```

**`Modifier.weight(1f)` 的作用**：让内容区占满 Column 中除了底部导航栏之外的所有高度。底部导航栏会根据自身内容高度占一小块，其余全给内容区。

### 3.6 ReplyListAndDetailContent 的内部结构

`ReplyListAndDetailContent` 在 `ReplyHomeContent.kt` 中定义，内部是一个 `Row`：左边邮件列表，右边邮件详情。

```kotlin
@Composable
fun ReplyListAndDetailContent(
    replyUiState: ReplyUiState,
    onEmailCardPressed: (Email) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as Activity

    Row(modifier = modifier) {
        // 左侧：邮件列表（占 Row 宽度的一部分）
        ReplyListOnlyContent(
            replyUiState = replyUiState,
            onEmailCardPressed = onEmailCardPressed,
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = dimensionResource(R.dimen.email_list_only_horizontal_padding)
                )
        )

        // 右侧：邮件详情（嵌入式，不是全屏）
        ReplyDetailsScreen(
            replyUiState = replyUiState,
            modifier = Modifier.weight(1f),
            onBackPressed = { activity.finish() }  // ← 大屏返回键直接退出应用
        )
    }
}
```

这里有一个很重要的细节：`ReplyDetailsScreen` 的 `onBackPressed` 不再是 `viewModel.resetHomeScreenStates()`，而是 `activity.finish()`。下一节会详细解释原因。

### 3.7 永久抽屉的条件判断调整

上节课的代码里，永久抽屉只在 `isShowingHomepage = true` 时显示：

```kotlin
// 上节课的写法（需要修改）
if (navigationType == PERMANENT_NAVIGATION_DRAWER && replyUiState.isShowingHomepage) {
    PermanentNavigationDrawer(...) { ReplyAppContent(...) }
} else {
    if (replyUiState.isShowingHomepage) {
        ReplyAppContent(...)
    } else {
        ReplyDetailsScreen(...)  // 全屏详情
    }
}
```

现在需要去掉 `&& replyUiState.isShowingHomepage` 这个条件：

```kotlin
// 新写法
if (navigationType == ReplyNavigationType.PERMANENT_NAVIGATION_DRAWER) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(Modifier.width(dimensionResource(R.dimen.drawer_width))) {
                NavigationDrawerContent(...)
            }
        }
    ) {
        ReplyAppContent(
            navigationType = navigationType,
            contentType = contentType,
            replyUiState = replyUiState,
            ...
        )
    }
} else {
    if (replyUiState.isShowingHomepage) {
        ReplyAppContent(...)
    } else {
        ReplyDetailsScreen(isFullScreen = true, ...)  // 小/中屏的全屏详情页
    }
}
```

**为什么要去掉 `&& replyUiState.isShowingHomepage`？**

逻辑变了：
- **上节课（只有 LIST_ONLY）**：大屏上用户点击邮件，也会跳到全屏详情页（`isShowingHomepage = false`）。这时候没有列表，也不需要抽屉导航，所以才加了 `&& isShowingHomepage` 的限制。
- **本节课（有 LIST_AND_DETAIL）**：大屏上列表和详情始终并排显示，`isShowingHomepage` 对大屏来说失去了意义——用户点击邮件只是更新右侧面板的内容，没有"跳转"这个动作。永久抽屉应该始终显示，不受 `isShowingHomepage` 影响。

---

## 第四部分：详情页的双模式适配

### 4.1 问题描述

`ReplyDetailsScreen` 最初是作为独立的全屏页面设计的，包含这些元素：

```
┌────────────────────────────────────────┐
│ ← 返回    邮件主题（大标题）            │  ← ReplyDetailsScreenTopBar
├────────────────────────────────────────┤
│  [头像]  发件人                         │
│  邮件主题（在卡片内再次显示）            │  ← ReplyEmailDetailsCard
│  邮件正文                               │
│  [回复] [回复全部]                      │
└────────────────────────────────────────┘
  大的左右内边距（因为是全屏独占）
```

当它被嵌入到大屏的右侧面板时，这些元素就变得多余甚至碍事：

| 元素 | 全屏独立页面 | 嵌入式右侧面板 |
|------|------------|--------------|
| 返回按钮 + 顶栏 | 需要，用来导航回列表 | **不需要**，列表始终可见 |
| 顶栏里的邮件主题 | 需要，作为页面标题 | **不需要**，详情卡片里会显示 |
| 卡片内的邮件主题 | 不需要，顶栏已有 | **需要**，没有顶栏 |
| 左右大内边距 | 需要，全屏独占时的视觉均衡 | **不需要**，只需右边距 |

解决方案：给 `ReplyDetailsScreen` 加一个 `isFullScreen` 参数，根据模式条件渲染不同的 UI 元素。

### 4.2 修改 ReplyDetailsScreen

**ReplyDetailsScreen.kt**：

```kotlin
@Composable
fun ReplyDetailsScreen(
    replyUiState: ReplyUiState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false    // ← 新增，默认 false（嵌入式模式）
) {
    BackHandler {
        onBackPressed()
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.inverseOnSurface)
                .padding(top = dimensionResource(R.dimen.detail_card_list_padding_top))
        ) {
            item {
                // ① 顶栏（返回按钮 + 标题）：只在全屏模式显示
                if (isFullScreen) {
                    ReplyDetailsScreenTopBar(
                        onBackPressed = onBackPressed,
                        replyUiState = replyUiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(R.dimen.detail_topbar_padding_bottom))
                    )
                }

                // ② 详情卡片：内边距根据模式调整
                ReplyEmailDetailsCard(
                    email = replyUiState.currentSelectedEmail,
                    mailboxType = replyUiState.currentMailbox,
                    isFullScreen = isFullScreen,
                    modifier = if (isFullScreen) {
                        Modifier.padding(
                            horizontal = dimensionResource(R.dimen.detail_card_outer_padding_horizontal)
                        )
                    } else {
                        Modifier.padding(
                            end = dimensionResource(R.dimen.detail_card_outer_padding_horizontal)
                        )
                    }
                )
            }
        }
    }
}
```

**`isFullScreen: Boolean = false` 默认值的设计意图**：

默认值设为 `false`（嵌入式模式），是因为在 `ReplyListAndDetailContent` 中调用时不需要传这个参数：

```kotlin
// 大屏并排布局中，不传 isFullScreen，默认就是 false（嵌入式）
ReplyDetailsScreen(
    replyUiState = replyUiState,
    onBackPressed = { activity.finish() }
)
```

只有在手机/中屏的全屏详情页中，才需要显式传 `isFullScreen = true`：

```kotlin
// 小/中屏的全屏详情页
ReplyDetailsScreen(
    replyUiState = replyUiState,
    isFullScreen = true,
    onBackPressed = onDetailScreenBackPressed
)
```

### 4.3 修改 ReplyEmailDetailsCard

`ReplyEmailDetailsCard` 是详情卡片的内部组件，也需要接收 `isFullScreen` 参数，控制邮件主题文字的显示：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplyEmailDetailsCard(
    email: Email,
    mailboxType: MailboxType,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false       // ← 新增
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.detail_card_inner_padding))
        ) {
            // 发件人信息（头像 + 姓名 + 时间）：始终显示
            DetailsScreenHeader(
                email = email,
                modifier = Modifier.fillMaxWidth()
            )

            // 邮件主题：根据模式决定显示方式
            if (isFullScreen) {
                // 全屏模式：顶栏已经显示了主题，这里只加间距
                Spacer(
                    modifier = Modifier.height(
                        dimensionResource(R.dimen.detail_content_padding_top)
                    )
                )
            } else {
                // 嵌入式模式：没有顶栏，需要在卡片内显示主题
                Text(
                    text = stringResource(email.subject),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.detail_content_padding_top),
                        bottom = dimensionResource(R.dimen.detail_expanded_subject_body_spacing)
                    )
                )
            }

            // 邮件正文：始终显示
            Text(
                text = stringResource(email.body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 操作按钮（回复/删除等）：始终显示
            DetailsScreenButtonBar(mailboxType, displayToast)
        }
    }
}
```

**信息不重复原则**：邮件主题这个信息在全屏和嵌入式两种模式下都需要显示，但由不同的 UI 元素承载：
- 全屏：由顶栏（`ReplyDetailsScreenTopBar`）里的大标题显示
- 嵌入式：由卡片内（`ReplyEmailDetailsCard`）里的文字显示

任何时候主题只出现一次，不重复。这是 Material Design 信息层级设计的基本原则。

---

## 第五部分：返回键行为的差异

### 5.1 两种模式下返回键应该干什么

| 模式 | 场景 | 按返回键的期望行为 |
|------|------|-----------------|
| 手机全屏详情 | 用户点击邮件跳到全屏详情页 | **回到邮件列表**（`isShowingHomepage = true`）|
| 平板嵌入式详情 | 列表和详情始终并排，没有"跳转" | **退出应用**（`activity.finish()`）|

在平板上，用户随时都能看到左侧的邮件列表并点击切换，不存在"需要返回列表"的场景。这时按系统返回键，应该像浏览器按后退键一样——如果没有可后退的页面，就关闭应用。

### 5.2 代码实现

在 `ReplyListAndDetailContent` 中调用 `ReplyDetailsScreen` 时，`onBackPressed` 传入的是 `activity.finish()`：

```kotlin
@Composable
fun ReplyListAndDetailContent(
    replyUiState: ReplyUiState,
    onEmailCardPressed: (Email) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as Activity  // ← 获取当前 Activity

    Row(modifier = modifier) {
        ReplyListOnlyContent(
            replyUiState = replyUiState,
            onEmailCardPressed = onEmailCardPressed,
            modifier = Modifier.weight(1f)
        )

        ReplyDetailsScreen(
            replyUiState = replyUiState,
            modifier = Modifier.weight(1f),
            onBackPressed = { activity.finish() }   // ← 退出应用
        )
    }
}
```

**`LocalContext.current as Activity` 的原理**：

`LocalContext` 是 Compose 提供的 `CompositionLocal`，存储了当前的 `Context`。在 Activity 中运行的 Compose，`LocalContext.current` 就是这个 Activity 本身（`Context` 的子类）。把它强转为 `Activity` 就能调用 `finish()` 方法。

这是在 Compose 中退出 Activity 的标准写法。

---

## 第六部分：测试

### 6.1 为什么自适应应用的测试更复杂

普通应用的测试：一种布局，写一套测试，跑通就行。

自适应应用的测试：三种屏幕有三种不同的 UI，每套测试只在对应的屏幕上有意义：
- "验证底部导航栏存在" → 只在 Compact 屏幕上应该通过
- "验证侧边导航栏存在" → 只在 Medium 屏幕上应该通过
- "验证永久抽屉存在" → 只在 Expanded 屏幕上应该通过

如果在手机上运行"验证侧边导航栏存在"的测试，它会失败——不是代码有 bug，而是这个测试本来就不该在手机上跑。

解决方案：**用自定义注解对测试分组**，让每套测试只在匹配的设备上运行。

### 6.2 给导航组件添加 testTag

Compose 测试通过 `testTag` 来查找 UI 节点，需要提前给要测试的组件打标签。

**第一步**：在 `strings.xml` 中添加标签字符串（用字符串资源而不是硬编码，避免拼写不一致）：

```xml
<resources>
    <string name="navigation_bottom">Navigation Bottom</string>
    <string name="navigation_rail">Navigation Rail</string>
    <string name="navigation_drawer">Navigation Drawer</string>
    <string name="details_screen">Details Screen</string>
    <string name="navigation_back">Back</string>
</resources>
```

**第二步**：给各导航组件添加 `testTag`：

```kotlin
// 底部导航栏（ReplyHomeScreen.kt）
val bottomNavTag = stringResource(R.string.navigation_bottom)
AnimatedVisibility(visible = navigationType == BOTTOM_NAVIGATION) {
    ReplyBottomNavigationBar(
        modifier = Modifier.testTag(bottomNavTag)
        // ...
    )
}

// 侧边导航栏（ReplyHomeScreen.kt）
val railTag = stringResource(R.string.navigation_rail)
AnimatedVisibility(visible = navigationType == NAVIGATION_RAIL) {
    ReplyNavigationRail(
        modifier = Modifier.testTag(railTag)
        // ...
    )
}

// 永久抽屉（ReplyHomeScreen.kt）
val drawerTag = stringResource(R.string.navigation_drawer)
PermanentNavigationDrawer(
    modifier = Modifier.testTag(drawerTag)
    // ...
)

// 详情屏幕（ReplyDetailsScreen.kt）
val detailsTag = stringResource(R.string.details_screen)
Box(modifier = modifier.testTag(detailsTag)) { ... }
```

### 6.3 创建导航组件存在性测试

在 `androidTest/` 目录下创建 `ReplyAppTest.kt`：

```kotlin
class ReplyAppTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    // ComponentActivity 是空的 Activity，不是 MainActivity
    // 用它的好处：可以精确控制传给 ReplyApp 的参数，不受真实设备屏幕尺寸影响

    @Test
    fun compactDevice_verifyUsingBottomNavigation() {
        composeTestRule.setContent {
            ReplyApp(windowSize = WindowWidthSizeClass.Compact)  // 直接传 Compact
        }
        composeTestRule
            .onNodeWithTagForStringId(R.string.navigation_bottom)
            .assertExists()
    }

    @Test
    fun mediumDevice_verifyUsingNavigationRail() {
        composeTestRule.setContent {
            ReplyApp(windowSize = WindowWidthSizeClass.Medium)
        }
        composeTestRule
            .onNodeWithTagForStringId(R.string.navigation_rail)
            .assertExists()
    }

    @Test
    fun expandedDevice_verifyUsingNavigationDrawer() {
        composeTestRule.setContent {
            ReplyApp(windowSize = WindowWidthSizeClass.Expanded)
        }
        composeTestRule
            .onNodeWithTagForStringId(R.string.navigation_drawer)
            .assertExists()
    }
}
```

**`createAndroidComposeRule<ComponentActivity>()` 而不是 `createAndroidComposeRule<MainActivity>()`**：

如果用 `MainActivity`，测试运行时会执行 `calculateWindowSizeClass(this)`，计算出**真实设备**的屏幕尺寸，然后你传的 `windowSize = WindowWidthSizeClass.Medium` 就被忽略了——因为在真实手机模拟器上，真实宽度是 Compact。

用 `ComponentActivity` 启动一个空白 Activity，然后用 `setContent` 精确控制传入的参数，测试结果不受运行设备的实际尺寸影响。

### 6.4 配置更改（屏幕旋转）状态保留测试

用户旋转屏幕时，Android 系统默认会销毁并重建 Activity。如果状态没有正确保存，用户旋转后就会看到应用回到初始状态（比如正在看的邮件被重置为第一封）。

大屏应用质量要求（Google Play 第 3 层级）明确规定：应用必须在配置更改后保留或恢复状态。

在 `androidTest/` 目录下创建 `ReplyAppStateRestorationTest.kt`：

```kotlin
class ReplyAppStateRestorationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // 测试：Compact 屏幕，旋转后选中的邮件保留
    @Test
    fun compactDevice_selectedEmailRetained_afterConfigChange() {

        // 1. 使用 StateRestorationTester 而不是直接 setContent
        //    原因：它能模拟 Activity 销毁并用保存的状态重建的过程
        val stateRestorationTester = StateRestorationTester(composeTestRule)
        stateRestorationTester.setContent {
            ReplyApp(windowSize = WindowWidthSizeClass.Compact)
        }

        // 2. 确认第 3 封邮件（索引 2）在列表中可见
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].body)
        ).assertIsDisplayed()

        // 3. 点击第 3 封邮件的主题 → 触发 onEmailCardPressed → 跳转详情页
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].subject)
        ).performClick()

        // 4. 验证已经进入详情页：返回按钮存在
        composeTestRule.onNodeWithContentDescriptionForStringId(
            R.string.navigation_back
        ).assertExists()

        // 5. 验证详情页显示的是第 3 封邮件
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].body)
        ).assertExists()

        // 6. 模拟配置更改（类似屏幕旋转）
        stateRestorationTester.emulateSavedInstanceStateRestore()

        // 7. 验证：配置更改后仍在详情页，仍显示第 3 封邮件
        composeTestRule.onNodeWithContentDescriptionForStringId(
            R.string.navigation_back
        ).assertExists()
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].body)
        ).assertExists()
    }
}
```

**`stateRestorationTester.emulateSavedInstanceStateRestore()` 内部做了什么**：

1. 把当前 Compose 树的 `SavedInstanceState` 保存下来
2. 销毁当前 Composable 树
3. 用保存的 `SavedInstanceState` 重新创建 Composable 树

这模拟了 Activity 被系统销毁并用保存的状态重建的完整过程，不需要真正旋转设备。

**为什么这个测试应该通过**：

Reply 应用使用 `ViewModel` + `StateFlow` 管理状态，而 `ViewModel` 的生命周期比 Activity 长——Activity 因配置更改被销毁时，ViewModel 不会被清除。所以新的 Activity 重建后，`collectAsState()` 仍然收到之前的状态值，UI 恢复到配置更改前的状态。

**对应的 Expanded 屏幕测试**：

```kotlin
@Test
fun expandedDevice_selectedEmailRetained_afterConfigChange() {
    val stateRestorationTester = StateRestorationTester(composeTestRule)
    stateRestorationTester.setContent {
        ReplyApp(windowSize = WindowWidthSizeClass.Expanded)
    }

    // 确认第 3 封邮件可见
    composeTestRule.onNodeWithText(
        composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].body)
    ).assertIsDisplayed()

    // 点击第 3 封邮件的主题（大屏上点击更新右侧面板，不跳转）
    composeTestRule.onNodeWithText(
        composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].subject)
    ).performClick()

    // 验证右侧详情面板显示了第 3 封邮件
    // 注意：不用检查返回按钮（大屏的详情是嵌入式，没有返回按钮）
    // 改为通过 testTag 找到详情面板，再检查其子节点
    composeTestRule
        .onNodeWithTagForStringId(R.string.details_screen)
        .onChildren()
        .assertAny(
            hasAnyDescendant(
                hasText(composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].body))
            )
        )

    // 模拟配置更改
    stateRestorationTester.emulateSavedInstanceStateRestore()

    // 验证仍显示第 3 封邮件
    composeTestRule
        .onNodeWithTagForStringId(R.string.details_screen)
        .onChildren()
        .assertAny(
            hasAnyDescendant(
                hasText(composeTestRule.activity.getString(LocalEmailsDataProvider.allEmails[2].body))
            )
        )
}
```

**Compact 和 Expanded 测试的差异**：

| | Compact（全屏详情） | Expanded（并排详情） |
|--|------------------|-------------------|
| 点击邮件后 | 跳转到全屏详情页 | 更新右侧面板（同屏） |
| 验证详情页方式 | 检查返回按钮是否存在 | 通过 `testTag` 找详情面板 |
| 查找层级 | 直接查找邮件正文文字 | `.onChildren().assertAny(hasAnyDescendant(hasText(...)))` |

Expanded 需要多层级查找，是因为详情是嵌入在 `Row` 里的面板，不是独占页面——需要先定位到面板容器，再在它的子节点里找邮件内容。

### 6.5 用自定义注解分组测试

**问题**：如果在手机模拟器上运行全部测试，`expandedDevice_verifyUsingNavigationDrawer` 会失败（因为真实屏幕是 Compact，没有抽屉）——不是代码有 bug，是测试在错误的设备上运行了。

**解决方案**：创建三个注解类，给测试函数贴标签，然后按注解过滤运行。

**第一步**：新建 `TestAnnotations.kt`（在 `androidTest/` 目录下）：

```kotlin
package com.example.reply.test

// 三个注解类，没有参数，纯粹作为标记
annotation class TestCompactWidth
annotation class TestMediumWidth
annotation class TestExpandedWidth
```

**第二步**：给测试函数贴对应的注解：

```kotlin
// ReplyAppTest.kt
@TestCompactWidth
@Test
fun compactDevice_verifyUsingBottomNavigation() { ... }

@TestMediumWidth
@Test
fun mediumDevice_verifyUsingNavigationRail() { ... }

@TestExpandedWidth
@Test
fun expandedDevice_verifyUsingNavigationDrawer() { ... }

// ReplyAppStateRestorationTest.kt
@TestCompactWidth
@Test
fun compactDevice_selectedEmailRetained_afterConfigChange() { ... }

@TestExpandedWidth
@Test
fun expandedDevice_selectedEmailRetained_afterConfigChange() { ... }
```

**第三步**：在 Android Studio 中创建分组运行配置：

1. **Run > Edit Configurations > +（添加）> Android Instrumented Tests**
2. 配置名称：`Compact tests`
3. 运行方式：`All in Package`，包名填 `com.example.reply`
4. 点击 `Instrumentation arguments` 旁边的 `...`
5. 点 `+`，添加键 `annotation`，值 `com.example.reply.test.TestCompactWidth`
6. 确定保存

对 Medium 和 Expanded 重复上述步骤，得到三套运行配置：

| 配置名 | annotation 值 | 在哪个设备上运行 |
|-------|--------------|---------------|
| Compact tests | `TestCompactWidth` | 手机模拟器 |
| Medium tests | `TestMediumWidth` | 可折叠/小平板 |
| Expanded tests | `TestExpandedWidth` | 大型平板 |

---

## 第七部分：总结

### 7.1 本节课的完整改动

| 文件 | 改了什么 |
|------|---------|
| `ui/utils/WindowStateUtils.kt` | 新增 `ReplyContentType` 枚举（LIST_ONLY / LIST_AND_DETAIL） |
| `ui/ReplyApp.kt` | `when(windowSize)` 同时决定 `navigationType` 和 `contentType`，两者都传给 `ReplyHomeScreen` |
| `ui/ReplyHomeScreen.kt` | 添加 `contentType` 参数；去掉永久抽屉的 `isShowingHomepage` 条件；`ReplyAppContent` 内根据 `contentType` 选择 `ReplyListAndDetailContent` 或 `ReplyListOnlyContent` |
| `ui/ReplyHomeContent.kt` | 新增 `ReplyListAndDetailContent`：Row 布局，左邮件列表 + 右详情面板 |
| `ui/ReplyDetailsScreen.kt` | 添加 `isFullScreen` 参数；条件控制顶栏、主题文字、内边距的显示方式 |
| `MainActivity.kt` | 新增 Medium 和 Expanded 两个 Preview 函数 |
| `androidTest/ReplyAppTest.kt` | 新建：三种屏幕的导航组件存在性测试 |
| `androidTest/ReplyAppStateRestorationTest.kt` | 新建：配置更改后状态保留测试 |
| `androidTest/TestAnnotations.kt` | 新建：测试分组注解 |
| `data/` 目录 | **没有改动** |

### 7.2 两节课合并的完整自适应逻辑

```
windowSize（来自 MainActivity）
       ↓
ReplyApp 中的 when(windowSize) 产生两个决策：
       ↓                    ↓
 navigationType          contentType
       ↓                    ↓
  控制导航外壳           控制内容布局
       ↓                    ↓
 ┌─────────────┐      ┌──────────────────┐
 │ Compact     │      │ LIST_ONLY        │
 │ 底部导航栏  │      │ 邮件列表（全宽）  │
 │             │      │ 点击→全屏详情页  │
 ├─────────────┤      ├──────────────────┤
 │ Medium      │      │ LIST_ONLY        │
 │ 侧边导航栏  │      │ 邮件列表（去除   │
 │             │      │ 侧边栏宽度）     │
 ├─────────────┤      ├──────────────────┤
 │ Expanded    │      │ LIST_AND_DETAIL  │
 │ 永久抽屉    │      │ 左:列表 右:详情  │
 │             │      │ 点击→更新右面板  │
 └─────────────┘      └──────────────────┘
```

### 7.3 关键概念总结

| 概念 | 核心要点 |
|------|---------|
| 规范布局（Canonical Layouts） | Material Design 为大屏定义的三种标准布局：列表-详情视图、支持面板、信息流 |
| ReplyContentType | 枚举，区分 LIST_ONLY 和 LIST_AND_DETAIL 两种内容模式 |
| isFullScreen | 让同一个 Composable（`ReplyDetailsScreen`）在全屏和嵌入式两种场景下有不同的 UI 细节 |
| activity.finish() | 在 Compose 中退出 Activity 的标准写法：`LocalContext.current as Activity` 再 `.finish()` |
| StateRestorationTester | 模拟 Activity 销毁重建（配置更改），测试状态是否正确保留 |
| 测试注解分组 | 用自定义注解（`@TestCompactWidth` 等）给测试打标签，通过 instrumentation arguments 过滤，确保每套测试在正确的设备上运行 |
