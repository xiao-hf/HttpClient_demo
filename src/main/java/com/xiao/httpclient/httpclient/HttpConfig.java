package com.xiao.httpclient.httpclient;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class HttpConfig {
    
    @Bean
    public CloseableHttpClient closeableHttpClient() {
        try {
            // 创建SSL上下文，信任所有证书
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                    .build();
            
            // 创建SSL连接工厂，添加所有TLS版本支持
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[] { "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3" },
                    null,
                    NoopHostnameVerifier.INSTANCE);
            
            // 注册HTTP和HTTPS协议处理器
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();
            
            // 创建连接池管理器
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
            connectionManager.setMaxTotal(200); // 最大连接数
            connectionManager.setDefaultMaxPerRoute(20); // 每个路由最大连接数
            
            // 配置请求超时
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(10000)  // 连接超时时间，单位毫秒
                    .setSocketTimeout(15000)   // 数据传输超时时间，单位毫秒
                    .setConnectionRequestTimeout(5000) // 从连接池获取连接的超时时间
                    .build();
            
            // 创建HttpClient
            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    // 添加重试策略
                    .setRetryHandler((exception, executionCount, context) -> executionCount <= 3)
                    .build();
            
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("创建HTTP客户端失败：" + e.getMessage(), e);
        }
    }
}
