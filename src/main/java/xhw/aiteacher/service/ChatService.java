package xhw.aiteacher.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final int MAX_HISTORY = 100;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final Duration QUESTION_TTL = Duration.ofMinutes(30);

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private QuestionService questionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String sessionId, String message, String mode) {
        try {
            validateMessage(message);

            List<Map<String, String>> history = loadHistory(sessionId);
            history.add(Map.of("role", "user", "content", message));

            String prompt = buildPrompt(history, mode);
            String reply = chatClient.prompt(prompt).call().content();

            saveHistory(sessionId, history, reply);
            return reply;
        } catch (IllegalArgumentException e) {
            return "输入错误: " + e.getMessage();
        } catch (Exception e) {
            return "对话出错: " + e.getMessage();
        }
    }

    public Flux<String> chatStream(String sessionId, String message, String mode) {
        try {
            validateMessage(message);

            List<Map<String, String>> history = loadHistory(sessionId);
            history.add(Map.of("role", "user", "content", message));

            String prompt = buildPrompt(history, mode);

            StringBuilder fullReply = new StringBuilder();
            final List<Map<String, String>> finalHistory = new ArrayList<>(history);
            final String finalSessionId = sessionId;

            return Flux.from(chatClient.prompt(prompt).stream().content())
                    .doOnNext(fullReply::append)
                    .doOnComplete(() -> {
                        try {
                            saveHistory(finalSessionId, finalHistory, fullReply.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IllegalArgumentException e) {
            return Flux.error(new RuntimeException("输入错误: " + e.getMessage()));
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
                Map<String, String> currentQuestion = objectMapper.readValue(questionJson, new TypeReference<>() {});
                redisTemplate.delete("question:" + sessionId);

                String prompt = buildEvaluatePrompt(currentQuestion, message, mode);
                String reply = chatClient.prompt(prompt).call().content();

                int index = incrementQuestionIndex(sessionId);
                return Map.of("type", "evaluate", "reply", reply, "answer", currentQuestion.get("answer"), "index", index);
            }

            String keyword = extractKeyword(message);
            Map<String, String> question = questionService.getRandomByKeyword(keyword);
            redisTemplate.opsForValue().set("question:" + sessionId, objectMapper.writeValueAsString(question), QUESTION_TTL);

            int index = getQuestionIndex(sessionId) + 1;
            if ("interview".equals(mode)) {
                return Map.of("type", "question", "question", question.get("question"), "category", question.get("category"), "index", index);
            }
            return Map.of("type", "question", "question", question.get("question"), "category", question.get("category"), "keywords", question.get("keywords"));

        } catch (Exception e) {
            return Map.of("error", "操作失败: " + e.getMessage());
        }
    }

    private void validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("消息过长，最多" + MAX_MESSAGE_LENGTH + "字");
        }
    }

    private List<Map<String, String>> loadHistory(String sessionId) {
        try {
            String historyJson = redisTemplate.opsForValue().get("session:" + sessionId);
            if (historyJson == null) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(historyJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveHistory(String sessionId, List<Map<String, String>> history, String reply) {
        try {
            history.add(Map.of("role", "assistant", "content", reply));
            while (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
            redisTemplate.opsForValue().set("session:" + sessionId, objectMapper.writeValueAsString(history), SESSION_TTL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildPrompt(List<Map<String, String>> history, String mode) {
        StringBuilder sb = new StringBuilder();

        if ("interview".equals(mode)) {
            sb.append("【模式：模拟面试】\n你是资深 Java 面试官。根据题库出题，候选人回答后评分（1-10分）并给改进建议。\n\n");
        }

        String knowledge = matchKnowledge(history.get(history.size() - 1).get("content"));
        if (!knowledge.isEmpty()) {
            sb.append("【参考题库】\n").append(knowledge).append("\n\n");
        }

        for (Map<String, String> msg : history) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        return sb.toString();
    }

    private String buildEvaluatePrompt(Map<String, String> currentQuestion, String userAnswer, String mode) {
        if ("interview".equals(mode)) {
            return "你是资深 Java 面试官。请评价候选人的回答。\n\n"
                    + "题目：" + currentQuestion.get("question") + "\n"
                    + "标准答案：" + currentQuestion.get("answer") + "\n"
                    + "候选人回答：" + userAnswer + "\n\n"
                    + "请给出：1.评分（1-10分）2.优点 3.不足 4.改进建议\n"
                    + "最后附上标准答案。";
        } else {
            return "你是Java面试评分官。请评价用户的回答。\n\n"
                    + "题目：" + currentQuestion.get("question") + "\n"
                    + "标准答案：" + currentQuestion.get("answer") + "\n"
                    + "用户回答：" + userAnswer + "\n\n"
                    + "请给出：1.对错判断 2.得分（1-10分）3.简短点评\n"
                    + "最后请附上完整标准答案。";
        }
    }

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

    private int getQuestionIndex(String sessionId) {
        String indexStr = redisTemplate.opsForValue().get("index:" + sessionId);
        return indexStr != null ? Integer.parseInt(indexStr) : 0;
    }

    private int incrementQuestionIndex(String sessionId) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String key = "index:" + sessionId;
        String val = ops.get(key);
        int newIndex = (val != null ? Integer.parseInt(val) : 0) + 1;
        ops.set(key, String.valueOf(newIndex), QUESTION_TTL);
        return newIndex;
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
