package com.xiao.httpclient;

import com.alibaba.fastjson.JSON;
import com.xiao.httpclient.entity.User;
import javafx.util.Pair;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

@SpringBootTest
class HttpClientApplicationTests {
    @Test
    void main() throws Exception {
//        doPostJson("http://localhost:8080/hc/testPost");
//        doPostForm("http://localhost:8080/hc/testForm");
//        uploadFile("http://localhost:8080/hc/files");
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        for (int i = 0; i < 100; i++) {
            String urlStr = "http://localhost:8080/hc/testForm";
            // 构造HttpGet对象
            HttpPost httpPost = new HttpPost(urlStr);
            // 给httpPost设置参数
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(Arrays.asList(
                    new BasicNameValuePair("username", "admin"),
                    new BasicNameValuePair("password", "admin")
            ), Consts.UTF_8);
            formEntity.setContentType(ContentType.APPLICATION_FORM_URLENCODED.toString());
            httpPost.setEntity(formEntity);

            CloseableHttpResponse resp = closeableHttpClient.execute(httpPost);
            HttpEntity res = resp.getEntity();
            System.out.println(i + " => " + EntityUtils.toString(res, StandardCharsets.UTF_8));
        }
//        doGetHttp("https://localhost:8080/hc/test");
    }

    @Test
    String doGetHttp(String urlStr, Pair<String, String>... args) throws Exception {
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", trustHttpsCertificates())
                .build();
        // 创建一个ConnectionManager
        PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager(registry);
        // 定制CloseableHttpClient对象
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(pool);
        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();

        int len = args.length;
        if (len > 0) {
            urlStr += "?";
            StringBuilder urlStrBuilder = new StringBuilder(urlStr);
            for (int i = 0; i < len - 1; i++) {
                // 做url的encode, 如果是浏览器, 自动encode. eg: 空格 => +
                urlStrBuilder.append(URLEncoder.encode(args[i].getKey(), StandardCharsets.UTF_8.name())).append("=")
                        .append(URLEncoder.encode(args[i].getValue(), StandardCharsets.UTF_8.name())).append("&");
            }
            urlStr = urlStrBuilder.toString();
            urlStr += URLEncoder.encode(args[len - 1].getKey(), StandardCharsets.UTF_8.name()) +
                    "=" + URLEncoder.encode(args[len - 1].getValue(), StandardCharsets.UTF_8.name());
        }
        // 构造HttpGet对象
        HttpGet httpGet = new HttpGet(urlStr);
        // 响应
        CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
        // 代表本次请求成功/失败的状态
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        // 获取响应结果
        // HttpClient不仅可以作为结果, 也可以作为请求的参数实体, 有很多实现
        HttpEntity entity = response.getEntity();
        Header contentType = entity.getContentType();
        System.out.println("ContentType=>" + contentType);
        String strRes = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        if (HttpStatus.SC_OK == statusCode) {
            System.out.println("请求成功!");
            // 获取响应头
            Header[] allHeaders = response.getAllHeaders();
            for (Header header : allHeaders)
                System.out.println(header.getName() + "=>" + header.getValue());
        } else {
            System.out.println("请求失败! 响应码: " + statusCode);
        }
        // 输入流关闭
        EntityUtils.consume(entity);
        return strRes;
    }
    // 创建支持安全协议的连接工厂
    private ConnectionSocketFactory trustHttpsCertificates() throws Exception {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        // 判断是否信任url
        sslContextBuilder.loadTrustMaterial(null, (x509Certificates, s) -> true);
        SSLContext sslContext = sslContextBuilder.build();
        return new SSLConnectionSocketFactory(sslContext,
                new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"},
                null, NoopHostnameVerifier.INSTANCE);
    }


    @Test
    public void uploadFile(String urlStr) throws Exception { // POST multipart/form-data类型上传文件
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(urlStr);
        // 构造上传文件的entity
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(Consts.UTF_8); // 设置编码
        builder.setContentType(ContentType.MULTIPART_FORM_DATA); // 设置Content-Type
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE); // 浏览器模式
        // 构造一个ContentBody的实现类对象
        FileBody fileBody = new FileBody(new File("D:\\bak\\妹子.jpg"));
        HttpEntity httpEntity = builder.addPart("fileName", fileBody)
                .addBinaryBody("fileName", new File("D:\\bak\\back.jpg"))
                .addPart("username", new StringBody("小明",
                        ContentType.create("text/plain", Consts.UTF_8))) // 解决中文乱码
                // .addTextBody("username", "小明") // addTextBody 中文乱码
                .addTextBody("password", "12345")
                .build();
        httpPost.setEntity(httpEntity);
        CloseableHttpResponse resp = closeableHttpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(resp.getEntity()));
    }

    @Test
    void doPostForm(String urlStr) throws Exception {
        // 可关闭的httpclient客户端, 相当于打开的一个浏览器
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        // 构造HttpGet对象
        HttpPost httpPost = new HttpPost(urlStr);
        // 给httpPost设置参数
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(Arrays.asList(
                new BasicNameValuePair("username", "admin"),
                new BasicNameValuePair("password", "admin")
        ), Consts.UTF_8);
        formEntity.setContentType(ContentType.APPLICATION_FORM_URLENCODED.toString());
        httpPost.setEntity(formEntity);

        CloseableHttpResponse resp = closeableHttpClient.execute(httpPost);
        HttpEntity res = resp.getEntity();
        System.out.println(EntityUtils.toString(res, StandardCharsets.UTF_8));
        EntityUtils.consume(res);
        closeableHttpClient.close();
    }

    // 发送带json参数的post请求
    @Test
    void doPostJson(String urlStr) throws Exception {
        // 可关闭的httpclient客户端, 相当于打开的一个浏览器
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        // 构造HttpGet对象
        HttpPost httpPost = new HttpPost(urlStr);
        httpPost.setHeader("Content-Type", "application/json");

        // 给httpPost设置参数
        User user = new User();
        user.setUsername("admin");
        user.setPassword("admin");
        StringEntity entity = new StringEntity(JSON.toJSONString(user));
        entity.setContentType(ContentType.APPLICATION_JSON.toString());
        httpPost.setEntity(entity);

        CloseableHttpResponse resp = closeableHttpClient.execute(httpPost);
        HttpEntity res = resp.getEntity();
        System.out.println(EntityUtils.toString(res, StandardCharsets.UTF_8));
        EntityUtils.consume(res);
    }

    @Test
    void doTimeout(String urlStr) throws Exception {
        // 可关闭的httpclient客户端, 相当于打开的一个浏览器
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        // 构造HttpGet对象
        HttpGet httpGet = new HttpGet(urlStr);
        // 防盗链, value: 要是发生防盗链的网站url
        httpGet.addHeader("Referer", urlStr);
        // 对每一个请求进行配置, 会覆盖全局的默认请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                // 连接超时 ms 完成tcp3次握手的时间上限
                .setConnectTimeout(5000)
                // 读取超时 ms 表示从请求的网址处获得响应数据的时间间隔
                .setSocketTimeout(3000)
                // 指的是从连接池里获取connection的超时时间
                .setConnectionRequestTimeout(3000)
                .build();
        httpGet.setConfig(requestConfig);
        // 响应
        CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        System.out.println(EntityUtils.toString(entity, StandardCharsets.UTF_8));
        EntityUtils.consume(entity);
    }

    @Test
    void doProxy(String urlStr) throws Exception {
        // 可关闭的httpclient客户端, 相当于打开的一个浏览器
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        // 构造HttpGet对象
        HttpGet httpGet = new HttpGet(urlStr);
        // 防盗链, value: 要是发生防盗链的网站url
        httpGet.addHeader("Referer", urlStr);

        // 创建一个代理
        String host = "159.226.227.117";
        int port = 80;
        HttpHost proxy = new HttpHost(host, port);

        // 对每一个请求进行配置, 会覆盖全局的默认请求配置
        RequestConfig requestConfig = RequestConfig.custom().setProxy(proxy).build();
        httpGet.setConfig(requestConfig);

        // 响应
        CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        System.out.println(EntityUtils.toString(entity, StandardCharsets.UTF_8));
        EntityUtils.consume(entity);
    }

    @Test
    void downLoad(String urlStr, String dir, String fileName) throws Exception {// (下载链接, 保存目录, 保存文件名)
        // 可关闭的httpclient客户端, 相当于打开的一个浏览器
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        // 构造HttpGet对象
        HttpGet httpGet = new HttpGet(urlStr);
        // 防盗链, value: 要是发生防盗链的网站url
        httpGet.addHeader("Referer", urlStr);
        // 解决httpClient被认为不是真人行为
        httpGet.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/5" +
                        "37.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");
        // 响应
        CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String contentType = entity.getContentType().getValue();
        String suffix = ".jpg";
        if (contentType.contains("bmp") || contentType.contains("bitmap")) {
            suffix = ".bmp";
        } else if (contentType.contains("png")) {
            suffix = ".png";
        } else if (contentType.contains(".gif")) {
            suffix = ".gif";
        }
        byte[] bytes = EntityUtils.toByteArray(entity);
        FileOutputStream fos = new FileOutputStream(dir + "\\" + fileName + suffix);
        fos.write(bytes);
        fos.close();
        EntityUtils.consume(entity);
    }

    @Test
    String doGet(String urlStr, Pair<String, String>... args) throws Exception {
        int len = args.length;
        if (len > 0) {
            urlStr += "?";
            StringBuilder urlStrBuilder = new StringBuilder(urlStr);
            for (int i = 0; i < len - 1; i++) {
                // 做url的encode, 如果是浏览器, 自动encode. eg: 空格 => +
                urlStrBuilder.append(URLEncoder.encode(args[i].getKey(), StandardCharsets.UTF_8.name())).append("=")
                        .append(URLEncoder.encode(args[i].getValue(), StandardCharsets.UTF_8.name())).append("&");
            }
            urlStr = urlStrBuilder.toString();
            urlStr += URLEncoder.encode(args[len - 1].getKey(), StandardCharsets.UTF_8.name()) +
                        "=" + URLEncoder.encode(args[len - 1].getValue(), StandardCharsets.UTF_8.name());
        }
        // 可关闭的httpclient客户端, 相当于打开的一个浏览器
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        // 构造HttpGet对象
        HttpGet httpGet = new HttpGet(urlStr);
        // 防盗链, value: 要是发生防盗链的网站url
        httpGet.addHeader("Referer", urlStr);
        // 解决httpClient被认为不是真人行为
        httpGet.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/5" +
                        "37.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");
        // 响应
        CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
        // 代表本次请求成功/失败的状态
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        // 获取响应结果
        // HttpClient不仅可以作为结果, 也可以作为请求的参数实体, 有很多实现
        HttpEntity entity = response.getEntity();
        Header contentType = entity.getContentType();
        System.out.println("ContentType=>" + contentType);
        String strRes = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        if (HttpStatus.SC_OK == statusCode) {
            System.out.println("请求成功!");
            // 获取响应头
            Header[] allHeaders = response.getAllHeaders();
            for (Header header : allHeaders)
                System.out.println(header.getName() + "=>" + header.getValue());
        } else {
            System.out.println("请求失败! 响应码: " + statusCode);
        }
        // 输入流关闭
        EntityUtils.consume(entity);
        return strRes;
    }

    @Test
    void javaApi() throws Exception {
        String urlStr = "https://www.baidu.com/";
        URL url = new URL(urlStr);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        // 设置请求类型
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("Accept-Charset", "utf-8");
        // 获取httpURLConnection的输入流
        InputStream is = httpURLConnection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null)
            System.out.println(line);
    }

}
