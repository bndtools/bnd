package org.bndtools.core.templates.enroute;

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

import org.bndtools.api.BndProjectResource;
import org.bndtools.api.IBndProject;
import org.bndtools.api.IProjectTemplate;
import org.bndtools.api.ProjectPaths;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;

public class ProjectTemplate implements IProjectTemplate {
    static Pattern LAST_PART = Pattern.compile(".*\\.([^.]+)");
    static Pattern SKIP = Pattern.compile("\\.classpath|\\.project");
    static Pattern TOP_LEVEL = Pattern.compile("^(?:(?:com|biz|org|net|uk.co|gnu|gov|mil|[a-z][a-z]|info|name)\\.)(.*)", Pattern.CASE_INSENSITIVE);

    @Override
    public void modifyInitialBndModel(BndEditModel model, String projectName, ProjectPaths projectPaths) {

        String pkg = projectName.replace('/', '.');

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
    }

    static Pattern API = Pattern.compile("(.*\\.([^.]+))\\.api");
    static Pattern PROVIDER = Pattern.compile("(.*\\.([^.]+))\\.(provider|adapter)");
    static Pattern TEST = Pattern.compile("(.*\\.([^.]+))\\.test");
    static Pattern APPLICATION = Pattern.compile("(.*\\.([^.]+))\\.(app|webapp|application)");
    static Pattern EXAMPLE = Pattern.compile("(.*\\.([^.]+))\\.example");
    static Pattern TUTORIAL_BASE = Pattern.compile("tutorial.base");
    static Pattern UNKNOWN = Pattern.compile(".*\\.([^.]+)");

    @Override
    public void modifyInitialBndProject(IBndProject project, String projectName, ProjectPaths projectPaths) {
        String pkg = projectName;
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
                            m = EXAMPLE.matcher(projectName);
                            if (m.matches()) {
                                pid = m.group(1);
                                stem = m.group(2);
                                pkg = stem;
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

        stem = Character.toUpperCase(stem.charAt(0)) + stem.substring(1);

        String src = projectPaths.getSrc();
        String testsrc = projectPaths.getTestSrc();

        String pkgPath = toBinaryPackage(pkg);

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
        regex.put("_unknown_", projectName);
        regex.put("_cmd_", toCmd(stem));
        regex.put("_project_", projectName);
        regex.put("_project(\\.[a-zA-Z]+)_", projectName);
        regex.put("_PROJECT_", toPROJECT(projectName));
        regex.put("_pid_", pid);
        regex.put("_src_", src);
        regex.put("_test_", testsrc);

        copy(project, "/enroute/osgi.enroute.template/" + type, regex);
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

    protected void copy(IBndProject project, String prefix, Map<String,String> regex) {
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        assert bundle != null;

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
            BndProjectResource bpr = new BndProjectResource(e.getValue(), regex);
            project.addResource(e.getKey(), bpr);
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
    public boolean enableTestSourceFolder() {
        return true;
    }
}
