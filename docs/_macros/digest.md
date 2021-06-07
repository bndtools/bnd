---
layout: default
class: Macro
title: digest ';' ALGORITHM ';' FILE
summary: Get a digest of a file
---

    static final String _digestHelp = "${digest;<algo>;<in>}, get a digest (e.g. MD5, SHA-256) of a file";

    /**
     * Get a digest of a file.
     *
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public String _digest(String... args) throws NoSuchAlgorithmException, IOException {
        verifyCommand(args, _digestHelp, null, 3, 3);

        MessageDigest digester = MessageDigest.getInstance(args[1]);
        File f = domain.getFile(args[2]);
        IO.copy(f, digester);
        byte[] digest = digester.digest();
        return Hex.toHexString(digest);
    }
