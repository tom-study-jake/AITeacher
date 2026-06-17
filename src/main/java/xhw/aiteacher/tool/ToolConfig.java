package xhw.aiteacher.tool;

import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        @Tool(description = "查询指定城市的天气")
        public String getWeather(String city) {
            try {
                String url = "https://wttr.in/" + city + "?format=j1";
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .build();
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                String weather = body.replaceAll(".*\"weatherDesc\"\\s*:\\s*\\[\\s*\\{\\s*\"value\"\\s*:\\s*\"(.*)\".*", "$1");
                String temp = body.replaceAll(".*\"temp_C\"\\s*:\\s*\"(.*)\".*", "$1");
                return city + "天气: " + weather + "，" + temp + "°C";
            } catch (Exception e) {
                return "查询天气失败: " + e.getMessage();
            }
        }
    }
}
