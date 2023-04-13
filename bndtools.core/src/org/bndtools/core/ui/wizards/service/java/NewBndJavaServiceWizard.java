package org.bndtools.core.ui.wizards.service.java;

import org.bndtools.core.ui.wizards.service.NewBndServiceWizard;
import org.bndtools.core.ui.wizards.service.NewBndServiceWizardPageOne;
import org.bndtools.core.ui.wizards.shared.BuiltInServiceTemplate;
import org.bndtools.templating.Template;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

public class NewBndJavaServiceWizard extends NewBndServiceWizard {

	private static final String		SERVICE_TEMPLATES_DIR	= "/serviceTemplates/";
	private static final String		SERVICE_API_PATH		= SERVICE_TEMPLATES_DIR + "java-service-api";
	private static final String		SERVICE_IMPL_PATH		= SERVICE_TEMPLATES_DIR + "java-service-impl";
	private static final String		SERVICE_CONSUMER_PATH	= SERVICE_TEMPLATES_DIR + "java-service-consumer";
	private static final String		SERVICE_API_HELPPATH	= SERVICE_TEMPLATES_DIR + "javaservice.xml";
	private BuiltInServiceTemplate	apiTemplate;
	private BuiltInServiceTemplate	implTemplate;
	private BuiltInServiceTemplate	consumerTemplate;

	public NewBndJavaServiceWizard(NewBndServiceWizardPageOne pageOne, NewJavaProjectWizardPageTwo pageTwo) {
		super(pageOne, pageTwo, null);
		this.apiTemplate = new BuiltInServiceTemplate("java-api", SERVICE_API_PATH);
		this.apiTemplate.setHelpPath(SERVICE_API_HELPPATH);
		this.implTemplate = new BuiltInServiceTemplate("java-impl", SERVICE_IMPL_PATH);
		this.consumerTemplate = new BuiltInServiceTemplate("java-consumer", SERVICE_CONSUMER_PATH);
	}

	@Override
	protected Template getServiceApiTemplate() {
		return this.apiTemplate;
	}

	@Override
	protected Template getServiceImplTemplate() {
		return this.implTemplate;
	}

	@Override
	protected Template getServiceConsumerTemplate() {
		return this.consumerTemplate;
	}
}
