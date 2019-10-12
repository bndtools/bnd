___
___
# Maven
The Maven plugin is described at [Felix maven plugin][http://felix.apache.org/site/apache-felix-maven-bundle-plugin-bnd.html]. Defaults for Maven are:

 Bundle-SymbolicName: <groupId>.<artifactId>
 Bundle-Name:         project.getName();
 Bundle-Version:      <version>
 Import-Package:      *
 Export-Package:      <groupId>.<artifactId>.* (unless Private-package is set)
 Bundle-Description:  project.getDescription()
 Bundle-License:      project.getLicenses())
 Bundle-Vendor:       project.getOrganization();
 Include-Resource:    src/main/resources

The repository with the latest plugin is:

  https://repo.maven.apache.org/maven2

  Group ID: org.apache.felix
  Artifact: maven-bundle-plugin

Other details are found at the Felix site, the plugin is independently maintained by Felix.

