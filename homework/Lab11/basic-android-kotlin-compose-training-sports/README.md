# Sports - 自适应布局练习（起始代码）

## 简介

Sports 应用是一个运动资讯浏览应用，展示了 Jetpack Compose 中自适应布局的概念。

该应用包含 11 种运动的列表和详情展示。起始代码在手机设备上运行正常，但尚未适配大屏幕——本次实验的目标是添加大屏自适应布局，利用"列表-详情"规范布局在平板设备上实现并排显示。

## 预备知识

- Kotlin 语法基础
- 在 Android Studio 中创建和运行项目
- 编写 Composable 函数
- 已学习 ViewModel 和 StateFlow 的基本用法
- 已学习 `WindowSizeClass` 和自适应布局相关概念

## 快速开始

1. 安装 [Android Studio](https://developer.android.com/studio)（如未安装）
2. 在 Android Studio 中打开此项目目录
3. 等待 Gradle 同步完成
4. 在模拟器或真机上构建并运行

## 项目结构

```
app/src/main/java/com/example/sports/
├── MainActivity.kt              # 应用入口（需修改）
├── model/Sport.kt               # 运动数据模型
├── data/
│   └── LocalSportsDataProvider.kt  # 本地数据源
├── utils/
│   └── WindowStateUtils.kt      # SportsContentType 枚举（已定义）
└── ui/
    ├── SportsScreens.kt         # 核心 UI 文件（需修改）
    ├── SportsViewModel.kt       # ViewModel
    └── theme/                   # 主题文件
```

## 实验任务概要

1. 在 `MainActivity` 中计算 `WindowSizeClass` 并传入 `SportsApp`
2. 修改 `SportsApp` 根据窗口宽度判断布局类型
3. 创建 `SportsListAndDetails` 双窗格并排布局
4. 更新 `SportsAppBar` 适配大屏行为
5. 处理不同布局模式下的返回键行为

详见 [Lab11.md](../../Lab11.md)。
