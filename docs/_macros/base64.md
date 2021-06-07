---
layout: default
class: Macro
title: base64 ';' FILE [';' LONG ]
summary: Get the Base64 encoding of a file
---

    static final String _base64Help = "${base64;<file>[;fileSizeLimit]}, get the Base64 encoding of a file";

    /**
     * Get the Base64 encoding of a file.
     *
     * @throws IOException
     */
    public String _base64(String... args) throws IOException {
        verifyCommand(args, _base64Help, null, 2, 3);

        File file = domain.getFile(args[1]);
        long maxLength = 100_000;
        if (args.length > 2)
            maxLength = Long.parseLong(args[2]);

        if (file.length() > maxLength)
            throw new IllegalArgumentException(
                "Maximum file size (" + maxLength + ") for base64 macro exceeded for file " + file);

        return Base64.encodeBase64(file);
    }

