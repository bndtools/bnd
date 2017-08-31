<bnd-gen>

# Title

A short description</bnd-gen>

# This is my Title

Hello, this readme has title sections

## just subsection

# Section 2
<bnd-gen>

# Components

## compo1 - `not enabled`

### Services

This component provides the following services:

|Service	|
|---	|
|[com.dede.Dada](url)	|
|[com.dede.Dada2](url)	|

### Properties

This component has the following properties:

|Name	|Type	|Description	|Default	|
|---	|---	|---	|---	|
|test.test	|String	|A test config	|"truc"	|
|test.test2	|String	|A test config	|	|
|test.test.3	|String	|	|"dede"	|
|test.test4	|String	|	|""	|

### Configuration

This component `must be` configured with the following Pids:

* **Pid:	pid1**

* **Pid:	pid1**

	Properties:	prop1, prop2, prop3

* **Pid:	pid1**


### Lifecycle

This component is a `delayed component` with a `singleton` scope, it will be activated if needed as soon as all its requirements will be satisfied.



## compo1 - `enabled`

### Services

This component does not provide services.

### Properties

This component does not define properties.

### Configuration

This component can `optionally` be configured with the following Pids:


### Lifecycle

This component is an `immediate component`, it will be activated on bundle start as soon as all its requirements will be satisfied.



## compo2 - `not enabled`

### Services

This component provides the following services:

|Service	|
|---	|
|	|
|	|

### Properties

This component has the following properties:

|Name	|Type	|Description	|Default	|
|---	|---	|---	|---	|
|	|String	|	|	|

### Configuration

This component do not use any configuration.

### Lifecycle

This component is an `immediate component`, it will be activated on bundle start as soon as all its requirements will be satisfied.



## compo3 - `enabled`

### Services

This component does not provide services.

### Properties

This component does not define properties.

### Configuration

This component can `optionally` be configured with the following Pids:


### Lifecycle

This component is an `immediate component`, it will be activated on bundle start as soon as all its requirements will be satisfied.



## compo4 - `not enabled`

### Services

This component does not provide services.

### Properties

This component does not define properties.

### Configuration

This component `must be` configured with the following Pids:


### Lifecycle

This component is a `factory component`, it must be managed with a **FactoryComponent** service with the following property:
```
component.factory=facto
```



## compo5 - `not enabled`

### Services

This component provides the following services:

|Service	|
|---	|
|[com.dede.Dada](url)	|

### Properties

This component has the following properties:

|Name	|Type	|Description	|Default	|
|---	|---	|---	|---	|
|test.test.3	|String	|	|"dede"	|

### Configuration

This component can `optionally` be configured with the following Pids:

* **Pid:	pid1**


### Lifecycle

This component is a `delayed component` with a `singleton` scope, it will be activated if needed as soon as all its requirements will be satisfied.

## Additional Information

* This is really good
</bnd-gen>

# Section 3
 Cool<bnd-gen>

# License

[Apache-2.0](www.apache.com)

Do what you want!</bnd-gen>

<bnd-gen>

# Copyright

A copyright</bnd-gen>

<bnd-gen>

---
[Vendor](www.vendor.org) - version 1.0.0</bnd-gen>

