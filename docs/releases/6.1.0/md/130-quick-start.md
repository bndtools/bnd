___
___
# Quick Start

Assume we need to create a bundle in Eclipse. Each Java project in Eclipse has a set of sources and a class path. bnd therefore knows, about all the classes. However, it does not know how you want to structure your JARs/Bundles. It therefore needs a description file, the ''bnd file''. If you create such a file, you should give it the same as name as the bundle symbolic name with a `.bnd` extension. For example `aQute.example.bnd` is a well chosen name. If the name is not `bnd.bnd`, the file name without the .bnd extension is the default for your bundle symbolic name.

Lets build a bundle for the aQute OSGi tutorial Chat example. This bundle has 2 packages.

* aQute.service.channel
* aQute.tutorial.chat

The `aQute.service.channel` package must be exported and the other package may remain private. All packages that are referred from the source code must be imported. To achieve this, the following manifest will suffice:

    Export-Package: aQute.service.channel; version=1.0 
    Private-Package: aQute.tutorial.chat

This is all you need! In Eclipse, you can select the bnd file and run the `Make Bundle` command. This will create a JAR with the proper content:

    META-INF 
      MANIFEST.MF 
    aQute/service/channel 
      Channel.class 
      aQute/tutorial/chat 
      Chat$ChannelTracker.class 
      Chat.class

You can run the same command from the command line

    $ bnd aQute.tutorial.chat.bnd

Now take a look at the JAR file's manifest. With the command line version of you can do this with `bnd aQute.tutorial.chat.jar`. Otherwise just open the JAR with WinZip.

   Manifest-Version: 1
   Bundle-Name: aQute.tutorial.chat
   Private-Package: aQute.tutorial.chat
   Import-Package: aQute.service.channel;version=1.0,
     org.osgi.framework; version=1.3,
     org.osgi.util.tracker;version=1.3
   Bundle-ManifestVersion: 2
   Bundle-SymbolicName: aQute.tutorial.chat
   Export-Package: aQute.service.channel;version=1.0
   Bundle-Version: 0

As you can see, bnd filled in a number of headers. The first header, Manifest-Version is required by the JAR standard. Bundle-Name is derived from the Bundle-SymbolicName because we did not specify it. The Private-Package header specifies the packages that ended up not exported. The Import-Package header comes from the packages that were referred to from the contained packages (private and exported). As you can see, bnd picked up versions for the imported packages. These versions come from the manifest of the JARs that source these packages, or from the packageinfo file. The Export-Package shows the export of the service package. On top of this, bnd has verified that all your headers really match the OSGi specifications, or you will get errors and warnings.

