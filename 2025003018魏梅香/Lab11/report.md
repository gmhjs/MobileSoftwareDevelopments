# Lab11 大屏自适应布局实验报告

## 一、实验目的
1. 学习使用 `WindowSizeClass` 实现响应式布局，掌握屏幕尺寸分类方法
2. 完成 Sports 应用的大屏自适应适配，实现「单窗格/双窗格」布局自动切换
3. 理解大屏与小屏的交互差异，完成导航栏、返回键等核心元素的适配
4. 学会通过枚举抽象布局类型，实现尺寸判断与布局逻辑的解耦

## 二、实验环境
- 开发工具：Android Studio
- 开发语言：Kotlin
- 界面框架：Jetpack Compose
- 依赖：Compose Material3、WindowSizeClass、Navigation Compose（可选）

## 三、实验内容
1. 学习 `WindowSizeClass` 概念，掌握 `WindowWidthSizeClass` 三类宽度的适配场景
   - `Compact`（<600dp）：适配手机竖屏，采用单窗格布局
   - `Medium`（600dp~840dp）：适配手机横屏/小平板，采用优化单窗格
   - `Expanded`（>840dp）：适配大屏设备，采用双窗格布局
2. 设计 `SportsContentType` 枚举，抽象布局类型
   - `ListOnly`：适配小屏，实现列表↔详情跳转
   - `ListAndDetail`：适配大屏，实现列表+详情并排展示
3. 实现 `SportsListAndDetails` 双窗格布局
   - 基于 `Row` 实现横向分栏，左侧列表占 `weight=1f`，右侧详情占 `weight=2f`
   - 比例分配理由：列表轻量化信息用1/3宽度，详情多类信息用2/3宽度，同时保证自适应缩放
4. 适配 `SportsAppBar` 在大屏/小屏下的行为差异
   - 大屏：固定标题为「Sports」，不显示返回按钮
   - 小屏：列表页标题为「Sports List」，详情页标题为「Sport Detail」，显示返回按钮
5. 处理返回键在不同模式下的交互逻辑
   - 小屏模式：详情页点击返回键切换回列表页，符合页面跳转逻辑
   - 大屏模式：无返回键，点击列表项仅更新右侧详情，无页面层级变化
6. 解决实验中遇到的问题并完成调试
   - 编译报错：删除冗余 `SportsViewModel.kt`，改用 `remember + mutableStateOf` 管理状态
   - 大屏逻辑不生效：通过模拟器「Extended controls」面板旋转设备，重新运行 App 触发尺寸判断
   - API 过时警告：替换 `Icons.Filled.ArrowBack` 为 `Icons.AutoMirrored.Filled.ArrowBack`
   - 模拟器进程冲突：终止重复进程后重新运行 App

## 四、实验结果
1. 成功实现了 Sports 应用的大屏自适应布局，可在手机（竖屏/横屏）、平板设备上自动切换单/双窗格布局
2. 大屏模式下，列表与详情并排展示，点击列表项可实时更新详情内容，无页面跳转
3. 小屏模式下，列表与详情通过页面跳转实现，返回键功能正常，交互逻辑闭环
4. 应用无编译错误，运行稳定，符合实验预期要求

## 五、实验总结
本次实验掌握了 `WindowSizeClass` 响应式布局的核心用法，理解了大屏与小屏的交互差异，学会了通过枚举抽象布局类型，提升了代码的可维护性。同时解决了 Compose 编译、模拟器运行等实操问题，提升了安卓开发的问题排查能力。实验最终实现的应用满足多终端交互体验要求，达成了实验目标。