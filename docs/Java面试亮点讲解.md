# PyStarter 项目中的 Java 面试亮点（边做边学版）

这份文档把你项目里新增的 **Spring Boot 在线排行榜后端 + 策略模式 + Java 8 Stream + OkHttp** 拆成大白话，方便你准备 Java 开发面试。

---

## 一、Spring Boot + MyBatis-Plus + Spring Cache + AOP 在线排行榜后端（`backend/`）

### 1. 三层架构与技术框架分工
- **Controller 层**（`LeaderboardController.java`）：接收 HTTP 请求，用 `@Valid` 做参数校验，并用 **SpringDoc OpenAPI（Swagger 3）** 注解（`@Tag`、`@Operation`）自动生成 `/swagger-ui.html` 在线接口文档。
- **AOP 切面层**（`ApiLogAspect.java`）：基于 **Spring AOP**（`@Aspect` + `@Around`）统一拦截 Controller 方法，自动统计每个接口的耗时（ms）和异常日志，不用在业务代码里写重复日志。
- **Service + Cache 层**（`LeaderboardService.java`）：核心业务与事务边界（`@Transactional`），同时使用 **Spring Cache + Caffeine**：
  - 查询 Top 50 加 `@Cacheable("leaderboard")`，读多写少直接命中内存缓存；
  - 上传新成绩加 `@CacheEvict(allEntries = true)`，在更新数据库后自动淘汰缓存，保证缓存与数据库一致。
- **Mapper 层**（`LeaderboardMapper.java`）：使用 **MyBatis-Plus** 继承 `BaseMapper`，配合 `LambdaQueryWrapper`（编译期字段安全、底层 `#{} ` 预编译防 SQL 注入）和 `PaginationInnerInterceptor` 分页插件实现 Top 50 排序查询。

### 2. 面试常问：MySQL 建表和索引怎么设计的？
看 `backend/src/main/resources/schema.sql`：
- `UNIQUE KEY uk_player_id (player_id)`：保证每个设备/玩家只占一行，查询单人记录走唯一索引（`O(log N)`）。
- `KEY idx_rank_sort (stars DESC, xp DESC, updated_at ASC)`：联合索引直接覆盖排行榜的 `ORDER BY`，查询前 50 名时避免全表排序（`Using filesort`）。

### 3. 面试常问：怎么防止别人抓包伪造分数？
在项目里做了两层基础校验，同时要**诚实认知客户端签名的局限性**：
1. **参数边界校验**（DTO `@Min`/`@Max`）：总星数最多 36、经验有上限、不能为负，超限直接被 `@Valid` + `@RestControllerAdvice` 拦截返回 400。
2. **基础请求签名 + 只升不降保护**：客户端上传带 SHA-256 签名，拦截最简单的明文抓包改参；Service 层只在星数/经验真正提升时才更新 `updatedAt`，防止重复上传刷新时间戳导致同分排名下降。
3. **面试加分回答（局限与演进）**：“因为客户端签名密钥写在 APK 里，反编译仍可拿到，所以它只能防入门级抓包，**不是真正的防作弊**。生产环境真正的方案是把判题和经验发放放到服务端，或者由服务端下发一次性提交凭证（Nonce）+ HTTPS + 登录态 Token。”

### 4. 面试常问：服务端没启动时 App 会崩溃吗？
不会。安卓端 `LeaderboardClient.java` 使用 **OkHttp**（带 4 秒连接超时、6 秒读超时）在独立后台线程请求，捕获 `IOException` 后通过 `Handler(Looper.getMainLooper())` 切回主线程提示“离线模式照常可用”，App 其他全部学习、闯关和迷宫功能完全不受影响。

---

## 二、设计模式：策略模式重构题型处理（`app/.../strategy/`）

### 1. 为什么要改掉原来的 `switch-case`？
- **重构前**：`BattleActivity` 的 `showQuestion()` 和 `currentAnswer()` 里用 `switch (st.type)` 判断 `"choice"` / `"fill"` / `"code"`，所有题型 UI 渲染和取答案逻辑全耦合在 Activity 里。
- **重构后**：
  - 定义统一接口 `QuestionStrategy`（`render(...)` + `extractAnswer(...)`）；
  - 3 个实现类：`ChoiceQuestionStrategy`、`FillQuestionStrategy`、`CodeQuestionStrategy`；
  - `QuestionStrategyFactory` 用 `Map<String, QuestionStrategy>` 托管，直接 `get(type)`。
- **面试话术**：“我用**策略模式 + 工厂映射**消除了战斗页里的多分支 `switch`，后续如果新增‘拖拽排序题’或‘代码纠错题’，只需新增一个策略类并注册进 Map，不用改 `BattleActivity` 主流程，符合**开闭原则**。”

---

## 三、Java 8 Stream & Lambda（`ProgressActivity.java`）

在我的进度页统计技能书、已解锁徽章数量与错题列表时，使用了 `stream().filter().map().sorted().collect(Collectors.joining("、"))` 替代手写多层循环和临时集合，代码更声明式、可读性更好。

---

## 四、如何启动后端联调（可选）
1. 本地快速体验（内置 H2 数据库，零配置）：
   ```bash
   cd py-starter-app/backend
   mvn spring-boot:run
   ```
2. 切换 MySQL：先执行 `schema.sql` 建表，再运行 `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`。
3. 安卓模拟器里打开「🏆 排行榜」，默认地址 `http://10.0.2.2:8080` 即可直接上传成绩与查看排名。
