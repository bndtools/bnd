---
layout: default
class: Project
title: -baseline selector
summary: Control what bundles are enabled for baselining and optionally specify the baseline version or file.
---
_Baselining_ uses the previous revision of a project with a _baseline_ bundle to detect any changes in semantic versioning using the rules of binary compatibility. The `-baseline` instruction enables baselining for one or more symbolic names, the instruction takes a _selector_ as input. Each bundle that is being build is held against this selector and if it matches then it is baselined. The following example will baseline any bundle whose name starts with `com.example.`. 

	-baseline: com.example.*

By default a bundle's baseline is the revision with the highest version in the repositories. However, the baseline can also be set with a `file` or `version` attribute on the selector. 

* `version` – The target will be the bundle with the lowest version that is higher than the given version.
* `file` – The target will be the given bundle. The file is relative to the project directory.

	-baseline com.example.foo;version=1.2, com.example.bar;file=foo-1.2.3.jar

Detected violations of the semantic versioning are reported as errors.

See [baselining](../chapters/180-baselining.html) for more information.

	
