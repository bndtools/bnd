package bndtools.ace.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.amdatu.ace.client.AceClient;
import org.amdatu.ace.client.AceClientWorkspace;
import org.apache.ace.bnd.repository.AceRepository;

import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryPlugin.PutOptions;


public class AceLauncher {
	private String m_aceUrl = "http://localhost:8080";
	private String m_target = "default";
	private String m_feature = "default";
	private String m_distribution = "default";
	private String[] m_jarUrls;
	private int m_port;
	
	public static void main(String[] args) throws Exception {
		if(args.length != 6) {
			throw new IllegalArgumentException("Invalid arguments. Need: [eclipseport] [host] [feature] [distribution] [target] [bundl1;bundle2;bundle3...]");
		}
				
		int port = Integer.parseInt(args[0]);
		String host = args[1];
		String feature = args[2];
		String distribution = args[3];
		String target = args[4];
		String bundles = args[5];
		
		String[] jarUrls = bundles.split(";");
		
		new AceLauncher(host, feature, distribution, target, jarUrls, port).installBundles();
	}

	public AceLauncher(String host, String feature, String distribution, String target, String[] jarUrls, int port) {
		m_aceUrl = host;
		m_feature = feature;
		m_distribution = distribution;
		m_target = target;
		m_jarUrls = jarUrls;
		m_port = port;
		connectToServer();
	}

	private void installBundles() throws Exception {
		AceRepository aceRepository = new AceRepository();
		Map<String, String> props = new HashMap<String, String>();
		props.put(AceRepository.ACEURL_PROPERTY, m_aceUrl);
		aceRepository.setProperties(props);
		
		
		for (String jarUrl : m_jarUrls) {
			File file = new File(jarUrl);
			aceRepository.put(new FileInputStream(file), new PutOptions());
		
			AceClient aceClient = new AceClient(m_aceUrl + "/client/work");
			AceClientWorkspace workspace = aceClient.createNewWorkspace();
			AceAssociationsInstaller associationsInstaller = new AceAssociationsInstaller(workspace, m_feature, m_distribution, m_target);
			Jar jar = new Jar(file);
			associationsInstaller.installArtifact(jar);
			workspace.commit();
		}
	}
	

	private void connectToServer() {
		ExecutorService threadPool = Executors.newFixedThreadPool(1);
		threadPool.execute(new Runnable() {
			@Override
			public void run() {

				try {
					Socket socket = new Socket("localhost", m_port);
					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					String input;

					while ((input = in.readLine()) != null) {
						switch (SocketProtocol.valueOf(input)) {
						case UPDATE:
							installBundles();
							break;

						default:
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
