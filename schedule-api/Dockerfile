FROM openjdk:21-jdk

RUN groupadd scheduler -g 1002 && useradd -ms /bin/bash scheduler -u 1000 -g 1002

USER scheduler

COPY --chown=1000:1002 target/schedule-api.jar /usr/app/

WORKDIR /usr/app

RUN touch schedule-api.jar

ENTRYPOINT ["java", "-jar", "schedule-api.jar"]