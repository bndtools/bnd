/**
 * Wrap task type for Gradle.
 *
 * <p>
 * This task type can be used to wrap a jar into a bundle.
 *
 * <p>
 * Here is examples of using the Wrap task type:
 * <pre>
 * import aQute.bnd.gradle.Wrap
 * task wrap(type: Wrap) {
 *     classpath "to-wrap.jar"
 *     bundlename "to.wrap"
 *     bundleversion "1.0.0"
 *     output "to-wrap-1.0.0.jar"
 * }
 * </pre>
 *
 * <p>
 * Properties:
 * <ul>
 * <li>classpath - a comma seperated list of jars to be included in the bundle.</li>
 * <li>bundlename - the SymbolicName of the bundle.</li>
 * <li>bundleversion - the BundleVersion of the bundle.</li>
 * <li>output - the path of the output file.</li>
 * <li>exportpackage - bnd instruction to be used for generating the bundle Export-Package manifest header</li>
 * </ul>
 */

package aQute.bnd.gradle

import aQute.bnd.osgi.Builder
import aQute.bnd.osgi.Processor
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar

class Wrap extends Jar {

    private File output

    private File classpath

    private String bundlename

    private String bundleversion

    private String exportpackage = '*;version=${bundleversion}'

    /**
     * Gets classpath files.
     */
    @InputFile
    File getClasspath() {
        return classpath
    }

    /**
     * Sets classpath files.
     * @param classpath
     */
    void setClasspath(String classpath) {
        this.classpath = project.file(classpath)
    }

    /**
     * Gets the manifest header Bundle-SymbolicName property.
     */
    @Input
    String getBundlename() {
        return bundlename
    }

    /**
     * Sets the manifest header Bundle-SymbolicName property.
     * @param bundlename
     */
    void setBundlename(String bundlename) {
        this.bundlename = bundlename
    }

    /**
     * Gets the manifest header Bundle-Version property.
     */
    @Input
    String getBundleversion() {
        return bundleversion
    }

    /**
     * Sets the manifest header Bundle-Version property.
     * @param bundleversion
     */
    void setBundleversion(String bundleversion) {
        this.bundleversion = bundleversion
    }

    /**
     * Gets the manifest header Export-Package Property
     */
    @Input
    @Optional
    String getExportpackage() {
        return exportpackage
    }

    void setExportpackage(String exportpackage) {
        this.exportpackage = exportpackage
    }

    /**
     * Returns the output file.
     */
    @OutputFile
    public File getOutput() {
        return output
    }

    /**
     * Set the output file.
     */
    public void setOutput(String file) {
        output = project.file(file)
    }

    /**
     * Wraps a bundle.
     */
    @TaskAction
    void wrap() {
        Properties props = toProperties()

        Processor processor = new Processor(props)

        Builder builder = new Builder(processor)

        builder.addClasspath(classpath)

        aQute.bnd.osgi.Jar jar = builder.build()

        File outputFile = builder.getOutputFile(output.path)

        builder.save(outputFile, false)
        builder.close()
    }

    /**
     * Gets properties to be used for wrapping.
     * @return
     */
    private Properties toProperties() {
        new Properties([
                "Bundle-SymbolicName": bundlename,
                "Bundle-Version": bundleversion,
                "Export-Package": exportpackage
        ])
    }
}
