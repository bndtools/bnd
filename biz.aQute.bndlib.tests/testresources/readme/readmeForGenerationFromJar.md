<bnd-gen>

# com.service.provider</bnd-gen>

<bnd-gen>

# Components

## com.service.provider.Provider1 - `not enabled`

### Services

This component does not provide services.

### Properties

This component does not define properties.

### Configuration

This component can `optionally` be configured with the following Pids:

* **Pid:	com.service.provider.Provider1**


### Lifecycle

This component is an `immediate component`, it will be activated on bundle start as soon as all its requirements will be satisfied.



## com.service.provider2 - `not enabled`

### Services

This component provides the following services:

|Service	|
|---	|
|org.service.api.Test	|

### Properties

This component has the following properties:

|Name	|Type	|Description	|Default	|
|---	|---	|---	|---	|
|name	|String	|	|"bob"	|
|two	|String	|	|"2"	|
|one	|String	|	|"one"	|

### Configuration

This component can `optionally` be configured with the following Pids:

* **Pid:	com.service.provider2**

	Properties:	name


### Lifecycle

This component is a `factory component`, it must be managed with a **FactoryComponent** service with the following property:
```
component.factory=fact
```



## com.service.provider3 - `not enabled`

### Services

This component provides the following services:

|Service	|
|---	|
|org.service.api.ExampleService	|
|org.service.api.ExampleService2	|

### Properties

This component has the following properties:

|Name	|Type	|Description	|Default	|
|---	|---	|---	|---	|
|name	|String	|A super property	|	|
|name3	|String[]	|	|"de", "da, \da"	|
|name4	|Boolean[]	|	|true, false	|
|name2	|String	|	|"dede"	|
|one	|String	|	|"one"	|
|truc19	|Short[]	|	|72, 2	|
|truc18	|Short	|	|45	|
|kiki	|String	|	|"hunt"	|
|truc17	|Long[]	|	|45, 452452	|
|truc16	|Long	|	|63	|
|truc15	|Boolean[]	|	|false, false	|
|truc9	|Integer[]	|	|89456, 48	|
|truc13	|Character[]	|	|Y, X	|
|two	|String	|	|"2"	|
|truc8	|Integer	|	|1	|
|truc14	|Boolean	|	|true	|
|truc11	|Byte[]	|	|0, 1	|
|truc12	|Character	|	|P	|
|truc5	|Double[]	|	|2.2, 5.6	|
|truc4	|Double	|	|3.0	|
|truc10	|Byte	|	|0	|
|truc7	|Float[]	|	|5.5, 5.0	|
|truc6	|Float	|	|4.5	|
|truc1	|String	|	|"one"	|
|truc2	|String[]	|	|"ji", "txo"	|

### Configuration

This component `must be` configured with the following Pids:

* **Pid:	com.service.provider.pid2**

	Properties:	name3, name4, name, name2

* **Pid:	com.service.provider.pid1**

	Properties:	name3, name4, name, name2

* **Pid:	com.service.provider.pid**	-	`factory`

	Properties:	name, name2


### Lifecycle

This component is an `immediate component`, it will be activated on bundle start as soon as all its requirements will be satisfied.

</bnd-gen>


<bnd-gen>

---
version 1.0.0.201708310931</bnd-gen>

