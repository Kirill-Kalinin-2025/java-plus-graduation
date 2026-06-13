FROM amazoncorretto:21-alpine-jdk
COPY stats-service/analyzer/target/analyzer-*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]