package servlet.hello;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Hello extends HttpServlet implements BundleActivator {
	private static final long serialVersionUID = 1L;
	ServiceTracker<HttpService,HttpService> tracker;
	
	public void doGet(HttpServletRequest rq, HttpServletResponse rsp)
			throws IOException {
		rsp.getWriter().println("Hello");
	}

	@Override
	public void start(BundleContext context) throws Exception {
		tracker= new ServiceTracker<HttpService,HttpService>(context, HttpService.class, null) {
			@Override
			public HttpService addingService(
					ServiceReference<HttpService> reference) {
				HttpService http = super.addingService(reference);
				try {
					http.registerServlet("/hello", Hello.this, null, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return http;
			}
		};
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		tracker.close();
	}
	

}
