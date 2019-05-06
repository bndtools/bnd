---
order: 240
title: SPI Annotations
layout: default
---

Writing Java libraries which support **OSGi** does not typically require more than generating proper OSGi metadata. **bnd** helps accomplish with minimal effort on the part of developers. One issue that remains somewhat complex is properly handling the use of the [Java SPI](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html) such that it works seamlessly in OSGi and avoiding the need for custom code to accomplish a similar goal. OSGi defines the [Service Loader Mediator Specification](https://osgi.org/specification/osgi.cmpn/7.0.0/service.loader.html) but applying it requires a significant amount of manifest metadata.

## SPI Annotations

bnd defines a set of *SPI annotations* which provide a simple solution for developers who wish to maintain OSGi friendly libraries.

### @ServiceConsumer

The Java SPI use cases are broken into two groups; providers and consumers. The `@aQute.bnd.annotation.spi.ServiceConsumer` annotation is used in consumer code to express a requirement on an SPI service type adding the appropriate OSGi metadata to the manifest.

```java
@ServiceConsumer(
    value = JsonProvider.class
)
public abstract class JsonProvider {
    public static JsonProvider provider() {
        for (JsonProvider provider : ServiceLoader.load(JsonProvider.class)) {
            return provider;
        }
        throw new JsonException("provider not found");
    }
    ...
}
```

`@ServiceConsumer` also grants the developer control in defining many facets of the generated OSGi requirements such as `cardinality`, `effective` time and `resolution`.

### @ServiceProvider

The provider side is supported by the `@aQute.bnd.annotation.spi.ServiceProvider` annotation. It is used to express a capability for a given SPI service type adding all the appropriate OSGi manifest metadata.

```java
@ServiceProvider(JsonProvider.class)
public class JsonProviderImpl extends JsonProvider {
    ...
}
```

`@ServiceProvider` also grants the developer control in defining many facets of the generated OSGi requirements and capabilities such as additional attributes and directives (attributes becoming service properties), `cardinality`, `effective` time, `resolution`, and package `uses`.

#### OSGi Services

The `@ServiceProvider` annotation will automatically result in publishing OSGi services for each provider discovered. Any attributes (excluding directives) specified on the annotation will be automatically added as OSGi service properties.

```java
@ServiceProvider(
    value = JsonProvider.class,
    attribute = {
        "colors:List<String>='blue,green,red'"
    }
)
public class JsonProviderImpl extends JsonProvider {
    ...
}
```

An `osgi.service` capability is also generated for each provider type.

#### Service Descriptor Files

An additional feature provided by bnd is the ability to manage the service descriptor files (a.k.a. `META-INF/services/*`) automatically for any `osgi.serviceloader` capabilities it finds having the `register:` directive containing a provider's fully qualified class name. The `register:` directive is automatically generated from all instances of `@ServiceProvider`.

