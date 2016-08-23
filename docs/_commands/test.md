---
layout: default
title:   test [options] <testclass[:method]...>
summary: Test a project according to an OSGi test 
---

OPTIONS

   [ -c, --continuous ]       - Set the -testcontinuous flag
   [ -f, --force ]            - Launch the test even if this bundle does not
                                contain Test-Cases
   [ -p, --project <string> ] - Path to another project than the current project
   [ -t, --trace ]            - Set the -runtrace flag
   [ -v, --verify ]           - Verify all the dependencies before launching
                                (runpath, runbundles, testpath)
