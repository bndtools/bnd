# bnd-baseline-maven-plugin

The `bnd-baseline-maven-plugin` is a bnd based plugin that verifies OSGi 
bundles correctly obey semantic versioning rules. If the bundle does not
correctly follow the rules then the build will fail.

## What does the `bnd-baseline-maven-plugin` do?

This plugin performs version metadata validation by comparing the bundle
that has just been built against a previously released version, known as
the *baseline*. Where differences exist the plugin scans the class 
bytecode to determine whether the changes are backward compatible or not, 
and suggests updated versions for the packages and bundle.

If the newly built project has not increased the version(s) of the
affected package(s) then the verification of the bundle will fail. In 
this way the `bnd-baseline-maven-plugin` can be used to guarantee that
semantic versioning policies are correctly followed.

## How do I use the `bnd-baseline-maven-plugin` in my project?

Normally you will add the `bnd-baseline-maven-plugin` directly to the 
build of your OSGi bundle.

    <plugin>
        <groupId>biz.aQute.bnd</groupId>
        <artifactId>bnd-baseline-maven-plugin</artifactId>
        <version>${bnd.version}</version>
        <configuration>...</configuration>
        <executions>
            <execution>
                <id>baseline</id>
                <goals>
                    <goal>baseline</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    
### Running the `bnd-baseline-maven-plugin`

The only goal of the `bnd-baseline-maven-plugin` is `baseline` which verifies the
OSGi bundle. By default the `bnd-baseline-maven-plugin` binds to the 
`verify` phase of your build.

The `bnd-baseline-maven-plugin` outputs any encountered versioning errors to the
build log. 

### What versions should I use?

The [Semantic Versioning Whitepaper](https://www.osgi.org/wp-content/uploads/Semantic-Versioning-20190110.pdf)
defines the rules for semantic versions. In summary for a version `X.Y.Z`:

1. A change which breaks backward compatibility for consumers necessitates a major version change 
(i.e. the new version is `X+1.0.0`).
2. A change which remains backward compatible for consumers but is breaking for providers necessitates a minor version change (i.e. the new version is `X.Y+1.0`).
3. A change which is compatible for all, consumer and provider alike, necessitates a micro version change
(i.e. the new version is `X.Y.Z+1`)

#### What's the difference between a *Provider* and a *Consumer*

In general a *Provider* is a person implementing an API and a *Consumer is a person using the API, and
there are many more consumers than providers. Note that this is not the same as saying "The person who implements the interface is the provider".

A good example is the Servlet API. Obviously Jetty and TomCat are providers of the Servlet API, while
WAR files are Consumers of the Servlet API. This is true even though WAR files contain Servlets, and
Servlets *implement* the `javax.servlet.Servlet` interface.

In the case of the Servlet API adding a method to the ServletContext would be a *minor* change because it
only requires an update to TomCat and Jetty. Adding a method to the Servlet interface, however, would 
break all WAR files and therefore be a *major* change.

The distinction between interfaces designed for providers to implement (like `javax.servlet.ServletContext`)
 and those designed for consumers (like `javax.servlet.Servlet`) can be made using the 
`org.osgi.annotation.versioning.ProviderType` and `org.osgi.annotation.versioningConsumerType` annotations

### Configuring the `bnd-baseline-maven-plugin`

The `bnd-baseline-maven-plugin` typically requires no configuration. By
default the plugin will search the available maven repositories for
artifacts with the same group id and artifact id as the current module,
and with a version less than the current module version. The highest
version artifact which matches these rules will be used as the baseline.

#### Changing the baseline search parameters

By default the `bnd-baseline-maven-plugin` will use information from the
current project to set its search parameters, however sometimes an artifact
will move or change name over time, or a particular version should be 
targeted. In this case the search parameters can be overridden. If a 
search parameter is omitted then it will take its default value:

    <configuration>
        <base>
            <groupId>${a.different.groupId}</groupId>
            <artifactId>${a.different.artifactId}</artifactId>
            <version>${single.version.or.version.range}</version>
        </base>
    </configuration>

N.B. The default version range is `(,${project.version})`, i.e. all versions
up to but not including the current version. Whilst the base version search
parameter is usually a version range a fixed version, e.g. `1.2.3`,
may also be used in the configuration.

#### Fail on missing baseline

By default the `bnd-baseline-maven-plugin` will fail the build if it cannot 
locate a baseline. Sometimes (for example if a project has never been 
released before) then there isn't a baseline to use. This failure can
be disabled as follows:

    <configuration>
        <failOnMissing>false</failOnMissing>
    </configuration>

#### Include Distribution Management 

By default the `bnd-baseline-maven-plugin` will include repositories
listed in the Distribution Management section of the POM when 
searching for the baseline. This is usually the right choice as the 
distribution repository does not normally change between releases.
This behaviour can be disabled as follows:

    <configuration>
        <includeDistributionManagement>false</includeDistributionManagement>
    </configuration>

#### Continue on Error

By default the `bnd-baseline-maven-plugin` will fail the build if a
verification error is encountered. Sometimes it is preferable to issue
warnings, rather than failing the build. In this case the plugin can
be configured as follows:

    <configuration>
        <continueOnError>false</continueOnError>
    </configuration>

#### Full Reporting

By default the `bnd-baseline-maven-plugin` will only output version
information in cases where the version has not been incremented 
sufficiently. If the user wishes to output full details of the baseline
calculation then this can be configured as follows:

    <configuration>
        <fullReport>true</fullReport>
    </configuration>

#### Diffpackages

The names of the exported packages in the bundle to baseline. The default is all the exported packages but this property can be used to [specify](https://bnd.bndtools.org/chapters/820-instructions.html#selector) which exported packages are baselined.

    <configuration>
        <diffpackages>
            <diffpackage>!*.internal.*</diffpackage>
            <diffpackage>*</diffpackage>
        </diffpackages>
    </configuration>

#### Diffignores

The manifest header names and resource paths to ignore when baseline comparing. This property can be used to [exclude](https://bnd.bndtools.org/chapters/820-instructions.html#selector) items from baseline comparison.

    <configuration>
        <diffignores>
            <diffignore>com/foo/xyz.properties</diffignore>
            <diffignore>Some-Manifest-Header</diffignore>
        </diffignores>
    </configuration>

## Configuration Properties

|Configuration Property | Description |
| ---                   | ---         |
|`base`                 | See [Changing the baseline search parameters](#changing-the-baseline-search-parameters). _Defaults to the highest version of the project's artifact that is less than the version of the project's artifact._|
|`failOnMissing`        | See [Fail on missing baseline](#fail-on-missing-baseline). _Defaults to `true`._ Override with property `bnd.baseline.fail.on.missing`.|
|`includeDistributionManagement`| See [Include Distribution Management](#include-distribution-management). _Defaults to `true`._ Override with property `bnd.baseline.include.distribution.management`.|
|`fullReport`           | See [Full Reporting](#full-reporting). _Defaults to `false`._ Override with property `bnd.baseline.full.report`.|
|`diffpackages`         | See [Diffpackages](#diffpackages). _Defaults to `*`._|
|`diffignores`          | See [Diffignores](#diffignores). _Optional._|
|`continueOnError`      | See [Continue on Error](#continue-on-error). _Defaults to `false`._ Override with property `bnd.baseline.continue.on.error`.|
|`skip`                 | Skip the baseline process altogether. _Defaults to `false`._ Override with property `bnd.baseline.skip`.|

