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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    @Resource
    private SiliconflowUtil siliconflowUtil;
    
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    
    // 最大超时时间：30分钟
    private static final long MAX_TIMEOUT_MINUTES = 30;
    
    // 用于存储会话状态的Map
    private final Map<String, ChatSession> sessionMap = new ConcurrentHashMap<>();
    
    // 会话清理任务，每小时运行一次，清理超过2小时的会话
    {
        executorService.scheduleAtFixedRate(() -> {
            try {
                long expireTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2);
                sessionMap.entrySet().removeIf(entry -> entry.getValue().getLastAccessTime() < expireTime);
                System.out.println("已清理过期会话，当前会话数: " + sessionMap.size());
            } catch (Exception e) {
                System.err.println("会话清理任务异常: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 流式输出AI回答
     * 
     * @param question 用户问题
     * @param sessionId 会话ID，用于断点续传，为空时自动生成
     * @param lastSequence 上次接收到的最后序号，用于断点续传
     * @return SSE事件流
     */
    @GetMapping("/chat")
    public SseEmitter streamChat(
            @RequestParam String question,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Integer lastSequence) {
        
        // 创建或获取会话ID
        String currentSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : UUID.randomUUID().toString();
        int startSequence = (lastSequence != null) ? lastSequence : 0;
        
        System.out.println("接收到请求 - sessionId: " + currentSessionId + ", lastSequence: " + startSequence + 
                ", 问题: " + (question.length() > 50 ? question.substring(0, 50) + "..." : question));
        
        // 创建SseEmitter实例，设置超时时间为30分钟
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(MAX_TIMEOUT_MINUTES));
        
        // 设置SSE连接建立时的回调
        emitter.onCompletion(() -> System.out.println("会话[" + currentSessionId + "] SSE连接完成"));
        emitter.onTimeout(() -> System.out.println("会话[" + currentSessionId + "] SSE连接超时: " + TimeUnit.MINUTES.toMillis(MAX_TIMEOUT_MINUTES) + "ms"));
        emitter.onError(ex -> System.out.println("会话[" + currentSessionId + "] SSE连接发生错误: " + ex.getMessage()));
        
        // 获取或创建会话
        ChatSession chatSession = sessionMap.computeIfAbsent(currentSessionId, id -> new ChatSession(id, question));
        chatSession.updateAccessTime();
        
        // 创建用于同步的CountDownLatch和完成标志
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicBoolean streamCompleted = new AtomicBoolean(false);
        AtomicLong lastChunkTime = new AtomicLong(System.currentTimeMillis());
        AtomicBoolean isHeartbeatActive = new AtomicBoolean(true);
        
        // 启动心跳检测，保持连接活跃并监测活动状态
        ScheduledFuture<?> heartbeatFuture = executorService.scheduleAtFixedRate(() -> {
            try {
                if (!isHeartbeatActive.get()) {
                    return; // 如果心跳已停止，不再发送
                }
                
                // 检查自上次接收数据的时间
                long timeSinceLastChunk = System.currentTimeMillis() - lastChunkTime.get();
                
                // 如果超过30秒没有数据，发送心跳保持连接
                if (timeSinceLastChunk > 30000 && !streamCompleted.get()) {
                    JSONObject heartbeatMessage = new JSONObject();
                    heartbeatMessage.put("type", "heartbeat");
                    heartbeatMessage.put("data", "");
                    heartbeatMessage.put("seq", -1); // 特殊序列号，前端会忽略
                    heartbeatMessage.put("sessionId", currentSessionId); // 增加会话ID
                    
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(heartbeatMessage.toJSONString(), MediaType.APPLICATION_JSON));
                    
                    System.out.println("会话[" + currentSessionId + "] 发送心跳包，距上次数据: " + timeSinceLastChunk + "ms");
                }
                
                // 如果超过3分钟没有收到数据，并且尚未标记完成，可能是卡住了，强制结束
                if (timeSinceLastChunk > 180000 && !streamCompleted.get()) {
                    System.out.println("会话[" + currentSessionId + "] 长时间未收到数据，触发超时机制");
                    // 如果CountDownLatch还没有计数到0，手动让它计数到0
                    completionLatch.countDown();
                }
            } catch (Exception e) {
                System.out.println("会话[" + currentSessionId + "] 心跳检测异常: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        // 使用线程池异步处理，避免阻塞主线程
        executorService.submit(() -> {
            try {
                // 发送会话信息作为第一个消息
                JSONObject sessionMessage = new JSONObject();
                sessionMessage.put("type", "session");
                sessionMessage.put("data", "会话已建立");
                sessionMessage.put("sessionId", currentSessionId);
                sessionMessage.put("seq", 0);
                
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(sessionMessage.toJSONString(), MediaType.APPLICATION_JSON));
                
                // 发送初始消息
                JSONObject startMessage = new JSONObject();
                startMessage.put("type", "start");
                startMessage.put("data", "开始生成回答...");
                startMessage.put("seq", 1);
                startMessage.put("sessionId", currentSessionId);
                
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(startMessage.toJSONString(), MediaType.APPLICATION_JSON));
                
                // 如果是续传，先发送已缓存的内容
                if (startSequence > 0 && !chatSession.getResponseChunks().isEmpty()) {
                    System.out.println("会话[" + currentSessionId + "] 开始断点续传，从序号 " + startSequence + " 开始");
                    
                    // 发送之前缓存的内容
                    for (JSONObject chunk : chatSession.getResponseChunks()) {
                        int seq = chunk.getIntValue("seq");
                        if (seq > startSequence) {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chunk.toJSONString(), MediaType.APPLICATION_JSON));
                            
                            System.out.println("会话[" + currentSessionId + "] 续传消息: seq=" + seq);
                        }
                    }
                    
                    // 如果会话已完成，发送结束消息并关闭连接
                    if (chatSession.isCompleted()) {
                        JSONObject endMessage = new JSONObject();
                        endMessage.put("type", "end");
                        endMessage.put("data", "回答生成完毕");
                        endMessage.put("seq", chatSession.getNextSequence());
                        endMessage.put("sessionId", currentSessionId);
                        
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(endMessage.toJSONString(), MediaType.APPLICATION_JSON));
                        
                        // 完成连接
                        emitter.complete();
                        return;
                    }
                } else if (chatSession.isGenerating()) {
                    // 如果已经在生成中，不要再次启动生成过程
                    System.out.println("会话[" + currentSessionId + "] 已在生成中，当前进度：" + chatSession.getResponseChunks().size() + " 个片段");
                    return;
                }
                
                // 标记会话为生成中
                chatSession.setGenerating(true);
                
                // 使用SiliconflowUtil的流式API获取回答
                siliconflowUtil.askStream(question, chunk -> {
                    try {
                        // 更新最后接收数据的时间
                        lastChunkTime.set(System.currentTimeMillis());
                        
                        // 获取下一个序列号
                        int seq = chatSession.getNextSequence();
                        
                        // 为每个块创建带序号的消息
                        JSONObject contentMessage = new JSONObject();
                        contentMessage.put("type", "content");
                        contentMessage.put("data", chunk);
                        contentMessage.put("seq", seq);
                        contentMessage.put("sessionId", currentSessionId);
                        
                        // 保存到会话缓存
                        chatSession.addResponseChunk(contentMessage);
                        
                        // 发送每个响应片段
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(contentMessage.toJSONString(), MediaType.APPLICATION_JSON));
                        
                        // 计数器记录
                        if (seq % 50 == 0) {
                            System.out.println("会话[" + currentSessionId + "] 已接收 " + seq + " 个响应片段");
                        }
                    } catch (IOException e) {
                        // 发送失败，中断处理
                        System.err.println("会话[" + currentSessionId + "] 发送响应片段时出错: " + e.getMessage());
                        e.printStackTrace();
                        completionLatch.countDown(); // 出错时解除阻塞
                    }
                });
                
                System.out.println("会话[" + currentSessionId + "] 流式API调用结束，共接收 " + 
                        chatSession.getResponseChunks().size() + " 个响应片段");
                
                // 标记会话完成
                chatSession.setCompleted(true);
                chatSession.setGenerating(false);
                
                // 标记流式传输已完成
                streamCompleted.set(true);
                
                // 释放CountDownLatch
                completionLatch.countDown();
                
            } catch (Exception e) {
                System.err.println("会话[" + currentSessionId + "] 流式处理异常: " + e.getMessage());
                e.printStackTrace();
                
                // 关闭与错误相关的指示器
                streamCompleted.set(true);
                completionLatch.countDown();
                
                // 更新会话状态
                chatSession.setGenerating(false);
                
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // 忽略关闭时的错误
                }
            }
        });
        
        // 在另一个线程中等待流式传输完成，然后发送结束消息
        executorService.submit(() -> {
            try {
                // 等待流式传输完成或超时(最多等待最大超时时间)
                boolean completed = completionLatch.await(MAX_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                
                // 停止心跳
                isHeartbeatActive.set(false);
                heartbeatFuture.cancel(false);
                
                System.out.println("会话[" + currentSessionId + "] 完成等待，latch状态: " + completed + 
                        ", streamCompleted: " + streamCompleted.get());
                
                // 确保有足够的时间让最后的内容发送完成
                Thread.sleep(2000);
                
                // 如果完成或超时，发送结束消息
                if (completed || streamCompleted.get()) {
                    // 发送完成消息
                    JSONObject endMessage = new JSONObject();
                    endMessage.put("type", "end");
                    endMessage.put("data", "回答生成完毕");
                    endMessage.put("seq", chatSession.getNextSequence());
                    endMessage.put("sessionId", currentSessionId);
                    
                    // 保存到会话缓存
                    chatSession.addResponseChunk(endMessage);
                    
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(endMessage.toJSONString(), MediaType.APPLICATION_JSON));
                    
                    // 再等一小段时间确保结束消息发送完成
                    Thread.sleep(500);
                    
                    // 标记SSE连接完成
                    emitter.complete();
                    
                    System.out.println("会话[" + currentSessionId + "] 流式响应已完成");
                } else {
                    // 超时未完成
                    JSONObject timeoutMessage = new JSONObject();
                    timeoutMessage.put("type", "error");
                    timeoutMessage.put("data", "生成响应超时");
                    timeoutMessage.put("seq", chatSession.getNextSequence());
                    timeoutMessage.put("sessionId", currentSessionId);
                    
                    // 保存到会话缓存
                    chatSession.addResponseChunk(timeoutMessage);
                    
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(timeoutMessage.toJSONString(), MediaType.APPLICATION_JSON));
                    
                    // 标记SSE连接完成
                    Thread.sleep(500);
                    emitter.complete();
                    
                    System.out.println("会话[" + currentSessionId + "] 流式响应超时结束");
                }
            } catch (Exception e) {
                System.err.println("会话[" + currentSessionId + "] 结束处理异常: " + e.getMessage());
                e.printStackTrace();
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // 忽略关闭时的错误
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 会话类，用于保存会话状态和响应内容
     */
    private static class ChatSession {
        private final String sessionId;
        private final String question;
        private final List<JSONObject> responseChunks = new ArrayList<>();
        private final AtomicInteger sequenceGenerator = new AtomicInteger(2); // 从2开始，因为0和1已用于会话和开始消息
        private AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());
        private volatile boolean completed = false;
        private volatile boolean generating = false;
        
        public ChatSession(String sessionId, String question) {
            this.sessionId = sessionId;
            this.question = question;
        }
        
        public synchronized void addResponseChunk(JSONObject chunk) {
            responseChunks.add(chunk);
        }
        
        public int getNextSequence() {
            return sequenceGenerator.getAndIncrement();
        }
        
        public List<JSONObject> getResponseChunks() {
            return responseChunks;
        }
        
        public void updateAccessTime() {
            this.lastAccessTime.set(System.currentTimeMillis());
        }
        
        public long getLastAccessTime() {
            return lastAccessTime.get();
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public boolean isGenerating() {
            return generating;
        }
        
        public void setGenerating(boolean generating) {
            this.generating = generating;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getQuestion() {
            return question;
        }
    }
}
