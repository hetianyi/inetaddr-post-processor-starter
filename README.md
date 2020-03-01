# inetaddr-post-processor-starter
spring cloud注册中心IP地址修正程序。


引入依赖：
```xml
<dependency>
    <groupId>com.github.hetianyi</groupId>
    <artifactId>inetaddr-post-processor-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

处理并影响spring cloud应用注册到注册中心的IP地址。 
使用需要设置环境变量（application.yml和bootstrap.yml无效）: 

networkFilterBy = ipAddress|interfaceName 

和 

preferredNetwork  

如果networkFilterBy=ipAddress，那么preferredNetwork是特定IP地址前缀，如192.168. 

如果networkFilterBy=interfaceName，那么preferredNetwork是网卡名称，如eth0.

设置环境变量：
```properties

networkFilterBy=interfaceName
preferredNetwork=eth0
#或者
# networkFilterBy=ipAddress
# preferredNetwork=192.168.0.
```