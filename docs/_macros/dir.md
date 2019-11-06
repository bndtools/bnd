---
layout: default
class: Macro
title: dir ( ';' FILE )*
summary: Returns a list of the directories containing each specified file
---

    public String _dir(String[] args) {
        if (args.length < 2) {
            reporter.warning("Need at least one file name for ${dir;...}");
            return null;
        }
        String result = Arrays.stream(args, 1, args.length)
            .map(domain::getFile)
            .filter(File::exists)
            .map(File::getParentFile)
            .filter(File::exists)
            .map(IO::absolutePath)
            .collect(Strings.joining());
        return result;
    }
