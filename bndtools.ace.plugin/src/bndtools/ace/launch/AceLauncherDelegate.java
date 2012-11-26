package bndtools.ace.launch;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.management.RuntimeErrorException;

import org.bndtools.build.api.BuildListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import bndtools.launch.util.LaunchUtils;

public class AceLauncherDelegate extends JavaLaunchDelegate implements BuildListener {
    public final static String CONSOLE_NAME = "ACE";
    private Project m_project;
    private AceLaunchSocket m_aceLaunchSocket;
    private int m_port;
    private String m_aceUrl;
    private String m_feature;
    private String m_distribution;
    private String m_target;
    private ILaunch m_launch;
    private ServiceRegistration m_service;

    public AceLauncherDelegate() {
        m_aceLaunchSocket = new AceLaunchSocket();
        m_port = m_aceLaunchSocket.openSocket();
    }

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        m_aceLaunchSocket.start();
        m_aceUrl = configuration.getAttribute(LaunchConstants.ATTR_ACE_ADDRESS, "http://localhost:8080");
        m_feature = configuration.getAttribute(LaunchConstants.ATTR_ACE_FEATURE, "default");
        m_distribution = configuration.getAttribute(LaunchConstants.ATTR_ACE_DISTRIBUTION, "default");
        m_target = configuration.getAttribute(LaunchConstants.ATTR_ACE_TARGET, "default");
        m_launch = launch;
        
        super.launch(configuration, mode, launch, monitor);
        registerBuildListener();
       }

    @Override
    public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        return "bndtools.ace.launcher.AceLauncher";
    }

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        org.osgi.framework.Bundle bundle = Platform.getBundle("bndtools.ace.launcher");
        org.osgi.framework.Bundle aceRepoBundle = Platform.getBundle("org.apache.ace.bnd.repository");
        org.osgi.framework.Bundle bndRepoBundle = Platform.getBundle("biz.aQute.bndlib");
        try {
			String decode = URLDecoder.decode(bndRepoBundle.getLocation(), "UTF-8");
			return new String[] { bundle.getLocation(), aceRepoBundle.getLocation(), decode };
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
    }

    @Override
    public String getProgramArguments(ILaunchConfiguration configuration) {
    	 
    	
        try {
            m_project = LaunchUtils.getBndProject(configuration);
            
            StringBuilder sb = new StringBuilder();
            sb.append(m_port).append(" ")
               .append(m_aceUrl).append(" ")
               .append(m_feature).append(" ")
               .append(m_distribution).append(" ")
               .append(m_target).append(" ");
            
            for (Container bundle : m_project.getDeliverables()) {
                sb.append(bundle.getFile().getAbsolutePath()).append(";");
            }
            
            for (Container bundle : m_project.getRunbundles()) {
                sb.append(bundle.getFile().getAbsolutePath()).append(";");
            }
            
            
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void registerBuildListener() throws CoreException {
    	Bundle bundle = FrameworkUtil.getBundle(AceLauncherDelegate.class);
    	BundleContext bundleContext = bundle.getBundleContext();
    	m_service = bundleContext.registerService(BuildListener.class.getName(), this, null);
    }

	@Override
	public void buildStarting(IProject project) {
		if(m_launch.isTerminated()) {
	    	m_service.unregister();
		}
	}

	@Override
	public void builtBundles(IProject project, IPath[] paths) {
		m_aceLaunchSocket.sendUpdated();
	}	
}