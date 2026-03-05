# DOCKERFILE TO BUILD THE DEMO
# Builds the V25 addon and tables extension from local sources, then builds the demo.

FROM ghcr.io/jqlang/jq:latest AS jq-stage

FROM eclipse-temurin:21-jdk AS build
COPY --from=jq-stage /jq /usr/bin/jq
RUN jq --version

WORKDIR /build

# Copy the full multi-module project
COPY pom.xml ./
COPY enhanced-rich-text-editor/ enhanced-rich-text-editor/
COPY enhanced-rich-text-editor-tables/ enhanced-rich-text-editor-tables/
COPY enhanced-rich-text-editor-demo/ enhanced-rich-text-editor-demo/

# If you have a Vaadin Pro key, pass it as a secret with id "proKey":
#
#   $ docker build --secret id=proKey,src=$HOME/.vaadin/proKey .
#
# If you have a Vaadin Offline key, pass it as a secret with id "offlineKey":
#
#   $ docker build --secret id=offlineKey,src=$HOME/.vaadin/offlineKey .

# 1) Install addon + tables extension into local Maven repo
# 2) Build the demo with production profile
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.vaadin \
    --mount=type=secret,id=proKey \
    --mount=type=secret,id=offlineKey \
    sh -c 'PRO_KEY=$(jq -r ".proKey // empty" /run/secrets/proKey 2>/dev/null || echo "") && \
    OFFLINE_KEY=$(cat /run/secrets/offlineKey 2>/dev/null || echo "") && \
    cd enhanced-rich-text-editor-demo && \
    ./mvnw -f /build/pom.xml clean install -pl enhanced-rich-text-editor,enhanced-rich-text-editor-tables -DskipTests && \
    ./mvnw clean package -Pproduction -DskipTests -Dvaadin.proKey=${PRO_KEY} -Dvaadin.offlineKey=${OFFLINE_KEY}'

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /build/enhanced-rich-text-editor-demo/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
