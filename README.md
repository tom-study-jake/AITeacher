看八股题目枯燥效率低,用上这个嘎嘎爽

AITeacher - Java面试教练
基于 Spring AI + DeepSeek 的 Java 面试练习平台，支持三种模式：随机抽题、解答问题、模拟面试。
技术栈
- 框架: Spring Boot 3.4.7 + Spring AI 1.0.9
- AI模型: DeepSeek（OpenAI兼容接口）
- 缓存: Redis（多轮对话记忆）
- 前端: 原生HTML + JavaScript + marked.js
核心功能
三模式切换
- 随机抽题: 从179道Java面试题库随机抽取，支持关键词筛选
- 解答问题: 流式对话，可自由提问Java相关问题
- 模拟面试: AI面试官出题，回答后评分并给出改进建议
SSE流式输出
- 后端通过 Flux<String> 推送token
- 前端 ReadableStream 实时渲染，打字机效果
Function Calling
- 注册计算器工具：数学表达式计算
- 注册天气查询工具：调用 wttr.in API
- AI自动识别用户意图并调用对应工具
多轮对话记忆
- Redis存储会话历史（24小时过期）
- Jackson TypeReference 处理泛型序列化
- 模式切换保留聊天记录
快速开始
环境要求
- JDK 21
- Redis
- DeepSeek API Key
启动步骤
1. 配置 application.yml：
      spring:
     ai:
       openai:
         api-key: your-deepseek-api-key
         base-url: https://api.deepseek.com
     data:
       redis:
         host: localhost
         port: 6379
   
2. 启动应用：
      mvn spring-boot:run
   
3. 访问 http://localhost:8080
API 接口
聊天接口（流式）
POST /chat/stream
Content-Type: application/x-www-form-urlencoded
message=你好&sessionId=user_123&mode=chat
随机抽题/面试接口
GET /chat?message=随机&sessionId=user_123&mode=random
GET /chat?message=开始模拟面试&sessionId=user_123&mode=interview
项目结构
src/main/java/xhw/aiteacher/
├── AITeacherApplication.java    # 启动类
├── config/
│   ├── ChatClientConfig.java    # ChatClient配置
│   └── RedisConfig.java         # Redis配置
├── Controller/
│   └── ChatController.java      # 接口层
├── service/
│   ├── ChatService.java         # 核心业务逻辑
│   └── QuestionService.java     # 题库管理
└── tool/
    └── ToolConfig.java          # Function Calling工具
题库说明
- 共179道Java面试题，覆盖13个分类
- 数据来源：questions.json
- 支持关键词筛选：Java、HashMap、并发、Redis、MySQL、JVM等
