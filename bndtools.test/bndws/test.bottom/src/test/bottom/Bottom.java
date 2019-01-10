package test.bottom;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import test.top.api.Api;

//
// check visibility
// Api is exported from another project
// BundleActivator is transiently visible from test.exports.external
//

public class Bottom extends Api implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
}
