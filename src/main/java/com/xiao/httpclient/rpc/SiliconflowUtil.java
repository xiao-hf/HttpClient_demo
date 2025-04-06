package com.xiao.httpclient.rpc;

import com.alibaba.fastjson.JSONObject;
import com.xiao.httpclient.utils.HttpUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SiliconflowUtil {

    @Resource
    HttpUtil httpUtil;
    private String model = "Qwen/Qwen2.5-Coder-32B-Instruct";
    private String key = "sk-gknzcvpnaixfdsghfftvhlqhywhtujrdhicwyuzguwtfzrtf";
    private String url = "https://api.siliconflow.cn/v1/chat/completions";

    /**
     * 发送请求到 SiliconFlow API 获取模型回答
     * 
     * @param userQuestion 用户问题
     * @param maxTokens 最大生成 token 数，默认为 512
     * @param temperature 温度参数，默认为 0.7
     * @return API 响应结果
     */
    public String ask(String userQuestion, Integer maxTokens, Double temperature) {
        // 设置默认值
        if (maxTokens == null) {
            maxTokens = 512;
        }
        if (temperature == null) {
            temperature = 0.7;
        }

        // 设置请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + key);
        headers.put("Content-Type", "application/json");

        // 构建消息数组
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userQuestion);
        messages.add(userMessage);

        // 构建响应格式对象
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "text");

        // 构建请求体对象
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("stop", null);
        requestBody.put("temperature", temperature);
        requestBody.put("top_p", 0.7);
        requestBody.put("top_k", 50);
        requestBody.put("frequency_penalty", 0.5);
        requestBody.put("n", 1);
        requestBody.put("response_format", responseFormat);

        // 发送 POST 请求并返回结果
        return httpUtil.doPost(url, headers, requestBody.toJSONString());
    }
    
    /**
     * 使用默认参数发送请求到 SiliconFlow API
     * 
     * @param userQuestion 用户问题
     * @return API 响应结果
     */
    public String ask(String userQuestion) {
        return ask(userQuestion, null, null);
    }
}
