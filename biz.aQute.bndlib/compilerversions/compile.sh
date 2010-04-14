javac -target 1.1 -source 1.2 -cp src src/sun_1_1/*.java
javac -target 1.2 -source 1.2 -cp src src/sun_1_2/*.java
javac -target 1.3 -source 1.3 -cp src src/sun_1_3/*.java
javac -target 1.4 -source 1.4 -cp src src/sun_1_4/*.java
javac -target 1.5 -source 1.5 -cp src src/sun_1_5/*.java
#javac -target 1.6 -source 1.6 -cp src src/sun_1_6/*.java
javac -target jsr14 -source 1.5 -cp src src/sun_jsr14/*.java
java -jar ../jar/ecj_3.2.2.jar -target 1.1 -source 1.3 -cp src src/eclipse_1_1/*.java
java -jar ../jar/ecj_3.2.2.jar -target 1.2 -source 1.3 -cp src src/eclipse_1_2/*.java
java -jar ../jar/ecj_3.2.2.jar -target 1.3 -source 1.3 -cp src src/eclipse_1_3/*.java
java -jar ../jar/ecj_3.2.2.jar -target 1.4 -source 1.4 -cp src src/eclipse_1_4/*.java
java -jar ../jar/ecj_3.2.2.jar -target 1.5 -source 1.5 -cp src src/eclipse_1_5/*.java
java -jar ../jar/ecj_3.2.2.jar -target 1.6 -source 1.6 -cp src src/eclipse_1_6/*.java
java -jar ../jar/ecj_3.2.2.jar -target jsr14 -source 1.5 -cp src src/eclipse_jsr14/*.java
