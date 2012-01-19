package bndtools.api;

import org.eclipse.core.runtime.IStatus;

import aQute.lib.osgi.Builder;

public interface IValidator {
    IStatus validate(Builder builder);
}
