---
layout: default
title: -invalidfilenames
class: Project
summary: |
   Specify file/directory names that should not be used because they are not portable.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-invalidfilenames:CON(\..+)?|PRN(\..+)?|AUX(\..+)?|CLOCK\$|NUL(\..+)?|COM[1-9](\..+)?|LPT[1-9](\..+)?|\$Mft|\$MftMirr|\$LogFile|\$Volume|\$AttrDef|\$Bitmap|\$Boot|\$BadClus|\$Secure|\$Upcase|\$Extend|\$Quota|\$ObjId|\$Reparse`

- Pattern: `.*`

<!-- Manual content from: ext/invalidfilenames.md --><br /><br />

The `-invalidfilenames` instruction allows you to specify file or directory names that should not be used in your project because they are not portable across all operating systems (especially Windows). By default, bnd checks for reserved names that are problematic on Windows, such as `CON`, `PRN`, `AUX`, `NUL`, `COM1`, `LPT1`, and others.

You can customize the regular expression used to detect invalid names by setting this instruction. If any files or directories in your JAR match the specified pattern, bnd will report an error, helping you avoid portability issues when distributing your bundles.


TODO Needs review - AI Generated content
