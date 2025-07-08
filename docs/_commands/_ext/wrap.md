
The wrap command takes an existing JAR file and guesses the manifest headers that will make this JAR useful for an OSGi Service Platform. If the output file is not overridden, the name of the input file is used with a .bar extension. The default bnd file for the header calculation is:

    Export-Package: * 
    Import-Package: <packages inside the target jar>

If the target bundle has a manifest, the headers are merged with the properties.

The defaults can be overridden with a specific properties file.


## Examples
`bnd wrap -classpath osgi.jar *.jar`
