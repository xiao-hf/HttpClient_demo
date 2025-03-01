package com.xiao.httpclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

@Slf4j
@RestController
public class TestController {
    @PostMapping("post")
    public String post(HttpServletRequest request) {
        log.info("================接收到post请求==============");
        log.info("url:{}", request.getRequestURL());
        log.info("headers:");
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            log.info("{}:{}", name, request.getHeader(name));
        }
        log.info("parameters:");
        // 或者直接使用 getParameterMap() 获取所有参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            StringBuilder res = new StringBuilder(paramName + ":");
            for (String value : paramValues) {
                res.append(value).append(", ");
            }
            log.info("{}", res);
        }
        // 打印请求体
        BufferedReader reader = null;
        try {
            // 获取请求体的输入流
            reader = request.getReader();
            String line;
            StringBuilder requestBody = new StringBuilder();

            // 逐行读取请求体
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }

            // 打印请求体
            log.info("Request Body: {}", requestBody);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "成功接收到post请求";
    }

    @GetMapping("get")
    public String get(HttpServletRequest request) {
        log.info("================接收到GET请求==============");
        log.info("url:{}", request.getRequestURL());

        // 打印请求头
        log.info("headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            log.info("{}:{}", name, request.getHeader(name));
        }

        // 打印请求参数
        log.info("parameters:");
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            StringBuilder res = new StringBuilder(paramName + ":");
            for (String value : paramValues) {
                res.append(value).append(", ");
            }
            log.info("{}", res);
        }

        return "成功接收到GET请求";
    }

    @DeleteMapping("delete")
    public String delete(HttpServletRequest request) {
        log.info("================接收到DELETE请求==============");
        log.info("url:{}", request.getRequestURL());

        // 打印请求头
        log.info("headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            log.info("{}:{}", name, request.getHeader(name));
        }

        // 打印请求参数
        log.info("parameters:");
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            StringBuilder res = new StringBuilder(paramName + ":");
            for (String value : paramValues) {
                res.append(value).append(", ");
            }
            log.info("{}", res);
        }

        // 打印请求体（如果有）
        BufferedReader reader = null;
        try {
            reader = request.getReader();
            String line;
            StringBuilder requestBody = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            log.info("Request Body: {}", requestBody.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "成功接收到DELETE请求";
    }

    @PutMapping("put")
    public String put(HttpServletRequest request) {
        log.info("================接收到PUT请求==============");
        log.info("url:{}", request.getRequestURL());

        // 打印请求头
        log.info("headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            log.info("{}:{}", name, request.getHeader(name));
        }

        // 打印请求参数
        log.info("parameters:");
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            StringBuilder res = new StringBuilder(paramName + ":");
            for (String value : paramValues) {
                res.append(value).append(", ");
            }
            log.info("{}", res);
        }

        // 打印请求体
        BufferedReader reader = null;
        try {
            reader = request.getReader();
            String line;
            StringBuilder requestBody = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            log.info("Request Body: {}", requestBody.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "成功接收到PUT请求";
    }

}
