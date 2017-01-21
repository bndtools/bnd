package osgi.enroute.bndtools.templates;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(id = "osgi.enroute.bndtools.templates.application", name = ApplTemplateMeta.NAME, description = ApplTemplateMeta.NAME)
public @interface ApplTemplateMeta {
	
	public static final String NAME = "enRoute Application Project Template";

	@AttributeDefinition(name = "Application Title", description = "Title of the application, to appear in the generated XML")
	String title() default "Example";

	@AttributeDefinition(name = "JPM Command", description = "Name of the JPM command")
	String jpmCommand();
}
