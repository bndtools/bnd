---
layout: default
class: Macro
title: vcompare VERSION VERSION
summary: Compare two version strings 
---

Compare two version strings. The result "0" means the two versions are equal, "1" means the first version is greater than the second version, "-1" means the first version is less than the second version.

    ${vcompare;versionA;versionB}
