package org.bndtools.utils.parse;

public class ParserUtil {
    /**
     * This is used to deal with the trailing tilde characters introduced by the OSGiHeader.parseHeader method.
     * 
     * @param pkgName a package name
     * @return the package name without trailing tildes
     */
    public static String stripTrailingTildes(String pkgName) {
        String p = pkgName;
        while (p.length() > 0 && p.charAt(p.length() - 1) == '~')
            p = p.substring(0, p.length() - 1);
        return p;
    }

}
