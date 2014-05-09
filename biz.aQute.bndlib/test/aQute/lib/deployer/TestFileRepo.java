package aQute.lib.deployer;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.bnd.version.*;

public class TestFileRepo extends TestCase {

	private File		repoDir;
	private FileRepo	repo;

	protected void setUp() throws Exception {
		repoDir = new File("testresources/FileRepo/getVersions");
		repo = new FileRepo("testrepo", repoDir, false);
	}

	protected void tearDown() throws Exception {
		repo.close();
	}

	private void getExpectedVersionsLists(String bsn, List<Version> versionsList, List<VersionFilePair> versionsPairList)
			throws Exception {
		File dir = new File(repoDir, bsn);
		List<String> fileNames = Arrays.asList(dir.list());
		Collections.sort(fileNames);
		Version v;
		for (String fileName : fileNames) {
			if ("ee.minimum-1.1.99.jar".equals(fileName)) {
				v = new Version("1.2.0.x200908310645");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("ee.minimum-1.2.0.jar".equals(fileName)) {
				v = new Version("1.2.0.200908310645");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("ee.minimum-1.2.1.2013.jar".equals(fileName)) {
				v = new Version("1.2.1.201305092016");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("ee.minimum.jar".equals(fileName)) {
				v = new Version("1.2.1.201305092016");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("ee.minimum-latest.jar".equals(fileName)) {
				v = new Version("1.2.1.201305092016");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("ee.minimum-no-matches.jar".equals(fileName)) {
				v = new Version("1.2.0.x200908310645");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("hamcrest-core.jar".equals(fileName)) {
				v = new Version("1.3.0");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("junit-4.11.0.jar".equals(fileName)) {
				// nothing
			} else if ("junit-4.11.jar".equals(fileName)) {
				v = new Version("4.11.0");
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("junit-latest.jar".equals(fileName)) {
				v = FileRepo.LATEST_VERSION;
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("junit.jar".equals(fileName)) {
				v = FileRepo.ZERO_VERSION;
				versionsList.add(v);
				versionsPairList.add(new VersionFilePair(v, new File(dir, fileName)));
			} else if ("junit_no_manifest.jar".equals(fileName)) {
				// nothing
			} else {
				throw new Exception("unhandled file name " + bsn + "/" + fileName);
			}
		}
	}

	private File getExpectedGet(String bsn, String version) throws Exception {
		if (FileRepo.ZERO_VERSION.toString().equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return null;
			} else if ("org.hamcrest".equals(bsn)) {
				return null;
			} else if ("org.junit".equals(bsn)) {
				return new File(repoDir, "org.junit/junit.jar");
			}
		} else if ("1.1.99".equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return null;
			} else if ("org.hamcrest".equals(bsn)) {
				return null;
			} else if ("org.junit".equals(bsn)) {
				return null;
			}
		} else if ("1.2.0.x200908310645".equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return new File(repoDir, "ee.minimum/ee.minimum-1.1.99.jar");
			} else if ("org.hamcrest".equals(bsn)) {
				return null;
			} else if ("org.junit".equals(bsn)) {
				return null;
			}
		} else if ("1.2.0.200908310645".equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return new File(repoDir, "ee.minimum/ee.minimum-1.2.0.jar");
			} else if ("org.hamcrest".equals(bsn)) {
				return null;
			} else if ("org.junit".equals(bsn)) {
				return null;
			}
		} else if ("1.2.1.201305092016".equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return new File(repoDir, "ee.minimum/ee.minimum-1.2.1.2013.jar");
			} else if ("org.hamcrest".equals(bsn)) {
				return null;
			} else if ("org.junit".equals(bsn)) {
				return null;
			}
		} else if ("1.3.0".equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return null;
			} else if ("org.hamcrest".equals(bsn)) {
				return new File(repoDir, "org.hamcrest/hamcrest-core.jar");
			} else if ("org.junit".equals(bsn)) {
				return null;
			}
		} else if ("4.11.0".equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return null;
			} else if ("org.hamcrest".equals(bsn)) {
				return null;
			} else if ("org.junit".equals(bsn)) {
				return new File(repoDir, "org.junit/junit-4.11.jar");
			}
		} else if (FileRepo.LATEST_VERSION.toString().equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return new File(repoDir, "ee.minimum/ee.minimum-1.2.1.2013.jar");
			} else if ("org.hamcrest".equals(bsn)) {
				return new File(repoDir, "org.hamcrest/hamcrest-core.jar");
			} else if ("org.junit".equals(bsn)) {
				return new File(repoDir, "org.junit/junit-latest.jar");
			}
		} else if ("123.321.123".toString().equals(version)) {
			if ("ee.minimum".equals(bsn)) {
				return null;
			} else if ("org.hamcrest".equals(bsn)) {
				return null;
			} else if ("org.junit".equals(bsn)) {
				return null;
			}
		}

		throw new Exception("unhandled bsn/version " + bsn + "/" + version);
	}

	public void testVersionsLists() throws Exception {
		String[] bsns = {
				"ee.minimum", "org.hamcrest", "org.junit"
		};

		LinkedList<Version> versionsList = new LinkedList<Version>();
		LinkedList<VersionFilePair> versionsPairList = new LinkedList<VersionFilePair>();
		LinkedList<Version> expectedVersionsList = new LinkedList<Version>();
		LinkedList<VersionFilePair> expectedVersionsPairList = new LinkedList<VersionFilePair>();

		for (String bsn : bsns) {
			versionsList.clear();
			versionsPairList.clear();
			expectedVersionsList.clear();
			expectedVersionsPairList.clear();
			getExpectedVersionsLists(bsn, expectedVersionsList, expectedVersionsPairList);

			repo.getVersionsLists(bsn, versionsList, versionsPairList);

			assertEquals(expectedVersionsList.toString(), versionsList.toString());
			assertEquals(expectedVersionsPairList.toString(), versionsPairList.toString());
		}
	}

	public void testGet() throws Exception {
		String[] bsns = {
				"ee.minimum", "org.junit", "org.hamcrest"
		};
		String[] versions = {
				FileRepo.ZERO_VERSION.toString(), "1.1.99", "1.2.0.200908310645", "1.2.1.201305092016", "1.3.0",
				"4.11.0", "123.321.123", FileRepo.LATEST_VERSION.toString()
		};

		for (String bsn : bsns) {
			for (String version : versions) {
				File repoFile = repo.get(bsn, new Version(version), null);
				File expectedFile = getExpectedGet(bsn, version);
				if (repoFile == null) {
					assertNull(expectedFile);
				} else {
					assertEquals(expectedFile.getAbsoluteFile(), repoFile);
				}
			}
		}
	}
}