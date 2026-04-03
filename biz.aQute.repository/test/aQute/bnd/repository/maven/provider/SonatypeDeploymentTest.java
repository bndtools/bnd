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

import org.junit.jupiter.api.BeforeAll;
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

public class SonatypeDeploymentTest {

	static final String	CENTRAL_SONATYPE_PUBLISHER_URL	= "https://central.sonatype.com/api/v1/publisher";

	@InjectTemporaryDirectory
	static File			wsDir;
	static File			localMavenRepo					= new File(wsDir, ".m2");

	File				sonatypeRepoFile				= new File(wsDir, "cnf/ext/sonatype_release.bnd");
	File				releasedVersionFile				= new File(wsDir, "cnf/ext/gav_30_sonatype.mvn");
	File				deploymentIDFile				= new File(wsDir, MavenBndRepository.SONATYPE_RELEASE_DIR + "/"
		+ "biz_aQute_eval_" + MavenBndRepository.SONATYPE_DEPLOYMENTID_FILE);
	boolean				remoteTest						= true;

	@BeforeAll
	public static void checkGpg() {
		System.out.println("using wrkdir: " + wsDir);
		assumeTrue(isGpgAvailable(), "Skipping test: GPG executable not found in system PATH");
		assumeTrue(hasRequiredEnvironment(),
			"Skipping test: Required environment variables (SONATYPE_BEARER, GPG_KEY_ID, GPG_PASSPHRASE) are not set");
		// configure local Maven repository directory
		localMavenRepo.mkdirs();
		assertTrue("Local Maven repository should be created", localMavenRepo.exists());
		System.setProperty("maven.repo.local", localMavenRepo.getAbsolutePath());
	}

	@BeforeEach
	public void prepareTest() throws IOException {
		// clean workspace directory - start fresh for each test
		IO.delete(wsDir);
		assertFalse("wrkdir directory could not be deleted " + wsDir, wsDir.exists());

		IO.copy(IO.getFile("testresources/sonatype"), wsDir);
		assertFalse(" Sonatype repo config must not exist before test", sonatypeRepoFile.exists());
		assertFalse("Released version file must not exist before test", releasedVersionFile.exists());
	}

	@Test
	public void testReleaseUrl() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_release.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		testDeployment(false);
	}

	@Test
	public void testStagingUrl() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_staging.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		testDeployment(false);
	}

	@Test
	public void testSonatypeModeNone() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_none.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		remoteTest = false;
		testDeployment(false);
	}

	@Test
	public void testSonatypeModeUnset() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_null.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		remoteTest = false;
		testDeployment(false);
	}

	@Test
	public void testReleaseUrlSnapshot() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_release.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		commentSnapshotLine();
		testDeployment(true);
	}

	@Test
	public void testStagingUrlSnapshot() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_staging.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		commentSnapshotLine();
		testDeployment(true);
	}

	@Test
	public void testSonatypeModeNoneSnapshot() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_none.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		remoteTest = false;
		commentSnapshotLine();
		testDeployment(true);
	}

	@Test
	public void testSonatypeModeUnsetSnapshot() throws Exception {
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			new File(wsDir, "cnf/ext/sonatype_null.bnd_").renameTo(sonatypeRepoFile);
			assertTrue(sonatypeRepoFile.exists());
		}
		remoteTest = false;
		commentSnapshotLine();
		testDeployment(true);
	}

	private void commentSnapshotLine() throws IOException {
		File buildBnd = new File(wsDir, "cnf/build.bnd");
		String content = Files.readString(buildBnd.toPath());
		// Comment out the -snapshot: line
		content = content.replaceAll("(?m)^-snapshot:", "#-snapshot:");
		Files.writeString(buildBnd.toPath(), content);
	}

	private void testDeployment(boolean isSnapshot) throws Exception, IOException {
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
					System.out.println("Release project: " + p.getName() + " with bundle(s) BSN(s) " + p.getBsns());
					p.release();
				} else {
					System.out.println(
						"Release last wrkspc project: " + p.getName() + " with bundle(s) BSN(s) " + p.getBsns());
					p.release(new ReleaseParameter(null, false, true));
				}
			}
			ws.refresh();
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
			if (remoteTest && !isSnapshot) {
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

		// Wait for validation before dropping
		if (statusOk) {
			waitForValidation(client, deploymentId);
			dropDeployment(client, deploymentId);
		}
	}

	private void waitForValidation(HttpClient client, String deploymentId) throws Exception {
		String statusUrl = CENTRAL_SONATYPE_PUBLISHER_URL + "/status?id=" + deploymentId;
		System.out.println("Waiting for deployment validation at: " + statusUrl);

		long startTime = System.currentTimeMillis();
		long maxWaitTime = 30 * 1000; // 30 seconds
		boolean validated = false;

		while (System.currentTimeMillis() - startTime < maxWaitTime) {
			try {
				TaggedData taggedData = client.build()
					.post()
					.asTag()
					.go(new URI(statusUrl).toURL());

				if (taggedData.isOk()) {
					String responseBody = aQute.lib.io.IO.collect(taggedData.getInputStream());
					System.out.println("Deployment status response: " + responseBody);

					if (responseBody.contains("\"deploymentState\":\"VALIDATED\"")) {
						System.out.println("Deployment validated successfully");
						validated = true;
						break;
					}
				}

				// Wait 1 second before next check
				Thread.sleep(1000);
			} catch (Exception e) {
				System.err.println("Error checking validation status: " + e.getMessage());
				e.printStackTrace();
				break;
			}
		}

		if (!validated) {
			System.out.printf("Deployment validation not confirmed within %d seconds%n", maxWaitTime / 1000);
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

	private static boolean isGpgAvailable() {
		String[] gpgCommands = {
			"gpg", "gpg2"
		};

		for (String gpgCommand : gpgCommands) {
			try {
				ProcessBuilder pb = new ProcessBuilder(gpgCommand, "--version");
				Process process = pb.start();
				int exitCode = process.waitFor();

				if (exitCode == 0) {
					System.out.println("Found GPG executable: " + gpgCommand);
					return true;
				}
			} catch (IOException e) {
				// Command not found, try next
				System.out.println("GPG command '" + gpgCommand + "' not found: " + e.getMessage());
			} catch (InterruptedException e) {
				Thread.currentThread()
					.interrupt();
				return false;
			}
		}

		return false;
	}

	private static boolean hasRequiredEnvironment() {
		String[] vars = {
			"SONATYPE_BEARER", "GPG_KEY_ID", "GPG_PASSPHRASE"
		};
		boolean existing = true; // initialize to true
		for (String var : vars) {
			existing = existing && ensureEnvVar(var);
		}
		if ("true".equals(System.getenv("GITHUB_ACTIONS"))) {
			// Running in GitHub Actions
			if ("false".equals(System.getenv("CANONICAL"))) {
				System.out.println("skipping tests cause in GITHUB but NOT in CANONICAL");
				existing = false;
			}
		}
		return existing;
	}

	private static boolean ensureEnvVar(String varName) {
		boolean found = false;
		String value = System.getenv(varName);
		if (value == null || value.isEmpty()) {
			System.err.println("Mandatory environment variable '" + varName + "' is not set or empty.");
		} else {
			found = true;
		}
		return found;
	}

}
