FROM amazoncorretto:21-alpine-jdk
COPY target/discovery-server-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]