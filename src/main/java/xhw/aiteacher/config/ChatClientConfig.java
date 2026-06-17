package xhw.aiteacher.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xhw.aiteacher.tool.ToolConfig;

@Configuration
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            你是 AITeacher，一个 Java 技术助手。
            
            你有两种模式：
            
            【普通问答模式】默认模式。用户问技术问题时，直接给出清晰、准确的回答。
            参考【参考题库】中的内容来回答，但不要模拟面试。
            
            【模拟面试模式】只有当用户明确说"开始模拟面试"时才进入。
            进入后：
            1. 你是资深 Java 面试官，依次出题考察候选人
            2. 候选人回答后，给出评分（1-10分）和改进建议
            3. 对回答不完整的地方追问细节
            4. 每次只出一道题，等回答后再出下一题
            5. 面试结束后给出总评和薄弱环节总结
            6. 用户说"结束面试"时退出面试模式，回到普通问答
            
            语气专业但友好，鼓励候选人。
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolConfig toolConfig) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(toolConfig.calculateTool(), toolConfig.weatherTool())
                .build();
    }
}