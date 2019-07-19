package aQute.maven.provider;

import java.io.File;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import aQute.bnd.util.dto.DTO;
import aQute.bnd.version.MavenVersion;
import aQute.lib.date.Dates;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;

/**
 * Utilities to parse the metadata XML files. Maven uses a single XSD (see
 * http://maven.apache.org/ref/3.2.5/maven-repository-metadata/repository-
 * metadata.html) to represent both the snapshot archives in a
 * group/artifact/version directory and the version of a group/artifact
 * combination. In this file we use {@code Program} for the GA combination and
 * {@code Revision} for the GAV combination since there is actually very little
 * overlap. The parser is a best effort, no validation will take place.
 */
public class MetadataParser {
	final static XMLInputFactory	inputFactory		= XMLInputFactory.newInstance();
	static final DateTimeFormatter	MAVEN_DATE_TIME	= DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT)
		.withZone(Dates.UTC_ZONE_ID);

	public static class Metadata extends DTO {
		public String	modelVersion	= "1.1.0";
		public String	group;
		public String	artifact;
		public long		lastUpdated		= System.currentTimeMillis();

		public Tag toTag() {
			Tag top = new Tag("metadata");
			if (modelVersion != null)
				top.addAttribute("modelVersion", modelVersion);
			new Tag(top, "groupId").addContent(group);
			new Tag(top, "artifactId").addContent(artifact);
			return top;
		}

		@Override
		public String toString() {
			Tag tag = toTag();
			return tag.toString();
		}
	}

	public static class ProgramMetadata extends Metadata {
		public MavenVersion			latest;
		public MavenVersion			release;
		public List<MavenVersion>	versions	= new ArrayList<>();

		@Override
		public Tag toTag() {
			Tag top = super.toTag();

			if (latest != null)
				new Tag(top, "latest").addContent(latest.toString());
			if (release != null)
				new Tag(top, "release").addContent(release.toString());

			Tag versioning = new Tag(top, "versioning");

			Tag versionsTag = new Tag(versioning, "versions");
			for (MavenVersion mv : versions) {
				new Tag(versionsTag, "version").addContent(mv.toString());
			}
			new Tag(versioning, "lastUpdated", Dates.formatMillis(MAVEN_DATE_TIME, lastUpdated));
			return top;
		}
	}

	public static class Snapshot extends DTO {
		public String	timestamp;
		public String	buildNumber	= "0";
		public boolean	localCopy	= false;
	}

	public static class SnapshotVersion extends DTO {
		public String		classifier;
		public String		extension;
		public MavenVersion	value;
		public long			updated;
	}

	public static class RevisionMetadata extends Metadata {
		public MavenVersion				version;
		public Snapshot					snapshot			= new Snapshot();
		public List<SnapshotVersion>	snapshotVersions	= new ArrayList<>();

		@Override
		public Tag toTag() {
			Tag top = super.toTag();
			new Tag(top, "version", version.toString());

			Tag versioning = new Tag(top, "versioning");

			Tag snapshot = new Tag(versioning, "snapshot");

			if (this.snapshot.localCopy) {
				new Tag(snapshot, "localCopy", this.snapshot.localCopy);
			} else {
				new Tag(snapshot, "buildNumber", this.snapshot.buildNumber);
				new Tag(snapshot, "timestamp", this.snapshot.timestamp);
			}

			new Tag(versioning, "lastUpdated", Dates.formatMillis(MAVEN_DATE_TIME, lastUpdated));

			Tag snapshotVersions = new Tag(versioning, "snapshotVersions");
			for (SnapshotVersion sv : this.snapshotVersions) {
				Tag x = new Tag(snapshotVersions, "snapshotVersion");
				new Tag(x, "extension", sv.extension);
				if (sv.classifier != null)
					new Tag(x, "classifier", sv.classifier);
				new Tag(x, "value", sv.value + "");
				new Tag(x, "updated", Dates.formatMillis(MAVEN_DATE_TIME, sv.updated));
			}

			return top;
		}
	}

	/**
	 * Will return a Program Metadata
	 *
	 * @param in The inputstream that must point to XML or null of could not be
	 *            parsed
	 * @return A description of the XML
	 */
	public static ProgramMetadata parseProgramMetadata(InputStream in) throws Exception {
		XMLStreamReader sr = inputFactory.createXMLStreamReader(in);
		sr.nextTag();
		sr.require(XMLStreamConstants.START_ELEMENT, null, "metadata");

		return programMetadata(sr);
	}

	/**
	 * Will return a Revision Metadata
	 *
	 * @param in the input stream
	 * @return the representation
	 */
	public static RevisionMetadata parseRevisionMetadata(InputStream in) throws Exception {
		XMLStreamReader sr = inputFactory.createXMLStreamReader(in);
		sr.nextTag();
		sr.require(XMLStreamConstants.START_ELEMENT, null, "metadata");
		return revisionMetadata(sr);
	}

	public static RevisionMetadata parseRevisionMetadata(File metadataFile) throws Exception {
		try (InputStream in = IO.stream(metadataFile)) {
			return parseRevisionMetadata(in);
		}
	}

	/*
	 * Revision parsing
	 */

	private static RevisionMetadata revisionMetadata(XMLStreamReader sr) throws Exception {
		try {
			RevisionMetadata metadata = new RevisionMetadata();
			metadata.modelVersion = getModelVersion(sr);

			while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
				String name = sr.getLocalName();
				switch (name) {
					case "groupId" :
						metadata.group = getText(sr);
						break;

					case "artifactId" :
						metadata.artifact = getText(sr);
						break;

					case "version" :
						metadata.version = getVersion(sr);
						break;

					case "versioning" :
						snapshots(sr, metadata);
						break;

					default :
						skip(sr);
						break;
				}

				sr.require(XMLStreamConstants.END_ELEMENT, null, name);
			}
			return metadata;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private static void snapshots(XMLStreamReader sr, RevisionMetadata metadata) throws Exception {
		while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			String name = sr.getLocalName();
			switch (name) {
				case "lastUpdated" :
					metadata.lastUpdated = getTimestamp(sr);
					break;

				case "snapshot" :
					metadata.snapshot = snapshot(sr);
					break;

				case "snapshotVersions" :
					snapshotVersions(sr, metadata.snapshotVersions);
					break;

				default :
					skip(sr);
					break;
			}
			sr.require(XMLStreamConstants.END_ELEMENT, null, name);
		}

	}

	private static void snapshotVersions(XMLStreamReader sr, List<SnapshotVersion> snapshotVersions) throws Exception {

		while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			String name = sr.getLocalName();
			switch (name) {
				case "snapshotVersion" :
					snapshotVersions.add(getSnapshotVersion(sr));
					break;

				default :
					skip(sr);
					break;
			}
			sr.require(XMLStreamConstants.END_ELEMENT, null, name);
		}
	}

	private static SnapshotVersion getSnapshotVersion(XMLStreamReader sr) throws Exception {
		SnapshotVersion sv = new SnapshotVersion();

		while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			String name = sr.getLocalName();
			switch (name) {
				case "classifier" :
					sv.classifier = getText(sr);
					break;

				case "extension" :
					sv.extension = getText(sr);
					break;

				case "value" :
					sv.value = getVersion(sr);
					break;

				case "updated" :
					sv.updated = getTimestamp(sr);
					break;

				default :
					skip(sr);
					break;
			}
			sr.require(XMLStreamConstants.END_ELEMENT, null, name);
		}
		return sv;
	}

	private static Snapshot snapshot(XMLStreamReader sr) throws Exception {
		Snapshot snapshot = new Snapshot();

		while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			String name = sr.getLocalName();
			switch (name) {
				case "timestamp" :
					snapshot.timestamp = getText(sr);
					break;

				case "buildNumber" :
					snapshot.buildNumber = getText(sr);
					break;

				default :
					skip(sr);
					break;
			}
			sr.require(XMLStreamConstants.END_ELEMENT, null, name);
		}
		return snapshot;
	}

	/*
	 * Program parsing
	 */

	private static ProgramMetadata programMetadata(XMLStreamReader sr) throws Exception {
		ProgramMetadata gameta = new ProgramMetadata();
		gameta.modelVersion = getModelVersion(sr);

		while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			String name = sr.getLocalName();
			switch (name) {
				case "groupId" :
					gameta.group = getText(sr);
					break;

				case "artifactId" :
					gameta.artifact = getText(sr);
					break;

				case "versioning" :
					versioning(sr, gameta);
					break;

				default :
					skip(sr);
					break;
			}

			sr.require(XMLStreamConstants.END_ELEMENT, null, name);
		}
		return gameta;
	}

	private static void versioning(XMLStreamReader sr, ProgramMetadata gameta) throws Exception {
		while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			String name = sr.getLocalName();
			switch (name) {
				case "lastUpdated" :
					gameta.lastUpdated = getTimestamp(sr);
					break;

				case "versions" :
					versions(sr, gameta.versions);
					break;

				case "latest" :
					gameta.latest = getVersion(sr);
					break;

				case "release" :
					gameta.release = getVersion(sr);

					break;
				default :
					skip(sr);
					break;
			}
			sr.require(XMLStreamConstants.END_ELEMENT, null, name);
		}

	}

	private static void versions(XMLStreamReader sr, List<MavenVersion> versions) throws Exception {
		while (sr.nextTag() == XMLStreamConstants.START_ELEMENT) {
			String name = sr.getLocalName();
			switch (name) {
				case "version" :
					versions.add(getVersion(sr));
					break;

				default :
					skip(sr);
					break;
			}
			sr.require(XMLStreamConstants.END_ELEMENT, null, null);
		}
	}

	/*
	 * Utils
	 */

	private static String getText(XMLStreamReader sr) throws Exception {
		String elementText = sr.getElementText();
		if (elementText == null)
			return null;

		return elementText.trim();
	}

	private static void skip(XMLStreamReader sr) throws Exception {
		while (sr.next() != XMLStreamConstants.END_ELEMENT) {
			switch (sr.getEventType()) {
				case XMLStreamConstants.START_ELEMENT :
					skip(sr);
					break;
				default :
					;
			}
		}
	}

	private static long getTimestamp(XMLStreamReader sr) throws Exception {
		String date = getText(sr);
		if (date == null)
			return 0;

		return Dates.parseMillis(MAVEN_DATE_TIME, date.trim());
	}

	private static MavenVersion getVersion(XMLStreamReader sr) throws Exception {
		String version = getText(sr);
		if (version == null || version.trim()
			.isEmpty())
			return null;

		return new MavenVersion(version);
	}

	private static String getModelVersion(XMLStreamReader sr) throws Exception {
		for (int i = 0; i < sr.getAttributeCount(); i++) {
			String name = sr.getAttributeLocalName(i);
			if (name.equals("modelVersion")) {
				return sr.getAttributeValue(i)
					.trim();
			}
		}
		return null;
	}

	public static ProgramMetadata parseProgramMetadata(File metafile) throws Exception {
		try (InputStream in = IO.stream(metafile)) {
			return parseProgramMetadata(in);
		} catch (Exception e) {
			System.out.println("File " + metafile + " failed");
			throw e;
		}
	}

}
