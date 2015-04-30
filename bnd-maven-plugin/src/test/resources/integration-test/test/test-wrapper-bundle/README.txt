This is an example bundle that has no source but wraps some jar.

Points of interest:

* The target/classes folder will not be created by other maven plugins since
there is no source code in this project. bnd-maven-plugin must handle that it
does not exist and create it when necessary.
