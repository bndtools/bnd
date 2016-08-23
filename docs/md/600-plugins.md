___
___
# Plugins
Plugins are objects that can extend the functionality of bnd. They are called from inside bnd when a certain action should take place. For example, bnd uses a repository and plugins provide the actual repository implementations. Or for example, the SpringComponent analyzes the Spring files and adds references found in that XML to the imports.

A plugin is defined as:

  PLUGIN ::= FQN ( ';' <directive|attribute> )*

The following directive is defined for all plugin:

||`path:` ||A path to the jar file that contains the plugin. The directory/jar at that location is placed on your classpath for that plugin.||

bnd current supports the following plugin types:

  /**
   * An optional interface for plugins. If a plugin implements 
   * this interface then it can receive the remaining attributes 
   * and directives given in its clause as
   * well as the reporter to use.
   */
  public interface Plugin {
    /**
     * Give the plugin the remaining properties.
     * When a plugin is declared, the clause can contain 
     * extra properties. All the properties and directives 
     * are given to the plugin to use.
     * @param map attributes and directives for this plugin's clause
     */
    void setProperties(Map<String,String> map);
    
    /**
     * Set the current reporter. This is called at init time. 
     * This plugin should report all errors and warnings 
     * to this reporter.
     * @param processor
     */
    void setReporter(Reporter processor);
 }

  public interface AnalyzerPlugin {
    /**
     * This plugin is called after analysis. The plugin 
     * is free to modify the jar and/or change the classpath 
     * information (see referred, contained).
     * This plugin is called after analysis of the JAR 
     * but before manifest generation.
     * 
     * @param analyzer
     * @return true if the classpace has been modified so that the bundle
     *         classpath must be reanalyzed
     * @throws Exception
     */
    boolean analyzeJar(Analyzer analyzer) throws Exception;
  }

  public interface SignerPlugin {
    /**
     * Sign the current jar. The alias is the given certificate 
     * keystore.
     * 
     * @param builder   The current builder that contains the 
                        jar to sign
     * @param alias     The keystore certificate alias
     * @throws Exception When anything goes wrong
     */
    void sign(Builder builder, String alias) throws Exception;
  }

  public interface RepositoryPlugin {
    /**
     * Return a URL to a matching version of the given bundle.
     * 
     * @param bsn
     *            Bundle-SymbolicName of the searched bundle
     * @param range
     *            Version range for this bundle,"latest" 
     *            if you only want the
     *            latest, or null when you want all.
     * @return    A list of URLs sorted on version, lowest version 
     *            is at index 0.
     *            null is returned when no files with the given 
     *            bsn ould be found.
     * @throws Exception
     *             when anything goes wrong
     */
    File[] get(String bsn, String range) throws Exception;
    
    /**
     * Answer if this repository can be used to store files.
     * 
     * @return true if writable
     */
    boolean canWrite();
    
    /**
     * Put a JAR file in the repository.
     * 
     * @param jar
     * @throws Exception
     */
    File  put(Jar jar) throws Exception;
    
    /**
     * Return a list of bsns that are present in the repository.
     * 
     * @param  regex if not null, match against the bsn and if 
     *         matches, return otherwise skip
     * @return A list of bsns that match the regex parameter 
     *         or all if regex is null
     */
    List<String> list(String regex);
    
    /**
     * Return a list of versions.
     */
    
    List<Version> versions(String bsn);
  }

  public interface MakePlugin {

    /**
     * This plugin is called when Include-Resource detects 
     * a reference to a resource that it can not find in the 
     * file system.
     * 
     * @param builder   The current builder
     * @param source    The source string (i.e. the place 
     *                  where bnd looked)
     * @param arguments Any arguments on the clause in 
     *                  Include-Resource
     * @return          A resource or null if no resource 
     *                  could be made
     * @throws Exception
     */
    Resource make(Builder builder, String source, 
          Map<String,String> arguments) throws Exception;
  }

[[#make]]
###Make
Make plugins kick in when the `Include-Resource` header tries to locate a resource but it cannot find that resource. The `-make` option defines a number of patterns that are mapped to a make instruction.

For example, if you have

  Include-Resource:       com.acme.Abc.ann

If no such resource is found, bnd will look in the -make instruction. This instruction associates a pattern with a plugin type. For example:

       -make:                  (*.jar); type=xyz; abc=3; def="$1"

The first name part of the clause is matched against the unfound resource. All plugins are called sequentially until one returns non-null. The arguments on the -make clause are given as parameters to the make plugin. Normally all Make Plugins should verify the type field.

bnd has a bnd and a copy Make Plugin.

###Buildin Plugins
Some plugins are provided by bnd itself.

||aQute.lib.spring.SpringComponent||AnalyzerPlugin||This component will analyze the Spring XML for Spring DM and will add any classes in these files to the referred set of classes.||
||aQute.bnd.make.MakeBnd||MakePlugin||If Include-Resource can't find a referred resource it will use the make plugins to see if any of them can make the requested resource. The MakeBnd has type=bnd. This plugin can recursively call bnd to create embedded jars that are not created sequentially||
||aQute.bnd.make.MakeCopy||MakePlugin||Copies resources from other places in the file system||
||aQute.lib.deployer.FileRepo||RepositoryPlugin||Provides a file based repository.||
||aQute.bnd.maven.MavenRepository||RepositoryPlugin||Provides an interface to a the default maven repository in the user's home directory. Can be used with aQute.bnd.maven.MavenGroup to support converting bundle symbolic names to group and artifact id.||
