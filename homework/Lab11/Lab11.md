# Lab11：为 Sports 应用添加大屏自适应布局

## 实验背景

本次实验基于 Jetpack Compose 中的 `material3-window-size-class` 库，为现有的 Sports（运动）应用添加大屏自适应布局功能。

Sports 是一款运动资讯浏览应用，包含 11 种运动的列表和详情展示。当前起始代码在手机（紧凑宽度）上运行正常——用户点击列表中的运动项目，跳转到详情页面。但在平板或大屏设备上，这种"列表/详情二选一"的单窗格布局会浪费屏幕空间。

本次实验的目标是：利用 `WindowSizeClass` 检测设备屏幕尺寸，在大屏设备上采用"列表-详情"（List-Detail）规范布局，让列表和详情并排显示，提升大屏用户体验。

**adaptive_reference** 为参考文件夹（包含两篇 Codelab 教程文档），提交代码时无需提交此文件夹内的任何文件！

---

## 前提条件

- 已掌握 Compose 布局基础、状态管理和 ViewModel 用法
- 已学习 构建具有动态导航栏的自适应应用 相关内容
- 已学习 构建具有自适应布局的应用相关内容
- 了解 `WindowSizeClass` 和 `WindowWidthSizeClass` 的基本概念
- 了解 Material 3 规范布局中的"列表-详情"（List-Detail）模式
- 了解 Compose 中的 `Row`、`Modifier.weight()` 等并排布局技术

---

## 实验目标

完成本实验后，你应能够：

- 使用 `calculateWindowSizeClass()` 获取设备的窗口尺寸类别
- 使用 `WindowWidthSizeClass` 判断当前设备属于紧凑宽度还是展开宽度
- 构建"列表-详情"并排布局，在大屏设备上同时显示列表和详情
- 根据窗口尺寸切换单窗格/双窗格布局
- 处理不同布局模式下的 TopAppBar 行为（标题切换、返回按钮显隐）
- 正确处理大屏模式下的返回键行为
- 编写实验报告说明自适应布局设计思路

---

## 所需资源

### 起始代码

本目录中的 `basic-android-kotlin-compose-training-sports/` 是 Sports 的起始项目代码。请在 Android Studio 中打开该目录即可开始实验。

起始代码结构概述：

| 文件 | 说明 |
|------|------|
| `MainActivity.kt` | 应用入口，调用 `SportsApp()`，**需要修改** |
| `ui/SportsScreens.kt` | **核心文件，包含所有页面的 Composable，需要修改** |
| `ui/SportsViewModel.kt` | ViewModel，管理 UI 状态（列表/详情切换） |
| `utils/WindowStateUtils.kt` | 已定义 `SportsContentType` 枚举（`ListOnly`、`ListAndDetail`），**需要集成使用** |
| `model/Sport.kt` | 运动数据类（id、名称、描述、运动员数、奥运项目等） |
| `data/LocalSportsDataProvider.kt` | 本地运动数据源（11 种运动） |
| `ui/theme/` | 主题颜色、字体和 Material 3 主题配置 |

---

## 起始代码现状分析

### 当前功能

当前 Sports 应用可以在手机设备上正常运行：

- **列表页面**：显示 11 种运动的列表，每项包含图片、名称、描述、运动员数和奥运标识
- **详情页面**：显示选中运动的横幅图、渐变蒙层、运动名称、运动员数和详细描述
- **导航方式**：通过 `isShowingListPage` 状态在列表和详情之间切换（单窗格模式）

### 存在的问题

1. **所有屏幕都使用单窗格布局**：即使在平板等大屏设备上，也只能看到列表或详情中的一个
2. **未使用 `WindowSizeClass`**：虽然 `build.gradle.kts` 中已添加 `material3-window-size-class` 依赖，但代码中未导入和使用
3. **`SportsContentType` 枚举未被使用**：`utils/WindowStateUtils.kt` 中已定义 `ListOnly` 和 `ListAndDetail` 两种内容类型，但无任何代码引用
4. **`MainActivity` 未计算窗口尺寸**：入口 Activity 没有计算 `WindowSizeClass`
5. **缺少大屏并排布局**：没有"列表-详情"并排显示的 Composable

### 好消息

以下内容**已完成**，你无需处理：

- `material3-window-size-class` 依赖已在 `app/build.gradle.kts` 中配置
- `SportsContentType` 枚举已在 `utils/WindowStateUtils.kt` 中定义
- 所有运动数据和 UI 组件已完整实现

---

## 实验任务

### 任务一：打开起始项目

在 Android Studio 中打开本目录下的 `basic-android-kotlin-compose-training-sports/` 项目。

等待 Gradle 同步完成后，浏览项目文件结构，了解各个 Composable 函数的职责和已有代码的组织方式。

**建议**：先在手机模拟器上运行一次应用，体验当前的单窗格导航行为，了解列表页和详情页的外观和交互。

---

### 任务二：在 MainActivity 中计算并传递 WindowSizeClass

#### 目标

在 `MainActivity` 中计算设备的窗口尺寸类别，并将宽度尺寸类别传递给 `SportsApp`。

#### 步骤

1. 在 `MainActivity.kt` 中添加必要的导入：

```kotlin
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
```

2. 在 `setContent` 中，调用 `calculateWindowSizeClass(activity = this)` 获取窗口尺寸类别：

```kotlin
val windowSizeClass = calculateWindowSizeClass(activity = this)
val widthSizeClass = windowSizeClass.widthSizeClass
```

3. 将 `widthSizeClass` 传入 `SportsApp()`。

4. 由于使用了实验性 API，需要在 `MainActivity` 类上添加 `@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)` 注解。

---

### 任务三：修改 SportsApp 以支持自适应布局

#### 目标

修改 `SportsApp` 可组合项，使其接收 `WindowWidthSizeClass` 参数，并根据它决定使用单窗格还是双窗格布局。

#### 步骤

1. 修改 `SportsApp` 的函数签名，添加 `windowWidthSizeClass` 参数：

```kotlin
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.example.sports.utils.SportsContentType

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SportsApp(
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    // ...
}
```

2. 在 `SportsApp` 内部，根据 `windowWidthSizeClass` 判断内容类型：

```kotlin
val contentType = when (windowWidthSizeClass) {
    WindowWidthSizeClass.Expanded -> SportsContentType.ListAndDetail
    else -> SportsContentType.ListOnly
}
```

3. 根据 `contentType` 决定渲染逻辑：
   - 当 `contentType == ListAndDetail` 时，显示 `SportsListAndDetails` 并排布局
   - 当 `contentType == ListOnly` 时，保持原有的单窗格行为

#### 提示

- `WindowWidthSizeClass` 有三个值：`Compact`（紧凑宽度，典型手机）、`Medium`（中等宽度）、`Expanded`（展开宽度，典型平板）
- 本实验建议当 `widthSizeClass == WindowWidthSizeClass.Expanded` 时使用双窗格布局

---

### 任务四：创建 SportsListAndDetails 双窗格布局

#### 目标

在 `SportsScreens.kt` 中创建一个新的 `SportsListAndDetails` 可组合项，在大屏设备上同时显示列表和详情。

#### 要求

1. 使用 `Row` 布局将列表和详情并排显示
2. 左侧显示 `SportsList`（运动列表），右侧显示 `SportsDetail`（当前选中运动的详情）
3. 使用 `Modifier.weight()` 分配比例，例如列表占 1/3，详情占 2/3
4. 点击列表中的某个运动时，右侧详情应同步更新

#### 核心结构

```kotlin
@Composable
private fun SportsListAndDetails(
    sports: List<Sport>,
    currentSport: Sport,
    onSportClick: (Sport) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // 左侧列表
        SportsList(
            sports = sports,
            onClick = onSportClick,
            modifier = Modifier.weight(1f),
            contentPadding = contentPadding
        )
        // 右侧详情
        SportsDetail(
            selectedSport = currentSport,
            onBackPressed = { /* 大屏下无需返回，可设置空实现 */ },
            contentPadding = contentPadding,
            modifier = Modifier.weight(2f)
        )
    }
}
```

#### 注意事项

- 在 `SportsListAndDetails` 模式下，用户同时可以看到列表和详情，因此列表项被点击后无需"导航"到详情页（详情已在右侧显示）
- 详情页的返回按钮在大屏模式下不需要

---

### 任务五：更新 SportsAppBar 适配大屏

#### 目标

修改 `SportsAppBar` 可组合项，使其在大屏（列表+详情并排）模式下表现不同。

#### 要求

1. **大屏模式**：当 `contentType == ListAndDetail` 时：
   - 应用栏始终显示 **"Sports"**（使用 `R.string.list_fragment_label`）
   - **不显示**返回按钮

2. **小屏模式**：当 `contentType == ListOnly` 时：
   - 保持原有行为：列表页显示 "Sports"，详情页显示 "Sport Info"
   - 详情页显示返回按钮，列表页不显示

#### 修改建议

为 `SportsAppBar` 增加参数来控制标题和导航图标：

```kotlin
@Composable
fun SportsAppBar(
    isShowingListPage: Boolean,
    isListAndDetail: Boolean = false,
    onBackButtonClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

---

### 任务六：处理大屏模式下的返回键行为

#### 目标

在大屏（并排）模式下，确保按系统返回键能正确退出应用。

#### 步骤

在 `SportsListAndDetails` 中，当用户按返回键时，应退出整个 Activity（因为用户已经在主屏幕了）。

使用以下方式：

```kotlin
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

val context = LocalContext.current
BackHandler {
    (context as Activity).finish()
}
```

#### 说明

在小屏模式下，`SportsDetail` 已有 `BackHandler` 用于返回列表页；在大屏模式下，列表和详情同时显示，按返回键应退出应用而不是"返回"。

---

### 任务七：添加预览并验证

#### 目标

为 `SportsListAndDetails` 添加 `@Preview` 注解，方便预览效果，并在模拟器上测试。

#### 验证清单

使用**可调整大小的模拟器**（如 Pixel Tablet 或可折叠设备模拟器），分别在以下条件下验证：

- [ ] 在小屏（手机）上，应用行为与修改前一致：列表页 → 点击 → 详情页
- [ ] 在大屏（平板）上，列表和详情并排显示
- [ ] 大屏上点击列表项，右侧详情正确更新
- [ ] 大屏上应用栏始终显示 "Sports"，无返回按钮
- [ ] 大屏上按系统返回键，应用退出
- [ ] 小屏上详情页有返回按钮，点击返回列表
- [ ] 浅色和深色模式下界面正常显示
- [ ] `@Preview` 能正常显示 `SportsListAndDetails`

截图请使用 Android Studio 或模拟器内置截图功能，**严禁使用手机拍屏幕**。

---

## 代码结构参考

完成后的核心代码结构：

```text
app/
└── src/
    └── main/
        └── java/com/example/sports/
            ├── MainActivity.kt              # 计算 WindowSizeClass，传入 SportsApp
            ├── model/
            │   └── Sport.kt                 # 运动数据类
            ├── data/
            │   └── LocalSportsDataProvider.kt  # 本地数据源
            ├── utils/
            │   └── WindowStateUtils.kt      # SportsContentType 枚举
            └── ui/
                ├── SportsScreens.kt         # 核心文件：SportsApp、SportsListAndDetails、SportsAppBar 等
                ├── SportsViewModel.kt       # ViewModel
                └── theme/
                    ├── Color.kt
                    ├── Theme.kt
                    └── Type.kt
```

---

## 提交要求

在自己的文件夹下新建 `Lab11/` 目录，提交以下文件：

```text
学号姓名/
└── Lab11/
    ├── MainActivity.kt             # 修改后的入口 Activity
    ├── SportsScreens.kt            # 修改后的核心 UI 文件
    ├── screenshot_phone.png        # 小屏模式截图（列表页或详情页）
    ├── screenshot_tablet.png       # 大屏模式截图（列表+详情并排显示）
    └── report.md                   # 实验报告
```

> **注意：不要提交整个项目代码。** 只提交上述修改过的源码文件、截图和报告。

`report.md` 需包含：

1. `WindowSizeClass` 的概念简介，以及 `WindowWidthSizeClass` 的三种宽度类别分别适用于什么设备
2. `SportsContentType` 枚举的设计思路，以及为什么使用 `ListOnly` 和 `ListAndDetail` 两种类型
3. `SportsListAndDetails` 的布局设计说明，包括 `Row` 中比例的分配理由
4. `SportsAppBar` 在大屏/小屏下行为差异的设计考虑
5. 返回键的处理策略：小屏和大屏模式下返回键行为不同的原因
6. 实验中遇到的问题与解决过程

---

## 验收标准

满足以下条件可视为完成实验：

- 在 `MainActivity` 中正确计算 `WindowSizeClass` 并传入 `SportsApp`
- `SportsApp` 根据 `WindowWidthSizeClass.Expanded` 正确判断内容布局类型
- 创建了 `SportsListAndDetails` 并排布局，列表在左、详情在右
- 小屏模式（Compact/Medium）保持原有的单窗格导航行为
- 大屏模式（Expanded）下同时显示列表和详情，点击列表项可更新详情
- 大屏模式下 `SportsAppBar` 显示 "Sports" 标题且无返回按钮
- 小屏模式下的返回按钮行为正常
- 大屏模式下按返回键能退出应用
- 报告中能清晰说明自适应布局设计思路

---

## 提示

- `material3-window-size-class` 依赖和 `SportsContentType` 枚举已在起始代码中提供，无需重新创建
- 使用 `calculateWindowSizeClass(activity)` 计算窗口尺寸类别，需要 `Activity` 引用
- `Row` 中使用 `Modifier.weight()` 可以实现灵活的并排比例分配
- 大屏模式下的 `SportsDetail` 不需要 `BackHandler` 返回到列表（因为列表始终可见）
- `BackHandler` 会拦截系统返回键，使用时确保场景正确
- 可以在 Android Studio 中创建平板 AVD（如 Pixel Tablet）或使用可调整大小模拟器来测试大屏效果
- 如果 `@Preview` 需要模拟不同窗口尺寸，可使用 `@Preview(widthDp = 840)` 等参数

---

## 截止时间

**2026-05-25**，届时关于 Lab11 的 PR 请求将不会被合并。

---
