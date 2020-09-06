# Spring Cloud 网关集群初入搭建



网关集群原因 :

​	随着用户访问量的增加,导致请求增加 ,我们知道很多技术都是有并发限制的(mysql tomcat redis.....) 只是 并发量不同。 网关也不例外,当请求过多时候,一个网关无法满足对服务的调用这时候我们就可以用网关集群来解决网关服务的高可用问题。

具体实现

​	![img](access\webp.jpg)

将Zuul的客户端放到注册中心的客户端 ，来搭建Zuul集



## 项目搭建

### 1.创建maven项目 

![image-20200906095818168](access\image-20200906095818168.png)

#### 导入pom依赖

```
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <java.version>1.8</java.version>
    <spring-cloud.version>Hoxton.RC1</spring-cloud.version>
    <b2c.latest.version>1.0.0-SNAPSHOT</b2c.latest.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- springCloud -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

    </dependencies>
</dependencyManagement>

<build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
```



### 2.创建子工程（注册中心）

![image-20200906100659627](access\image-20200906100659627.png)

#### 导入pom依赖

```
  <packaging>jar</packaging>
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        <version>2.0.0.RELEASE</version>
    </dependency>
</dependencies>
```

#### 启动类

![image-20200906101359152](access\image-20200906101359152.png)



#### yml配置

![image-20200906103432325](access\image-20200906103432325.png)



### 3.创建网关 集群

![image-20200906103649352](access\image-20200906103649352.png)

#### 在父类工程中导入pom依赖

```
 
 <packaging>pom</packaging>

<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```



#### 创建网关子类工程

![image-20200906103914978](access\image-20200906103914978.png)

![image-20200906132624831](access\image-20200906132624831.png)



(子类工程中不需要导入依赖 因为他是在父类里创建的可以直接使用父类项目中的依赖文件)

#### 子类网关yml配置文件

```
server:
  port: 91/92.... #服务端口
spring:
  application:
    name: gateway #指定服务名

#注册入注册器
eureka:
  client:
    registry-fetch-interval-seconds: 5 # 获取服务列表的周期：5s
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
      instance:
        prefer-ip-address: true
        ip-address: 127.0.0.1

zuul:
  routes:   #路由规则
    user-service:
        path: /user/**
        serviceId: user-service
```

server:
  port: 92 #服务端口 因为是网关集群 所以我们这里 设置 第zuul-one 为 91  zuul-two 为 92

其余配置相同。

#### 子类过滤器

子类网关中我们需配置自定义过滤器(来方便我们观察网关的调用过程)

![image-20200906133408170](access\image-20200906133408170.png)

```
package com.mr.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class LoginFilter extends ZuulFilter {
    @Override
    public String filterType() {
        System.out.println("com.mr.filter running");
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        System.out.println("优先级判定");
        return FilterConstants.PRE_DECORATION_FILTER_ORDER-1;
    }

    @Override
    public boolean shouldFilter() {
        System.out.println("启动Filter/过滤器");
        return true;
    }

    @Value("${server.port}")
    private String serverPort;
    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String token = request.getParameter("access-token");
        System.out.println(token);
        if (token == null || token.isEmpty()){
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        }
        System.out.println("网关端口+:"+serverPort);
        return null;
    }
}
```

#### 注意：事项

1.String token = request.getParameter("access-token"); 这里"access-token" 为自定义token可以更改 

2.@Value("${server.port}")
    private String serverPort;

代表的是是获取yml中配置的端口号信息



#### 启动类

```
@SpringBootApplication
@EnableZuulProxy //开启zuul 网关
@EnableDiscoveryClient //注册器客户端
public class ZuulOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuulOneApplication.class);
    }

}
```



### 4.创建user-service

![image-20200906133616326](access\image-20200906133616326.png)

#### 导入pom依赖

```
<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Eureka客户端 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```



#### yml配置

```
server:
  port: 8082
spring:
  application:
    name: user-service # 应用名称

eureka:
  client:
    service-url: # EurekaServer地址
      defaultZone: http://127.0.0.1:10086/eureka
  instance:
    prefer-ip-address: true # 当调用getHostname获取实例的hostname时，返回ip而不是host名称
    ip-address: 127.0.0.1 # 指定自己的ip信息，不指定的话会自己寻找
```

#### 启动类

```
@SpringBootApplication
@EnableDiscoveryClient
public class
UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class);
    }
}
```

#### controller

```
@Controller
@RequestMapping("indexs")
public class UserController {

    @GetMapping
    public String list(){
        System.out.println("8082Hellow");
        return "8082Hellow";
    }
}
```



### 5.Nginx 配置 

为什么需要nginx 配置呢?

因为nginx能够对我们所配置的网关进行负载均衡，从而改善并发问题和单点问题， 这也就是我们进行网关集群的原因

（配置nginx最好是用新的nginx来实现----复制粘贴--因为我们另一个nginx需要在项目中进行使用）

![image-20200906134716389](access\image-20200906134716389.png)

将nginx中的nginx.conf文件下的http区域更改为下方代码

    http {
        include       mime.types;
        default_type  application/octet-stream;
    #log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
    #                  '$status $body_bytes_sent "$http_referer" '
    #                  '"$http_user_agent" "$http_x_forwarded_for"';
    
    #access_log  logs/access.log  main;
    
    sendfile        on;
    #tcp_nopush     on;
    
    #keepalive_timeout  0;
    keepalive_timeout  65;
    
    #gzip  on;
     #配置上游服务器网关端口集群
        upstream  backServer{
            server 127.0.0.1:91 weight=1;
            server 127.0.0.1:92 weight=1;
        }



    server {
        listen       80;
        server_name  www.bufa.com;
    
        #charset koi8-r;
    
        #access_log  logs/host.access.log  main;
    
        location / {
            ### 指定上游服务器负载均衡服务器
            proxy_pass http://backServer/;
            index  index.html index.htm;
        }
    
        #error_page  404              /404.html;
    
        # redirect server error pages to the static page /50x.html
        #
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    
        # proxy the PHP scripts to Apache listening on 127.0.0.1:80
        #
        #location ~ \.php$ {
        #    proxy_pass   http://127.0.0.1;
        #}
    
        # pass the PHP scripts to FastCGI server listening on 127.0.0.1:9000
        #
        #location ~ \.php$ {
        #    root           html;
        #    fastcgi_pass   127.0.0.1:9000;
        #    fastcgi_index  index.php;
        #    fastcgi_param  SCRIPT_FILENAME  /scripts$fastcgi_script_name;
        #    include        fastcgi_params;
        #}
    
        # deny access to .htaccess files, if Apache's document root
        # concurs with nginx's one
        #
        #location ~ /\.ht {
        #    deny  all;
        #}
    }
注意：www.bufa.com （上方server_name的配置）要在本地host中配置哦 可根据自己喜好更改

![image-20200906135243143](access\image-20200906135243143.png)



### 6.启动nginx 启动后端项目 

配置好后 启动项目 和nginx

先进入打开浏览器去eureka（注册中心）进行查看我们的服务状态 

![image-20200906135047614](C:\Users\liwenxu\Desktop\git\CloudGetwayColony\access\image-20200906135047614.png)



发现getway 和user-service 注册都ok

输入地址进行访问 2-3次（都行）

![image-20200906135520476](access\image-20200906135520476.png)

这是后你会发现页面是404 （慌得一批！） 但是这是正常的 

```
return "8082Hellow";
```

因为我们后端设置的返回结果是这个 到那时我们没有这个页面



![image-20200906135736523](access\image-20200906135736523.png)

我们打开userservice发现请求成功了 （因为system方法执行了）

但是我们这次的目的不是 执行服务中的方法 而是查看 zuul网关的集群配置是否实现

因为我们在 自定义过滤器中进行了网关调用时候输出 端口信息 所以这里我们要检查的是 网关服务

![image-20200906135949126](access\image-20200906135949126.png)

![image-20200906140003372](access\image-20200906140003372.png)

我们检查运行结果 发现 zuul调用成功



以上就是 spring cloud 网关 基本搭建及实现的全过程 ！！