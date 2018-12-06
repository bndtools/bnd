package org.bndtools.utils.copy.bundleresource;

public enum CopyMode {
    /**
     * Add if not already present.
     */
    ADD,

    /**
     * Add and overwrite if already present.
     */
    REPLACE,

    /**
     * Remove if present.
     */
    REMOVE,

    /**
     * Do nothing, just check if the file exists.
     */
    CHECK

}
