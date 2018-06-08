# 简单微服务的Demo（基于jar部署，可修改为war+docker方式）

分为配置服务（端口8888）、文件服务（端口8081）、登录服务（可选，端口8080）

依赖的扩展服务：mysql、redis、rabbitmq

## 配置说明

配置服务的文件为application.yml

当前的版本为本地测试使用。修改properites文件后，可部署在多台电脑上或者云服务上

## 配置的属性

login.next-root=登录成功后跳转的地址

login.root-path=从文件服务返回的路径

file.local-dir=本地根路径

file.server-root=服务器的根路径，例如/dl/，端口号8081，实际访问路径为http://localhost:8081/dl/

file.authorization=是否包含登录服务

file.watcher=是否开启watcher组件，用于跟踪一小段间隔检测本地文件的变化情况

