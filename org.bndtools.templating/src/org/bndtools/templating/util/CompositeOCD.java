package org.bndtools.templating.util;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public class CompositeOCD extends BaseOCD {

    private final List<ObjectClassDefinition> delegates = new LinkedList<>();

    public CompositeOCD(String name, String description, URI iconUri, ObjectClassDefinition... delegates) {
        super(name, description, iconUri);
        this.delegates.addAll(Arrays.asList(delegates));
    }

    public void addDelegate(ObjectClassDefinition delegate) {
        delegates.add(delegate);
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions(int filter) {
        final Map<String, AttributeDefinition> ads = new LinkedHashMap<>();

        for (ObjectClassDefinition delegate : delegates) {
            AttributeDefinition[] entryAds = delegate.getAttributeDefinitions(filter);
            if (entryAds != null) {
                for (AttributeDefinition ad : entryAds) {
                    if (!ads.containsKey(ad.getID()))
                        ads.put(ad.getID(), ad);
                }
            }
        }
        return ads.values()
            .toArray(new AttributeDefinition[0]);
    }

}
