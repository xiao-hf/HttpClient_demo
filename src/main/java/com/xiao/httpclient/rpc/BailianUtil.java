package com.xiao.httpclient.rpc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiao.httpclient.utils.HttpUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class BailianUtil {
    @Resource
    HttpUtil httpUtil;
    
    private String key = "sk-31c42cd4ae9243cd9e2faecb56f4acc9";
    private String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private String model = "qwen2.5-vl-32b-instruct";
    
    /**
     * 发送纯文本请求到百炼API
     * 
     * @param question 用户问题
     * @return API响应结果
     */
    public String ask(String question) {
        return ask(question, null);
    }
    
    /**
     * 发送请求到百炼API，支持文本和图片
     * 
     * @param question 用户问题
     * @param imageUrl 图片URL，可以为null
     * @return API响应结果
     */
    public String ask(String question, String imageUrl) {
        try {
            // 1.构建请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + key.trim());
            headers.put("Content-Type", "application/json");
            
            // 2.构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            
            // 3.构建消息内容
            JSONArray messagesArray = new JSONArray();
            JSONObject messageObject = new JSONObject();
            messageObject.put("role", "user");
            
            // 4.构建content数组（包含文本和可选的图片）
            JSONArray contentArray = new JSONArray();
            
            // 添加文本内容
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", question);
            contentArray.add(textContent);
            
            // 如果提供了图片URL，添加图片内容
            if (imageUrl != null && !imageUrl.isEmpty()) {
                JSONObject imageContent = new JSONObject();
                imageContent.put("type", "image_url");
                
                JSONObject imageUrlObj = new JSONObject();
                imageUrlObj.put("url", imageUrl);
                imageContent.put("image_url", imageUrlObj);
                
                contentArray.add(imageContent);
            }
            
            // 5.设置消息内容
            messageObject.put("content", contentArray);
            messagesArray.add(messageObject);
            requestBody.put("messages", messagesArray);
            
            // 打印请求体用于调试
            System.out.println("请求URL: " + url);
            System.out.println("请求头: " + headers);
            System.out.println("请求体: " + requestBody.toJSONString());
            
            // 6.发送请求并返回结果
            return httpUtil.doPost(url, headers, requestBody.toJSONString());
        } catch (Exception e) {
            // 捕获并记录异常，提供更多信息
            System.err.println("调用百炼API时发生异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("调用百炼API失败: " + e.getMessage(), e);
        }
    }
}
