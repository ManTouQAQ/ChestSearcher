# 箱子搜索器(ChestSearcher)
`ChestSearcher`是一个搜索附近有没有存在某些物品的箱子的小插件
## 需求
- Paper 1.20.6
- Java 21
## 截图
![001](/screenshots/001.png "001")
## 特点
- 搜索后会高亮查找到的容器 (基于数据包)
- 异步查询
## 使用方法
- 将插件放入服务器的/plugins文件夹下
- 然后就没有了=w=
## 命令
### `/search [type] [radius]`
- **介绍:** 搜索附近的箱子是否存在指定物品, 默认`raduis = 16`
- **用法:**
    - `/search` - 搜索手上的物品
    - `/search <type>` - 搜索指定的物品, 使用的是`minecraft:stone`这样的注册名
    - `/search <type> <radius>` - 搜索附近`radius`格的箱子是否存在物品`type`
## 支持的语言
- en_US (默认语言)
- zh_CN

语言通过AdventureAPI会自动选择需要的语言
