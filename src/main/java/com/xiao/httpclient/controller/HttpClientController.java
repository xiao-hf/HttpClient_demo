package com.xiao.httpclient.controller;

import com.xiao.httpclient.entity.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.sound.midi.Soundbank;
import java.util.Arrays;
import java.util.Enumeration;

@RestController
@RequestMapping("hc")
public class HttpClientController {

    @PostMapping("file")
    public String files(MultipartFile file, HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headName = headerNames.nextElement();
            System.out.println(headName + "=>" + request.getHeader(headName));
        }
        return file.getName();
    }

    @PostMapping("files")
    public String[] files(@RequestParam("fileName") MultipartFile[] files, User user, HttpServletRequest request) {
        String[] res = new String[files.length];
        for (int i = 0; i < files.length; i++)
            res[i] = files[i].getOriginalFilename();
        System.out.println(Arrays.toString(res));
        System.out.println(user);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headName = headerNames.nextElement();
            System.out.println(headName + "=>" + request.getHeader(headName));
        }
        return res;
    }

    @PostMapping("testForm")
    public String testForm(HttpServletRequest request, String username, String password, User user) {
        System.out.println(username);
        System.out.println(password);
        System.out.println(request.getParameter("username"));
        System.out.println(request.getParameter("password"));
        System.out.println(user);
        System.out.println(username);
        System.out.println(password);
        return "username=>" + username + " " + "password=>" + password;
    }

    @PostMapping("testPost")
    public User testPost(@RequestBody User user) {
        System.out.println(user);
        return user;
    }

    @GetMapping("test")
    public String helloWorld() throws InterruptedException {
        return "hello world!";
    }

    @GetMapping("params")
    public String getParams(@RequestParam Integer id, @RequestParam String name) {
        return "id=" + id + " " + "name=" + name;
    }
}
