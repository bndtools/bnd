package org.bndtools.api;

import org.eclipse.core.runtime.IStatus;

import aQute.bnd.osgi.Builder;

public interface IValidator {
    IStatus validate(Builder builder) throws Exception;
}
