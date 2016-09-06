package org.bndtools.templating.jgit;

public class GitHubValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    public GitHubValidationException(String message) {
        super(message);
    }

    public GitHubValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
