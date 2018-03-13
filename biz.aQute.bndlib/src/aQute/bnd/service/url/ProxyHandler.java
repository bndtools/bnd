package aQute.bnd.service.url;

import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

public interface ProxyHandler {

	ProxySetup forURL(URL url) throws Exception;

	public class ProxySetup {
		public Proxy					proxy;
		public PasswordAuthentication	authentication;

		@Override
		public String toString() {
			return "Proxy [proxy=" + proxy + ", authentication="
				+ (authentication == null ? null : authentication.getUserName()) + "]";
		}
	}

}
