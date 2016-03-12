package org.bndtools.templating;

import java.util.List;

import aQute.bnd.annotation.ProviderType;
import aQute.service.reporter.Reporter;

@ProviderType
public interface TemplateLoader {

    List<Template> findTemplates(String type, Reporter reporter);

}
