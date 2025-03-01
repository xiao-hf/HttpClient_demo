package com.xiao.httpclient.utils;

import com.alibaba.fastjson.JSON;
import com.xiao.httpclient.entity.User;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpUtil {
    public static void main(String[] args) {
        // 创建一个Map来存储请求头
        Map<String, String> headers = new HashMap<>();
        // 添加一些常见的HTTP请求头
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer abc123xyz");
        headers.put("Host", "api.example.com");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Connection", "keep-alive");
        headers.put("Cache-Control", "no-cache");

        // 创建一个Map来存储请求参数（每个参数名对应一个字符串值）
        Map<String, String> params = new HashMap<>();
        // 添加一些常见的HTTP请求参数
        params.put("username", "john_doe");
        params.put("password", "s3cr3t");
        params.put("age", "30");
        params.put("email", "john.doe@example.com");
        params.put("subscribe", "true");
        params.put("country", "USA");

        User user = new User();
        user.setUsername("傻逼");
        user.setPassword("dsfhskf");
        String jsonBody = JSON.toJSONString(user);

        System.out.println(doGet("http://localhost:8080/get", headers, params));
        System.out.println(doPost("http://localhost:8080/post", headers, jsonBody));
        System.out.println(doPut("http://localhost:8080/put", headers, jsonBody));
        System.out.println(doDelete("http://localhost:8080/delete", headers));
    }

    /**
     * 发送GET请求
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param params      请求参数(可为null)
     * @return            响应内容字符串
     */
    public static String doGet(String url, Map<String, String> headers, Map<String, String> params) {
        try {
            // 创建HttpClient实例
            CloseableHttpClient httpClient = HttpClients.createDefault();

            // 构建带参数的URI
            URIBuilder builder = new URIBuilder(url);
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    builder.addParameter(param.getKey(), param.getValue());
                }
            }
            URI uri = builder.build();

            // 创建HttpGet请求
            HttpGet httpGet = new HttpGet(uri);

            // 添加请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpGet.addHeader(header.getKey(), header.getValue());
                }
            }

            // 执行请求并获取响应
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            // 转换响应内容为字符串并返回
            String result = null;
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }

            // 关闭资源
            httpClient.close();

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 发送POST请求，请求体为JSON字符串，内部处理异常不向外抛出
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param jsonBody    JSON格式的请求体
     * @return            响应内容字符串，出错时返回null
     */
    public static String doPost(String url, Map<String, String> headers, String jsonBody) {
        CloseableHttpClient httpClient = null;
        String result = null;

        try {
            // 创建HttpClient实例
            httpClient = HttpClients.createDefault();

            // 创建HttpPost请求
            HttpPost httpPost = new HttpPost(url);

            // 设置JSON请求体
            if (jsonBody != null && !jsonBody.isEmpty()) {
                StringEntity entity = new StringEntity(jsonBody, "UTF-8");
                entity.setContentType("application/json");
                httpPost.setEntity(entity);
            }

            // 添加请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpPost.addHeader(header.getKey(), header.getValue());
                }
            }

            // 如果没有明确设置Content-Type头，则默认设置为application/json
            if (headers == null || !headers.containsKey("Content-Type")) {
                httpPost.addHeader("Content-Type", "application/json");
            }

            // 执行请求并获取响应
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            // 转换响应内容为字符串
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {
            // 记录异常但不抛出
            e.printStackTrace();
            // 或者使用日志框架记录
            // logger.error("POST请求发生异常: " + e.getMessage(), e);
        } finally {
            // 确保关闭资源
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    // 或者使用日志框架记录
                    // logger.error("关闭HttpClient时发生异常: " + e.getMessage(), e);
                }
            }
        }

        return result;
    }

    /**
     * 发送PUT请求，请求体为JSON字符串，内部处理异常不向外抛出
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param jsonBody    JSON格式的请求体
     * @return            响应内容字符串，出错时返回null
     */
    public static String doPut(String url, Map<String, String> headers, String jsonBody) {
        CloseableHttpClient httpClient = null;
        String result = null;
        try {
            // 创建HttpClient实例
            httpClient = HttpClients.createDefault();
            // 创建HttpPut请求
            HttpPut httpPut = new HttpPut(url);
            // 设置JSON请求体
            if (jsonBody != null && !jsonBody.isEmpty()) {
                StringEntity entity = new StringEntity(jsonBody, "UTF-8");
                entity.setContentType("application/json");
                httpPut.setEntity(entity);
            }
            // 添加请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpPut.addHeader(header.getKey(), header.getValue());
                }
            }
            // 如果没有明确设置Content-Type头，则默认设置为application/json
            if (headers == null || !headers.containsKey("Content-Type")) {
                httpPut.addHeader("Content-Type", "application/json");
            }
            // 执行请求并获取响应
            HttpResponse response = httpClient.execute(httpPut);
            HttpEntity entity = response.getEntity();
            // 转换响应内容为字符串
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {
            // 记录异常但不抛出
            e.printStackTrace();
            // 或者使用日志框架记录
            // logger.error("PUT请求发生异常: " + e.getMessage(), e);
        } finally {
            // 确保关闭资源
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    // 或者使用日志框架记录
                    // logger.error("关闭HttpClient时发生异常: " + e.getMessage(), e);
                }
            }
        }

        return result;
    }

    /**
     * 发送DELETE请求，内部处理异常不向外抛出
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @return            响应内容字符串，出错时返回null
     */
    public static String doDelete(String url, Map<String, String> headers) {
        CloseableHttpClient httpClient = null;
        String result = null;
        try {
            // 创建HttpClient实例
            httpClient = HttpClients.createDefault();
            // 创建HttpDelete请求
            HttpDelete httpDelete = new HttpDelete(url);
            // 添加请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpDelete.addHeader(header.getKey(), header.getValue());
                }
            }
            // 执行请求并获取响应
            HttpResponse response = httpClient.execute(httpDelete);
            HttpEntity entity = response.getEntity();
            // 转换响应内容为字符串
            if (entity != null) {
                result = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {
            // 记录异常但不抛出
            e.printStackTrace();
            // 或者使用日志框架记录
            // logger.error("DELETE请求发生异常: " + e.getMessage(), e);
        } finally {
            // 确保关闭资源
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    // 或者使用日志框架记录
                    // logger.error("关闭HttpClient时发生异常: " + e.getMessage(), e);
                }
            }
        }
        return result;
    }
}
