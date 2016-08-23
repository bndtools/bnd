---
layout: default
class: Analyzer
title: -check 'ALL' | ( 'IMPORTS' | 'EXPORTS' ) *
summary: Enable additional checking
---

Adding extra checks to bnd will break existing builds and some people get a tad upset about that. However, some checks are actually really valuable. So this instruction is a contract. New checks that can break build will add additional enums to this list. So in theory builds should not be broken. Currently we have the following values defined

* `ALL` – Enable all checking, including checks that are added in the future. So for `ALL`, one day we might break your build. Your choice. Then again, this is the wisest choice since the checks we do are really useful to know if they are not satisfied.
* `EXPORTS` – Enable checking for exports. This checks if an exported package has contents.
* `IMPORTS` – Enable checking of imports. If an import is not exported by any bundle on the `-buildpath` then we have a bit of a problem. Trying to resolve the current bundle on a framework will almost certainly fail. If you really need an import but have no export then you can get rid of the warning by explicitly importing it in Import-Package. For example:

	Import-Package: i.do.no.exist;version=@1.0.0, *

