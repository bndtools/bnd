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
package org.apache.felix.metatype;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import bndtools.Plugin;

/**
 * The <code>MetaDataReader</code> provides two methods to read meta type documents according to the MetaType schema
 * (105.8 XML Schema). The {@link #parse(URL)} and {@link #parse(InputStream)} methods may be called multiple times to
 * parse such documents.
 * <p>
 * While reading the XML document java objects are created to hold the data. These objects are created by factory
 * methods. Users of this may extend this class by overwriting the the factory methods to create specialized versions.
 * One notable use of this is the extension of the {@link AD} class to overwrite the {@link AD#validate(String)} method.
 * In this case, the {@link #createAD()} method would be overwritten to return an instance of the extending class.
 * <p>
 * This class is not thread safe. Using instances of this class in multiple threads concurrently is not supported and
 * will fail.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({
    "rawtypes"
})
public class MetaDataReader {

    /**
     * The initial XML Namespace for Metatype 1.1 descriptors. This has been replaced by the v1.1.0 namespace in the
     * Compendium Specification 4.2. We still have to support this namespace for backwards compatibility.
     */
    static final String NAMESPACE_1_0 = "http://www.osgi.org/xmlns/metatype/v1.0.0";

    /**
     * The XML Namespace for Metatype 1.1 descriptors.
     */
    static final String NAMESPACE_1_1 = "http://www.osgi.org/xmlns/metatype/v1.1.0";

    /**
     * The XML Namespace for Metatype 1.2 descriptors.
     */
    static final String NAMESPACE_1_2 = "http://www.osgi.org/xmlns/metatype/v1.2.0";

    /**
     * The XML Namespace for Metatype 1.3 descriptors.
     */
    static final String NAMESPACE_1_3 = "http://www.osgi.org/xmlns/metatype/v1.3.0";

    /** The XML parser used to read the XML documents */
    private final KXmlParser parser = new KXmlParser();
    private String namespace = NAMESPACE_1_0;

    private URL documentURL;

    /** Sets of attributes belonging to XML elements. */
    private static final Set<String> AD_ATTRIBUTES = new HashSet<String>(Arrays.asList(new String[] {
        "name", "description", "id", "type", "cardinality", "min", "max", "default", "required"
    }));
    private static final Set<String> ATTRIBUTE_ATTRIBUTES = new HashSet<String>(Arrays.asList(new String[] {
        "adref", "content"
    }));
    private static final Set<String> DESIGNATE_ATTRIBUTES = new HashSet<String>(Arrays.asList(new String[] {
        "pid", "factoryPid", "bundle", "optional", "merge"
    }));
    private static final Set<String> DESIGNATEOBJECT_ATTRIBUTES = new HashSet<String>(Arrays.asList(new String[] {
        "ocdref"
    }));
    private static final Set<String> METADATA_ATTRIBUTES = new HashSet<String>(Arrays.asList(new String[] {
        "localization"
    }));
    private static final Set<String> OCD_ATTRIBUTES = new HashSet<String>(Arrays.asList(new String[] {
        "name", "description", "id"
    }));

    /**
     * Parses the XML document provided by the <code>url</code>. The XML document must be at the beginning of the stream
     * contents.
     * <p>
     * This method is almost identical to <code>return parse(url.openStream());</code> but also sets the string
     * representation of the URL as a location helper for error messages.
     *
     * @param url The <code>URL</code> providing access to the XML document.
     * @return A {@link MetaData} providing access to the raw contents of the XML document.
     * @throws IOException If an I/O error occurs accessing the stream or parsing the XML document.
     */
    public MetaData parse(URL url) throws IOException {
        this.documentURL = url;
        try (InputStream ins = url.openStream()) {

            this.parser.setProperty("http://xmlpull.org/v1/doc/properties.html#location", url.toString());
            MetaData md = parse(ins);
            if (md != null) {
                md.setSource(url);
            }
            return md;
        } catch (XmlPullParserException e) {
            throw new IOException("XML parsing exception while reading metadata: " + e.getMessage());
        } finally {
            this.documentURL = null;
        }
    }

    /**
     * Parses the XML document in the given input stream.
     * <p>
     * This method starts reading at the current position of the input stream and returns immediately after completely
     * reading a single meta type document. The stream is not closed by this method.
     *
     * @param ins The <code>InputStream</code> providing the XML document
     * @return A {@link MetaData} providing access to the raw contents of the XML document.
     * @throws IOException If an I/O error occurs accessing the stream or parsing the XML document.
     */
    public MetaData parse(InputStream ins) throws IOException {
        MetaData mti = null;
        try {
            this.parser.setFeature(KXmlParser.FEATURE_PROCESS_NAMESPACES, true);
            // set the parser input, use null encoding to force detection with <?xml?>
            this.parser.setInput(ins, null);

            int eventType = this.parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = this.parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if ("MetaData".equals(tagName)) {
                        mti = readMetaData();
                    } else {
                        ignoreElement();
                    }
                }
                eventType = this.parser.next();
            }
        } catch (XmlPullParserException e) {
            throw new IOException("XML parsing exception while reading metadata: " + e.getMessage());
        }

        return mti;
    }

    /**
     * Checks if this document has a meta type name space.
     *
     * @throws IOException when there the meta type name space is not valid
     */
    private void checkMetatypeNamespace() throws IOException {
        final String namespace = this.parser.getNamespace();
        if (namespace != null && !"".equals(namespace.trim())) {
            if (!NAMESPACE_1_0.equals(namespace) && !NAMESPACE_1_1.equals(namespace) && !NAMESPACE_1_2.equals(namespace) && !NAMESPACE_1_3.equals(namespace)) {
                throw new IOException("Unsupported Namespace: '" + namespace + "'");
            }
            this.namespace = namespace;
        }
    }

    private void readOptionalAttributes(OptionalAttributes entity, Set attributes) {
        int count = this.parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = this.parser.getAttributeName(i);
            if (!attributes.contains(name)) {
                String value = this.parser.getAttributeValue(i);
                entity.addOptionalAttribute(name, value);
            }
        }
    }

    private MetaData readMetaData() throws IOException, XmlPullParserException {
        checkMetatypeNamespace();

        MetaData mti = createMetaData();
        mti.setNamespace(this.namespace);
        mti.setLocalePrefix(getOptionalAttribute("localization"));

        readOptionalAttributes(mti, METADATA_ATTRIBUTES);

        int eventType = this.parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = this.parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("OCD".equals(tagName)) {
                    mti.addObjectClassDefinition(readOCD());
                } else if ("Designate".equals(tagName)) {
                    mti.addDesignate(readDesignate());
                } else {
                    ignoreElement();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("MetaData".equals(tagName)) {
                    break;
                }

                throw unexpectedElement(tagName);
            }
            eventType = this.parser.next();
        }

        return mti;
    }

    private OCD readOCD() throws IOException, XmlPullParserException {
        OCD ocd = createOCD();
        ocd.setId(getRequiredAttribute("id"));
        ocd.setName(getRequiredAttribute("name"));
        ocd.setDescription(getOptionalAttribute("description"));

        readOptionalAttributes(ocd, OCD_ATTRIBUTES);

        int eventType = this.parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = this.parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("AD".equals(tagName)) {
                    ocd.addAttributeDefinition(readAD());
                } else if ("Icon".equals(tagName)) {
                    String res = getRequiredAttribute("resource");
                    String sizeString = getRequiredAttribute("size");
                    try {
                        Integer size = Integer.decode(sizeString);
                        ocd.addIcon(size, res);
                    } catch (NumberFormatException nfe) {
                        // Activator.log(LogService.LOG_DEBUG, "readOCD: Icon size '" + sizeString + "' is not a valid
                        // number");
                    }
                } else {
                    ignoreElement();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("OCD".equals(tagName)) {
                    if (getNamespaceVersion() < 12 && ocd.getIcons() != null && ocd.getIcons()
                        .size() > 1) {
                        // Only one icon is allowed in versions 1.0 & 1.1...
                        throw unexpectedElement("Icon");
                    }
                    if (getNamespaceVersion() < 13 && ocd.getAttributeDefinitions() == null) {
                        // Need at least one AD in versions 1.0, 1.1 & 1.2...
                        logMissingElement("AD");
                        ocd = null;
                    }
                    break;
                } else if (!"Icon".equals(tagName)) {
                    throw unexpectedElement(tagName);
                }
            }
            eventType = this.parser.next();
        }

        return ocd;
    }

    private Designate readDesignate() throws IOException, XmlPullParserException {
        final String pid = getOptionalAttribute("pid");
        final String factoryPid = getOptionalAttribute("factoryPid");
        if (pid == null && factoryPid == null) {
            missingAttribute("pid or factoryPid");
        }

        Designate designate = this.createDesignate();
        designate.setPid(pid);
        designate.setFactoryPid(factoryPid);
        designate.setBundleLocation(getOptionalAttribute("bundle"));
        designate.setOptional(getOptionalAttribute("optional", false));
        designate.setMerge(getOptionalAttribute("merge", false));

        readOptionalAttributes(designate, DESIGNATE_ATTRIBUTES);

        int eventType = this.parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = this.parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("Object".equals(tagName)) {
                    if (designate.getObject() != null) {
                        // Only 1 Object is allowed...
                        throw unexpectedElement(tagName);
                    }

                    designate.setObject(readObject());
                } else {
                    this.ignoreElement();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("Designate".equals(tagName)) {
                    if (designate.getObject() == null) {
                        // Exactly 1 Object is allowed...
                        logMissingElement("Object");
                        designate = null;
                    }
                    break;
                }

                throw unexpectedElement(tagName);
            }
            eventType = this.parser.next();
        }

        return designate;
    }

    private AD readAD() throws IOException, XmlPullParserException {
        AD ad = createAD();
        ad.setID(getRequiredAttribute("id"));
        ad.setName(getOptionalAttribute("name"));
        ad.setDescription(getOptionalAttribute("description"));
        ad.setType(getRequiredAttribute("type"));
        ad.setCardinality(getOptionalAttribute("cardinality", 0));
        ad.setMin(getOptionalAttribute("min"));
        ad.setMax(getOptionalAttribute("max"));
        ad.setRequired(getOptionalAttribute("required", true));
        String dfltValue = getOptionalAttribute("default");

        readOptionalAttributes(ad, AD_ATTRIBUTES);

        Map<String, String> options = new LinkedHashMap<String, String>();
        int eventType = this.parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = this.parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("Option".equals(tagName)) {
                    String value = getRequiredAttribute("value");
                    String label = getRequiredAttribute("label");
                    options.put(value, label);
                } else {
                    ignoreElement();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("AD".equals(tagName)) {
                    break;
                } else if (!"Option".equals(tagName)) {
                    throw unexpectedElement(tagName);
                }
            }
            eventType = this.parser.next();
        }

        ad.setOptions(options);

        // set value as late as possible to force an options check (FELIX-3884, FELIX-4665)...
        if (dfltValue != null) {
            ad.setDefaultValue(dfltValue);
        }

        return ad;
    }

    private DesignateObject readObject() throws IOException, XmlPullParserException {
        DesignateObject oh = createDesignateObject();
        oh.setOcdRef(getRequiredAttribute("ocdref"));

        readOptionalAttributes(oh, DESIGNATEOBJECT_ATTRIBUTES);

        int eventType = this.parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = this.parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("Attribute".equals(tagName)) {
                    oh.addAttribute(readAttribute());
                } else {
                    ignoreElement();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("Object".equals(tagName)) {
                    break;
                }

                throw unexpectedElement(tagName);
            }
            eventType = this.parser.next();
        }

        return oh;
    }

    private Attribute readAttribute() throws IOException, XmlPullParserException {
        Attribute ah = createAttribute();
        ah.setAdRef(getRequiredAttribute("adref"));
        ah.addContent(getOptionalAttribute("content"), true);

        readOptionalAttributes(ah, ATTRIBUTE_ATTRIBUTES);

        int eventType = this.parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = this.parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("Value".equals(tagName)) {
                    ah.addContent(this.parser.nextText(), false);
                    eventType = this.parser.getEventType();
                } else {
                    ignoreElement();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("Attribute".equals(tagName)) {
                    break;
                } else if (!"Value".equals(tagName)) {
                    throw unexpectedElement(tagName);
                }
            }
            eventType = this.parser.next();
        }

        return ah;
    }

    // ---------- Attribute access helper --------------------------------------

    private String getRequiredAttribute(String attrName) throws XmlPullParserException {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        if (attrVal != null) {
            return attrVal;
        }
        // fail if value is missing
        throw missingAttribute(attrName);
    }

    private String getOptionalAttribute(String attrName) {
        return getOptionalAttribute(attrName, (String) null);
    }

    private String getOptionalAttribute(String attrName, String defaultValue) {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? attrVal : defaultValue;
    }

    private boolean getOptionalAttribute(String attrName, boolean defaultValue) {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        return (attrVal != null) ? "true".equalsIgnoreCase(attrVal) : defaultValue;
    }

    private int getOptionalAttribute(String attrName, int defaultValue) {
        String attrVal = this.parser.getAttributeValue(null, attrName);
        if (attrVal != null && !"".equals(attrVal)) {
            try {
                return Integer.decode(attrVal)
                    .intValue();
            } catch (NumberFormatException nfe) {
                // Activator.log(LogService.LOG_DEBUG, "getOptionalAttribute: Value '" + attrVal + "' of attribute " +
                // attrName + " is not a valid number. Using default value " + defaultValue);
            }
        }
        // fallback to default
        return defaultValue;
    }

    private int getNamespaceVersion() {
        if (NAMESPACE_1_0.equals(this.namespace)) {
            return 10;
        } else if (NAMESPACE_1_1.equals(this.namespace)) {
            return 11;
        } else if (NAMESPACE_1_2.equals(this.namespace)) {
            return 12;
        } else if (NAMESPACE_1_3.equals(this.namespace)) {
            return 13;
        }
        // Undetermined...
        return Integer.MAX_VALUE;
    }

    // ---------- Error Handling support ---------------------------------------

    private void ignoreElement() throws IOException, XmlPullParserException {
        String ignoredElement = this.parser.getName();

        int depth = 0; // enable nested ignored elements
        int eventType = this.parser.next();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (ignoredElement.equals(this.parser.getName())) {
                    depth++;
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (ignoredElement.equals(this.parser.getName())) {
                    if (depth <= 0) {
                        return;
                    }

                    depth--;
                }
            }
            eventType = this.parser.next();
        }
    }

    private XmlPullParserException missingAttribute(String attrName) {
        String message = "Missing attribute " + attrName + " in element " + this.parser.getName();
        return new XmlPullParserException(message, this.parser, null);
    }

    private void logMissingElement(final String elementName) {
        String message = "Missing element " + elementName + " in element " + this.parser.getName();
        if (documentURL != null) {
            message = message + " : " + this.documentURL;
        }
        Plugin.getDefault()
            .getLog()
            .log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, null));
    }

    private XmlPullParserException unexpectedElement(String elementName) {
        String message = "Unexpected element " + elementName;
        return new XmlPullParserException(message, this.parser, null);
    }

    // ---------- Factory methods ----------------------------------------------

    /**
     * Creates a new {@link MetaData} object to hold the contents of the <code>MetaData</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected MetaData createMetaData() {
        return new MetaData();
    }

    /**
     * Creates a new {@link OCD} object to hold the contents of the <code>OCD</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected OCD createOCD() {
        return new OCD();
    }

    /**
     * Creates a new {@link AD} object to hold the contents of the <code>AD</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected AD createAD() {
        return new AD();
    }

    /**
     * Creates a new {@link DesignateObject} object to hold the contents of the <code>Object</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected DesignateObject createDesignateObject() {
        return new DesignateObject();
    }

    /**
     * Creates a new {@link Attribute} object to hold the contents of the <code>Attribute</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected Attribute createAttribute() {
        return new Attribute();
    }

    /**
     * Creates a new {@link Designate} object to hold the contents of the <code>Designate</code> element.
     * <p>
     * This method may be overwritten to return a customized extension.
     */
    protected Designate createDesignate() {
        return new Designate();
    }
}
