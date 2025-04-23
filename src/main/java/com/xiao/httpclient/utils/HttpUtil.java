package com.xiao.httpclient.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * HTTP请求工具类
 */
@Slf4j
@Component
public class HttpUtil {
    @Resource
    CloseableHttpClient httpClient;

    /**
     * 发送GET请求
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param params      请求参数(可为null)
     * @return            响应内容字符串
     */
    public String doGet(String url, Map<String, String> headers, Map<String, String> params) {
        try {
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

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 发送POST请求
     *
     * @param url 请求URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @return 响应字符串
     */
    public String doPost(String url, Map<String, String> headers, String requestBody) {
        try {
            HttpPost httpPost = new HttpPost(url);

            // 设置请求头
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(httpPost::setHeader);
            }

            // 设置请求体
            if (requestBody != null) {
                StringEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
                httpPost.setEntity(entity);
            }

            // 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 获取响应体
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                // 检查响应状态
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return responseBody;
                } else {
                    log.error("HTTP请求失败, URL: {}, 状态码: {}, 响应: {}", url, statusCode, responseBody);
                    throw new RuntimeException("HTTP请求失败: " + statusCode);
                }
            }
        } catch (Exception e) {
            log.error("HTTP请求异常, URL: {}", url, e);
            throw new RuntimeException("HTTP请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * 发送POST请求，支持流式处理响应
     *
     * @param url 请求URL
     * @param headers 请求头
     * @param requestBody 请求体
     * @param streamResponseHandler 流式响应处理器
     */
    public void doPostStream(String url, Map<String, String> headers, String requestBody, StreamResponseHandler streamResponseHandler) {
        try {
            HttpPost httpPost = new HttpPost(url);

            // 设置请求头
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(httpPost::setHeader);
            }

            // 设置请求体
            if (requestBody != null) {
                StringEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
                httpPost.setEntity(entity);
            }

            // 执行请求，但不关闭响应，而是将其传递给处理器
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 检查响应状态
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    // 获取响应实体
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        // 将响应流传递给处理器
                        streamResponseHandler.handleStream(entity.getContent());
                    }
                } else {
                    // 处理错误状态码
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    log.error("HTTP流式请求失败, URL: {}, 状态码: {}, 响应: {}", url, statusCode, responseBody);
                    throw new RuntimeException("HTTP流式请求失败: " + statusCode);
                }
            }
            // 确保响应被关闭
        } catch (Exception e) {
            log.error("HTTP流式请求异常, URL: {}", url, e);
            throw new RuntimeException("HTTP流式请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * 发送PUT请求，请求体为JSON字符串，内部处理异常不向外抛出
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param jsonBody    JSON格式的请求体
     * @return            响应内容字符串，出错时返回null
     */
    public String doPut(String url, Map<String, String> headers, String jsonBody) {
        String result = null;
        try {
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
        }

        return result;
    }

    /**
     * 发送DELETE请求，不带请求体
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @return            响应内容字符串，出错时返回null
     */
    public String doDelete(String url, Map<String, String> headers) {
        return doDelete(url, headers, null);
    }

    /**
     * 发送DELETE请求，支持请求体
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param jsonBody    JSON格式的请求体(可为null)
     * @return            响应内容字符串，出错时返回null
     */
    public String doDelete(String url, Map<String, String> headers, String jsonBody) {
        String result = null;
        try {
            // 创建HttpDelete请求，Apache HttpClient标准实现不支持带请求体的DELETE
            // 使用自定义的HttpEntityEnclosingDeleteRequest或直接使用HttpDeleteWithBody类
            HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);

            // 设置JSON请求体
            if (jsonBody != null && !jsonBody.isEmpty()) {
                StringEntity entity = new StringEntity(jsonBody, "UTF-8");
                entity.setContentType("application/json");
                httpDelete.setEntity(entity);
            }

            // 添加请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpDelete.addHeader(header.getKey(), header.getValue());
                }
            }

            // 如果没有明确设置Content-Type头且有请求体，则默认设置为application/json
            if (jsonBody != null && !jsonBody.isEmpty() && (headers == null || !headers.containsKey("Content-Type"))) {
                httpDelete.addHeader("Content-Type", "application/json");
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
            log.error("DELETE请求发生异常: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 支持请求体的HttpDelete请求
     */
    private static class HttpDeleteWithBody extends HttpPost {
        public HttpDeleteWithBody(final String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }

    /**
     * 流式响应处理器接口
     */
    public interface StreamResponseHandler {
        /**
         * 处理输入流
         * @param inputStream 响应输入流
         * @throws Exception 可能发生的异常
         */
        void handleStream(java.io.InputStream inputStream) throws Exception;
    }

    /**
     * 发送包含文件的multipart/form-data格式POST请求
     *
     * @param url       请求URL
     * @param headers   请求头(可为null)
     * @param params    请求参数(可为null)，会作为普通表单字段
     * @param files     文件参数，key为表单字段名，value为文件对象
     * @return          响应内容字符串
     */
    public String doPostWithFiles(String url, Map<String, String> headers, Map<String, String> params, Map<String, File> files) {
        try {
            HttpPost httpPost = new HttpPost(url);

            // 设置请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpPost.addHeader(header.getKey(), header.getValue());
                }
            }

            // 创建MultipartEntityBuilder，用于构建multipart请求体
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            // 添加文本参数
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    // 使用UTF-8编码添加字符串字段
                    StringBody stringBody = new StringBody(param.getValue(), ContentType.create("text/plain", StandardCharsets.UTF_8));
                    builder.addPart(param.getKey(), stringBody);
                }
            }

            // 添加文件参数
            if (files != null && !files.isEmpty()) {
                for (Map.Entry<String, File> fileEntry : files.entrySet()) {
                    // 使用FileBody添加文件，自动检测文件类型
                    FileBody fileBody = new FileBody(fileEntry.getValue());
                    builder.addPart(fileEntry.getKey(), fileBody);
                }
            }

            // 设置请求实体
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);

            // 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 获取响应体
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                // 检查响应状态
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return responseBody;
                } else {
                    log.error("HTTP文件上传请求失败, URL: {}, 状态码: {}, 响应: {}", url, statusCode, responseBody);
                    throw new RuntimeException("HTTP文件上传请求失败: " + statusCode);
                }
            }
        } catch (Exception e) {
            log.error("HTTP文件上传请求异常, URL: {}", url, e);
            throw new RuntimeException("HTTP文件上传请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * 发送包含单个文件的multipart/form-data格式POST请求
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param params      请求参数(可为null)，会作为普通表单字段
     * @param fileField   文件字段名
     * @param file        要上传的文件
     * @return            响应内容字符串
     */
    public String doPostWithFile(String url, Map<String, String> headers, Map<String, String> params, String fileField, File file) {
        Map<String, File> fileMap = new java.util.HashMap<>();
        fileMap.put(fileField, file);
        return doPostWithFiles(url, headers, params, fileMap);
    }

    /**
     * 发送包含文件二进制数据的multipart/form-data格式POST请求
     * 适用于从内存中上传文件
     *
     * @param url         请求URL
     * @param headers     请求头(可为null)
     * @param params      请求参数(可为null)，会作为普通表单字段
     * @param fileField   文件字段名
     * @param fileName    文件名
     * @param fileData    文件二进制数据
     * @param contentType 文件内容类型，如image/jpeg，为null时自动检测
     * @return            响应内容字符串
     */
    public String doPostWithFileBytes(String url, Map<String, String> headers, Map<String, String> params, 
                                     String fileField, String fileName, byte[] fileData, ContentType contentType) {
        try {
            HttpPost httpPost = new HttpPost(url);

            // 设置请求头
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpPost.addHeader(header.getKey(), header.getValue());
                }
            }

            // 创建MultipartEntityBuilder
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            // 添加文本参数
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    StringBody stringBody = new StringBody(param.getValue(), ContentType.create("text/plain", StandardCharsets.UTF_8));
                    builder.addPart(param.getKey(), stringBody);
                }
            }

            // 添加二进制文件
            if (contentType == null) {
                contentType = ContentType.APPLICATION_OCTET_STREAM;
            }
            builder.addBinaryBody(fileField, fileData, contentType, fileName);

            // 设置请求实体
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);

            // 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 获取响应体
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                // 检查响应状态
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return responseBody;
                } else {
                    log.error("HTTP文件字节上传请求失败, URL: {}, 状态码: {}, 响应: {}", url, statusCode, responseBody);
                    throw new RuntimeException("HTTP文件字节上传请求失败: " + statusCode);
                }
            }
        } catch (Exception e) {
            log.error("HTTP文件字节上传请求异常, URL: {}", url, e);
            throw new RuntimeException("HTTP文件字节上传请求异常: " + e.getMessage(), e);
        }
    }
}