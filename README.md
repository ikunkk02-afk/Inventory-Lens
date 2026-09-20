# Inventory Lens

Minecraft Java **1.21.1 / Fabric / Mojang Mappings / Java 21**。

第一阶段：准星直接指向有装备的生物时，在其身体旁边显示真正处于世界空间中的装备图标面板。
面板随实体插值移动并面向摄像机，使用浅灰色立体边框和凹槽布局，包含数量、原版附魔光效和耐久条。
左列从上到下为头盔、胸甲、护腿、靴子；右下两槽为主手、副手。空槽保留原版装备轮廓，完全没有装备时仍不显示面板。

## 构建与进入游戏

在项目目录打开 PowerShell，使用 JDK 21（将下面的示例路径替换为自己的 JDK 21 安装目录）：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat build
.\gradlew.bat runClient
```

Gradle 配置通过 Java toolchain 固定编译器和开发运行的 Java 版本为 21，字节码目标同为 21。
成品为 `build/libs/inventorylens-1.0.0.jar`；`-sources.jar` 是源码包，不要放进 mods 文件夹。
也可以将成品放进安装了 Fabric Loader 0.19.5 或更新兼容版本、Fabric API 0.116.17+1.21.1 的 1.21.1 客户端。
功能只在客户端运行，不需要新增服务端网络协议。

## 检测范围与默认行为

- 只读取原版 `crosshairPickEntity` 和一致的 `EntityHitResult`，最多显示一个目标，不遍历世界实体。
- **8 格只是额外上限**。生存模式原版实体交互距离通常为 3 格、创造模式通常为 5 格；本模组不扩展交互或攻击距离。
- 排除所有玩家、盔甲架、死亡/移除的实体及对当前玩家不可见的实体。
- 没有装备不显示；移开准星立即消失；打开屏幕或按 F1 隐藏界面时也不显示。
- 暂无 Tooltip、配置、动画、容器 GUI、玩家装备或特殊 Boss 逻辑。
- 装备面板为 48×84 px，实际比例为 0.02 格/像素（约 0.96×1.68 格）。浅灰底、明暗立体边框，18×18 凹槽内放置 16×16 图标。
- 准星落在生物视觉左半边时，面板放右侧；落在右半边时，面板放左侧。只左右换位，不上下换位；中心左右各 0.04 格内保留当前侧，避免抖动，首次瞄准正中默认右侧。
- 面板中心向所选侧偏移至少 1.2 格（较宽生物为半宽加 0.8 格），并沿摄像机视线反方向前移 0.35 格，为身体、手臂和盾牌留出空间；仍接受世界深度遮挡。
- 避障：先检测准星规则选出的侧边；被坑壁、地面或墙角挡住时尝试另一侧。两侧都受挡时，沿摄像机到面板的射线将面板拉到遮挡物前，并同比缩小世界尺寸，维持原来的屏幕位置和视觉大小。不会改放头顶或脚下。
- 只对当前面板做固定数量的方块射线检测（每侧 15 条，最多 30 条/帧），不扫描实体。拉近后仍保留至少 0.65 格的视线深度；镜头紧贴实体方块、没有安全位置时暂时隐藏面板。
- 图标采用明亮的原版物品照明，仍接受墙体深度遮挡。面板不按窗口大小或 GUI Scale 固定在屏幕上。

## 游戏测试（开启作弊的测试世界）

建议创造模式、普通难度、夜间和平坦空地，先靠近到 2～3 格再瞄准僵尸身体。
以下只操作带 `inventorylens_test` 标签的测试僵尸。每组测试前可以清理上一组，避免指向错误实体：

```mcfunction
/gamemode creative
/difficulty normal
/time set midnight
/kill @e[type=minecraft:zombie,tag=inventorylens_test]
```

### 1. 无装备：不出现面板

```mcfunction
/summon minecraft:zombie ~ ~ ~2 {Tags:["inventorylens_test"],PersistenceRequired:1b,NoAI:1b,CanPickUpLoot:0b,HandItems:[{},{}],ArmorItems:[{},{},{},{}]}
```

### 2. 单件铁剑：主手槽显示铁剑，其余槽显示轮廓

对上一只空装备僵尸执行：

```mcfunction
/item replace entity @e[type=minecraft:zombie,tag=inventorylens_test,sort=nearest,limit=1] weapon.mainhand with minecraft:iron_sword
```

### 3. 六件装备：一条完整 summon 命令

**这条命令超过聊天输入长度，请使用命令方块。** 执行 `/give @s minecraft:command_block`，放置命令方块，粘贴下列命令（命令方块可省略开头 `/`），用按钮触发一次。坐标相对命令方块，确保其同高度、Z 正方向两格处有可站立空间。

```mcfunction
/summon minecraft:zombie ~ ~ ~2 {Tags:["inventorylens_test"],PersistenceRequired:1b,NoAI:1b,CanPickUpLoot:0b,HandItems:[{id:"minecraft:diamond_sword",count:1},{id:"minecraft:shield",count:1}],ArmorItems:[{id:"minecraft:diamond_boots",count:1},{id:"minecraft:diamond_leggings",count:1},{id:"minecraft:diamond_chestplate",count:1},{id:"minecraft:diamond_helmet",count:1}]}
```

1.21.1 实体数据仍使用 `HandItems`（主手、副手）和 `ArmorItems`（靴子、护腿、胸甲、头盔）。物品使用小写 `count` 和 `components`，空槽使用 `{}`。
不要套用更新版本的 `equipment` 实体字段，也不要使用旧物品格式的 `Count`/`tag`。
格式已与本地 1.21.1 Mojang 类及 [Mojang 物品格式说明](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-20-5)核对。

也可以对第 2 步的僵尸逐条执行短命令，在聊天框完成相同测试：

```mcfunction
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] weapon.mainhand with minecraft:diamond_sword
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] weapon.offhand with minecraft:shield
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] armor.head with minecraft:diamond_helmet
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] armor.chest with minecraft:diamond_chestplate
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] armor.legs with minecraft:diamond_leggings
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] armor.feet with minecraft:diamond_boots
```

左列从上到下应显示：头盔、胸甲、护腿、靴子；右列下方两槽分别显示剑、盾。

### 4. 稀疏装备：固定槽位与空槽轮廓

```mcfunction
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] weapon.offhand with minecraft:air
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] armor.chest with minecraft:air
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] armor.legs with minecraft:air
```

现在应在各自固定位置显示剑、头盔、靴子；副手、胸甲、护腿槽显示灰色轮廓，面板大小保持不变。
这是按参考图更新后的样式，替代最初的压缩竖排布局。

### 5. Glint、耐久和堆叠数量

```mcfunction
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] weapon.mainhand with minecraft:diamond_sword[minecraft:enchantments={levels:{"minecraft:sharpness":3}},minecraft:damage=400]
/item replace entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] weapon.offhand with minecraft:apple 32
```

剑应有原版附魔 Glint 和耐久条；苹果右下角显示 32。再将苹果数量改成 1，应不显示数量数字。
可将副手换成 `minecraft:shield[minecraft:enchantments={levels:{"minecraft:unbreaking":3}},minecraft:damage=100]` 检查盾牌的特殊模型渲染。

### 6. 朝向、移动和立即消失

- 准星分别瞄准僵尸的视觉左半边、右半边，面板应分别切到右侧、左侧，切换后图标和数量不镜像、不倒置。
- 在中心小范围晃动准星，面板应保持当前侧；保持横向落点相同时上下瞄准，面板不应换到头顶或脚下。绕僵尸一周，左右判断仍以玩家视角为准。
- 移开准星，再看回来，面板应立即消失并重新出现。
- 执行下面命令恢复 AI，再用生存模式引导僵尸移动；测试高帧率下的平滑跟随。

```mcfunction
/data merge entity @e[tag=inventorylens_test,type=minecraft:zombie,sort=nearest,limit=1] {NoAI:0b}
/gamemode survival
```

### 7. 遮挡与其他边界

- 在玩家与僵尸之间放置实心墙：看不到目标时不显示面板。
- 保持准星能看到僵尸，仅用墙挡住当前侧面板：面板应优先换到另一侧；两侧都受挡时拉近，图标不能突然变大，也不应上下换位。
- 将有装备的生物放进一格宽、两格深的坑，从边缘向下瞄准：检查坑壁遮挡时的换侧/拉近，再回到空地检查能恢复原来的位置。
- 面板始终保留深度检测。采样没有覆盖到的细杆、复杂模型仍可能挡住局部像素，不能因此出现穿墙渲染；完全挡住生物后面板必须立即消失。
- 分别检查快速、精美、极佳画质；切换 GUI Scale、窗口尺寸、第一/第三人称后面板仍应处于世界空间。
- 检查玩家、盔甲架不触发；目标死亡、退出世界、切换维度后不应残留上一个面板。

## 实现与验证边界

Fabric 事件仅集中在客户端入口；目标检测、世界数学和物品渲染不导入 Fabric API。
物品基底与 Glint 使用分离的复用缓冲，先写物品深度再绘制 Glint；世界末尾渲染时，将原版延迟透明物品层定向到主世界 framebuffer，以适配极佳画质已完成的透明合成。
背景和耐久条使用接受深度测试、仅写颜色的原版四边形层；文字使用非透视穿透的 `Font.DisplayMode.NORMAL`。

第一阶段已通过本地构建和作者的游戏测试。上面的测试步骤可用于后续修改后的回归检查；编译通过不代替游戏视觉验证。
第三方着色器、替代渲染器和自定义超大物品模型不属于本阶段的兼容性承诺。
当前版本仅包含生物装备面板，尚未实现容器面板。

## License

本项目采用 [MIT License](LICENSE)，Copyright (c) 2026 寿云。
