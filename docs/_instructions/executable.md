---
layout: default
class: Project
title: -executable ( rejar= STORE | DEFLATE ) ( ','  strip= true|false )
summary: Process an executable jar to strip optional directories of the contained bundles and/or change their compression
---

When an executable JAR is created by the Project Launcher the compression is controlled with the [-compression](compression.html) 
instruction. However, this über JAR contains bundles and JARs for the run path. Since executable JARs are sometimes used in 
embedded environments it is important that they are small and the code can loaded quickly. It is difficult to 
have defaults for this since there are many aspects:

* Speed of memory for accessing. I.e. RAM is faster than FLASH
* Cost of memory, FLASH is cheaper
* Speed of the CPU
* Number of cores
* Compression of the inner JARs
* Compression of the outer über JAR

In such a complex trade off there are no singular answers. This `-executable` instruction allows therefore to
set the different options and then benchmark the result.

## Hints

Experience shows that the size can be minimized by rejarring the inner bundles and JARs with the STORE compression
but using DEFLATE for the outer/über JAR. This decreases the size because the compression algorithm then works on 
all class files of all bundles. This is much more efficient for the DEFLATE algorithm then compressing each class file
on its own. Gains seen are 20%-30%.

## rejar = (DEFLATE|STORE)

The `rejar` option in the `-executable` instruction can take the values `STORE` or `DEFLATE`. This will open
the embedded JARs and write them in the given compression in the über JAR. 

For example:

    -executable: rejar=STORE

The default is to not touch the bundle.

## strip= (true|false)

The `strip` option is a boolean. If it is true, the `OSGI-OPT` directory is removed from all bundles and JARs.

For example:

    -executable: rejar=STORE, strip-true 
    
The default is to not strip.

## Signed Bundles

Rejarring and stripping should work for unsigned bundles since the signatures should not be affected by the
compression algorithms used.
