# Base image
FROM openjdk:17-jdk-alpine
# 애플리케이션 파일을 복사
COPY ./build/libs/*.jar app.jar
# 애플리케이션 실행 명령어
CMD ["java", "-jar", "app.jar"]
