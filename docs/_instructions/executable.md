---
layout: default
class: Project
title: -executable ( rejar= STORE | DEFLATE ) ( ','  strip= matcher ( ',' matcher )* )
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

## strip= matcher ( ',' matcher )*

The `strip` option can be used to strip resources from JARs embedded in the execetable. Its parameter can define
a list of two GLOBs. The first GLOB must match the file name of the resource and the second GLOB must match the 
resource name in the bundle. GLOBs are separated with a colon (':'). If no colon is present then the first GLOB 
is assumed to be the wildcard GLOB that matches anything.

Syntax:

    strip   ::=  matcher ( ',' matcher )*
    matcher ::=  ( GLOB ':' ) GLOB

Some examples:

* `*.map` – Match all resources ending in `.map`
* `OSGI-OPT/*` – Remove the optional resources in bundle
* `com.example.some.bundle.*:readme.md` – Remove the readme's from bundles withe file name matching `com.example.some.bundle.*`.

Note hat if you combine multiple GLOBs then you must separate them with a comma (',') and that implies the total must be
quoted with single or double quotes. If it is a single GLOB (pair) then quoting is optional. For example:

    -executable: rejar=STORE, strip='OSGI-OPT,*.map'
    
The default is to not strip anything.

## Signed Bundles

Rejarring and stripping should work for unsigned bundles since the signatures should not be affected by the
compression algorithms used.
