FROM amazoncorretto:21-alpine-jdk
COPY stats-service/collector/target/collector-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]