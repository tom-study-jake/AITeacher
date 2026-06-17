package xhw.aiteacher.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    @GetMapping("/chat")
    public Object chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(defaultValue = "chat") String mode) {
        if ("random".equals(mode) || "interview".equals(mode)) {
            return chatService.handleRandom(sessionId, message, mode);
        }
        return chatService.chat(sessionId, message, mode);
    }

    @GetMapping("/random-question")
    public Map<String,String> randomQuestion(){
        return questionService.getRandomQuestion();
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(defaultValue = "chat") String mode) {
        return chatService.chatStream(sessionId, message, mode);
    }
}
