---
layout: default
class: Workspace
title: global ';' KEY ( ';' DEFAULT )? 
summary: A current user setting from the ~/.bnd/settings.json file
---

    static final String _globalHelp = "${global;<name>[;<default>]}, get a global setting from ~/.bnd/settings.json";

    /**
     * Provide access to the global settings of this machine.
     *
     * @throws Exception
     */

    public String _global(String[] args) throws Exception {
        Macro.verifyCommand(args, _globalHelp, null, 2, 3);

        String key = args[1];
        if (key.equals("key.public"))
            return Hex.toHexString(settings.getPublicKey());
        if (key.equals("key.private"))
            return Hex.toHexString(settings.getPrivateKey());

        String s = settings.get(key);
        if (s != null)
            return s;

        if (args.length == 3)
            return args[2];

        return null;
    }
