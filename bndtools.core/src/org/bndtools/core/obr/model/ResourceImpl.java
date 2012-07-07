/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.bndtools.core.obr.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.version.VersionTable;
import org.osgi.framework.Version;

public class ResourceImpl implements Resource {

    private final Map<String,Object> m_map = new HashMap<String,Object>();
    private final List<Capability> m_capList = new ArrayList<Capability>();
    private final List<Requirement> m_reqList = new ArrayList<Requirement>();
    private Repository m_repo;
    private Map<String,String> m_uris;
    private transient int m_hash;

    public ResourceImpl() {}

    @Override
    public boolean equals(Object o) {
        if (o instanceof Resource) {
            if (getSymbolicName() == null || getVersion() == null) {
                return this == o;
            }
            return getSymbolicName().equals(((Resource) o).getSymbolicName()) && getVersion().equals(((Resource) o).getVersion());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (m_hash == 0) {
            if (getSymbolicName() == null || getVersion() == null) {
                m_hash = super.hashCode();
            } else {
                m_hash = getSymbolicName().hashCode() ^ getVersion().hashCode();
            }
        }
        return m_hash;
    }

    public Repository getRepository() {
        return m_repo;
    }

    public void setRepository(Repository repository) {
        this.m_repo = repository;
    }

    public Map<String,Object> getProperties() {
        convertURIs();
        return m_map;
    }

    public String getPresentationName() {
        return (String) m_map.get(PRESENTATION_NAME);
    }

    public String getSymbolicName() {
        return (String) m_map.get(SYMBOLIC_NAME);
    }

    public String getId() {
        return (String) m_map.get(ID);
    }

    public Version getVersion() {
        Version v = (Version) m_map.get(VERSION);
        v = (v == null) ? Version.emptyVersion : v;
        return v;
    }

    public String getURI() {
        convertURIs();
        return (String) m_map.get(Resource.URI);
    }

    public Long getSize() {
        return ((Long) m_map.get(Resource.SIZE));
    }

    public Requirement[] getRequirements() {
        return m_reqList.toArray(new Requirement[m_reqList.size()]);
    }

    public void addRequire(Requirement req) {
        m_reqList.add(req);
    }

    public Capability[] getCapabilities() {
        return m_capList.toArray(new Capability[m_capList.size()]);
    }

    public void addCapability(Capability cap) {
        m_capList.add(cap);
    }

    public String[] getCategories() {
        @SuppressWarnings("unchecked")
        List<String> catList = (List<String>) m_map.get(CATEGORY);
        if (catList == null) {
            return new String[0];
        }
        return catList.toArray(new String[catList.size()]);
    }

    public void addCategory(String category) {
        @SuppressWarnings("unchecked")
        List<String> catList = (List<String>) m_map.get(CATEGORY);
        if (catList == null) {
            catList = new ArrayList<String>();
            m_map.put(CATEGORY, catList);
        }
        catList.add(category);
    }

    public boolean isLocal() {
        return false;
    }

    /**
     * Default setter method when setting parsed data from the XML file.
     **/
    public Object put(Object key, Object value) {
        put(key.toString(), value.toString(), null);
        return null;
    }

    public void put(String key, String value, String type) {
        key = key.toLowerCase();
        m_hash = 0;
        if (Property.URI.equals(type) || URI.equals(key)) {
            if (m_uris == null) {
                m_uris = new HashMap<String,String>();
            }
            m_uris.put(key, value);
        } else if (Property.VERSION.equals(type) || VERSION.equals(key)) {
            m_map.put(key, VersionTable.getVersion(value));
        } else if (Property.LONG.equals(type) || SIZE.equals(key)) {
            m_map.put(key, Long.valueOf(value));
        } else if (Property.SET.equals(type) || CATEGORY.equals(key)) {
            StringTokenizer st = new StringTokenizer(value, ",");
            Set<String> s = new HashSet<String>();
            while (st.hasMoreTokens()) {
                s.add(st.nextToken().trim());
            }
            m_map.put(key, s);
        } else {
            m_map.put(key, value);
        }
    }

    private void convertURIs() {
        if (m_uris != null) {
            for (Iterator<String> it = m_uris.keySet().iterator(); it.hasNext();) {
                String key = it.next();
                String val = m_uris.get(key);
                m_map.put(key, resolveUri(val));
            }
            m_uris = null;
        }
    }

    private String resolveUri(String uri) {
        try {
            if (m_repo != null && m_repo.getURI() != null) {
                return new URI(m_repo.getURI()).resolve(uri).toString();
            }
        } catch (Throwable t) {}
        return uri;
    }

    @Override
    public String toString() {
        return getId();
    }
}