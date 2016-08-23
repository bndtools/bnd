___
___
# Service Components
''Version 1.42''

The Service-Component header is compatible with the standard OSGi header syntax. Any element in the list that does not have attributes must have a resource in the JAR and is copied as is to the manifest. However, simple components can also be defined inline, and it is even possible to pickup annotations. 

The syntax for these component definitions is:

  component ::= <name> ( ';' parameter ) * 
  parameter ::= provide | reference | multiple | optional
              | reference | properties | factory | servicefactory
              | immediate | enabled | implementation 
              | activate | deactivate | modified | configuration-policy
              | version | designate

  reference ::= <name> '=' <interface-class> 
                 ( '(' <target-filter> ')')? cardinality?
  cardinality ::= '?' | '*' | '+' | '~'
  provide  ::= 'provide:=' LIST 
  multiple  ::= 'multiple:=' LIST 
  optional  ::= 'optional:=' LIST 
  dynamic   ::= 'dynamic:=' LIST
  designate  ::= ( 'designate' | 'designateFactory' ) CLASS
  factory   ::= 'factory:=' true | false
  servicefactory := 'servicefactory:=' true | false
  immediate ::= 'immediate:=' true | false
  enabled   ::= 'enabled:=' true | false
  configuration-policy ::= "configuration-policy:=' 
       ( 'optional' | 'require' | 'ignore' )
  activate  ::= 'activate:=' METHOD
  modified  ::= 'modified:=' METHOD
  deactivate::= 'deactivate:=' METHOD
  implementation::= 'implementation:=' <implementation-class>
  properties::= 'properties:=' key '=' value  [=\=]
                ( ',' key '=' value ) *
  key       ::= NAME (( '@' | ':' ) type )?
  value     ::= value ( '|' value )*

If the name of the component maps to a resource, or ends in XML, or there are attributes set, then that clause is copied to the output Service-Component header.

If the name can be expanded to one or more classes that have component annotations (they must be inside the JAR), then each of those classes is analyzed for its component annotations. These annotations are then merged with the attributes from the header, where the header attributes override annotations. The expansion uses the normal wildcard rules. For example, `biz.aQute.components.*` will search for component annotated classes in the  `biz.aQute.components` package or one of its descendants. The classes must be present in the JAR. If no classes with annotations can be found for the `name` then it is assumed to be name or implementation class name without annotations.

The name of the component is also the implementation class (unless overridden by the implementation: directive). It is then followed with a number of references and directives. A reference defines a name that can be used with the `locateService` method from the `ComponentContext` class. If the name starts with a lower case character, it is assume to be a bean property. In that case the reference is augmented with a `set<Name>` and `unset<Name>` method according to the standard bean rules. Bnd will interpret the header, read the annotations if possible, and create the corresponding resources in the output jar under the name `OSGI-INF/<id>.xml`. 

Annotations are only recognized on the component class, super classes are not inspected for the components.

The supported annotations in the `aQute.bnd.annotations.component` package are:

||!Component||
Annotated the class, indicates this class is required to be a component. It has the following properties:

||  provide||Class[]||Service interfaces, the default is all directly implemented interfaces||
||name     ||String||Name of the component||
||factory  ||Boolean||Factory component||
||servicefactory||Boolean||Service Factory||
||immediate||Boolean||Immediate activation||
||designate||CLASS||Designate a class as a [[MetaType]] interface used for configurations for unitary configurations, see [[#metatype]]. This changes the default of the configurationPolicy to `require`.||
||designateFactory||CLASS||Designate a class as a [[MetaType]] interface used for configurations for factory configurations, see [[#metatype]]. This changes the default of the configurationPolicy to `require`.||
||configurationPolicy||OPTIONAL, REQUIRE, IGNORE||Configuration Policy||
||enabled||Boolean||Enabled component||
||properties||String[]||Properties specified as an array of `key=value`. The property type can be specified in the key as `name:Integer`. The value can contain multiple values, the parts must then be separated by a vertical bar ('|') or a line feed (\n), for example `properties = {"primes:Integer==1|2|3|5|7|11"`}.|| 

||!Reference||
On a method. Indicates this method is the activate method. It has the following attributes

||name||String||Name of the reference. Default this is the name of the method without set on it.||
||service||Class||The service type, default is the argument type of the method. The unset method is derived from this name. I.e. setXX will have an unsetXX method to unset the reference.||
||type||Character||Standard cardinality type '?', '*', '+','~'||
||target||String||A filter expression applied to the properties of the target service||
||unbind||String||Optional name of the unbind method. By default this is the same name as the bind method name but with set/add replaced with unset/remove. E.g. setFoo() bind method becomes unsetFoo() unbind method.||


`@Reference` automatically sets the bind method. The unbind method is set by using a derived name from the bind method or providing it with the name of the unbind method. The following name patterns are supported:

||!bind||!unbind||
||`setX` ||`unsetX`||
||`addX` ||`removeX`||
||`xxxX` ||`unxxxX`||
For example:
  @Reference
  protected void setFoo(LogService l) { ... }
  protected void unsetFoo(LogService l) { ... }
If you want to override this, use
  @Reference(unbind="IRefuseToCallMyMethodUnFoo");
  protected void foo(LogService l) {}
  protected void IRefuseToCallMyMethodUnFoo(LogService l) {}
Unfortunately Java has no method references so it is not type safe.A non existent `@UnReference` annotation is not very useful because that still requires linking it up symbolically to the associated `@Reference`.

||!Activate, Modified, and Deactivate||
The life cycle methods. These annotations have no properties.

  
Assume the JAR contains the following class:

  package com.acme;
  import org.osgi.service.event.*;
  import org.osgi.service.log.*;
  import aQute.bnd.annotation.component.*;

  @Component
  public class AnnotatedComponent implements EventHandler {
    LogService log;

    @Reference
    void setLog(LogService log) { this.log=log; }

    public void handleEvent(Event event) {
      log.log(LogService.LOG_INFO, event.getTopic());
    }
  }

The only thing necessary to register the Declarative Service component is to add the following Service-Component header:

  Service-Component: com.acme.*

This header will look for annotations in all com.acme sub-packages for an annotated component. The resulting XML will look like:

  OSGI-INF/com.acme.AnnotatedComponent.xml:
    <?xml version='1.0' encoding='utf-8'?>
      <component name='com.acme.AnnotatedComponent'>
        <implementation class='com.acme.AnnotatedComponent'/>
        <service>
          <provide interface='org.osgi.service.event.EventHandler'/>
        </service>
        <reference name='log'
          interface='org.osgi.service.log.LogService' 
          bind='setLog'
          unbind='unsetLog'/>
      </component>

The following example shows a component that is bound to the log service via the setLog method without annotations:  

  Service-Component=aQute.tutorial.component.World; [=\=]
    log=org.osgi.service.log.LogService  
   
The Service Component Runtime (SCR) offers a variety of options on the reference. Some options like the target can be used by adding the target filter after the interface name (this likely requires putting quotes around the interface name+filter). 

References can be suffixed with the following characters to indicate their cardinality:

  Char          Cardinality    Policy
  ?             0..1           dynamic
  *             0..n           dynamic
  +             1..n           dynamic
  ~             0..1           static
                1              static

For a more complex example:

  Service-Component=aQute.tutorial.component.World; [=\=]
    log=org.osgi.service.log.LogService?; [=\=]
    http=org.osgi.service.http.HttpService; [=\=]
    PROCESSORS="xierpa.service.processor.Processor(priority>1)+"; [=\=]
    properties:="wazaabi=true"
    
The previous example will result in the following service component in the resource `OSGI-INF/aQute.tutorial.component.World.xml`:  

  <?xml version="1.0" encoding="utf-8" ?>
   <component name="aQute.tutorial.component.World">
     <implementation class="aQute.tutorial.component.World" /> 
     <reference name="log" 
       interface="org.osgi.service.log.LogService" 
       cardinality="0..1" 
       bind="setLog" 
       unbind="unsetLog" 
       policy="dynamic" /> 
     <reference name="http" 
       interface="org.osgi.service.http.HttpService" 
       bind="setHttp" 
       unbind="unsetHttp" />
     <reference name="PROCESSORS" 
       interface="xierpa.service.processor.Processor" 
       cardinality="1..n" 
       policy="dynamic" 
       target="(&(priority>=1)(link=false))" /> 
   </component> 

The description also supports the immediate, enabled, factory, target, servicefactory, configuration-policy, activate, deactivate, and modified attributes. Refer to the Declarative Services definition for their semantics.

If any feature of the V1.1 namespace is used, then bnd will declare the namespace in the `component` element. A specific namespace version can be set with the `version` directive. This detection only works when components are used.

Bnd also supports setting the policy and cardinality through the following directives:

  multiple:= LIST    names of references that have x..n
  optional:= LIST    names of references that have 0..x
  dynamic:=  LIST    names of references that are dynamic

## Components and Metatype
The Service Component Runtime works closely together with the Configuration Admin to allow the components to be controlled through configuration. Configuration Admin knows two types of configuration:

* Unitary
* Factory

A unitary configuration can be set and changed but there is at most one of them. A Factory configuration can be used to create multiple instances of the same component. A component has a configuration policy that defines when no configuration is set.

* optional - If no configuration is set (either unitary or factory) then the component is still created.
* require - This requires a unitary configuration to be set or one or more factory configurations before a component is created.
* ignore - Ignore configuration information

A related standard is the Metatype standard. The Metatype Service provides a repository of property descriptors. Bundles can provide these descriptors in their bundles in the OSGI-INF/metatype directory. There are tools, like the [Felix Webconsole][http://felix.apache.org/site/apache-felix-web-console.html], that can provide an editing window for a configuration that is typed with a metatype description.

In practice, this is a powerful model that provides a lot of configurability for your components with easy editing but getting it all right is not trivial. To make this easier, bnd has made it ease to use configurations.

In this model, configurations are declared in an interface. For example, the following interface defines a simple message:

  interface Config {
    String message(); // message to give
  }

To create a component that can work with this config, we need to designate that interface as the configuration interface for a component.

  @Component(designate=Config.class)
  public class BasicComponent {
      Config config;

      @Activate void activate(Map<String,Object> props) {
         config = Configurable.createConfig(props);
         System.out.println("Hi " + config.message());
      }

      @Deactivate void deactivate() {
         System.out.println("Bye " + config.message());
      }
  }

This is an immediate component because it does not implement a service interface. It also requires a configuration because we have not specified this explicitly. When you use designate (or designateFactory) the default becomes require. This means that your component will only be created when there is actually configuration for it set.

To run this component, make sure you have the Felix Webconsole running and the MetaType service installed. In the Webconsole, you can click on
'''Configuration''', your component should be listed on this page. By Clicking on the component with the name '''Basic Component Config''' you get an editor window.

The editor is aware of the proper types, it uses the [[MetaType]] standard to describe the properties. bnd uses the type information on the interface as well as the optional Metadata annotations to create a rich description that allows the web console to provide a good editor.

You can fill in the message in the ''Message'' field. If you save the editor, your component prints the message with "Hi" in front of it. Deleting the configuration will print the message with "Bye".

If you change the message, you will see that the component is first deactivated and then reactivated again. This is the only possibility for the SCR because the component has not implemented a modified method. Adding the following method will change this, now changes to the configuration are signaled to the component and the component can continue to work. This is more complicated then recycling the component but it can create a more optimized system.

  @Modified
  void modified( Map<String,Object> props) {
    // reuse activate method
    activate(props);
  }

It is also possible to take advantage of the configuration factories. In this model 

An example, that implements a simple socket server on a configurable port and returns a message when a telnet session is opened to that port can be found on [Github][https://github.com/bnd/aQute/blob/master/aQute.metatype/src/aQute/metatype/components/ServerSocketComponent.java].

