package com.tianji.learning.controller;

import com.tianji.learning.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api")
public class ChatController {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        new Thread(() -> {
            try {
                // 1. 检查 Redis 缓存
                String cachedResponse = checkRedisCache(prompt);
                if (cachedResponse != null) {
                    emitter.send(cachedResponse);
                    emitter.complete();
                    return;
                }

                // 2. 发送请求到 Ollama
                URL url = new URL("http://localhost:11434/api/generate");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // 3. 构造请求体
                Map<String, Object> request = new HashMap<>();
                request.put("model", "mistral");
                request.put("prompt", prompt);
                request.put("stream", true);

                // 4. 发送 JSON 请求
                connection.getOutputStream().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(request));

                // 5. 读取流式返回
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        emitter.send(line);
                        responseBuilder.append(line).append("\n"); // 累积完整的响应
                    }
                }

                // 6. 结束 SSE
                emitter.complete();

                // 7. 存入 Redis
                storeResponseToRedis(prompt, responseBuilder.toString());

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 查询 Redis，看是否有相似问题的回答
     */
    private String checkRedisCache(String prompt) {
        float[] vector = embeddingService.getEmbedding(prompt);
        String redisKey = vectorToRedisKey(vector);
        return redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 存储 Ollama 生成的结果到 Redis
     */
    private void storeResponseToRedis(String prompt, String response) {
        float[] vector = embeddingService.getEmbedding(prompt);
        String redisKey = vectorToRedisKey(vector);
        redisTemplate.opsForValue().set(redisKey, response, 30, TimeUnit.MINUTES); // 缓存 30 分钟
    }

    /**
     * 向量转换成 Redis Key
     */
    private String vectorToRedisKey(float[] vector) {
        return Arrays.stream(vector)
                .mapToObj(v -> String.format("%.4f", v))
                .collect(Collectors.joining("_"));
    }
}
}
