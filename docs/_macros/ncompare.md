---
layout: default
class: Macro
title: ncompare STRING STRING
summary: Compare two numbers. 0 is equal, 1 means a > b, -1 is a < b.
---

    static final String _ncompareHelp = "${ncompare;<anumber>;<bnumber>}";

    public int _ncompare(String[] args) throws Exception {
        verifyCommand(args, _ncompareHelp, null, 3, 3);
        double a = Double.parseDouble(args[1]);
        double b = Double.parseDouble(args[2]);
        return Integer.signum(Double.compare(a, b));
    }


