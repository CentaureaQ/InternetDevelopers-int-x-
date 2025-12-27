FROM eclipse-temurin:17-jre

LABEL maintainer="health-agent-platform"

# 创建应用目录
RUN mkdir -p /app
WORKDIR /app

# 复制 JAR 包
COPY target/*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Java 运行参数
ENV JAVA_OPTS="-Xms512m -Xmx1g -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_OPTS=""

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $SPRING_OPTS"]