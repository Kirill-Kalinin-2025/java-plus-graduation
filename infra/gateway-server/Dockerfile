FROM amazoncorretto:21-alpine-jdk
COPY target/gateway-server-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]