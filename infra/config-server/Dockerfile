FROM amazoncorretto:21-alpine-jdk
COPY target/config-server-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]