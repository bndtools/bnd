package org.bndtools.templating.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public class ObjectClassDefinitionImpl implements ObjectClassDefinition {
	
	private final String name;
	private final String description;
	private final URI iconUri;
	
	private final List<AttributeDefinition> reqdAttribs = new ArrayList<>();
	private final List<AttributeDefinition> optAttribs = new ArrayList<>();

	public ObjectClassDefinitionImpl(String name, String description, URI iconUri) {
		this.name = name;
		this.description = description;
		this.iconUri = iconUri;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getID() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public AttributeDefinition[] getAttributeDefinitions(int filter) {
		switch (filter) {
		case OPTIONAL:
			return optAttribs.toArray(new AttributeDefinition[optAttribs.size()]);
		case REQUIRED:
			return reqdAttribs.toArray(new AttributeDefinition[reqdAttribs.size()]);
		case ALL:
			List<AttributeDefinition> combined = new ArrayList<>(optAttribs.size() + reqdAttribs.size());
			combined.addAll(reqdAttribs);
			combined.addAll(optAttribs);
			return combined.toArray(new AttributeDefinition[combined.size()]);
		default:
			throw new IllegalArgumentException(String.format("Unexpected filter value %d in getAttributeDefinitions", filter));
		}
	}

	@Override
	public InputStream getIcon(int size) throws IOException {
		return iconUri != null ? iconUri.toURL().openStream() : null;
	}
	
	public void addAttribute(AttributeDefinition attr, boolean required) {
		List<AttributeDefinition> target = required ? reqdAttribs : optAttribs;
		target.add(attr);
	}

}
