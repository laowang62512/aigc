SpringAI 医疗智能问答系统
项目简介
基于 SpringAI 1.0-M6 构建的医疗场景大模型智能对话平台，集成 RAG 知识库检索、工具函数调用、全链路监控埋点、熔断降级、会话持久化记忆，适配医疗问诊、患者数据查询、文档知识库问答业务场景。
项目底层对接 ChatAnywhere OpenAI 兼容 API，支持同步问答 / 流式 SSE 问答，配套完整运维监控、定时统计、异常熔断防护能力。
技术栈
核心框架
Spring Boot 3.x + Spring AI 1.0.0-M6
Spring Data JDBC（会话记忆持久化）
MyBatis-Plus 4.x（业务 / 监控数据表 CRUD）
Resilience4j（接口熔断、超时、重试控制）
Spring Retry（AI 接口自动重试）
JDK 17
文档解析 & RAG
Tika（PDF 文档文本提取）
自定义文本切片工具（可配置切片长度、重叠度）
内存向量检索（可扩展替换 ES 向量库）
监控 & 运维
自定义 Advisor 全链路埋点（耗时、Token、工具调用统计）
定时任务：监控数据批量入库、无效向量自动清理、每日用量汇总
内存缓冲队列（降低数据库写入压力）
其他
Lombok、Maven、MDC 日志链路追踪
SSE 流式对话、JDBC 会话记忆持久化
项目模块目录说明
plaintext
src/main/java/com/lcd
├── advisor                // SpringAI 自定义拦截器Advisor
│   ├── SimpleLoggerAdvisor // 全链路监控埋点拦截器（耗时/Token/工具调用采集）
├── config                 // 全局配置类
│   ├── ChatClientConfiguration       // ChatClient多实例配置（主对话/摘要对话）
│   ├── ChunkProperties        // rag检索相关配置
├── controller             // 前端接口控制器
│   └── ChatController     // 同步/流式问答接口
├── domain                 // 数据库实体
│   ├── ChatRecord            // 会话记忆表
│   ├── monitor             // 日志记录（token使用量表、工具调用表）
│   └── tool               // 患者/血糖业务实体
├── mapper                 // MyBatis-Plus Mapper
│   ├── MonitorTokenMapper
│   ├── MonitorToolMapper
│   ├── MonitorCircuitMapper
│   └── JdbcChatMemoryMapper
├── memory                 // 自定义JDBC会话记忆实现
│   └── JdbcChatMemory     // 持久化多轮对话历史
├── monitor                // 监控埋点核心工具
│   └── MetricMonitorUtil  // 埋点采集、内存队列、批量入库工具
├── service                // 业务服务层
│   ├── impl
│   │   └── ChatServiceImpl // 问答主服务（熔断、重试、对话封装）
│   ├── ChatService        // 问答接口定义
│   └── RagService         // RAG知识库上传/检索服务
├── tools                  // AI工具函数（@Tool标注，LLM自动调用）
│   ├── DiseaseStandardSearchTool   // 疾病标准信息查询工具
│   ├── DrugSuggestSafetyTool       // 用药建议与安全提醒工具
│   ├── PatientBloodSugarRecordTool // 患者血糖记录查询工具
│   └── PatientTool                 // 患者基础信息查询工具
├── utils                  // 通用工具类
│   ├── PromptFileReader   // 读取本地系统提示词文件
│   └── PdfTextChunkUtil   // PDF解析、文本切片工具
src/main/resources
├── prompt/                // 系统提示词文件目录
│   └── system.txt         // 主对话系统角色提示词
├── application.yml        // 全局配置（AI接口、熔断、RAG、定时任务）
核心功能模块介绍
1. 多实例 ChatClient 对话客户端
   提供两套独立对话客户端隔离业务：
   mainChatClient：完整医疗问诊客户端
   加载医疗系统提示词
   内置患者数据查询工具 PatientTool
   加载三大 Advisor：埋点日志、会话记忆、RAG 知识库检索
   支持同步、流式 SSE 问答
   summaryChatClient：纯对话摘要客户端
   无会话记忆、无工具、无 RAG
   仅用于历史对话精简摘要，降低上下文 Token 消耗
2. JDBC 持久化会话记忆
   自定义 MessageChatMemoryAdvisor + JdbcChatMemory，将多轮对话存入数据库
   通过 convId 会话 ID 隔离不同用户对话，重启服务不丢失历史上下文
   支持自动滑动窗口截断超长会话，控制 Token 总量
3. RAG 本地知识库模块
   PDF 文档上传解析：Tika 提取全文，自定义分片算法（分片长度、重叠度 yml 可配置）
   混合检索策略：向量相似度检索 + 关键词匹配
   运维能力：
   定时自动清理空文本、过短无效切片
   上传文件携带疾病标签，检索可按病种过滤知识库
   埋点采集：PDF 解析耗时、切片耗时、向量入库耗时、检索总耗时全链路统计
4. LLM 工具函数调用（Function Call）
   内置医疗业务工具 PatientTool：
   LLM 自动识别用户查询患者血糖、病史需求，自动调用工具查询数据库患者数据
   工具执行失败后 LLM 自动反思重试，区分参数缺失、数据库异常场景
   工具调用全量埋点：工具名称、调用耗时、成功 / 失败状态入库统计
5. 全链路监控埋点系统（SimpleLoggerAdvisor）
   通过 SpringAI 统一 Advisor 拦截同步 / 流式对话，自动采集以下指标存入数据库：
   对话基础指标：同步 / 流式区分、总耗时、异常标记
   Token 用量指标：Prompt 输入 Token、Completion 输出 Token
   工具调用指标：每个工具调用耗时、成功失败状态
   内存缓冲队列削峰，每 5 分钟批量写入数据库，避免高频单条 DB 插入
   配套三张监控数据表：
   monitor_token_stat：单次对话 Token 明细
   monitor_tool_stat：单次工具调用明细
   monitor_circuit_record：熔断触发记录
6. 容错防护体系（Resilience4j + Spring Retry）
   6.1 Spring Retry 自动重试
   AI 接口网络异常（连接重置、超时）自动重试 1 次，重试间隔 1 秒
   6.2 Resilience4j 熔断降级
   滑动窗口 10 次调用，失败率 50% 触发熔断，冷却 10 秒
   单次接口强制 12 秒超时限制，杜绝 19 秒长阻塞占用线程池
   熔断触发直接返回友好降级文案，不再发起网络请求，熔断事件单独埋点入库
   同步 / 流式对话分开实现独立 fallback 降级方法，返回类型隔离无冲突
7. 定时运维任务
   5 分钟周期：内存监控队列批量刷入数据库
   每日凌晨 1 点：统计昨日 Token 总消耗、各工具调用成功率、平均耗时，生成日报汇总
   每日凌晨 2 点：自动扫描向量库，删除空内容、低于最小长度的无效切片，优化检索性能
   核心接口说明
1. 问答接口
   plaintext
   POST /chat/send       同步问答，返回完整文本
   POST /chat/stream     SSE流式问答，逐字实时输出
   参数：message 用户提问，convId 会话唯一ID
2. RAG 知识库接口
   plaintext
   POST /rag/upload      上传PDF知识库文件
   参数：file PDF文件，disease 疾病分类标签
   POST /rag/search      手动测试知识库检索
