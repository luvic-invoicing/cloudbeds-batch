## fetch basic image
# FROM openjdk:8-jdk-alpine
FROM amazoncorretto:8-alpine

# local application port

# local application port
EXPOSE 8050

# application placed into /opt/app
RUN mkdir -p /opt/service
WORKDIR /opt/service

RUN mkdir xml

COPY target/cloudbeds-batch-cr-0.0.1-SNAPSHOT.jar .

RUN mv cloudbeds-batch-cr-0.0.1-SNAPSHOT.jar cloudbeds-batch-cr.jar
CMD java -jar cloudbeds-batch-cr.jar