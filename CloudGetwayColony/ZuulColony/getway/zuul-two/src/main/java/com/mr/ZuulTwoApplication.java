package com.mr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy //开启zuul 网关
@EnableDiscoveryClient //注册器客户端
public class ZuulTwoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZuulTwoApplication.class);
    }

}
