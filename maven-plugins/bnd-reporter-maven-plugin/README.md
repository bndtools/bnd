# bnd-reporter-maven-plugin

The `bnd-reporter-maven-plugin` is a bnd based plugin that generates and exports reports of projects.

## What does the `bnd-reporter-maven-plugin` do?

This plugin generate and export reports of maven projects. See [here](https://bnd.bndtools.org/chapters/395-generating-documentation.html) for a general introduction
of the feature.

## Export operation

The `export` goal exports custom reports.

### Example

The most simple configuration is to generate a file which will contain all the OSGI headers, metatypes declarations, declarative services,… of a bundle built by a project. Add this configuration to the project pom:

```
    <plugin>
        <groupId>biz.aQute.bnd</groupId>
        <artifactId>bnd-reporter-maven-plugin</artifactId>
        <version>${bnd.version}</version>
        <configuration>
            <reports>
                <report>
                    <outputFile>metadata.json</outputFile>
                </report>
            </reports>
        </configuration>
    </plugin>
```
an XML file can also be generated:

```
    <configuration>
        <reports>
            <report>
                <outputFile>metadata.xml</outputFile>
            </report>
        </reports>
    </configuration>
```
### Advanced usage

#### Targeting the data source

With the scope element you can define all the reports in a parent pom and target the source of the extracted data. In maven, the two possible values are `aggregator` and `project`.

For example, the following configuration in a parent pom will generate a report at the project root, which will aggregate data of all the module projects, and one report per module project:

```
    <configuration>
        <reports>
            <report>
                <outputFile>all-metadata.json</outputFile>
                <scope>aggregator</scope>
            </report>
            <report>
                <outputFile>metadata.json</outputFile>
                <scope>project</scope>
            </report>
        </reports>
    </configuration>
```
#### Transformation

The template element allows to specify a path or an URL to a template file. Two template types are accepted: XSLT and TWIG.

For example, a template can be used to generate a markdown file:

```
    <configuration>
        <reports>
            <report>
                <outputFile>readme.md</outputFile>
                <templateFile>/home/me/templates/readme.twig</templateFile>
            </report>
        </reports>
    </configuration>
```
If the extension file is missing, the templateType attribute can be used to indicate the template type:

```
    <configuration>
        <reports>
            <report>
                <outputFile>readme.md</outputFile>
                <templateFile>http://.../f57ge56a</templateFile>
                <templateType>xslt</templateType>
            </report>
        </reports>
    </configuration>
```
If the template file needs to be parametrized, the parameters attribute can be used to provide a list of parameters and their values:

```
    <configuration>
        <reports>
            <report>
                <outputFile>readme.md</outputFile>
                <templateFile>http://.../f57ge56a</templateFile>
                <templateType>xslt</templateType>
                <parameters>
                    <myParam1>myValue</myParam1>
                    <myParam2>myValue</myParam2>
                <parameters>
            </report>
        </reports>
    </configuration>
```
> Note: If a file with the same name as the exported report but with a template file extension is found in the same folder as the exported report, this file will be used to transform the report instead of the optionally defined template attribute. This allows to quickly customize a report without redefining an inherited configuration.

#### Internationalisation

The locale element can be used to extract the data for a specific locale. For example, if a bundle defines some metatype description in French:

```
    <configuration>
        <reports>
            <report>
                <outputFile>metadata.xml</outputFile>
                <locale>fr-FR</locale>
            </report>
        </reports>
    </configuration>
```

#### Customization of the Report Content

In some case, it may be necessary to control what data should be present in the report, for example, if you want to extract further data from a project with plugins. For this you can use the `reportConfigs` element to create named configuration of reports. Then, with the `configName` element you can reference the configuration name that will be used to extract and aggregate the data of the report.

See the documentation [here](https://bnd.bndtools.org/instructions/reportconfig.html) for more information.

##### Example

For example, the following configuration will generate the file `metadata.json` with the specified `myConfigName` configuration name. This configuration will not use any default extraction plugins (`clearDefaults`), it will import the `configuration.json` file of the bundle into the report and we also add an arbitrary variable: 

```
    <configuration>
        <reportConfigs>
            <myConfigName>
                <clearDefaults>true</clearDefaults>
                <reportPlugins>
                    <reportPlugin>
                        <pluginName>importJarFile</pluginName>
                        <properties>
                            <path>OSGI-INF/configuration.json</path>
                        </properties>
                    </reportPlugin>
                </reportPlugins>
                <variables>
                    <myVariable>myValue</myVariable>
                </variables>
            </myConfigName>
        </reportConfigs>
        <reports>
            <report>
                <outputFile>metadata.json</outputFile>
                <configName>myConfigName</configName>
            </report>
        </reports>
    </configuration>
```

The resulting `metadata.json` file could be:

```json
{
  "configuration":{
      "prop1":3,
      "prop2":"myValue"
      },
  "myVariable":"myValue"
}
```

### Executing the export operation

Since the export operation is not associated with any maven build phase, it must be invoked manually.

Here's an invocation example:

```
mvn bnd-reporter:export
```

### Configuration Properties

Main configuration:

| Configuration Property | Description                                                                        |
|------------------------|------------------------------------------------------------------------------------|
| `reports`              | Can contain report child elements.                                                 |
| `reportConfigs`        | Can contain arbitrary named `reportConfig` child elements.                         |
| `skip`                 | Skip the project. Defaults to `false`. Override with property `bnd.reporter.skip`. |


`report` element:

| Configuration Property | Description                                                                                         |
|------------------------|-----------------------------------------------------------------------------------------------------|
| `outputFile`           | Output file path of the report (mandatory).                                                         |
| `templateFile`         | Path or URL to a template file.                                                                     |
| `templateType`         | The template type (eg; xslt, twig, ...). By default, it is guess from the template file extension.  |
| `parameters`           | An arbitrary map of template parameters and their value.                                            |
| `locale`               | A locale for the report (language-COUNTRY-variant, eg; en-US).                                      |
| `configName`           | The name of the configuration to be used to generate the report.                                        |
| `scope`                | The scope of the report (aggregator or project).                                                    |

`reportConfig` element:

| Configuration Property | Description                                                                                                                                                                                       |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `clearDefaults`        | Indicates to not include default report plugins. Default is `false`.                                                                                                                              |
| `reportPlugins`        | Can contain `reportPlugin` child elements. Each `reportPlugin` element must define the `pluginName` property and can define an arbitrary map of plugin properties under the `properties` element. |
| `variables`            | An arbitrary map of variables and their value.                                                                                                                                                    |

## Readme operation

The `readme` goal exports a set of readme files.

### Executing the readme operation

Since the readme operation is not associated with any maven build phase, it must be invoked manually.

Here's an invocation example:

```
mvn bnd-reporter:readme
```

### Configuration Properties

| Configuration Property | Description                                                                        |
|------------------------|------------------------------------------------------------------------------------|
| `parameters`           | Can contain arbitrary parameters, See [here](https://bnd.bndtools.org/chapters/395-generating-documentation.html) for a complete list of parameters. |
| `skip`                 | Skip the project. Defaults to `false`. Override with property `bnd.reporter.skip`. |