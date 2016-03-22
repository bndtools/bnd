package org.bndtools.core.templates.enroute;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.StringResource;
import org.bndtools.templating.Template;
import org.bndtools.templating.util.AttributeDefinitionImpl;
import org.bndtools.templating.util.ObjectClassDefinitionImpl;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.properties.Document;
import bndtools.Plugin;

public class EnrouteProjectTemplate implements Template, IExecutableExtension {

    private static final String PROP_PROJECT_NAME = "projectName";
    private static final String PROP_SRC_DIR = "srcDir";
    private static final String PROP_TEST_SRC_DIR = "testSrcDir";
    private static final String PROP_BASE_PACKAGE_NAME = "basePackageName";
    private static final String PROP_BASE_PACKAGE_DIR = "basePackageDir";

    static Pattern LAST_PART = Pattern.compile(".*\\.([^.]+)");
    static Pattern SKIP = Pattern.compile("\\.classpath|\\.project");
    static Pattern TOP_LEVEL = Pattern.compile("^(?:(?:com|biz|org|net|uk.co|gnu|gov|mil|[a-z][a-z]|info|name)\\.)(.*)", Pattern.CASE_INSENSITIVE);

    private static final Map<String,String> parameters = new HashMap<>();

    static {
        parameters.put(PROP_SRC_DIR, "Source Directory");
        parameters.put(PROP_TEST_SRC_DIR, "Test Source Directory");
        parameters.put(PROP_BASE_PACKAGE_NAME, "Base Java Package");
        parameters.put(PROP_BASE_PACKAGE_DIR, "Base Java Package Directory");
    }

    private String name;
    private String category;
    private String description;
    private int priority = 0;

    private Bundle bundle;
    private URI iconUri = null;
    private URI docUri = null;

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
        name = config.getAttribute("name");
        category = config.getAttribute("category");
        if (category == null)
            category = "Uncategorised";
        description = String.format("from %s (installed plug-in)", config.getContributor().getName());

        RegistryContributor contributor = (RegistryContributor) config.getContributor();
        long bundleId = Long.parseLong(contributor.getActualId());
        bundle = Plugin.getDefault().getBundleContext().getBundle(bundleId);
        try {
            iconUri = pathToURI(bundle, config.getAttribute("icon"));
            docUri = pathToURI(bundle, config.getAttribute("doc"));
        } catch (URISyntaxException e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error processing extension attributes.", e));
        }

        String priorityStr = config.getAttribute("priority");
        if (priorityStr != null)
            priority = Integer.parseInt(priorityStr);
    }

    private URI pathToURI(Bundle b, String path) throws URISyntaxException {
        if (path == null)
            return null;

        URL url = b.getEntry(path);
        if (url == null)
            return null;

        return url.toURI();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortDescription() {
        return description != null ? description : name;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public URI getIcon() {
        return iconUri;
    }

    @Override
    public URI getHelpContent() {
        return docUri;
    }

    @Override
    public int getRanking() {
        return priority;
    }

    @Override
    public Version getVersion() {
        return bundle.getVersion();
    }

    @Override
    public ObjectClassDefinition getMetadata() throws Exception {
        return getMetadata(new NullProgressMonitor());
    }

    @Override
    public ObjectClassDefinition getMetadata(IProgressMonitor monitor) throws Exception {
        ObjectClassDefinitionImpl ocd = new ObjectClassDefinitionImpl(name, description, iconUri);
        for (Entry<String,String> entry : parameters.entrySet())
            ocd.addAttribute(new AttributeDefinitionImpl(entry.getKey(), entry.getValue(), 0, AttributeDefinition.STRING), true);
        return ocd;
    }

    private static String param(String name, Map<String,List<Object>> params) {
        List<Object> list = params.get(name);
        if (list == null || list.isEmpty())
            throw new IllegalArgumentException("Missing required parameter: " + name);

        Object object = list.get(0);
        if (object instanceof String)
            return (String) object;

        throw new IllegalArgumentException(String.format("Unexpected type for parameter '%s': expected String, found %s", name, object != null ? object.getClass().getName() : "<null>"));
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> parameters) throws Exception {
        return generateOutputs(parameters, new NullProgressMonitor());
    }

    @Override
    public ResourceMap generateOutputs(Map<String,List<Object>> params, IProgressMonitor monitor) throws Exception {

        String projectName = param(PROP_PROJECT_NAME, params);
        String pkg = param(PROP_BASE_PACKAGE_NAME, params);
        String srcDir = param(PROP_SRC_DIR, params);
        String testSrcDir = param(PROP_TEST_SRC_DIR, params);

        ResourceMap resources = new ResourceMap();

        resources.put("bnd.bnd", generateBndFile(projectName, pkg));
        generateSources(resources, projectName, pkg, srcDir, testSrcDir);

        return resources;
    }

    private Resource generateBndFile(String projectName, String pkg) {
        BndEditModel model = new BndEditModel();

        model.setPrivatePackages(Arrays.asList(new String[] {
                pkg + ".provider"
        }));
        model.setExportedPackages(Arrays.asList(new ExportedPackage(projectName + ".api", new Attrs())));

        model.setBundleDescription("${warning:please explain what this bundle does}");
        model.setBundleVersion("1.0.0.${tstamp}");

        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;
        tmp = model.getBuildPath();
        if (tmp != null)
            buildPath.addAll(tmp);

        Attrs attrs = new Attrs();
        attrs.put("version", "@1.0");
        buildPath.add(new VersionedClause("osgi.enroute.base.api", attrs));
        buildPath.add(new VersionedClause("osgi.enroute.base.junit", attrs));

        model.setBuildPath(buildPath);

        Document doc = new Document("");
        model.saveChangesTo(doc);
        StringResource bndBndResource = new StringResource(doc.get());
        return bndBndResource;
    }

    static Pattern API = Pattern.compile("(.*\\.([^.]+))\\.api");
    static Pattern PROVIDER = Pattern.compile("(.*\\.([^.]+))\\.(provider|adapter)");
    static Pattern TEST = Pattern.compile("(.*\\.([^.]+))\\.test");
    static Pattern APPLICATION = Pattern.compile("(.*\\.([^.]+))\\.(app|webapp|application)");
    static Pattern WEBRESOURCE = Pattern.compile("(.*\\.([^.]+))\\.(resource|webresource)");
    static Pattern EXAMPLE = Pattern.compile("(.*\\.([^.]+))\\.example");
    static Pattern TUTORIAL_BASE = Pattern.compile("tutorial.base");
    static Pattern UNKNOWN = Pattern.compile(".*\\.([^.]+)");

    private void generateSources(ResourceMap resourceMap, String projectName, String basePackageName, String srcDir, String testSrcDir) {
        String stem;
        String type;
        String pid;

        Matcher m = API.matcher(projectName);
        if (m.matches()) {
            pid = m.group(1);
            stem = m.group(2);
            type = "_api_";
        } else {
            m = TUTORIAL_BASE.matcher(projectName);
            if (m.matches()) {
                pid = "tutorial.base";
                stem = "Speaker";
                type = "_tutorialbase_";
            } else {
                m = PROVIDER.matcher(projectName);
                if (m.matches()) {
                    pid = m.group(1);
                    stem = m.group(2);
                    type = "_provider_";
                } else {
                    m = TEST.matcher(projectName);
                    if (m.matches()) {
                        pid = m.group(1);
                        stem = m.group(2);
                        type = "_test_";
                    } else {
                        m = APPLICATION.matcher(projectName);
                        if (m.matches()) {
                            pid = m.group(1);
                            stem = m.group(2);
                            type = "_application_";
                        } else {
                            m = WEBRESOURCE.matcher(projectName);
                            if (m.matches()) {
                                pid = m.group(1);
                                stem = m.group(2);
                                type = "_webresource_";
                            } else {
                                m = EXAMPLE.matcher(projectName);
                                if (m.matches()) {
                                    pid = m.group(1);
                                    stem = m.group(2);
                                    basePackageName = stem;
                                    type = "_example_";
                                } else {
                                    m = UNKNOWN.matcher(projectName);
                                    if (m.matches()) {
                                        stem = m.group(1);
                                        pid = projectName;
                                        type = "_example_";
                                    } else {
                                        stem = projectName;
                                        pid = projectName;
                                        type = "_example_";
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stem = Character.toUpperCase(stem.charAt(0)) + stem.substring(1);

        String pkgPath = toBinaryPackage(basePackageName);

        Map<String,String> regex = new LinkedHashMap<String,String>();
        regex.put("_stem_", stem);
        regex.put("_STEM_", stem.toUpperCase());
        regex.put("_type_", type);
        regex.put("_package_", pkgPath);
        regex.put("_packagepath_", pkgPath);
        regex.put("_example_", projectName);
        regex.put("_api_", projectName);
        regex.put("_provider_", projectName);
        regex.put("_tutorialbase_", projectName);
        regex.put("_test_", projectName);
        regex.put("_application_", projectName);
        regex.put("_webresource_", projectName);
        regex.put("_unknown_", projectName);
        regex.put("_cmd_", toCmd(stem));
        regex.put("_project_", projectName);
        regex.put("_project(\\.[a-zA-Z]+)_", projectName);
        regex.put("_PROJECT_", toPROJECT(projectName));
        regex.put("_pid_", pid);
        regex.put("_src_", srcDir);
        regex.put("_test_", testSrcDir);

        copy(resourceMap, "/enroute/osgi.enroute.template/" + type, regex);
    }

    private String toBinaryPackage(String pkg) {
        StringBuilder sb = new StringBuilder(pkg.toLowerCase());
        boolean first = true;
        for (int i = 0; i < sb.length(); i++) {
            char ch = sb.charAt(i);
            if (ch == '.' || ch == '/') {
                sb.replace(i, i + 1, "/");
                continue;
            }

            if (!Character.isJavaIdentifierPart(ch)) {
                sb.delete(i, i + i);
                i--;
                continue;
            }

            if (first) {
                first = false;
                if (!Character.isJavaIdentifierStart(ch)) {
                    sb.delete(i, i + i);
                    i--;
                    continue;
                }
            }

            if (ch == '-') {
                sb.replace(i, i + 1, "_");
                continue;
            }
        }
        return sb.toString();
    }

    private String toPROJECT(String projectName) {
        String name = TOP_LEVEL.matcher(projectName).replaceFirst("");
        return name.toUpperCase().replace('.', ' ');
    }

    private String toCmd(String stem) {
        StringBuilder sb = new StringBuilder(stem.toLowerCase());

        outer: while (sb.length() > 7) {
            for (int i = sb.length() - 1; i >= 1; i--) {
                char c = sb.charAt(i);
                if ("aeiouy".indexOf(c) >= 0) {
                    sb.delete(i, i + 1);
                    continue outer;
                }
            }
            sb.delete(7, sb.length());
            break;
        }

        return sb.toString();
    }

    protected void copy(ResourceMap resourceMap, String prefix, Map<String,String> regex) {
        List<String> paths = new ArrayList<String>();

        Map<String,URL> resources = new HashMap<String,URL>();

        getPaths(bundle, prefix, paths);
        for (String path : paths) {

            URL resource = bundle.getResource(path);
            String out = path.substring(prefix.length());

            if (out.equals("src"))
                out = regex.get("_src_");
            else if (out.equals("test"))
                out = regex.get("_test_");
            else if (out.equals("bin") || out.equals("bin_test") || out.equals("generated"))
                continue;

            //
            // Paths are replaced where a package a path, using a slash
            //

            out = replace(out, regex);
            resources.put(out, resource);
        }

        //
        // Content should however, use the dotted variation of package
        //

        String replace = regex.get("_package_").replace('/', '.');
        regex.put("_package_", replace);

        for (Entry<String,URL> e : resources.entrySet()) {
            RegexReplacingResource resource = new RegexReplacingResource(e.getValue(), regex, "UTF-8");
            resourceMap.put(e.getKey(), resource);

        }
    }

    private void getPaths(Bundle bundle, String prefix, List<String> set) {
        Enumeration<String> paths = bundle.getEntryPaths(prefix);
        while (paths.hasMoreElements()) {
            String path = paths.nextElement();
            if (path.endsWith("/"))
                getPaths(bundle, path, set);
            else
                set.add(path);
        }
    }

    protected String replace(String out, Map<String,String> regex) {
        String out_ = out;
        for (Entry<String,String> e : regex.entrySet()) {
            out_ = out_.replace(e.getKey(), e.getValue());
        }
        return out_;
    }

    @Override
    public void close() throws IOException {
        // nothing to do
    }

}
