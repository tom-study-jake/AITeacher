package xhw.aiteacher.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private QuestionService questionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String sessionId, String message, String mode) {
        try {
            String historyJson = redisTemplate.opsForValue().get("session:" + sessionId);
            List<Map<String, String>> history = (historyJson != null)
                    ? objectMapper.readValue(historyJson, new TypeReference<List<Map<String, String>>>(){})
                    : new ArrayList<>();

            history.add(Map.of("role", "user", "content", message));

            StringBuilder sb = new StringBuilder();

            if ("interview".equals(mode)) {
                sb.append("【模式：模拟面试】\n你是资深 Java 面试官。根据题库出题，候选人回答后评分（1-10分）并给改进建议。\n\n");
            }

            String knowledge = matchKnowledge(message);
            if (!knowledge.isEmpty()) {
                sb.append("【参考题库】\n").append(knowledge).append("\n\n");
            }

            for (Map<String, String> msg : history) {
                sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }
            String prompt = sb.toString();

            String reply = chatClient.prompt(prompt).call().content();

            history.add(Map.of("role", "assistant", "content", reply));

            while (history.size() > 100) {
                history.remove(0);
            }

            redisTemplate.opsForValue().set(
                    "session:" + sessionId,
                    objectMapper.writeValueAsString(history),
                    java.time.Duration.ofHours(24)
            );

            return reply;
        } catch (Exception e) {
            return "对话出错: " + e.getMessage();
        }
    }

    // 用用户说的话搜题库
    private String matchKnowledge(String message) {
        List<Map<String, String>> matched = questionService.searchByKeyword(message);
        if (matched.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, matched.size()); i++) {
            Map<String, String> q = matched.get(i);
            sb.append("Q: ").append(q.get("question")).append("\n");
            sb.append("A: ").append(q.get("answer")).append("\n\n");
        }
        return sb.toString();
    }
    public Flux<String> chatStream(String sessionId, String message, String mode) {
        try {
            String historyJson = redisTemplate.opsForValue().get("session:" + sessionId);
            List<Map<String, String>> history = (historyJson != null)
                    ? objectMapper.readValue(historyJson, new TypeReference<List<Map<String, String>>>(){})
                    : new ArrayList<>();
            history.add(Map.of("role", "user", "content", message));

            StringBuilder sb = new StringBuilder();

            if ("interview".equals(mode)) {
                sb.append("【模式：模拟面试】\n你是资深 Java 面试官。根据题库出题，候选人回答后评分（1-10分）并给改进建议。\n\n");
            }

            String knowledge = matchKnowledge(message);
            if (!knowledge.isEmpty()) {
                sb.append("【参考题库】\n").append(knowledge).append("\n\n");
            }

            for (Map<String, String> msg : history) {
                sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
            }

            StringBuilder fullReply = new StringBuilder();
            final List<Map<String, String>> finalHistory = new ArrayList<>(history);
            final String finalSessionId = sessionId;
            return Flux.from(chatClient.prompt(sb.toString()).stream().content())
                    .doOnNext(fullReply::append)
                    .doOnComplete(() -> {
                        try {
                            finalHistory.add(Map.of("role", "assistant", "content", fullReply.toString()));
                            while (finalHistory.size() > 100) {
                                finalHistory.remove(0);
                            }
                            redisTemplate.opsForValue().set(
                                    "session:" + finalSessionId,
                                    objectMapper.writeValueAsString(finalHistory),
                                    java.time.Duration.ofHours(24)
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    public Object handleRandom(String sessionId, String message) {
        return handleRandom(sessionId, message, "random");
    }

    public Object handleRandom(String sessionId, String message, String mode) {
        try {
            String questionJson = redisTemplate.opsForValue().get("question:" + sessionId);

            if (questionJson != null) {
                Map<String, String> currentQuestion = objectMapper.readValue(questionJson, new TypeReference<Map<String, String>>(){});
                redisTemplate.delete("question:" + sessionId);

                String prompt;
                if ("interview".equals(mode)) {
                    prompt = "你是资深 Java 面试官。请评价候选人的回答。\n\n"
                            + "第 " + getQuestionIndex(sessionId) + " 题\n"
                            + "题目：" + currentQuestion.get("question") + "\n"
                            + "标准答案：" + currentQuestion.get("answer") + "\n"
                            + "候选人回答：" + message + "\n\n"
                            + "请给出：1.评分（1-10分）2.优点 3.不足 4.改进建议\n"
                            + "最后附上标准答案。";
                } else {
                    prompt = "你是Java面试评分官。请评价用户的回答。\n\n"
                            + "题目：" + currentQuestion.get("question") + "\n"
                            + "标准答案：" + currentQuestion.get("answer") + "\n"
                            + "用户回答：" + message + "\n\n"
                            + "请给出：1.对错判断 2.得分（1-10分）3.简短点评\n"
                            + "最后请附上完整标准答案。";
                }

                String reply = chatClient.prompt(prompt).call().content();
                incrementQuestionIndex(sessionId);
                return Map.of("type", "evaluate", "reply", reply, "answer", currentQuestion.get("answer"));
            }

            String keyword = extractKeyword(message);
            Map<String, String> question = questionService.getRandomByKeyword(keyword);
            redisTemplate.opsForValue().set("question:" + sessionId, objectMapper.writeValueAsString(question), java.time.Duration.ofMinutes(30));

            if ("interview".equals(mode)) {
                int index = getQuestionIndex(sessionId) + 1;
                return Map.of("type", "question", "question", question.get("question"), "category", question.get("category"), "index", index);
            }
            return Map.of("type", "question", "question", question.get("question"), "category", question.get("category"), "keywords", question.get("keywords"));

        } catch (Exception e) {
            return Map.of("error", "操作失败: " + e.getMessage());
        }
    }

    private int getQuestionIndex(String sessionId) {
        String indexStr = redisTemplate.opsForValue().get("index:" + sessionId);
        return indexStr != null ? Integer.parseInt(indexStr) : 0;
    }

    private void incrementQuestionIndex(String sessionId) {
        int index = getQuestionIndex(sessionId) + 1;
        redisTemplate.opsForValue().set("index:" + sessionId, String.valueOf(index), java.time.Duration.ofMinutes(30));
    }

    private String extractKeyword(String message) {
        String[] keywords = {"Spring", "Java", "HashMap", "并发", "Redis", "MySQL", "JVM", "微服务", "MQ", "设计模式"};
        for (String kw : keywords) {
            if (message.contains(kw) || message.contains(kw.toLowerCase())) {
                return kw;
            }
        }
        String cleaned = message.replaceAll("抽|题|问|一个|相关|的|吗|？|\\?", "").trim();
        return cleaned.isEmpty() ? "" : cleaned;
    }
}