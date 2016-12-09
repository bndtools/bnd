FROM java:8-jre-alpine
MAINTAINER Neil Bartlett <njbartlett@gmail.com>

ADD tmp/bnd.jar /

WORKDIR /data
ENTRYPOINT ["java", "-jar", "/bnd.jar"]
