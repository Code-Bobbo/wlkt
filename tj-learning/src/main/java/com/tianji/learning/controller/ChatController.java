package com.tianji.learning.controller;

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
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class ChatController {
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        new Thread(() -> {
            try {
                // 1. 发送请求到 Ollama
                URL url = new URL("http://localhost:11434/api/generate");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // 2. 构造请求体
                Map<String, Object> request = new HashMap<>();
                request.put("model", "deepseek-r1");  // Ollama 模型
                request.put("prompt", prompt);
                request.put("stream", true);  // 开启流式返回

                // 3. 发送 JSON 请求
                connection.getOutputStream().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(request));

                // 4. 读取流式返回
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        emitter.send(line);
                    }
                }

                // 5. 结束 SSE
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
