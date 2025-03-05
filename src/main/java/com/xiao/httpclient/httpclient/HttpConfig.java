package com.xiao.httpclient.httpclient;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
public class HttpConfig {
    @Bean
    public CloseableHttpClient closeableHttpClient() {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", trustHttpsCertificates())
                .build();
        // 创建一个ConnectionManager
        PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(registry);
        // 定制CloseableHttpClient对象
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(pool);
        return httpClientBuilder.build();
    }
    // 创建支持安全协议的连接工厂
    private ConnectionSocketFactory trustHttpsCertificates() {
        try {
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            // 判断是否信任url
            sslContextBuilder.loadTrustMaterial(null, (x509Certificates, s) -> true);
            SSLContext sslContext = sslContextBuilder.build();
            return new SSLConnectionSocketFactory(sslContext,
                    new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"},
                    null, NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
