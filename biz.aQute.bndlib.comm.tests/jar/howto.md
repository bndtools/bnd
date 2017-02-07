# Sockslib

Found this library at: https://github.com/fengyouchao/sockslib

We forked the project here and have added several commits that allow us to
consume it as a library for this project.

https://github.com/bndtools/sockslib

# How to build sockslib-1.0.0.jar

Clone above repo and execute:
```mvn clean package -DskipTests```

Copy project to this project
```cp target/sockslib-1.0.0-SNAPSHOTS.jar <bnd_repo>/biz.aQute.bndlib.comms.test/jar/sockslib-1.0.0.jar```