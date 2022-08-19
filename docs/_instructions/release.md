---
layout: default
class: Analyzer
title: -release NUMBER
summary: Specify the java release for which the Analyzer should generate meta-data. 
---
The `-release` instruction is very similar to the `--release` option of javac and instructs the Analyser to process Multi-Release-JARs with the specified release as defined in [JEP 238: Multi-Release JAR Files](https://openjdk.org/jeps/238).

If the `-release` is not specified or NUMBER is smaller than 0 then release processing is **disabled** no further processing is done

If the `-release` is specified and NUMBER is smaller than or equal to 8 the **default content** is processed, that means for every jar entries in the META-INF/versions/* directories are effectively ignored by the processor.

If the `-release` is specified and NUMBER is larger or equal than 9 the content is processed as with the rules from JEP 238 possibly hiding some of the default content.