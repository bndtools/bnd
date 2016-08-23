---
order: 210
title: Metatype
layout: default
---


The OSGi Metatype specification provides a language to describe configuration information in an XML file. However, XML is cumbersome to use and eXtreMeLy error prone, and refactoring often finds it hard to change references in XML text files. Especially with DS components managing the relations can be complex, see [[Components]]

For this reason, bnd provides annotations to define the XML based on a ''configuration'' interface. For example, the following interface defines a simple metatype:

  package com.acme;
  import aQute.bnd.annotation.metatype.*;

  @Meta.OCD interface Config {
    int port();
  }

To turn this into an XML file, the bnd file must contain:

  -metatype: *

The wild card will search through the whole bundle but it is possible to limit this to a restricted set of packages to speed up the processing. For example:

  -metatype: *Metatype*, com.libs.metatypes

If the previous `Config` interface is present in the output, the output will also contain an XML file at `OSGI-INF/metatype/com.acme.Config.xml`. This file will look as follows:

  <metatype:MetaData 
    xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.1.0'>
    <OCD 
      name='Config' 
      id='aQute.metatype.samples.Config' 
      localization='aQute.metatype.samples.Config'>
      <AD 
        name='Port' 
        id='port' 
        cardinality='0' 
        required='true' 
        type='Integer'/>
    </OCD>
    <Designate pid='aQute.metatype.samples.Config'>
      <Object ocdref='aQute.metatype.samples.Config'/>
    </Designate>
  </metatype:MetaData>

bnd leverages the rich type information the Java class files contain. It inspects the returns types and from this it automatically calculates the AD type as well as the cardinality. For example, a method like:

   List<Integer> ints()

Provides an AD of:

    <AD 
      name='Ints' 
      id='ints' 
      cardinality='-2147483648' 
      required='true' 
      type='Integer'/>

## Naming
bnd attempts to make the names of the OCD and AD human readable by un-camel-casing the id. This means that it uses the upper cases in the id to decide where to use spaces. It also attempts to replace '_' characters with spaces and removes '$'. If the result is not what is wanted, the name can always be explicitly set with the AD.name() field.

The id of the properties is by default derived from the method name. The name space of methods is restricted by the Java language both in character set as well as by the reserved keywords. Therefore, bnd mangles the name of the method to allow the method name to be adapted to common practices in proeprty names.

* '$' is removed from the name unless it is followed by another '$', in that case it is a single '$'. This can be used to use Java keywords as a name. For example, `$new` maps to the `new` name. To make the name `$x`, use a method with the name `$$x`.
* '_' is replaced with '.' unless it is followed by another '_', in that case it becomes a single '_'. This is useful for dotted names (`a.b.c` comes from `a_b_c`) and to indicate private properties, that is properties that start with a '.'. For example, `_secret` maps to `.secret` and this will be a property that is not registered as a service. 

For example:

  @OCD
  interface Config {
    String _password(); // .password - will not be registered as a service
    String $new();      // new - keyword
  }

## @Meta.OCD
The OCD annotation is necessary to know that an interface is a Metatype interface. It should be used preferably without any parameters (they all have good defaults). However, each default is possible to override. The following table discusses the fields:

|`name`|String|A human readable name of the component, can be localized if it starts with a % sign. The default is a string derived from the id where _, $, or camel casing is used to provide spaces.|
|`id`|String|The id of the OCD, this will also be used for the pid of the Designate element.|
|`localization`|String|The localization id of the metatype. This refers to a properties file in the OSGI-INF/i10l/<localization>.properties file that can be augmented with locale information.|
|`description`|String|A human readable description that can be localized. Default is empty.|
|`factory`|String|Will treat this OCD as intended to be for a factory configuration. The default is `false`|

## @Meta.AD
The `AD` is an optional annotation on methods of a OCD interface. The annotation makes it possible to override the defaults and provide extra information.

|`name`|String|A human readable name of the attribute, can be localized if it starts with a % sign. The default is a string derived from the method name where _, $, or camel casing is used to provide spaces.|
|`id`|String|The id of the attribute. By default this is the name of the method|
|`description`|String|A human readable description that can be localized. Default is empty.|
|`type`|String|The type of the attribute. This must be one of the types defined in the Metatype specification. By default the type will be derived from the return type of the method. If no applicable type can be found then the `String` type is used as final default.|
|`cardinality`|int|The cardinality of the attribute. If this is negative its absolute indicates maximum number of elements in a Vector. If it is positive it indicates the maximum number of values in an array. Zero indicates a scalar. If not provided, bnd will calculate the cardinality based on the return type. Collections will have a negative large value and arrays have a positive large value.|
|`min`|String|The minimum value allowed for this attribute. There is no default.|
|`max`|String|The maximum value allowed for this attribute. There is no default.|
|`deflt`|String|The default initial value. The default for this is an empty String|
|`required`|boolean|Indicates if this attribute is required. The default is that attributes are required.|
|`optionLabels`|String[]|Labels for any values specified in `optionValues`. The default value is unset if there are no optionValues are defined. If these are defined, then the labels are calculated from the values by making _, $ into spaces and providing spaces between camel cased words in the values.|
|`optionValues`|String[]|Optional values. If this field is not set and the return type is an enum type then the values are calculated from the enum members.|


## Runtime conversions
The type support the OSGi Metatype specification is limited to the primitives and strings. So what happens when you use another type? During the build phase, bnd will revert to a 'String' Metatype type. This means that those special types are going to be strings in the dictionary. However, the aQute.bnd.annotation.metatype package contains a helper class that simplifies the usage of properties in runtime. It has a @createConfigurable(Class<T> c, Map<?,?> props)`. This method returns an object that implements the given configuration interface. The implementation of these methods uses the properties to provide a value. That is, calling the method abc() on this interface will attempt to find the property "abc". It will then use the actual return type of the method to do conversion from the type in the properties to the return type. This takes generic information into account when present.

For example:

  @Meta.OCD
  interface Config {
    URI[] uris();
  }

  public void updated( Map<String,Object> props) {
    config = Configurable.createConfigurable(Config.class,props);
    for ( URI URI : config.uris() ) {
      ...
    }
  }

The Metatype specification does not support URIs, so how does this work? Lets first look at the AD:

    <AD 
     name='uris' 
     id='uris' 
     cardinality='2147483647' 
     required='true' 
     type='String'/>

Any editor will therefore put an array of strings (String[]) in the properties. When the proxy gets the string, it will therefore have to convert from the String[] -> URI[]. In this case, the URI has a String constructor and is used to do the conversion from String to URI. The converter can handle general array to collection, collection to array, and as indicated, any conversion to an object that has a String constructor.

For convenience there are a number of built int conversions provided that cannot leverage the String constructor:

* Pattern - Allows regular expression to be set
* Class<?> - Tries to load a class through the class loader of the configuration interface
* boolean - Converts wrappers and primitive boolean to 0 (false) or non-0 (true) and vice versa.
* Numbers - Converts any known Number subclass to another Number subclass, including the primitive types.
* Enums - If the return type is an enum, it will use the `valueOf` method to get an instance.

## Example
The following example shows a very simple metatype configuration interface:

  package aQute.metatype.samples;
  import aQute.bnd.annotation.metatype.Meta.*;
  @OCD
  public interface Config {
    int port();
  }

If the bnd.bnd file contains:

  -metatype: *

Then bnd will detect this class as a Metatype and it generates the following XML in `OSGi-INF/metatype/aQute.metatype.samples.Config.xml

  <?xml version='1.0'?>
  <metatype:MetaData 
     xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.1.0'>
    <OCD 
      name='Config' 
      id='aQute.metatype.samples.Config'   
      localization='aQute.metatype.samples.Config'>
      <AD 
        name='Port' 
        id='port' 
        cardinality='0' 
        required='true' 
        type='Integer'/>
    </OCD>
    <Designate pid='aQute.metatype.samples.Config'>
      <Object ocdref='aQute.metatype.samples.Config'/>
    </Designate>
  </metatype:MetaData>

As usual, XML does an outstanding job in obfuscating the interesting parts. If you're using the Apache Felix Webconsole (and if not, why not?) then you can edit this metatype on the web:

%width=500px% http://www.aqute.biz/uploads/Bnd/webconsole.png

This metatype can now be used in a simple example that prints the port number:

  package aQute.metatype.samples;
  import java.util.*;
  import org.osgi.service.cm.*;
  import aQute.bnd.annotation.component.*;
  import aQute.bnd.annotation.metatype.*;

  @Component(properties="service.pid=aQute.metatype.samples.Config")
  public class Echo implements ManagedService {
	
    public void updated(Dictionary properties) 
      throws ConfigurationException {
      if ( properties != null ) {
        Config config = Configurable.createConfigurable(
          Config.class, properties);
        System.out.println(config.port());
      }
    }
  }

The editor can get quite rich with the metatype information. For example:

http://www.aqute.biz/uploads/Bnd/complex.png

This information came from the following Meta interface:

  interface SampleConfig {
    String _secret();
    String $new();
    String name();
    enum X { A, B, C; }
    X x();
    int birthYear();
    URI uri();
    URI[] uris();
    Collection<URI> curis();
    Collection<Integer> ints(); // fails on webconsole
  } 


Though this is a big savings over normal fudging with properties, it gets better. The metatyping is fully integrated with DS. In this example, we're using DS to register the Managed Service but this is not necessary because DS will automatically use the name of a component as the PID. So with a component life can be as easy as:

  @Component(designate=Config.class)
  public class Echo2 {	
    @Activate
    void activate(Map<?,?> properties) throws ConfigurationException {
      Config config = Configurable.createConfigurable(
         Config.class, properties);
      System.out.println(config.port());
    }
  }

No more strings. Components and metatypes are extensively explained in [[Components]].