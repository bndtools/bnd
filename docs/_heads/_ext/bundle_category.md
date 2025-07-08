---
layout: default
class: Header
title: Bundle-Category STRING (',' STRING )
summary: The categories this bundle belongs to, can be set through the BundleCategory annotation
---

# Bundle-Category

The `Bundle-Category` header allows you to specify one or more categories for your bundle. These categories can be used by tools and repositories to group and filter bundles. The header can be set using the `BundleCategory` annotation or directly in the manifest.

Example:

```
Bundle-Category: utility, database
```

Categories are free-form strings and can be customized as needed. This header is optional and is mainly used for documentation and discovery purposes.

