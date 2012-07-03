package org.bndtools.core.jobs.startup;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

import bndtools.IStartupParticipant;
import bndtools.Plugin;
import bndtools.utils.BundleUtils;

/**
 * <p>
 * We require the bindex and bindex2.lib bundles to be active. But stupid old Eclipse doesn't activate bundles
 * automatically, so we have to give them a kick.
 * </p>
 * <p>
 * We should probably find a more elegant way to do this than hard-coding bundle names but I don't know what that might
 * be, so this works for now.
 * </p>
 * 
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
public class KickBundles implements IStartupParticipant {

    private static final BundleContext CONTEXT = FrameworkUtil.getBundle(KickBundles.class).getBundleContext();

    public void start() {
        kickBundle("org.osgi.impl.bundle.bindex");
        kickBundle("org.osgi.impl.bundle.bindex2.lib");
    }

    private static void kickBundle(String bsn) {
        Bundle bindex = BundleUtils.findBundle(CONTEXT, bsn, null);
        try {
            if (bindex != null)
                bindex.start();
        } catch (BundleException e) {
            Plugin.getDefault().getLogger().logError("Unable to start BIndex bundle", e);
        }
    }

    public void stop() {}

}
