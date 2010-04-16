package aQute.junit.runtime;

import java.io.*;
import java.net.URL;
import java.util.Vector;
import org.osgi.framework.*;
import org.osgi.service.permissionadmin.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implements a permissionpolicy. It will tried to read a resource from the
 * bundle. This resource should have the following format:
 * 
 * <pre>
 * 
 *  
 *    '(' permission-class [ '&quot;' name-parameter '&quot;' [ '&quot;' action [ ',' action ] ... '&quot;' ] ] ')'
 *   Or
 *    '#' *
 *   
 *  
 * </pre>
 * 
 * Each valid line is translated into a PermissionInfo object and these objects
 * together form the permissions for a specific bundle. The class will also
 * attempt to read a file called "default.perm" from the current bundle that
 * will have the same format. This is used for the default permissions.
 * <p>
 * If there is no permission admin service, this class does nothing relevant.
 */
public class SimplePermissionPolicy implements SynchronousBundleListener {
    static final String DEFAULT_PERMISSION_RESOURCE = "default.perm";
    BundleContext       context;
    ServiceTracker      tracker;
    Vector              bundles;
    PermissionInfo[]    defaultPermissions;

    /**
     * Create a new permission policy. This will set the default permissions and
     * the permissions for this bundle (if the resource is present).
     */
    SimplePermissionPolicy(BundleContext context) throws Exception {
        this.context = context;
        bundles = new Vector();
        tracker = new ServiceTracker(context, PermissionAdmin.class.getName(),
                null);
        tracker.open();
        context.addBundleListener(this);
        PermissionAdmin permissionAdmin = (PermissionAdmin) tracker
                .getService();
        if (permissionAdmin == null) /* no permission admin service */
            return;
        // Set the default permissions.
        InputStream in = getClass().getResourceAsStream(
                DEFAULT_PERMISSION_RESOURCE);
        if (in != null) {
            PermissionInfo[] info = parse(in);
            permissionAdmin.setDefaultPermissions(info);
        } else {
//            System.out.println("default permission not found "
//                    + DEFAULT_PERMISSION_RESOURCE);
        }
        //
        // Set this bundles permissions.
        //
        Bundle self = context.getBundle();
        setPermissions(self);
    }

    /**
     * Sets the permissions of a bundle from a resource, if exists.
     */
    public void setPermissions(Bundle bundle) {
        PermissionAdmin permissionAdmin = (PermissionAdmin) tracker
                .getService();
        if (permissionAdmin == null) /* no permission admin service */{
            return;
        }
        PermissionInfo[] info = getPermissions(bundle);
        if ( info == null )
            info = defaultPermissions;
        
        if (info != null && info.length > 0) {
            bundles.addElement(bundles);
            permissionAdmin.setPermissions(bundle.getLocation(), info);
        } else 
          ; //  System.out.println("No permissions for " + bundle.getLocation() );

    }

    /**
     * Get the resource and parse it into PermissionInfo objects.
     */
    public PermissionInfo[] getPermissions(Bundle bundle) {        
        URL    url = bundle.getEntry("/META-INF/permissions.perm");
        if (url == null)
            url = bundle.getEntry("/META-INF/permissions.perm".toUpperCase());

        PermissionInfo[] info = null;
        if (url != null)
            try {
                InputStream in = url.openStream();
                info = parse(in);
            } catch (IOException e) {
                System.out
                        .println("Unable to read permission info for bundle  "
                                + bundle.getLocation() + " " + e);
            }
        return info;
    }

    /**
     * Parse a permission info file.
     */
    public PermissionInfo[] parse(InputStream in) throws IOException {
        PermissionInfo[] info = null;
        if (in != null) {
            Vector permissions = new Vector();
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if ((line.length() == 0) || line.startsWith("#")
                            || line.startsWith("//")) /* comments */
                        continue;
                    try {
                        permissions.addElement(new PermissionInfo(line));
                    } catch (IllegalArgumentException iae) {
                        /* incorrectly encoded permission */
                        System.out.println("Permission incorrectly encoded: "
                                + line + " " + iae);
                    }
                }
            } finally {
                in.close();
            }
            int size = permissions.size();
            if (size > 0) {
                info = new PermissionInfo[size];
                permissions.copyInto(info);
            }
        }
        return info;
    }

    /**
     * Clear the permissions for a bundle.
     */
    public void clearPermissions(Bundle bundle) {
        PermissionAdmin permissionAdmin = (PermissionAdmin) tracker
                .getService();
        if (permissionAdmin == null) /* no permission admin service */
            return;
        if (bundles.removeElement(bundle)) {
            permissionAdmin.setPermissions(bundle.getLocation(), null);
        }
    }

    /**
     * Event when a bundle has changed so we need to inspect if it is installed,
     * and if so we need to set the permissions or remove it when it is
     * uninstalled.
     */
    public void bundleChanged(BundleEvent event) {
        Bundle bundle = event.getBundle();
        if (bundle.getBundleId() == 0) /* ignore the system bundle */
            return;
        int type = event.getType();
        switch (type) {
        case BundleEvent.INSTALLED:
        case BundleEvent.UPDATED:
            setPermissions(bundle);
            break;
        case BundleEvent.UNINSTALLED:
            clearPermissions(bundle);
            break;
        }
    }

    public void setDefaultPermissions(PermissionInfo defaultPermissions[]) {
        this.defaultPermissions = defaultPermissions;
    }
}
