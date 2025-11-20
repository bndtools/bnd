package aQute.bnd.repository.maven.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Project.ReleaseParameter;
import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class SonatypeReleaseTest {

	@InjectTemporaryDirectory
	static File			wsDir;

	static final String	CENTRAL_SONATYPE_PUBLISHER_URL	= "https://central.sonatype.com/api/v1/publisher";

	File				sonatypeRepoFile				= new File(wsDir, "cnf/ext/sonatype_release.bnd");
	File				releasedVersionFile				= new File(wsDir, "cnf/ext/gav_30_sonatype.mvn");
	File				deploymentIDFile				= new File(wsDir,
		MavenBndRepository.SONATYPE_RELEASE_DIR + "/" + MavenBndRepository.SONATYPE_DEPLOYMENTID_FILE);
	boolean				remoteTest						= true;

	@BeforeEach
	public void prepareTest() throws IOException {
		// Only run tests if required environment variables are set
		assumeTrue(hasRequiredEnvironmentVariables(),
			"Skipping test: Required environment variables (SONATYPE_BEARER, GPG_KEYNAME, GPG_PASSPHRASE) are not set");

		IO.delete(wsDir);
		assertFalse("directory could not be deleted " + wsDir, wsDir.exists());

		IO.copy(IO.getFile("testresources/sonatype"), wsDir);
		System.err.println("--" + wsDir);

		sonatypeRepoFile.delete();
		assertFalse(sonatypeRepoFile.exists());

		releasedVersionFile.delete();
		assertFalse(releasedVersionFile.exists());
	}

	private boolean hasRequiredEnvironmentVariables() {
		String sonatypeBearer = System.getenv("SONATYPE_BEARER");
		String gpgKeyname = System.getenv("GPG_KEYNAME");
		String gpgPassphrase = System.getenv("GPG_PASSPHRASE");

		return sonatypeBearer != null && !sonatypeBearer.isEmpty() && gpgKeyname != null && !gpgKeyname.isEmpty()
			&& gpgPassphrase != null && !gpgPassphrase.isEmpty();
	}

	@Test
	public void testReleaseDeployment() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_release.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		testDeployment();
	}

	@Test
	public void testStagingDeployment() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_staging.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		testDeployment();
	}

	@Test
	public void testSonatypeNone() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_none.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		remoteTest = false;
		testDeployment();
	}

	@Test
	public void testSonatypeNull() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_null.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		remoteTest = false;
		testDeployment();
	}

	private void testDeployment() throws Exception, IOException {
		LinkedList<File> releasedFiles = new LinkedList<File>();
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			ws.setPedantic(true);

			List<RepositoryPlugin> repos = ws.getRepositories();
			assertEquals(repos.size(), 5);

			Set<Project> projects = new LinkedHashSet<>();
			projects.addAll(ws.getAllProjects());
			for (Iterator<Project> iterator = projects.iterator(); iterator.hasNext();) {
				Project p = iterator.next();
				p.clean();
				p.compile(false);
				File[] files = p.build();
				assertNotNull(files);

				releasedFiles.addAll(Arrays.asList(files));
				if (iterator.hasNext()) {
					System.out.println("Release project: " + p.getName());
					System.out.println("  of builded BSNs " + p.getBsns());
					p.release();
				} else {
					System.out.println("Release of last project: " + p.getName());
					System.out.println("  of builded BSNs " + p.getBsns());
					p.release(new ReleaseParameter(null, false, true));
				}
			}
			assertTrue(releasedVersionFile.exists());

			String content = Files.readString(releasedVersionFile.toPath());
			// Check that all bundles are in released content
			for (File file : releasedFiles) {
				String filename = file.getName();
				String nameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
				boolean found = content
					.matches("(?s).*\\b" + java.util.regex.Pattern.quote(nameWithoutExtension) + "\\b.*");
				assertTrue("Filename without extension '" + nameWithoutExtension + "' not found as word in content",
					found);
			}
			if (remoteTest) {
				assertTrue("Deployment ID file not found: " + deploymentIDFile, deploymentIDFile.exists());

				String deploymentId = Files.readString(deploymentIDFile.toPath());
				assertNotNull("Deployment ID should not be null", deploymentId);

				testDeploymentStatusCheck(ws, deploymentId);
			}
		}
	}

	private void testDeploymentStatusCheck(Workspace ws, String deploymentId) throws Exception {
		MavenBndRepository releaseRepo = null;
		for (RepositoryPlugin repo : ws.getRepositories()) {
			if (repo instanceof MavenBndRepository) {
				try (MavenBndRepository mbr = (MavenBndRepository) repo) {
					if (mbr.getName()
						.contains("Sonatype")) {
						releaseRepo = mbr;
						break;
					}
				}
			}
		}
		assertNotNull("Sonatype release repository not found", releaseRepo);

		HttpClient client = releaseRepo.getClient();
		assertNotNull("HTTP client should not be null", client);

		String statusUrl = CENTRAL_SONATYPE_PUBLISHER_URL + "/status?id=" + deploymentId;

		System.out.println("Checking deployment status at: " + statusUrl);
		System.out.println("Deployment ID: " + deploymentId);

		boolean statusOk = false;
		try {
			TaggedData taggedData = client.build()
				.post()
				.asTag()
				.go(new URI(statusUrl).toURL());

			System.out.println("Response code: " + taggedData.getResponseCode());

			if (taggedData.isOk()) {
				String responseBody = aQute.lib.io.IO.collect(taggedData.getInputStream());
				System.out.println("Deployment status response: " + responseBody);
				assertNotNull("Response body should not be null", responseBody);
				statusOk = true;
			} else {
				System.out.println("Failed to get deployment status. HTTP " + taggedData.getResponseCode());
			}
		} catch (Exception e) {
			System.err.println("Error checking deployment status: " + e.getMessage());
			e.printStackTrace();
		}

		// Drop the deployment after successful status check
		if (statusOk) {
			dropDeployment(client, deploymentId);
		}
	}

	private void dropDeployment(HttpClient client, String deploymentId) throws Exception {
		String dropUrl = CENTRAL_SONATYPE_PUBLISHER_URL + "/deployment/" + deploymentId;

		System.out.println("Dropping deployment at: " + dropUrl);

		try {
			TaggedData taggedData = client.build()
				.delete()
				.asTag()
				.go(new URI(dropUrl).toURL());

			System.out.println("Drop response code: " + taggedData.getResponseCode());

			if (taggedData.isOk()) {
				String responseBody = aQute.lib.io.IO.collect(taggedData.getInputStream());
				System.out.println("Drop deployment response: " + responseBody);
				System.out.println("Successfully dropped deployment: " + deploymentId);
			} else {
				System.out.println("Failed to drop deployment. HTTP " + taggedData.getResponseCode());
			}
		} catch (Exception e) {
			System.err.println("Error dropping deployment: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
