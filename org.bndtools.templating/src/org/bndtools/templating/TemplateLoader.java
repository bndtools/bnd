package org.bndtools.templating;

import java.util.Collection;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.promise.Promise;

import aQute.service.reporter.Reporter;

@ProviderType
public interface TemplateLoader {

    Promise<? extends Collection<Template>> findTemplates(String type, Reporter reporter);

}
