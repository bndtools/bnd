This is an example bundle that exports some API.

Points of interest:

* The @Version annotation on package-info.java is used to generate the
Export-Package version.

* The interfaces in the API are annotated as ConsumerType and ProviderType.
These annotations effect the import version range of any bundle that contains
implementations of the interfaces.

* The -exportcontents bnd instruction is used rather than Export-Package. This
may be safer than Export-Package because it cannot pull packages from the
classpath that are not included in the project.

* The two exported packages have an interdependency: org.example.types is
USED-BY org.example.api because a type from org.example.types is visible on
the signature of a method in org.example.api. Note that the Export-Package
header in MANIFEST.MF contains a correct "uses" directive.
