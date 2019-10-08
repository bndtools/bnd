#
# Originally this file compiled all the compiler versions. However,
# this created problems when the compilers started to get more error
# checking and balked at the fact they did not get a boot classpath.
# so this is not incremental. The class files are actually stored in
# git now
#
# So for a new compiler, just create a new directory and compile it 
# by hand. 
#
# Then build the jar:
#
#  bnd buildx compilerversions.bnd
#
# Make sure to add the class files and the compilerversions.jar to git! 
#

# javac -target 1.1 -source 1.2 -cp src src/sun_1_1/*.java
# javac -target 1.2 -source 1.2 -cp src src/sun_1_2/*.java
# javac -target 1.3 -source 1.3 -cp src src/sun_1_3/*.java
# javac -target 1.4 -source 1.4 -cp src src/sun_1_4/*.java
# javac -target 1.5 -source 1.5 -cp src src/sun_1_5/*.java
# javac -target 1.6 -source 1.6 -cp src src/sun_1_6/*.java
# javac -target jsr14 -source 1.5 -cp src src/sun_jsr14/*.java
# javac -target 1.7 -source 1.7 -cp src src/sun_1_7/*.java
# javac -target 1.8 -source 1.8 -cp src src/sun_1_8/*.java
# javac -target 9 -source 9 -cp src src/jdk_9_0/*.java
# javac --release 10 -cp src src/jdk_10_0/*.java
# javac --release 11 -cp src src/jdk_11_0/*.java
# javac --release 12 -cp src src/jdk_12_0/*.java
# javac --release 13 -cp src src/jdk_13_0/*.java

# java -jar ../jar/ecj_3.2.2.jar -target 1.1 -source 1.3 -cp src src/eclipse_1_1/*.java
# java -jar ../jar/ecj_3.2.2.jar -target 1.2 -source 1.3 -cp src src/eclipse_1_2/*.java
# java -jar ../jar/ecj_3.2.2.jar -target 1.3 -source 1.3 -cp src src/eclipse_1_3/*.java
# java -jar ../jar/ecj_3.2.2.jar -target 1.4 -source 1.4 -cp src src/eclipse_1_4/*.java
# java -jar ../jar/ecj_3.2.2.jar -target 1.5 -source 1.5 -cp src src/eclipse_1_5/*.java
# java -jar ../jar/ecj_3.2.2.jar -target 1.6 -source 1.6 -cp src src/eclipse_1_6/*.java
# java -jar ../jar/ecj_3.2.2.jar -target jsr14 -source 1.5 -cp src src/eclipse_jsr14/*.java
# java -jar ../jar/ecj_4.2.2.jar -target 1.7 -source 1.7 -cp src src/eclipse_1_7/*.java
# java -jar ../jar/ecj-4.7.1.jar -target 1.8 -source 1.8 -cp src src/eclipse_1_8/*.java
# java -jar ../jar/ecj-4.7.1.50.jar -9 -cp src src/eclipse_9_0/*.java
# java -jar ../jar/ecj-4.7.3a.jar -10 -cp src src/eclipse_10_0/*.java
# java -jar ../jar/ecj-4.10.jar --release 11 -cp src src/eclipse_11_0/*.java
# java -jar ../jar/ecj-4.13.jar --release 12 -cp src src/eclipse_12_0/*.java
# java -jar ../jar/ecj-I20191007-0625.jar --release 13 -cp src src/eclipse_13_0/*.java


