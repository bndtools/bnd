---
layout: default
class: Processor
title: thisfile
summary: Return the name of the properties file for this Processor
---

    /**
     * Return the name of the properties file
     */

    public String _thisfile(String[] args) {
        if (propertiesFile == null) {
            error("${thisfile} executed on a processor without a properties file");
            return null;
        }

        return IO.absolutePath(propertiesFile);
    }
