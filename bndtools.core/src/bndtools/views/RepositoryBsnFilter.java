package bndtools.views;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.libg.header.Attrs;
import aQute.libg.header.Parameters;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;
import bndtools.model.repo.ProjectBundle;
import bndtools.model.repo.RepositoryBundle;
import bndtools.model.repo.RepositoryBundleVersion;

public class RepositoryBsnFilter extends ViewerFilter {

    public static final String IMPORTED = "imported:";
    public static final String EXPORTED = "exported:";
    public static final String PACKAGE = "package:";

    public static final String WILDCARD = "*";

    private final String filterStr;
    private boolean imported;
    private boolean exported;
    private boolean searchPackage;

    public RepositoryBsnFilter(String filterStr) {
        if (filterStr != null) {
            if (filterStr.startsWith(IMPORTED)) {
                imported = true;
                searchPackage = true;
                filterStr = filterStr.substring(IMPORTED.length());
            } else if (filterStr.startsWith(EXPORTED)) {
                exported = true;
                searchPackage = true;
                filterStr = filterStr.substring(EXPORTED.length());
            } else if (filterStr.startsWith(PACKAGE)) {
                imported = true;
                exported = true;
                searchPackage = true;
                filterStr = filterStr.substring(PACKAGE.length());
            }
        }
        this.filterStr = filterStr;
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (searchPackage) {
            if (element instanceof RepositoryBundle) {
                RepositoryBundle bundle = (RepositoryBundle) element;

                try {
                    List<Version> versions = bundle.getRepo().versions(bundle.getBsn());
                    for (Version version : versions) {
                        RepositoryBundleVersion bundleVersion = new RepositoryBundleVersion(bundle, version);
                        if (filterMatch(bundleVersion)) {
                            return true;
                        }
                    }
                } catch (Exception e) {}
                return false;
            }
            if (element instanceof RepositoryBundleVersion) {
                RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
                return filterMatch(bundleVersion);
            }
        } else {
            String bsn = null;
            if (element instanceof RepositoryBundle) {
                bsn = ((RepositoryBundle) element).getBsn();
            } else if (element instanceof ProjectBundle) {
                bsn = ((ProjectBundle) element).getBsn();
            }
            if (bsn != null) {
                if (filterStr != null && filterStr.length() > 0 && bsn.toLowerCase().indexOf(filterStr) == -1) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean filterMatch(RepositoryBundleVersion bundleVersion) {

        // TODO: Get bundle metadata without instantiating Jar, can we use
        // Resource?
        File bundleFile = (File) bundleVersion.getAdapter(File.class);
        Manifest manifest;
        try {
            Jar jar = new Jar(bundleVersion.getBundle().getBsn(), bundleFile);
            manifest = jar.getManifest();
        } catch (Exception e) {
            return false;
        }
        if (manifest == null)
            return false;
        Attributes attribs = manifest.getMainAttributes();
        if (imported) {
            Parameters importsMap = new Parameters(attribs.getValue(Constants.IMPORT_PACKAGE));
            if (filterMatch(importsMap, filterStr)) {
                return true;
            }
        }
        if (exported) {
            Parameters exportsMap = new Parameters(attribs.getValue(Constants.EXPORT_PACKAGE));
            if (filterMatch(exportsMap, filterStr)) {
                return true;
            }
        }
        return false;

    }

    private static boolean filterMatch(Map<String,Attrs> headers, String filterStr) {

        Parameters search = new Parameters(filterStr, null);
        String searchPackage = null;
        String searchVersion = null;
        boolean versionFound = false;
        for (Map.Entry<String,Attrs> entry : search.entrySet()) {
            searchPackage = entry.getKey();
            searchVersion = entry.getValue().get("version");

            VersionRange version = null;
            if (searchVersion == null) {
                versionFound = true;
            } else {
                version = new VersionRange(searchVersion);
            }
            boolean wildcard = false;
            int index = searchPackage.indexOf(WILDCARD);
            if (index > -1) {
                wildcard = true;
                searchPackage = searchPackage.substring(0, index);
            }
            for (Entry<String,Attrs> headerEntry : headers.entrySet()) {
                String pkgName = headerEntry.getKey();
                if (!wildcard && pkgName.equals(searchPackage) || wildcard && pkgName.startsWith(searchPackage)) {
                    String pkgVersion = headerEntry.getValue().get("version");
                    VersionRange vr = new VersionRange("0.0.0");
                    if (pkgVersion != null) {
                        vr = new VersionRange(pkgVersion);
                    }
                    if (!versionFound && version != null) {
                        if (!vr.isRange() && version.includes(vr.getHigh())) {
                            versionFound = true;
                        } else if (vr.isRange() && version.isRange() && vr.includeHigh() == version.includeHigh() && vr.includeLow() == version.includeLow() && vr.getHigh().equals(version.getHigh()) && vr.getLow().equals(version.getLow())) {
                            versionFound = true;
                        } else if (vr.isRange() && vr.includes(version.getHigh())) {
                            versionFound = true;
                        }
                    }
                    if (versionFound) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }
}