ARG MAVEN_IMAGE=m.daocloud.io/docker.io/library/maven:3.9.11-eclipse-temurin-17
ARG JRE_IMAGE=m.daocloud.io/docker.io/library/eclipse-temurin:17-jre-jammy
FROM ${MAVEN_IMAGE} AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM ${JRE_IMAGE}
WORKDIR /app
RUN groupadd --system --gid 10001 rdp \
    && useradd --system --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin rdp \
    && mkdir -p /var/lib/rdp/archive \
    && chown -R 10001:10001 /var/lib/rdp
COPY --from=build /workspace/target/rdp-platform-*.jar /app/rdp-platform.jar
USER rdp
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/rdp-platform.jar"]
