package com.xiao.httpclient;

import com.xiao.httpclient.rpc.BailianUtil;
import com.xiao.httpclient.rpc.SiliconflowUtil;
import com.xiao.httpclient.utils.HttpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class HttpTest {
    @Resource
    HttpUtil httpUtil;
    @Resource
    SiliconflowUtil siliconflowUtil;
    @Test
    public void test() {
        String url = "https://www.baidu.com";
//        System.out.println(httpUtil.doGet(url, null, null));

//        System.out.println(httpUtil.doPost(url, null, null));
        System.out.println(httpUtil.doPut(url, null, null));
        System.out.println("=================================================");
        System.out.println(httpUtil.doDelete(url, null));
    }

    @Test
    public void sc() {
        System.out.println(siliconflowUtil.ask("你好!"));
    }
    @Resource
    BailianUtil bailianUtil;
    @Test
    public void bl() {
        System.out.println(bailianUtil.ask("你好!"));
    }
    @Test
    public void bl2() {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer sk-31c42cd4ae9243cd9e2faecb56f4acc9");
            headers.put("Content-Type", "application/json");
            
            String body = "{\"messages\":[{\"role\":\"user\",\"content\":[{\"text\":\"你好!\",\"type\":\"text\"}]}],\"model\":\"qwen2.5-vl-32b-instruct\"}";
            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
            
            System.out.println("API请求：");
            System.out.println("URL: " + url);
            System.out.println("Headers: " + headers);
            System.out.println("Body: " + body);
            
            String response = httpUtil.doPost(url, headers, body);
            System.out.println("API响应: " + response);
        } catch (Exception e) {
            System.err.println("测试失败，异常信息：" + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，以便测试框架能够捕获
        }
    }

    @Test
    public void scStream() {
        StringBuilder fullResponse = new StringBuilder();
        
        System.out.println("开始流式请求测试...");
        siliconflowUtil.askStream("写一颗线段树", chunk -> {
            // 处理每个流式响应片段
            System.out.print(chunk); // 实时打印每个片段
            fullResponse.append(chunk); // 收集完整响应
        });
        
        System.out.println("\n\n完整响应:");
        System.out.println(fullResponse.toString());
    }
}
