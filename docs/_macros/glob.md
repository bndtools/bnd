---
layout: default
class: Macro
title: glob ';' GLOBEXP
summary: Return the regular expression for the specified glob expression
---

    static final String _globHelp = "${glob;<globexp>} (turn it into a regular expression)";

    public String _glob(String[] args) {
        verifyCommand(args, _globHelp, null, 2, 2);
        String glob = args[1];
        boolean negate = false;
        if (glob.startsWith("!")) {
            glob = glob.substring(1);
            negate = true;
        }

        Pattern pattern = Glob.toPattern(glob);
        if (negate)
            return "(?!" + pattern.pattern() + ")";
        else
            return pattern.pattern();
    }
