---
title: For Developers
layout: default
---



## API
It is quite easy to use bnd from Java, you only need to include biz.aQute.bndlib on your class path. This chapter shows you some samples of how to use bndlib.

## Creating a Manifest
By default, bnd creates a container with resources and then calculates the manifest. However, these phases are separated although they use the same instructions. The following snippet therefore shows how you can create a manifest from an existing file or directory.


    Analyzer analyzer = new Analyzer(); 
    Jar bin = new Jar( new File("bin") );  // where our data is
    analyzer.setJar( bin );                // give bnd the contents
    
    // You can provide additional class path entries to allow
    // bnd to pickup export version from the packageinfo file,
    // Version annotation, or their manifests.
    analyzer.addClasspath( new File("jar/spring.jar") );
    
    analyzer.setProperty("Bundle-SymbolicName","org.osgi.core");
    analyzer.setProperty("Export-Package", 
                       "org.osgi.framework,org.osgi.service.event");
    analyzer.setProperty("Bundle-Version","1.0");
    
    // There are no good defaults so make sure you set the 
    // Import-Package
    analyzer.setProperty("Import-Package","*");
    
    // Calculate the manifest
    Manifest manifest = analyzer.calcManifest();
  
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

### Make
Make plugins kick in when the `Include-Resource` header tries to locate a resource but it cannot find that resource. The `-make` option defines a number of patterns that are mapped to a make instruction.

For example, if you have

    Include-Resource:       com.acme.Abc.ann

If no such resource is found, bnd will look in the -make instruction. This instruction associates a pattern with a plugin type. For example:

    -make:                  (*.jar); type=xyz; abc=3; def="$1"

The first name part of the clause is matched against the unfound resource. All plugins are called sequentially until one returns non-null. The arguments on the -make clause are given as parameters to the make plugin. Normally all Make Plugins should verify the type field.

bnd has a bnd and a copy Make Plugin.
