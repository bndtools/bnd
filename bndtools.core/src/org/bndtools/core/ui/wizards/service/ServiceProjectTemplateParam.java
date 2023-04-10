package org.bndtools.core.ui.wizards.service;

public enum ServiceProjectTemplateParam {

	SERVICE_NAME("serviceName"), 
	API_PACKAGE("api_package");

	private final String string;

	ServiceProjectTemplateParam(String string) {
		this.string = string;
	}

	public String getString() {
		return string;
	}

	public static String[] valueStrings() {
		ServiceProjectTemplateParam[] vals = values();
		String[] strings = new String[vals.length];
		for (int i = 0; i < vals.length; i++) {
			strings[i] = vals[i].getString();
		}
		return strings;
	}

}
