# Sockslib

Found this library at: https://github.com/fengyouchao/sockslib

The project is compiled for 1.8 so had to recompile it to 1.7 to make it run in our bnd build. Had to remove mongo class and a number of test cases until it compiled.

For this reason, we store the binary here.

History:

  562  vi /Ws/bnd/sockslib/src/main/java/sockslib/server/manager/MongoDBBasedUserManager.java
  563  mvn clean install
  564  rm -rf src/main/java/sockslib/utils/mongo
  565  mvn clean install
  566  rm -rf src/main/java/sockslib/server/
  567  mvn clean install
  568  gitx
  569  mvn clean install
  570  rm -rf /Ws/bnd/sockslib/src/main/java/sockslib/server/manager/MongoDBBasedUserManager.java
  571  mvn clean install
  572  rm /Ws/bnd/sockslib/src/test/java/socklib/test/TestSessionListener.java
  573  rm /Ws/bnd/sockslib/src/test/java/sockslib/example/UseSessionListener.java
  574  rm /Ws/bnd/sockslib/src/test/java/sockslib/example/AddressLimit.java
  575  mvn clean install
  576  rm /Ws/bnd/sockslib/src/test/java/sockslib/example/TestSocksServerBuilder.java
  577  mvn clean install
  578  ls target/
  579  cp target/sockslib-1.0.0-SNAPSHOT.jar ../biz.aQute.bnd.maven/jar/sockslib-1.0.0.jar 
  580  more pom.xml 
