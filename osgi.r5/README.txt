This project is used to generate the OSGi R5 wrapper JAR, which selects
a small subset of packages from OSGi R5 core and enterprise APIs.

This is done so that we can build against packages added in R5 such as
`org.osgi.resource`, without accidentally creating dependencies on the
R5 version of core packages such as `org.osgi.framework 1.7`.