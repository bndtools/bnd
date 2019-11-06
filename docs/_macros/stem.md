---
layout: default
class: Macro
title: stem ';' STRING 
summary: Return the string up to but not including the first dot
---

    static final String _stemHelp = "${stem;<string>}";

    public String _stem(String[] args) throws Exception {
        verifyCommand(args, _stemHelp, null, 2, 2);
        String name = args[1];
        int n = name.indexOf('.');
        if (n < 0)
            return name;
        return name.substring(0, n);
    }
