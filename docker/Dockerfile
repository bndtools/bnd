FROM java:8-jdk-alpine
MAINTAINER Neil Bartlett <njbartlett@gmail.com>

ADD tmp/bnd.jar /

WORKDIR /data
ENTRYPOINT ["java", "-jar", "/bnd.jar"]
