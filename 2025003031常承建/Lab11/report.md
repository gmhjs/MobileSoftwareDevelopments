# Lab11 实验报告：为 Sports 应用添加大屏自适应布局

## 1. WindowSizeClass 概念简介
`WindowSizeClass` 是 Jetpack Compose Material3 提供的窗口尺寸分类工具，用于统一适配不同屏幕尺寸的设备。它将窗口尺寸分为**宽度**和**高度**两个维度，其中 `WindowWidthSizeClass` 是适配大屏的核心：
- `Compact`（紧凑）：宽度 < 600dp，典型手机竖屏
- `Medium`（中等）：600dp ≤ 宽度 < 840dp，典型手机横屏/小平板
- `Expanded`（展开）：宽度 ≥ 840dp，典型平板/大屏设备

本实验中，仅通过宽度维度判断布局类型：`Expanded` 用双窗格，其余用单窗格。

## 2. SportsContentType 枚举设计思路
`SportsContentType` 定义了两种内容展示类型：
- `ListOnly`：仅显示列表（小屏单窗格），用户需点击列表项跳转到详情页
- `ListAndDetail`：同时显示列表和详情（大屏双窗格），充分利用大屏空间

设计理由：
- 枚举将"布局类型"抽象为独立概念，与具体布局实现解耦
- 简化 `SportsApp` 中的逻辑判断，只需根据枚举值切换布局
- 便于后续扩展（如新增其他布局类型）

## 3. SportsListAndDetail 布局设计说明
### 核心结构
使用 `Row` 作为根布局，将 `SportsList` 和 `SportsDetail` 横向排列：
- 列表部分：设置 `Modifier.weight(2f)`，占总宽度的 2/5
- 详情部分：设置 `Modifier.weight(3f)`，占总宽度的 3/5

### 比例分配理由
- 列表仅需展示简洁的运动项，无需过多宽度（2/5 足够）
- 详情页包含横幅图、详细文本，需要更多视觉空间（3/5 提升阅读体验）
- 非对称比例符合 Material3 设计规范，避免列表/详情均分导致的视觉失衡

### 交互逻辑
- 点击列表项时，仅更新 `viewModel` 中的 `currentSport`，详情页自动响应状态变化
- 大屏模式下无需页面跳转，减少用户操作成本

## 4. SportsAppBar 大屏/小屏行为差异设计
### 设计原则
- **大屏模式（ListAndDetail）**：
  - 标题固定为 "Sports"：用户始终能看到列表，无需切换标题
  - 隐藏返回按钮：大屏无页面跳转，返回按钮无意义
- **小屏模式（ListOnly）**：
  - 列表页标题为 "Sports"，详情页为 "Sport Info"：明确当前页面定位
  - 详情页显示返回按钮：支持用户返回列表页

### 实现方式
为 `SportsAppBar` 新增 `isListAndDetail` 参数，通过该参数控制：
1. 标题文本的选择逻辑
2. 返回按钮的显隐状态

## 5. 返回键处理策略
### 小屏模式
- 列表页：返回键退出应用
- 详情页：`BackHandler` 拦截返回键，触发 `viewModel.navigateToListPage()` 回到列表页

### 大屏模式
- 列表和详情同时显示，无"页面层级"概念
- `SportsListAndDetail` 中通过 `BackHandler` 直接调用 `onBackPressed`（退出 Activity）
- 理由：大屏模式下用户始终在"主界面"，返回键应直接退出应用，符合用户对大屏应用的交互预期

## 6. 实验问题与解决过程
### 问题1：大屏模式下详情页仍显示返回按钮
- 现象：大屏布局中详情页的 `BackHandler` 仍生效，返回键会触发无意义的操作
- 解决：给大屏模式下的 `SportsDetail` 传递空实现的 `onBackPressed`，并在 `SportsListAndDetail` 中统一处理返回键

### 问题2：WindowSizeClass 计算后未正确传递
- 现象：大屏设备仍显示单窗格布局
- 解决：检查 `MainActivity` 中 `calculateWindowSizeClass` 的调用方式，确保传递 `Activity` 实例，并将 `widthSizeClass` 正确传入 `SportsApp`

### 问题3：预览大屏布局时尺寸不符
- 现象：`@Preview` 中大屏预览显示为小屏样式
- 解决：为预览添加 `device = Devices.TABLET` 和 `widthDp = 840` 参数，模拟平板尺寸

### 问题4：大屏布局中 padding 错乱
- 现象：大屏模式下列表/详情的内边距超出预期
- 解决：为大屏布局的 `SportsList` 和 `SportsDetail` 单独处理 `contentPadding`，仅保留顶部 padding，移除水平 padding 叠加

## 7. 实验总结
本次实验通过 `WindowSizeClass` 实现了 Sports 应用的大屏自适应：
- 小屏保持原有单窗格导航，符合手机用户习惯
- 大屏采用列表+详情双窗格布局，提升空间利用率
- 统一处理了标题栏、返回键等细节，保证不同布局下的交互一致性
- 核心思路：**状态驱动布局**，通过 ViewModel 管理选中状态，布局仅响应状态变化，实现了状态与UI的解耦