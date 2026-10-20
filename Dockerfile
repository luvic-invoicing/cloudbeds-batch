FROM amazoncorretto:8-alpine

EXPOSE 8051

WORKDIR /opt/service

RUN mkdir -p xml

COPY target/cloudbeds-batch-cr.jar .
ENTRYPOINT ["java", "-jar", "cloudbeds-batch-cr.jar"]