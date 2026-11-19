# 교재와 다름 (교재: eclipse-temurin:24_36-jre-noble, layertools)
FROM eclipse-temurin:21-jre-noble AS builder
WORKDIR /extracted
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
COPY --from=builder /extracted/layers/dependencies/ ./
COPY --from=builder /extracted/layers/spring-boot-loader/ ./
COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
COPY --from=builder /extracted/layers/application/ ./

EXPOSE 8888

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]