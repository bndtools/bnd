This is an example Multi-Release-Jar bundle.

It compiles a class HttpClient in three flavors:
- default version (Java 8) that uses a library to perform some task not part of JDK 8
- version 9 (Java 9) that uses the now build in support added in JDK 9
- version 11 (Java 11) that uses the new Java HttpClient added in JDK 11

This emulates what one often find in external libs where OSGi headers has to be added as part of a maven build.