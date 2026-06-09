FROM amazoncorretto:21-alpine-jdk
COPY ewm-service/target/ewm-service-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]