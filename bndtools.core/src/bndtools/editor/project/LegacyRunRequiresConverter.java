package bndtools.editor.project;

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class LegacyRunRequiresConverter implements Converter<Requirement,String> {

    public Requirement convert(String input) throws IllegalArgumentException {
        int index = input.indexOf(":");
        if (index < 0)
            throw new IllegalArgumentException("Invalid format for OBR requirement");

        String name = input.substring(0, index);
        String filter = input.substring(index + 1);

        Requirement req = new CapReqBuilder(name).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter).buildSyntheticRequirement();
        return req;
    }

}
