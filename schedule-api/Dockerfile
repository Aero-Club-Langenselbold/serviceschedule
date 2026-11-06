FROM eclipse-temurin:25

RUN groupadd scheduler -g 1002 && useradd -ms /bin/bash scheduler -u 1001 -g 1002

USER scheduler

COPY --chown=1001:1002 target/schedule-api.jar /usr/app/

WORKDIR /usr/app

RUN touch schedule-api.jar

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "schedule-api.jar"]
