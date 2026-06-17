package xhw.aiteacher.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuestionService {
    private List<Map<String,String>> questions= new ArrayList<>();

    /*创建 QuestionService 对象
    *创建完后自动调用 init()读取 questions.json，解析成 Java 集合
    * 后续调用 getRandomQuestion() 等方法时直接从内存取
    */
    @PostConstruct
    public void init() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = new ClassPathResource("questions.json").getInputStream();
        questions = mapper.readValue(is, new TypeReference<List<Map<String, String>>>(){});
    }
    //生成随机下标
    public Map<String, String> getRandomQuestion() {
        int index = ThreadLocalRandom.current().nextInt(questions.size());
        return questions.get(index);
    }
    //找关键字
    public List<Map<String, String>> searchByKeyword(String keyword) {
        return questions.stream()
                .filter(q -> q.get("question").contains(keyword) || q.get("keywords").contains(keyword))
                .toList();
    }
    //返回
    public List<Map<String,String>> getAllQuestions(){
        return questions;
    }

    public Map<String, String> getRandomByKeyword(String keyword) {
        List<Map<String, String>> filtered = searchByKeyword(keyword);
        if (filtered.isEmpty()) {
            return getRandomQuestion();
        }
        int index = ThreadLocalRandom.current().nextInt(filtered.size());
        return filtered.get(index);
    }
}
