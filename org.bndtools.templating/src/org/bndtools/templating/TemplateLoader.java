package org.bndtools.templating;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import aQute.service.reporter.Reporter;

@ProviderType
public interface TemplateLoader {

    List<Template> findTemplates(String type, Reporter reporter);

}
