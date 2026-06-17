# AITeacher — AI 面试教练学习计划

> 总时长：约 7 天，每天 2-4 小时
> 前提：已有 Spring Boot 基础（StudyFlow 项目经验）
> 目标：独立完成一个可演示的 Agent 项目
> 包名统一：`xhw.smartagent`
> Spring Boot 版本：`3.4.7`（与 Spring AI 1.0.0-M6 兼容）

---

## 整体路线

```
Day 1 ─→ 调通 DeepSeek API（最原始方式，理解原理）
Day 2 ─→ 集成 Spring AI，搭 REST 接口
Day 3 ─→ Function Calling —— Agent 核心
Day 4 ─→ 工具设计 + Redis 记忆
Day 5 ─→ 完善项目 + 写简历
Day 6~7 ─→ 缓冲（卡住时补）
```

---

## Day 1：调通 DeepSeek API（理解原理）

**目标：** 用最原始的方式调通大模型 API，打印返回结果

### 步骤

**① 注册 DeepSeek**
- 打开 https://platform.deepseek.com/
- 手机号注册
- 点左边「API Keys」→ 创建 Key → 复制保存（`sk-xxxxx`）

**② 验证 API Key 是否有效**
打开终端（CMD 或 PowerShell），运行：
```bash
curl https://api.deepseek.com/v1/models -H "Authorization: Bearer sk-你的key"
```
如果返回模型列表，说明 Key 有效。

**③ IDEA 新建一个空 Maven 项目**
```
File → New → Project
Name: SmartAgent
Language: Java 21
Spring Boot: 3.4.7（可选，也可以不用 Spring Boot）
Package name: xhw.smartagent
```

**③ 在 pom.xml 加依赖**
```xml
<dependencies>
    <!-- 只用 HttpURLConnection 调 API，什么都不加 -->
    <!-- 先不加 Spring Boot，确保原理搞懂 -->
</dependencies>
```

**⑤ 写一个 main 方法调 API（完整代码）**

```java
package xhw.smartagent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DeepSeekTest {
    public static void main(String[] args) throws Exception {
        String apiKey = "sk-你的key";  // ← 换成你的

        String json = """
            {
                "model": "deepseek-chat",
                "messages": [
                    {"role": "user", "content": "1+1等于几？用中文回答"}
                ]
            }
            """;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("返回结果：");
        System.out.println(response.body());
    }
}
```

**⑤ 运行，看到类似输出就成功了：**
```json
{
  "choices": [{"message": {"content": "1+1等于2。"}}]
}
```

### ✅ Day 1 完成标准
- [x] 能调通 API 并打印结果
- [x] 理解 AI 本质上就是一个 HTTP 接口

---

## Day 2：集成 Spring AI + REST 接口

**目标：** 用 Spring AI 框架封装 API 调用，提供 REST 接口

**① pom.xml 加 Spring Boot + Spring AI 依赖**

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.7</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M6</version>
    </dependency>
    <dependency>
        <groupId>com.googlecode.aviator</groupId>
        <artifactId>aviator</artifactId>
        <version>5.4.3</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
</dependencies>
```

**② 创建 application.yml**

位置：`src/main/resources/application.yml`

```yaml
spring:
  ai:
    openai:
      api-key: sk-你的key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
```

**③ 创建 ChatClientConfig.java**

位置：`src/main/java/xhw/smartagent/config/ChatClientConfig.java`

```java
package xhw.smartagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

**④ 创建 ChatController.java**

位置：`src/main/java/xhw/smartagent/Controller/ChatController.java`

```java
package xhw.smartagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt(message).call().content();
    }
}
```

**④ 启动应用**

方式一：IDEA 中运行 `SmartAgentApplication.java` 的 main 方法

方式二：终端运行
```bash
mvn spring-boot:run
```

**⑤ 测试接口**

浏览器打开：http://localhost:8080/chat?message=你好

应该能看到AI的回复。

### 常见问题

| 问题 | 解决 |
|------|------|
| Spring AI 版本报错 | 去 https://spring.io/projects/spring-ai 查最新版本 |
| base-url 配置不生效 | Spring AI 对 DeepSeek 兼容 OpenAI 格式，要确认 endpoint 路径 |
| 如果 Spring AI 版本适配麻烦 | 可以用 **LangChain4j** 替代，用法类似 |
| 401 Unauthorized | 检查 API Key 是否正确，是否过期 |
| 连接超时 | 检查网络，可能需要代理 |

**如果 Spring AI 配不通，退回 Day 1 的手写 HttpClient 方式，自己封装一个工具类，效果一样。**

---

## Day 3：Function Calling（Agent 核心）

**目标：** 让 AI 能调用你的 Java 方法

### 具体示例：AI 查天气

```
用户说："北京今天天气怎么样？"
  ↓
AI 识别 → 需要调用 weather 工具
  ↓
你的 Java 方法执行 getWeather("北京") 返回 "晴，25°C"
  ↓
AI 拿到结果 → 组织语言："北京今天天气晴朗，气温25°C，适合出行。"
```

### 原理

```
用户说："1+1等于几？"
  ↓
AI 识别 → 需要调用 calculate 工具
  ↓
你的 Java 方法执行 calculate(1, "+", 1) 返回 2
  ↓
AI 拿到结果 → 组织语言回复用户
```

### 实现（用工具类封装方式，不依赖特定框架）

**Tool.java** - 工具接口
```java
package xhw.smartagent.tool;

public interface Tool {
    String getName();           // 工具名，如 "calculate"
    String getDescription();    // 描述，让 AI 知道什么时候调用
    String execute(String args); // 执行
}
```

**CalculateTool.java** - 计算器工具
```java
package xhw.smartagent.tool;

import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.stereotype.Component;

@Component
public class CalculateTool implements Tool {

    @Override
    public String getName() { return "calculate"; }

    @Override
    public String getDescription() {
        return "计算数学表达式，参数格式：{\"expression\": \"1+1\"}";
    }

    @Override
    public String execute(String args) {
        try {
            // 解析JSON获取expression字段
            String expression = args.replaceAll(".*\"expression\"\\s*:\\s*\"(.*)\".*", "$1");
            // 用Aviator计算
            Object result = AviatorEvaluator.execute(expression);
            return result.toString();
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }
}
```

**ChatController.java** - 控制器

位置：`src/main/java/xhw/smartagent/Controller/ChatController.java`

```java
package xhw.smartagent.Controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt(message).call().content();
    }
}
```

### ✅ Day 3 完成标准

## Day 4：Redis 记忆 + 多轮对话

**目标：** 让 AI 记住之前的对话

### 前提：安装 Redis

Windows 可以用 Docker 运行 Redis：
```bash
docker run -d -p 6379:6379 redis
```

或下载 Windows 版 Redis：https://github.com/tporadowski/redis/releases

### pom.xml 加 Redis 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### application.yml 加 Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### RedisConfig.java

位置：`src/main/java/xhw/smartagent/config/RedisConfig.java`

```java
package xhw.smartagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

### ChatService.java

位置：`src/main/java/xhw/smartagent/service/ChatService.java`

```java
package xhw.smartagent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String sessionId, String message) {
        try {
            // 1. 从 Redis 取历史消息
            String historyJson = redisTemplate.opsForValue().get("session:" + sessionId);
            List<Map<String, String>> history;
            if (historyJson != null) {
                history = objectMapper.readValue(historyJson, new TypeReference<List<Map<String, String>>>(){});
            } else {
                history = new ArrayList<>();
            }

            // 2. 加上新消息
            history.add(Map.of("role", "user", "content", message));

            // 3. 调用 AI（带上历史消息）
            StringBuilder prompt = new StringBuilder();
            for (Map<String, String> msg : history) {
                prompt.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }

            String reply = chatClient.prompt(prompt.toString()).call().content();

            // 4. 保存 AI 回复到历史
            history.add(Map.of("role", "assistant", "content", reply));

            // 5. 只保留最近 10 条消息（防止太长）
            if (history.size() > 10) {
                history = history.subList(history.size() - 10, history.size());
            }

            // 6. 保存回 Redis（过期时间 1 小时）
            redisTemplate.opsForValue().set(
                    "session:" + sessionId,
                    objectMapper.writeValueAsString(history),
                    java.time.Duration.ofHours(1)
            );

            return reply;
        } catch (Exception e) {
            return "对话出错: " + e.getMessage();
        }
    }
}
```

### 更新 ChatController

```java
package xhw.smartagent.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xhw.smartagent.service.ChatService;

@RestController
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/chat")
    public String chat(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam String message) {
        return chatService.chat(sessionId, message);
    }
}
```

### 测试多轮对话

```
第一次：http://localhost:8080/chat?sessionId=user1&message=我叫张三
回复：你好，张三！

第二次：http://localhost:8080/chat?sessionId=user1&message=我叫什么名字
回复：你叫张三。
```

### ✅ Day 4 完成标准
- [ ] 安装并启动 Redis
- [ ] pom.xml 加 Redis 依赖
- [ ] 创建 RedisConfig.java
- [ ] 创建 ChatService.java
- [ ] 更新 ChatController 支持 sessionId
- [ ] 测试多轮对话，AI 能记住之前的对话

---

## Day 5：完善 + 写简历

### 项目结构最终版

```
SmartAgent/
├── pom.xml
├── SmartAgent_学习计划.md
├── src/main/java/xhw/smartagent/
│   ├── SmartAgentApplication.java
│   ├── DeepSeekTest.java               ← Day 1 的测试类
│   ├── Controller/
│   │   └── ChatController.java         ← GET /chat
│   ├── service/
│   │   └── ChatService.java            ← 对话逻辑 + 记忆
│   ├── tool/
│   │   ├── ToolConfig.java             ← 工具定义（calculate, getWeather）
│   │   ├── Tool.java                   ← 工具接口（可选）
│   │   ├── CalculateTool.java          ← 计算器（可选）
│   │   ├── WeatherTool.java            ← 查天气（可选）
│   │   └── ToolManager.java            ← 管理工具（可选）
│   └── config/
│       ├── ChatClientConfig.java       ← ChatClient 配置 + 注册工具
│       └── RedisConfig.java            ← Redis 配置
└── src/main/resources/
    ├── application.yml                 ← 主配置
    └── application-dev.yml             ← 开发环境配置
```

### application-dev.yml

```yaml
spring:
  ai:
    openai:
      api-key: sk-你的key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8080
```

### 简历写法

> **AITeacher AI 面试教练**  
> *Spring Boot / Spring AI / DeepSeek API / RAG / ChromaDB / Redis*
>
> - 集成大模型 API，实现自然语言对话接口
> - 设计 Function Calling 机制，AI 可调用计算器、天气查询等 Java 工具
> - 使用 Redis 维护多轮对话上下文
> - 采用策略模式管理多个 Tool，方便扩展

---

## 推荐学习资源

| 资源 | 链接 |
|------|------|
| DeepSeek 开放平台 | https://platform.deepseek.com |
| DeepSeek API 文档 | https://platform.deepseek.com/api-docs |
| Spring AI 官方文档 | https://spring.io/projects/spring-ai |
| LangChain4j（替代方案） | https://docs.langchain4j.dev |
| 免费天气 API | https://wttr.in （无需注册，直接用） |
| Function Calling 原理 | https://platform.deepseek.com/api-docs |
| curl 测试 API | 终端运行 `curl https://api.deepseek.com/v1/models -H "Authorization: Bearer sk-xxx"` |

---

## 如果卡住了

| 问题 | 怎么办 |
|------|--------|
| Spring AI 版本不兼容 | 退回手写 HttpClient 方式，自己封装 |
| DeepSeek 请求超时 | 检查代理/网络，换国内 API |
| Function Calling 不理解 | 先手动模拟：固定返回"假装 AI 调用了工具" |
| 时间不够 | 只做 Day 1 + Day 3，核心就两个功能 |
| API Key 无效 | 重新生成一个，检查余额 |
| pom.xml 报错 | 检查是否有重复的 `<dependencies>` 标签 |

**最重要的原则：先跑起来，再优化。**
