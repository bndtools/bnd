package org.bndtools.templating.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.metatype.AttributeDefinition;

public class ObjectClassDefinitionImpl extends BaseOCD {

    private final List<AttributeDefinition> reqdAttribs = new ArrayList<>();
    private final List<AttributeDefinition> optAttribs = new ArrayList<>();

    public ObjectClassDefinitionImpl(String name, String description, URI iconUri) {
        super(name, description, iconUri);
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions(int filter) {
        switch (filter) {
            case OPTIONAL :
                return optAttribs.toArray(new AttributeDefinition[0]);
            case REQUIRED :
                return reqdAttribs.toArray(new AttributeDefinition[0]);
            case ALL :
                List<AttributeDefinition> combined = new ArrayList<>(optAttribs.size() + reqdAttribs.size());
                combined.addAll(reqdAttribs);
                combined.addAll(optAttribs);
                return combined.toArray(new AttributeDefinition[0]);
            default :
                throw new IllegalArgumentException(String.format("Unexpected filter value %d in getAttributeDefinitions", filter));
        }
    }

    public void addAttribute(AttributeDefinition attr, boolean required) {
        List<AttributeDefinition> target = required ? reqdAttribs : optAttribs;
        target.add(attr);
    }

}
