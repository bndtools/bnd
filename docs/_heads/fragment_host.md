---
layout: default
title: Fragment-Host       ::= bundle-description
class: Header
summary: |
   The Fragment-Host header defines the host bundles for this fragment.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Fragment-Host: org.eclipse.swt; bundle-version="[3.0.0,4.0.0)"`

- Pattern: `.*`

### Options 

- `extension:` Indicates this extension is a system or boot class path extension. It is only applicable when the Fragment-Host is the System Bundle.
  - Example: `extension:=framework`

  - Values: `framework,bootclasspath`

  - Pattern: `framework|bootclasspath`


- `bundle-version` A version range to select the bundle version of the exporting bundle. The default value is 0.0.0.
  - Example: `bundle-version=1.3`

  - Pattern: `((\(|\[)\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?,\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?(\]|\)))|\d{1,9}(\.\d{1,9}(\.\d{1,9}(\.[-\w]+)?)?)?`

<!-- Manual content from: ext/fragment_host.md --><br /><br />

# Fragment-Host

The `Fragment-Host` header specifies the symbolic name of the host bundle(s) for a fragment bundle. A fragment is attached to its host at runtime and shares the same class loader. When a fragment is attached, some headers (such as `Import-Package`) are merged with those of the host.

Example:

```
Fragment-Host: com.example.hostbundle
```

bnd will automatically subtract any packages found in the host from the fragment's imports. This header is required for all fragment bundles.

The Fragment-Host manifest header links the fragment to its potential _hosts_. A fragment bundle is loaded in the 
same class loader as the _host_ that it will be attached to in runtime. When a fragment is attached to its host,
then some headers are _merged_. One of those headers is the Import Package header.

bnd will calculate the references without taking the host into account. If the fragment uses packages from the host, 
quite likely, then these would result in imports. For this reason, bnd will subtract any package that can be found
in the host from the import.

<hr />
TODO Needs review - AI Generated content
