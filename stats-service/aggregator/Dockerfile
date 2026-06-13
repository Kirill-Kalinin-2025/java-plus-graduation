FROM amazoncorretto:21-alpine-jdk
COPY stats-service/aggregator/target/aggregator-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]