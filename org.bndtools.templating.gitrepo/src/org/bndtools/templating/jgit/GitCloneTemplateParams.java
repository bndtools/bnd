package org.bndtools.templating.jgit;

import java.net.URI;

public class GitCloneTemplateParams {

    public static final String DEFAULT_BRANCH = "origin/master"; //$NON-NLS-1$

    String cloneUrl;
    String branch = DEFAULT_BRANCH;
    String name;
    String category;
    URI iconUri;
    URI helpUri;

}
