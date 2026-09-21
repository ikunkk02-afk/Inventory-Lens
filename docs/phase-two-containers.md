# 第二阶段：原版容器 World-Space Inventory Projection

支持 Minecraft 1.21.1 / Fabric / Mojang Mappings / Java 21，仅普通箱子、大箱子、木桶。
这是绑定容器世界坐标的只读面板，不是 HUD、Screen 或 Tooltip。不打开原版菜单，不修改物品，不触发开箱声音或箱盖动画。

## 安装与数据同步

联机需要客户端和服务端同时安装 Inventory Lens 与 Fabric API；单人游戏自动使用集成服务端。
没有服务端快照通道时只保留第一阶段装备面板，不尝试读取客户端 BlockEntity 库存。

- 客户端只沿玩家视线检测一个目标，最长 6 格，不扫描周围容器，不改变原版交互距离。
- 客户端首次发现目标后，在下一个客户端 Tick 发请求，通常不超过 50ms；同一个容器至少间隔 300ms 刷新。
- 服务端每玩家最多补充 10 次请求额度/秒，最多积累 2 次；不因切换目标绕过服务端限流。
- 请求只包含维度、命中坐标、请求编号；服务端重新从玩家眼睛沿服务端记录的朝向执行射线，验证真正命中的容器身份及距离。
- 射线沿途、目标及大箱子的配对区块都必须已加载，不会因预览强制加载区块。请求中伪造的远处坐标不会被拿来查询库存。
- 服务端检查方块类型、BlockEntity、原版锁权限；两半均验证完才读取大箱子。
- 未生成战利品的容器不预览。不会调用可能生成战利品的 `getItem` / `isEmpty`，直至确认每个成员均无待生成战利品表。
- 快照逐槽复制 ItemStack，含空槽；使用原版 `ItemStack.OPTIONAL_STREAM_CODEC` 保留 Data Components，不发送整个 BlockEntity NBT。
- 成功响应包含容器身份、类型、Facing、成员坐标、槽数、物品列表和请求编号；不可读的容器只返回无库存的状态。

缓存最多保留当前一个容器，身份包含维度和规范化坐标，并检查双箱结构、朝向。快照接收后 1500ms 过期；延迟达到 1500ms 的请求响应也不再采用。
刷新等待期间保留尚未过期的快照，首包到达前不绘制假空格子。较旧响应不能覆盖新快照，旧目标或旧世界的数据包不能让面板重新出现。
移开、破坏、结构变化、世界切换、断线、明确拒绝或超时会清理对应显示。方块破坏以客户端收到世界更新后的下一次检查为准。

## 容器与世界空间渲染

- 普通箱子 / 木桶：27 格，9×3。大箱子：54 格，9×6。完全空的容器仍显示全部槽位。
- 大箱子通过原版 `ChestBlock.TYPE`、连接方向和 Facing 配对。固定排序的坐标作为身份，RIGHT 半边在前、LEFT 半边在后组成原版 `CompoundContainer` 槽位顺序。
- 左右半边解析结果相同，切换半边不会清空缓存或重新选择锚点。配对暂时缺失时不伪装成单箱。
- 使用整个容器的实际边界计算中心、箱沿及面板位置，大箱子两半共享同一套坐标。摆放依据客户端射线的实际命中点和命中面，支持四周立面，不受箱子正面 Facing 限制；木桶六种 Facing 均适用。
- 根据作者提供的 12.5 秒参考视频与“默认贴在表面下半部”的确认，面板改为贴近实际命中面的下半部；不再按准星左右/上下位置切换，不再从箱沿向外展开。准星在同一面内移动不会重新选锚点，大箱子两半使用整体中心。
- 表面旋转作为基准，向 Camera 旋转插值但最多偏转 12°，保留有限 Billboard 跟随。侧看时仍有表面透视感，而非始终平行屏幕。侧面规则不变；顶面/底面独立处理：平贴在整个容器顶面/底面的几何中心，位置、旋转及比例全部固定。阅读方向由容器水平 Facing 决定，竖直木桶使用固定北向；不再依赖摄像机位置、转头方向或首次观察角度。
- 侧面面板下边距容器底部 0.06 格；根据旋转后的完整矩形和物品厚度计算最小外移，使全部角点距离当前表面至少 0.03 格。顶部/底部使用容器短边与面板对角线计算固定缩放，全部角点在任意旋转角度下均位于箱盖范围内，边缘至少留 0.025 格；绕行不会改变缩放。
- 四周立面视角与法线夹角不超过 60°进入显示范围，超过 65°退出；顶面/底面为 75°进入、80°退出，使用不同阈值减少临界闪烁。镜头处于面板背后或距面板平面小于 0.20 格时不显示；支持正上方观看。
- 直接复用原版 `textures/gui/container/generic_54.png` 的浅灰底、立体边框、凹槽和底边，标题使用原版翻译“箱子 / 大型箱子 / 木桶”。保留 18×18 Slot、16×16 Item 和原版坐标，不显示下方玩家物品栏。
- 侧面每像素 0.005 格：宽 0.88 格；单箱高 0.39 格，大箱子高 0.66 格。顶面/底面在此基础上按箱盖尺寸固定缩小（普通箱子约 86%，大箱子约 75%，木桶约 99%）。宽 176px，17px 标题区加槽位区和 7px 底边，使面板更贴近箱体尺寸。
- 直接复用 `WorldItemRenderer` 的 Full Bright 图标、数量、耐久条、Glint 分离缓冲及极佳画质兼容层。数量 1 沿用原版不显示数字。
- 深度测试保持启用，文字不使用穿透模式。PoseStack 成对恢复，渲染后恢复调用方的深度、混合、剔除、着色器、颜色与世界光照状态。

Fabric 的事件与网络注册集中在入口；容器识别、快照、验证、锚点数学不依赖 Fabric。渲染留在客户端源码集，服务端不加载客户端渲染类。
未引入 Architectury、NeoForge / Forge 模块、Transfer API 或通用模组容器支持。

## A–L 游戏验收清单

| 测试 | 操作 | 预期 |
|---|---|---|
| A 普通箱子 | 随机槽位放钻石、圆石、苹果、铁剑，准星看向箱子 | 显示 9×3 世界空间面板，槽位与原版库存一致 |
| B 空箱子 | 看完全空的普通箱子 | 显示完整 9×3 空格，不隐藏空槽 |
| C 数量 | 放 64 圆石、32 苹果、1 钻石 | 数字 64、32 正确；单个钻石按原版不显示数字 |
| D 附魔装备 | 放附魔钻石剑，并加入损伤工具、药水、附魔书 | Glint、耐久条、药水颜色和物品模型正常 |
| E 大箱子 | 合并两箱，分别向两半各槽放不同物品；在左右半边间移动准星 | 一个 9×6 面板，两半槽序与原版一致；锚点不变、不重复出现、不因换半边闪烁 |
| F 木桶 | 测水平、向上、向下的木桶 | 一个 9×3 面板，锚点依据实际观察面，图标不倒置 |
| G 移动视角 | 从正面、侧面、背面分别瞄左、右和下部，再绕行和俯仰视角 | 小面板保持在当前观察面下半部，不随准星跳边；有限 Billboard 跟随保留表面透视，超出显示视角时隐藏 |
| H 移开准星 | 瞄准其他方块或天空，再看回来 | 移开立即消失并停止请求；返回时请求新快照 |
| I 距离 | 距离命中表面从小于 6 格移到超过 6 格 | 超距无显示，不能通过伪造请求读取远处容器 |
| J 墙体 | 在玩家与箱子之间放实心墙 | 不显示、不读取墙后库存；不能构成 Chest ESP |
| K 实时修改 | 观察时让另一名玩家修改物品 | 下一轮刷新后更新，正常为最多约 300ms 加网络与服务端处理延迟 |
| L 第一阶段回归 | 用 README 的命令生成装备僵尸，在僵尸与容器间切换视线 | 装备布局、Billboard、Glint、耐久、数量及原有避障不变；世界画面无状态污染 |

额外测试：双箱四个水平方向、跨区块双箱、拆掉一半、破坏整个容器、传送维度、退出重进、F1、打开菜单、暗处、快速/精美/极佳画质。
首次验证建议第一人称，距离 2–3 格，站在容器正面。第三人称仍以玩家眼睛的目标射线为准，不允许分离摄像机绕过服务端视线检查。

## 验证边界与已知限制

- 面板贴在当前观察面的下半部，在棱角处切换观察面时位置与方向会切换，目前没有转场动画。超出指定视角会暂时隐藏。附近墙壁、天花板、正在打开的箱盖等其他几何仍可能遮挡面板，不会关闭深度测试。
- 自然生成但尚未生成战利品的箱子没有面板；待正常开启或其他原版机制生成库存后才可预览。不会显示虚假的空库存。
- 严格服务端视线检查在快速转头或网络延迟较大时可能暂时拒绝请求；后续 300ms 刷新会重试。1500ms 超时后隐藏，避免永久显示旧内容。
- 0.005 格/像素为固定世界尺寸，远处自然变小；没有距离缩放或配置界面。
- 只遵守原版锁权限，不承诺兼容领地保护、第三方权限插件或自定义容器规则。仅精确支持普通箱子与木桶，不含陷阱箱。
- 不支持第三方 Shader、替代渲染器或超大自定义物品模型的专门兼容。Glint、Lighting、Depth 与第一阶段实际视觉回归需要作者进游戏验收，不能以编译或逻辑测试代替。
- 只绘制容器里的物品图标，不展开潜影盒或其他物品的内部库存。

## 本地检查

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

自动测试使用内存中的方块世界、真实原版碰撞形状与物品注册表，不读取或修改 `run/saves`。
覆盖容器解析、真实射线遮挡、范围与维度、未加载区域拒绝、服务端限流、锁权限、战利品只读、54 格顺序、快照编码、客户端节流及缓存失效。
启动检查只到主菜单；不自动进入作者的存档。构建产物为 `build/libs/inventorylens-1.0.0.jar`。

开发与测试期间只进行本地构建。作者现已确认最终效果并授权提交、推送 GitHub；本次不创建 Release，后续功能等待新的开发要求。

## 本次完成记录（2026-09-21）

### 构建与启动结果

- 最终 `gradlew.bat build` 任务：**BUILD SUCCESSFUL**，20 项测试，0 失败、0 错误。
- 使用 Java 21。最初测试编译受到 Windows 提交内存不足影响，停止本次任务产生的闲置 Gradle 进程并降低并发后完成验证，没有关闭用户的其他应用。
- 最终执行命令：`gradlew.bat build --no-daemon --max-workers=1 "-Dorg.gradle.jvmargs=-Xmx512m -Xms64m"`；没有跳过测试或核心编译任务。
- 已执行 `gradlew.bat runClient`（同样使用上述 Gradle 内存与并发参数）。客户端识别 Java 21、Minecraft 1.21.1、Fabric API 0.116.17+1.21.1 和 Inventory Lens 1.0.0，初始化与资源加载成功。
- 启动后的日志随后显示集成服务端、单人世界成功加载，运行采样确认世界渲染循环在执行。助手没有执行进入存档、游戏输入或修改存档操作，没有关闭当前游戏。
- 日志存在 Mojang 资料服务的 SSL 握手失败、公钥服务连接超时，以及原版山羊音效缺失警告；未阻止本地启动。没有观察到 Inventory Lens、Mixin 或 Codec 的运行异常。
- **未宣称完成游戏画面的人工验收**：双箱切换、Glint、Lighting、Depth、锚点体验、GUI 大小和第一阶段视觉回归仍请按 A–L 清单实测。未进行远程多人服务器端到端测试。

### 修改文件：7 个

| 文件 | 修改内容 |
|---|---|
| `build.gradle` | 添加 JUnit 测试依赖、客户端缓存测试类路径、Java 21 与测试内存设置 |
| `README.md` | 补充第二阶段入口、双端安装要求与验证说明，保留第一阶段测试内容 |
| `src/main/java/com/shouyun/inventorylens/InventoryLens.java` | 注册快照协议和服务端接收器 |
| `src/main/resources/fabric.mod.json` | 更新模组功能描述 |
| `src/main/resources/inventorylens.mixins.json` | 注册只读锁字段 Accessor |
| `src/client/java/com/shouyun/inventorylens/client/InventoryLensClient.java` | 注册客户端收包、Tick 请求、容器渲染与生命周期清理 |
| `src/client/java/com/shouyun/inventorylens/client/render/WorldItemRenderer.java` | 增加无持有者的容器物品渲染重载，原装备入口转发至相同实现 |

### 新增文件：25 个

以下 Java 路径以包根目录 `com/shouyun/inventorylens/` 为基准。

| 源码集 / 子目录 | 新增文件 |
|---|---|
| `src/main/java/…/container/` | `ContainerType.java`、`ContainerIdentity.java`、`ResolvedContainer.java`、`ContainerSnapshot.java`、`VanillaContainerResolver.java`、`ContainerSight.java` |
| `src/main/java/…/network/` | `ContainerSnapshotRequestPayload.java`、`ContainerSnapshotPayload.java`、`InventoryLensNetworking.java` |
| `src/main/java/…/server/` | `ContainerRequestValidator.java`、`ContainerSnapshotProvider.java`、`RequestRateLimiter.java` |
| `src/main/java/…/mixin/` | `ContainerLockAccessor.java` |
| `src/client/java/…/client/container/` | `ContainerTargetTracker.java`、`ContainerSnapshotCache.java` |
| `src/client/java/…/client/render/` | `WorldContainerRenderer.java`、`WorldInventoryGridRenderer.java` |
| `src/test/java/…/` | `TestWorld.java` |
| `src/test/java/…/container/` | `VanillaContainerResolverTest.java` |
| `src/test/java/…/network/` | `ContainerSnapshotCodecTest.java` |
| `src/test/java/…/server/` | `ContainerRequestValidatorTest.java`、`ContainerSnapshotProviderTest.java`、`RequestRateLimiterTest.java` |
| `src/test/java/…/client/container/` | `ContainerSnapshotCacheTest.java` |
| 文档 | `docs/phase-two-containers.md`（本文，含完成报告及 A–L 清单） |

`WorldEquipmentRenderer`、`WorldUiTransform`、`WorldPanelPlacement` 和 `EquipmentTargetTracker` 均未修改。未开始潜影盒或其他后续阶段。

### 实测反馈修正：原版外观与箱体相交

根据作者提供的两张截图，将手工绘制的深色网格替换为原版箱子 GUI 贴图，加入原版字体标题，并增大为 0.01 格/像素。
新增 `WorldContainerPlacement.java`，按 Billboard 完整旋转矩形计算离开箱体所需的高度，悬在容器前上方。保留深度检测、原有网络同步、9×3 / 9×6 格子和只读限制。
新增 `WorldContainerPlacementTest.java`，检查六个 Facing、多组偏航/俯仰/倾斜角及两种行数下的所有角点均离开箱体，并验证双箱两半的摆放结果相同。
第一阶段装备路径不变；`WorldItemRenderer` 只新增供容器使用的世界空间贴图与标题方法。当前正在运行的游戏需退出并重启才能加载此次修正。
本轮本地 `build` 已 **BUILD SUCCESSFUL**，共 22 项测试、0 失败、0 错误。没有替用户关闭或重启正在运行的游戏；修正后的实际画面仍待重启后验收。

### 后续反馈修正：准星避让与受限视角 Billboard

作者明确选择“保留 Billboard，但仅在指定视角范围内显示”。据此替换始终悬在前上方的摆放规则，加入顶部/左右位置、实际命中面、分区缓冲和安全视角限制，保留原版贴图与只读网络协议。
本轮修改 `ContainerTargetTracker`（保留实际命中信息）、`InventoryLensClient`（传递命中信息并清理摆放状态）、`WorldContainerPlacement`、`WorldContainerRenderer`、摆放测试及说明文档。未修改第一阶段装备渲染代码或服务端验证规则。
重点验收：绕到四周各面，分别瞄左/右/下方；轻晃中线；看双箱两半交界；逐渐变成侧视或陡峭俯视再退回正常角度。正常视角显示，危险角度隐藏；物品顺序和数量不应因位置切换改变。
本轮本地 `build` 已 **BUILD SUCCESSFUL**，共 27 项测试、0 失败、0 错误、0 跳过；`git diff --check` 通过。自动测试覆盖摆放几何、视角限制、分区缓冲与大箱子一致性；最终游戏画面仍需重启客户端后由作者验收。未上传或推送。

### 视频参考修正：贴在表面下半部的小面板

参考作者提供的 `屏幕录制 2026-09-21 132319.mp4`（12.54 秒），抽帧检查面板大小、位置和侧视透视；作者确认以视频的下半部位置为准，覆盖上一版准星避让规则。
本次修改 `WorldContainerPlacement.java`、`WorldContainerRenderer.java`、`WorldContainerPlacementTest.java`、README 及本文。面板缩至 0.88 格宽，默认贴在命中面的下半部；表面朝向最多向摄像机偏转 12°。沿用原版灰色贴图、第一阶段共用世界变换与物品绘制，服务端快照协议及装备渲染路径未修改。
自动几何检查覆盖：所有水平面下半部位置、准星移动不跳边、旋转幅度、视角缓冲、六面所有角点不穿入箱体、大箱子两半一致、正上方显示和切换目标清理状态。
视频中的潜影盒、其他容器、顶部信息栏、字幕、着色器及额外交互未纳入功能范围。未修改存档，最终观感需要作者重新启动客户端后测试。
本轮使用 Java 21 运行 Gradle Wrapper `build`：**BUILD SUCCESSFUL**，27 项测试全部通过（0 失败、0 错误）。成品为 `build/libs/inventorylens-1.0.0.jar`。未启动新游戏实例，未上传、推送或创建 Release。

### 顶部录屏反馈修正（14:11 录屏）

录屏中顶部面板随视角改变在箱盖上滑动并越过箱沿。原因是顶面复用了侧面“下半部对齐”的切向偏移，该偏移方向由镜头偏航决定，而且没有约束面板在箱盖上的水平范围。
修改 `WorldContainerPlacement`：顶面/底面锚点居中，朝向使用观察者位置，近正上方保留阅读方向；倾角上限 6°；用箱盖短边和面板对角线计算与视角无关的固定缩放。
修改 `WorldContainerRenderer`：应用摆放返回的缩放，保证背景、文字与物品一致缩放。侧面仍返回原比例 1，不修改侧面下半部布局、第一阶段装备、物品渲染或网络。
新增三项摆放回归测试：原地转头不改变顶部姿态；绕行一周所有角点不越过箱盖且比例恒定（含两种大箱子轴向）；近正上方不翻转且两半箱子使用同一顶部姿态。
顶部与侧面之间仍直接切换，没有转场动画；实际画面及第一阶段视觉回归仍需作者重启客户端验证。没有进入或修改存档。
本轮 Java 21 / Gradle Wrapper `build`：**BUILD SUCCESSFUL**，30 项测试、0 失败、0 错误、0 跳过。仅本地生成 JAR，没有上传或推送。

### 顶部完全固定

按作者“箱子上方的 GUI 固定不动”的要求，移除顶部随观察者绕行旋转及俯仰跟随。顶面/底面平贴，阅读方向固定到容器 Facing；转头、绕行、移开再重新瞄准都保持相同姿态。中心、缩放、距离/视角限制和深度遮挡仍保留；侧面下半部与有限 Billboard 规则不变。
修改 `WorldContainerPlacement.java` 及现有摆放测试，新增到原绕行测试的断言验证整套姿态恒定、重新获取同一目标后仍一致。同步更新 README 与本文。
固定方向意味着从相反方向观看时文字会倒着，这是固定在箱盖表面的自然结果，不会再自动转正。
本轮本地 `build`：**BUILD SUCCESSFUL**，30 项测试全部通过。新画面仍待作者重启客户端实测；未上传或推送。

### 作者确认与提交

作者确认顶部固定版本效果可以，并授权将第二阶段代码、测试和说明文档提交并推送至 GitHub。以上各轮“待实测 / 未推送”均为当时的历史记录；本次保留最终原版外观、侧面下半部布局、顶部完全固定与服务端只读快照，不开始后续容器开发。
