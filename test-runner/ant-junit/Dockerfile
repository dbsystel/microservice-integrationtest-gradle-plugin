FROM openjdk:8-jdk-alpine

ARG http_proxy=${proxy}
RUN apk add --update --no-cache bash
RUN apk add --no-cache apache-ant
RUN mkdir /classpath

VOLUME ["/reports"]
COPY ["build.xml", "/"]
ENTRYPOINT ["ant"]
