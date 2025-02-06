package com.xiao.httpclient.controller;

import com.xiao.httpclient.entity.Dto;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("form")
public class FormController {

    @PostMapping("test")
    public Dto test(@RequestBody Dto dto) {
        return dto;
    }

}
