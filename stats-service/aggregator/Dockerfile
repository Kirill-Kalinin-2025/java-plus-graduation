FROM amazoncorretto:21-alpine-jdk
COPY target/aggregator-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]