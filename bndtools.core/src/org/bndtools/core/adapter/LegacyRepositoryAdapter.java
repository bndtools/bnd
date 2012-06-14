package org.bndtools.core.adapter;

import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.Registry;
import aQute.bnd.service.ResolutionPhase;

/**
 * <p>
 * This class is used to adapt the old {@link OBRIndexProvider} interface to the new {@link IndexProvider} interface.
 * </p>
 * <p>
 * At some point the OBRIndexProvider interface changed from returning URLs to returning URIs. Some users will still
 * have old providers in their workspace returning URLs. Because of erasure, we can detect these incompatibilities at
 * runtime and convert on the fly.
 * </p>
 * 
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
@SuppressWarnings("deprecation")
public class LegacyRepositoryAdapter implements IndexProvider {

    private final OBRIndexProvider delegate;

    public static List<URI> convertToUriList(@SuppressWarnings("rawtypes") Collection rawIndexes) throws Exception {
        List<URI> result;

        if (rawIndexes == null || rawIndexes.isEmpty())
            result = Collections.emptyList();
        else {
            Object first = rawIndexes.iterator().next();
            if (first instanceof URI) {
                @SuppressWarnings("unchecked")
                Collection<URI> uris = rawIndexes;
                result = collectionToList(uris);
            } else if (first instanceof URL) {
                result = new ArrayList<URI>(rawIndexes.size());
                for (Object rawEntry : rawIndexes) {
                    URL url = (URL) rawEntry;
                    result.add(url.toURI());
                }
            } else {
                throw new IllegalArgumentException(MessageFormat.format("Index provider should return either URIs nor URLs, actual type was: {0}", first.getClass().getName()));
            }
        }

        return result;
    }

    static Set<ResolutionPhase> convertResolutionModes(@SuppressWarnings("rawtypes") Set modes) {
        Set<ResolutionPhase> result = EnumSet.noneOf(ResolutionPhase.class);

        for (Object entry : modes) {
            if (entry instanceof OBRResolutionMode) {
                OBRResolutionMode mode = (OBRResolutionMode) entry;
                switch (mode) {
                case build :
                    result.add(ResolutionPhase.build);
                    break;
                case runtime :
                    result.add(ResolutionPhase.runtime);
                }
            } else if (entry instanceof ResolutionPhase) {
                result.add((ResolutionPhase) entry);
            }
        }

        return result;
    }

    public static List<IndexProvider> findIndexProviderPlugins(Registry registry) {
        List<IndexProvider> result = new LinkedList<IndexProvider>();
        Map<IndexProvider,Object> identityMap = new IdentityHashMap<IndexProvider,Object>();

        // Add the IndexProvider plugins
        List<IndexProvider> plugins = registry.getPlugins(IndexProvider.class);
        for (final IndexProvider plugin : plugins) {
            identityMap.put(plugin, null);
            IndexProvider wrapper = new IndexProvider() {
                public Set<ResolutionPhase> getSupportedPhases() {
                    return plugin.getSupportedPhases();
                }

                public List<URI> getIndexLocations() throws Exception {
                    return convertToUriList(plugin.getIndexLocations());
                }

                @Override
                public String toString() {
                    return plugin.toString();
                }
            };
            result.add(wrapper);
        }

        // Wrap the OBRIndexProviders and add to the result, ONLY if they are not
        // already there (i.e. because they implement both interfaces)
        List<OBRIndexProvider> legacyPlugins = registry.getPlugins(OBRIndexProvider.class);
        for (final OBRIndexProvider legacyPlugin : legacyPlugins) {
            if (!identityMap.containsKey(legacyPlugin)) {
                IndexProvider wrapper = new IndexProvider() {
                    public Set<ResolutionPhase> getSupportedPhases() {
                        return convertResolutionModes(legacyPlugin.getSupportedModes());
                    }

                    public List<URI> getIndexLocations() throws Exception {
                        return convertToUriList(legacyPlugin.getOBRIndexes());
                    }

                    @Override
                    public String toString() {
                        return legacyPlugin.toString();
                    }
                };
                result.add(wrapper);
            }
        }

        return result;
    }

    public LegacyRepositoryAdapter(OBRIndexProvider delegate) {
        this.delegate = delegate;
    }

    public List<URI> getIndexLocations() throws Exception {
        List<URI> result;

        @SuppressWarnings("rawtypes")
        Collection rawIndexes = delegate.getOBRIndexes();
        if (rawIndexes.isEmpty())
            result = Collections.emptyList();
        else {
            Object first = rawIndexes.iterator().next();
            if (first instanceof URI) {
                @SuppressWarnings("unchecked")
                Collection<URI> uris = rawIndexes;
                result = collectionToList(uris);
            } else if (first instanceof URL) {
                result = new ArrayList<URI>(rawIndexes.size());
                for (Object rawEntry : rawIndexes) {
                    URL url = (URL) rawEntry;
                    result.add(url.toURI());
                }
            } else {
                throw new IllegalArgumentException(MessageFormat.format("Index provider should return either URIs nor URLs, actual type was: {0}", first.getClass().getName()));
            }
        }

        return result;
    }

    private static <T> List<T> collectionToList(Collection<T> collection) {
        if (collection instanceof List) {
            List<T> list = (List<T>) collection;
            return list;
        }
        return new ArrayList<T>(collection);
    }

    public Set<ResolutionPhase> getSupportedPhases() {
        Set<ResolutionPhase> result = EnumSet.noneOf(ResolutionPhase.class);

        Set<OBRResolutionMode> modes = delegate.getSupportedModes();
        for (OBRResolutionMode mode : modes) {
            switch (mode) {
            case build :
                result.add(ResolutionPhase.build);
                break;
            case runtime :
                result.add(ResolutionPhase.runtime);
            }
        }

        return result;
    }

}
