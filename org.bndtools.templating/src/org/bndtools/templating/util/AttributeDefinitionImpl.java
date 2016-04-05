package org.bndtools.templating.util;

import org.osgi.service.metatype.AttributeDefinition;

public class AttributeDefinitionImpl implements AttributeDefinition {

    private final String name;
    private final String description;
    private final int cardinality;
    private final int type;

    private String[] defaultValue;

    public AttributeDefinitionImpl(String name, String description, int cardinality, int type) {
        this.name = name;
        this.description = description;
        this.cardinality = cardinality;
        this.type = type;
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
    public int getCardinality() {
        return cardinality;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String[] getOptionValues() {
        // TODO Not implemented
        return null;
    }

    @Override
    public String[] getOptionLabels() {
        // TODO Not implemented
        return null;
    }

    @Override
    public String validate(String value) {
        // TODO Not implemented
        return null;
    }

    @Override
    public String[] getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String[] defaultValue) {
        this.defaultValue = defaultValue;
    }

}
