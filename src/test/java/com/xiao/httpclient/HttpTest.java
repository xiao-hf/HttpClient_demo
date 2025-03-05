package com.xiao.httpclient;

import com.xiao.httpclient.httpclient.HttpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class HttpTest {
    @Resource
    HttpUtil httpUtil;
    @Test
    public void test() {
        String url = "https://www.baidu.com";
//        System.out.println(httpUtil.doGet(url, null, null));

//        System.out.println(httpUtil.doPost(url, null, null));
        System.out.println(httpUtil.doPut(url, null, null));
        System.out.println("=================================================");
        System.out.println(httpUtil.doDelete(url, null));
    }
}
