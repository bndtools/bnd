---
layout: default
title: Bundle-Category STRING (',' STRING )
class: Header
summary: |
   The categories this bundle belongs to, can be set through the BundleCategory annotation
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-Category: test`

- Values: `osgi,test,game,util,eclipse,netbeans,jdk,specification`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_category.md --><br /><br />

# Bundle-Category

The `Bundle-Category` header allows you to specify one or more categories for your bundle. These categories can be used by tools and repositories to group and filter bundles. The header can be set using the `BundleCategory` annotation or directly in the manifest.

Example:

```
Bundle-Category: utility, database
```

Categories are free-form strings and can be customized as needed. This header is optional and is mainly used for documentation and discovery purposes.



<hr />
TODO Needs review - AI Generated content
