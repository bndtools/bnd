---
layout: default
class: Macro
title: bytes ( ';' LONG )*
summary: Format bytes
---

    /**
     * Format bytes
     */
    public String _bytes(String[] args) {
        try (Formatter sb = new Formatter()) {
            for (int i = 0; i < args.length; i++) {
                long l = Long.parseLong(args[1]);
                bytes(sb, l, 0, new String[] {
                    "b", "Kb", "Mb", "Gb", "Tb", "Pb", "Eb", "Zb", "Yb", "Bb", "Geopbyte"
                });
            }
            return sb.toString();
        }
    }

    private void bytes(Formatter sb, double l, int i, String[] strings) {
        if (l > 1024 && i < strings.length - 1) {
            bytes(sb, l / 1024, i + 1, strings);
            return;
        }
        l = Math.round(l * 10) / 10;
        sb.format("%s %s", l, strings[i]);
    }
