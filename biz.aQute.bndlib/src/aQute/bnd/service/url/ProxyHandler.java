package aQute.bnd.service.url;

import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

public interface ProxyHandler {

	public class ProxySetup {
		public Proxy					proxy;
		public PasswordAuthentication	authentication;
	}

	ProxySetup forURL(URL url) throws Exception;
}
