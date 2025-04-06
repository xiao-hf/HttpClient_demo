package com.xiao.httpclient.controller;

import com.alibaba.fastjson.JSONObject;
import com.xiao.httpclient.rpc.SiliconflowUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    @Resource
    private SiliconflowUtil siliconflowUtil;
    
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    /**
     * 流式输出AI回答
     * 
     * @param question 用户问题
     * @return SSE事件流
     */
    @GetMapping("/chat")
    public SseEmitter streamChat(@RequestParam String question) {
        // 创建SseEmitter实例，设置超时时间为1小时
        SseEmitter emitter = new SseEmitter(3600000L);
        
        // 设置SSE连接建立时的回调
        emitter.onCompletion(() -> System.out.println("SSE连接完成"));
        emitter.onTimeout(() -> System.out.println("SSE连接超时"));
        emitter.onError(ex -> System.out.println("SSE连接发生错误: " + ex.getMessage()));
        
        // 创建序号生成器
        AtomicInteger sequenceGenerator = new AtomicInteger(0);
        
        // 使用线程池异步处理，避免阻塞主线程
        executorService.submit(() -> {
            try {
                // 发送初始消息
                JSONObject startMessage = new JSONObject();
                startMessage.put("type", "start");
                startMessage.put("data", "开始生成回答...");
                startMessage.put("seq", sequenceGenerator.getAndIncrement());
                
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(startMessage.toJSONString(), MediaType.APPLICATION_JSON));
                
                // 收集所有响应片段，确保客户端能够正确排序
                ConcurrentLinkedQueue<String> contentQueue = new ConcurrentLinkedQueue<>();
                
                // 使用SiliconflowUtil的流式API获取回答
                siliconflowUtil.askStream(question, chunk -> {
                    try {
                        // 为每个块创建带序号的消息
                        JSONObject contentMessage = new JSONObject();
                        contentMessage.put("type", "content");
                        contentMessage.put("data", chunk);
                        contentMessage.put("seq", sequenceGenerator.getAndIncrement());
                        
                        // 发送每个响应片段
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(contentMessage.toJSONString(), MediaType.APPLICATION_JSON));
                        
                        // 保存内容以便后续处理
                        contentQueue.add(chunk);
                    } catch (IOException e) {
                        // 发送失败，中断处理
                        e.printStackTrace();
                        emitter.completeWithError(e);
                    }
                });
                
                // 发送完成消息
                JSONObject endMessage = new JSONObject();
                endMessage.put("type", "end");
                endMessage.put("data", "回答生成完毕");
                endMessage.put("seq", sequenceGenerator.getAndIncrement());
                
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(endMessage.toJSONString(), MediaType.APPLICATION_JSON));
                
                // 标记SSE连接完成
                emitter.complete();
                
            } catch (Exception e) {
                e.printStackTrace();
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
