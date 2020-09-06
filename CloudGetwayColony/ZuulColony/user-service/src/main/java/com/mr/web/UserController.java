package com.mr.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("indexs")
public class UserController {

    @GetMapping
    public String list(){
        System.out.println("8082Hellow");
        return "8082Hellow";
    }
}
