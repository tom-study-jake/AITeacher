package xhw.aiteacher.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import xhw.aiteacher.service.ChatService;
import xhw.aiteacher.service.QuestionService;

import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private QuestionService questionService;

    @PostMapping("/chat")
    public Object chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", "default");
        String mode = request.getOrDefault("mode", "chat");

        if ("random".equals(mode) || "interview".equals(mode)) {
            return chatService.handleRandom(sessionId, message, mode);
        }
        return chatService.chat(sessionId, message, mode);
    }

    @GetMapping("/random-question")
    public Map<String, String> randomQuestion() {
        return questionService.getRandomQuestion();
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", "default");
        String mode = request.getOrDefault("mode", "chat");
        return chatService.chatStream(sessionId, message, mode);
    }
}
