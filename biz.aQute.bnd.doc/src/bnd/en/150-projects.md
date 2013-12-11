# Projects

## Project Properties

There are a number of built in properties that are set by bnd: 

<table width="100%"><tbody><tr><th align="left">Property name</th><th align="left">Description</th></tr>
<tr><td><code>project</code></td><td align="left">Name of the project. This is the name of the bnd file without the .bnd extension. If this name is bnd.bnd, then the directory name is used.</td></tr>
<tr><td><code>project.file</code></td><td align="left">Absolute path of the main bnd file.</td></tr>
<tr><td><code>project.name</code></td><td align="left">Just the name part of the file path</td></tr>
<tr><td><code>project.dir</code></td><td align="left">The absolute path of the directory in which the bnd file resides.</td></tr>
</tbody></table>


## Run instructions

Run instructions are used to start OSGi tests and OSGi runs. 

<table><tbody><tr><td valign="top">Instructions
</td><td valign="top">Argument
</td><td valign="top">Description
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-runbundles</code>
</td><td valign="top"><code>LIST SPEC</code>
</td><td valign="top">Additional bundles that will be installed and started when the framework is launched. This property is normally part of the project's <code>bnd.bnd</code> file.
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-runvm</code>
</td><td valign="top"><code>PROPERTIES</code>
</td><td valign="top">Properties given to the VM before launching. This property is normally set in the cnf/build.bnd file and only in rare cases overridden in the bnd.bnd file.
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-runproperties</code>
</td><td valign="top"><code>PROPERTIES</code>
</td><td valign="top">Properties given to the framework when launching. Usually project specific.
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-runsystempackages</code>
</td><td valign="top"><code>PACKAGES</code>
</td><td valign="top">A declaration like Import-Package that specifies additional system packages to import from the class path. Usually given in the cnf/build.bnd file.
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-runpath</code>
</td><td valign="top"><code>LIST SPEC</code>
</td><td valign="top">A path description of artifacts that must be on the classpath of the to be launched framework.  Usually given in the cnf/build.bnd file. This path should contain the framework. Any packages that a bundle on the -runpath should specify should be listed in the <code>export</code> attribute.
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-runtrace</code>
</td><td valign="top"><code>true|<strong>false</strong></code>
</td><td valign="top">Trace the startup of the framework to the console. Usually used during testing and development so project specific.
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-runframework</code>
</td><td valign="top"><code>none|<strong>services</strong></code>
</td><td valign="top"><code>NONE</code> indicates that a mini built in framework should be used. <code>SERVICES</code> indicates that the <code>META-INF/services</code> model must be followed for the <code>org.osgi.framework.launch.<span class="wikiword">FrameworkFactory</span></code> class. Project specific.
<div class="vspace"></div></td></tr><tr><td valign="top"><code>-testpath</code>
</td><td valign="top"><code>LIST SPEC</code>
</td><td valign="top">A path used to specify the test plugin
<div class="vspace"></div></td></tr></tbody></table>


## Launching

Launching is needed when the project's `run` action or `test` action is executed. The project creates a Project Launcher. A Project Launcher must launch a new VM and set up this VM correctly. The VM is launched with the following information. 



*   `java` - The command to launch a new VM is bu default `java`. However, this can be overridden by setting a property called `java`. 
*   `classpath` - The classpath set for the VM is derived from the `-runpath` property. Notice that this is supposed to contain the JAR with the framework. The `-runpath` requires bundle symbolic names for the JAR and an optional version range. bnd will use the latest version in the repository. Any packages that should be exported by the system bundle should have an export attribute containing the exported packages, like @@junit.osgi;version=3.8;export="junit.framework;version=3.8,junit.misc;version=3.8". 
*   `vm options` - These options can be set in the `-runvm` property. They are usually in the form of `-Dxya=15` or `-X:agent=bla`. Options should be separated by commas. 
*   `main` - The class implementing the main type is defined by the launcher plugin. 

And example of a launcher set is: 



    -runvm:   -Xmn100M, -Xms500M, -Xmx500M
      -runpath: 	org.apache.felix.framework; version=3.0, \   
    	junit.osgi;export="junit.framework;version=3.8"
      -runtrace: true
      -runproperties: launch=42, trace.xyz=true
      -runbundles: org.apache.felix.configadmin,\  
    	org.apache.felix.log,\  
    	org.apache.felix.scr,\  
    	org.apache.felix.http.jetty,\   
    	org.apache.felix.metatype,\  
    	org.apache.felix.webconsole
    

Debugging launching is greatly simplified with the -runtrace property set to true. This provides a lot of feedback what the launcher is doing. 



### Access to arguments

When the launcher is ready it will register itself as a service with the following properties: 


<table width="100%"><tbody><tr><td><code>launcher.arguments</code></td><td>The command line arguments</td></tr>
<tr><td><code>launcher.ready</code></td><td>Indicating the launcher is read</td></tr>
</tbody></table>

### Access to main thread

In certain cases it is necessary to grab the main thread after launching. The default launcher will launch all the bundles and then wait for any of those bundles to register a Runnable service with a service property `main.thread=true`. If such service is registered, the launcher will call the run method and exit when this method returns. 



### Timeout

The launcher will timeout after an hour. There is currently no way to override this timeout. 



### Mini Framework

The bnd launcher contains a mini framework that implements the bare bones of an OSGi framework. The purpose of this mini framework is to allow tests and runs that want to launch their own framework. A launch that wants to take advantage of this can launch with the following property: 



    -runframework: none
    



### Ant

In ant, the following task provides the run facility. 



    <target name="run" depends="compile">
        <bnd command="run" exceptions="true" basedir="\${project}" />
      </target>
    

These targets provide commands for `ant run`. 



## Testing

Testing is in principle the same as launching, it actually uses the launcher. Testing commences with the `test` action in the project. This creates a Project Tester. bnd carries a default Project Tester but this can be overridden. 

The basic model of the default Project Tester plugin is to look for bundles that have a `Test-Cases` manifest header after launching. The value of this header is a comma separated list of JUnit test case class names. For example: 



    Test-Cases: test.LaunchTest, test.OtherTest
    

Maintaining this list can be cumbersome and for that reason the `\${classes}` macro can be used to calculate its contents: 



    Test-Cases : \${classes;extending;junit.framework.TestCase;concrete}
    

See [classes macro][2] for more information. 



### Ant

    <target name="test" depends="compile">
        <bnd command="test" exceptions="true" basedir="\${project}" />
      </target>
    



## Overriding the plugins

Both the Project launcher and Project Tester are plugins. Defaults are provided by bnd itself (bnd carries a mini cache repo that is expanded in the cnf director), it is possible to add new launchers and testers as needed. 

Plugins are found on the -runpath or the -testpath properties respectively. bnd will look for an appropriate manifest header. The header has a class name as value. It will then instantiate the class and use as launcher/tester. The classes must extend an abstract base class. Each plugin has access to the Project object, containing all the details of the project. 

<table width="100%"><tbody><tr><td align="left">Plugin</td><td>Manifest header</td><td>Base Class</td><td>Where</td></tr>
<tr><td align="left">Project Launcher</td><td>Launcher-Plugin</td><td><span class="wikiword">ProjectLauncher</span></td><td>-runpath</td></tr>
<tr><td align="left">Project Test</td><td>Tester-Plugin</td><td><span class="wikiword">ProjectLauncher</span></td><td>-testpath</td></tr>
</tbody></table>

The plugin gets complete control and can implement many different strategies.

 [2]: Macros.html#classes