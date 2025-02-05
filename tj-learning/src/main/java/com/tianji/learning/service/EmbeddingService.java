package com.tianji.learning.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class EmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();

    public float[] getEmbedding(String text) {
        String url = "http://127.0.0.1:5000/embedding";  // Flask 服务器
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> request = new HashMap<>();
        request.put("text", text);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<float[]> response = restTemplate.postForEntity(url, entity, float[].class);

        return response.getBody();
    }
}