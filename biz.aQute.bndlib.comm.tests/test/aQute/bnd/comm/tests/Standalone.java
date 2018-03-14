package aQute.bnd.comm.tests;

import java.io.IOException;

import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import sockslib.common.AuthenticationException;
import sockslib.common.Credentials;
import sockslib.common.methods.UsernamePasswordMethod;
import sockslib.server.Session;
import sockslib.server.SocksProxyServer;
import sockslib.server.SocksServerBuilder;
import sockslib.server.UsernamePasswordAuthenticator;
import sockslib.server.listener.CloseSessionException;
import sockslib.server.listener.SessionListener;
import sockslib.server.manager.MemoryBasedUserManager;
import sockslib.server.manager.User;
import sockslib.server.manager.UserManager;
import sockslib.server.msg.CommandMessage;

public class Standalone {

	private SocksProxyServer socks5Proxy;

	void createSecureSocks5() throws IOException, InterruptedException {
		UserManager userManager = new MemoryBasedUserManager();
		userManager.create(new User("proxyuser", "good"));
		SocksServerBuilder builder = SocksServerBuilder.newSocks5ServerBuilder();
		builder.setBindPort(9090);
		// builder.setUserManager(userManager);

		UsernamePasswordMethod usernamePasswordMethod = new UsernamePasswordMethod();
		usernamePasswordMethod.setAuthenticator(new UsernamePasswordAuthenticator(userManager) {
			@Override
			public void doAuthenticate(Credentials arg0, Session arg1) throws AuthenticationException {
				super.doAuthenticate(arg0, arg1);
				System.out.println("Auth " + arg0 + " " + arg1);
			}
		});
		builder.setSocksMethods(usernamePasswordMethod);
		socks5Proxy = builder.build();

		socks5Proxy.getSessionManager()
			.addSessionListener("abc", new SessionListener() {

				@Override
				public void onException(Session arg0, Exception arg1) {
					System.err.println("Exception " + arg0 + " " + arg1);
					arg1.printStackTrace();
				}

				@Override
				public void onCommand(Session arg0, CommandMessage arg1) throws CloseSessionException {
					System.err.println("Command " + arg0 + " " + arg1);
				}

				@Override
				public void onClose(Session arg0) {
					System.err.println("Close " + arg0);
				}

				@Override
				public void onCreate(Session arg0) throws CloseSessionException {
					System.err.println("Create " + arg0);
				}
			});

		socks5Proxy.start();
	}

	void createHttpProxy() {
		HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap()
			.withPort(9091);
		bootstrap.withProxyAuthenticator(new ProxyAuthenticator() {

			@Override
			public boolean authenticate(String user, String password) {
				System.out.println("Authenticating " + user + " : " + password);
				return "proxyuser".equals(user) && "good".equals(password);

			}

			@Override
			public String getRealm() {
				// TODO Auto-generated method stub
				return null;
			}
		});
		bootstrap.start();
	}

	public static void main(String args[]) throws InterruptedException, IOException {
		Standalone s = new Standalone();
		s.createSecureSocks5();
		s.createHttpProxy();
		// Socks5Server socks5Server = new Socks5Server();
		// socks5Server.start(new String[] {"--port", "9090", "--auth",
		// "proxyuser:good"});

		Thread.sleep(10000000);
	}
}
