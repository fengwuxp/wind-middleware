FROM wind-java:21-jdk
LABEL authors="wuxp"

# 构建参数
## 应用名
ARG SPRING_APPLICATION_NAME=""
## 暴露的端口
ARG EXPOSE_PORT=8080

# 拷贝 boot jar 到设置工作目录
COPY --chown=admin:admin ./bootstrap.jar /home/admin/run

# 环境变量
ENV SPRING_APPLICATION_NAME=${SPRING_APPLICATION_NAME}

# 端口暴露
EXPOSE ${EXPOSE_PORT}

# 启动命令
ENTRYPOINT ["sh","-c","/home/admin/run/startup.sh"]