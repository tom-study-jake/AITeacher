package xhw.aiteacher.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Configuration
public class ToolConfig {

    @Bean
    public CalculateTool calculateTool() {
        return new CalculateTool();
    }

    @Bean
    public WeatherTool weatherTool() {
        return new WeatherTool();
    }

    public static class CalculateTool {
        @Tool(description = "计算数学表达式，如 1+1、2*3")
        public String calculate(String expression) {
            try {
                Object result = AviatorEvaluator.execute(expression);
                return "计算结果: " + result.toString();
            } catch (Exception e) {
                return "计算错误: " + e.getMessage();
            }
        }
    }

    public static class WeatherTool {
        private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Tool(description = "查询指定城市的天气")
        public String getWeather(String city) {
            try {
                String url = "https://wttr.in/" + city + "?format=j1";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = MAPPER.readTree(response.body());
                String weather = root.path("current_condition").get(0).path("weatherDesc").get(0).path("value").asText();
                String temp = root.path("current_condition").get(0).path("temp_C").asText();
                return city + "天气: " + weather + "，" + temp + "°C";
            } catch (Exception e) {
                return "查询天气失败: " + e.getMessage();
            }
        }
    }
}
