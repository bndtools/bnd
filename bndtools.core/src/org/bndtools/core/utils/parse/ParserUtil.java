package org.bndtools.core.utils.parse;

public class ParserUtil {
    /**
     * This is used to deal with the trailing tilde characters introduced by the
     * OSGiHeader.parseHeader method.
     *
     * @param pkgName
     * @return
     */
    public static String stripTrailingTildes(String pkgName) {
        while (pkgName.length() > 0 && pkgName.charAt(pkgName.length() - 1) == '~')
            pkgName = pkgName.substring(0, pkgName.length() - 1);
        return pkgName;
    }

}
