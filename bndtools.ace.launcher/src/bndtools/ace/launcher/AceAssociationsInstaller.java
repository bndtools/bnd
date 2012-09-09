package bndtools.ace.launcher;

import org.amdatu.ace.client.AceClientException;
import org.amdatu.ace.client.AceClientWorkspace;
import org.amdatu.ace.client.model.Artifact;
import org.amdatu.ace.client.model.Artifact2Feature;
import org.amdatu.ace.client.model.Artifact2FeatureBuilder;
import org.amdatu.ace.client.model.ArtifactBuilder;
import org.amdatu.ace.client.model.Distribution;
import org.amdatu.ace.client.model.Distribution2Target;
import org.amdatu.ace.client.model.Distribution2TargetBuilder;
import org.amdatu.ace.client.model.DistributionBuilder;
import org.amdatu.ace.client.model.Feature;
import org.amdatu.ace.client.model.Feature2Distribution;
import org.amdatu.ace.client.model.Feature2DistributionBuilder;
import org.amdatu.ace.client.model.FeatureBuilder;
import org.amdatu.ace.client.model.Target;
import org.amdatu.ace.client.model.TargetBuilder;

import aQute.bnd.osgi.Jar;



public class AceAssociationsInstaller {
	private AceClientWorkspace m_workspace;
	private String m_target = "default";
	private String m_feature = "default";
	private String m_distribution = "default";

	public AceAssociationsInstaller(AceClientWorkspace workspace,
			String feature, String distribution, String target) {
		m_workspace = workspace;
		m_feature = feature;
		m_distribution = distribution;
		m_target = target;
	}

	public void installArtifact(Jar jar) {
		Artifact artifact;
		try {
			artifact = new ArtifactBuilder().setBundleSymbolicName(jar.getBsn()).setBundleVersion(jar.getVersion()).build();

			createTarget();
			createDefaultDistribution();
			createDefaultFeature();
			createDistribution2Target();
			createFeature2Distribution();
			createArtifact2Feature(artifact);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

	private void createArtifact2Feature(Artifact artifact)
			throws AceClientException {
		if (!artifact2FeatureExists(artifact)) {

			Artifact2Feature artifact2Feature = new Artifact2FeatureBuilder()
					.setLeftEndpoint(
							"(&(Bundle-SymbolicName="
									+ artifact.getBundleSymbolicName()
									+ ")(Bundle-Version="
									+ artifact.getBundleVersion() + "))")
					.setRightEndpoint("(name=" + m_feature + ")")
					.setAttribute("left", "*").setAttribute("right", "*")
					.build();

			m_workspace.createResource(artifact2Feature);
		}
	}

	private void createDefaultFeature() throws AceClientException {
		if (!defaultFeatureExisists()) {
			Feature feature = new FeatureBuilder().setName(m_feature).build();
			m_workspace.createResource(feature);
		}

		createFeature2Distribution();
	}

	private void createFeature2Distribution() throws AceClientException {
		if (!defaultFeature2DistributionExisists()) {
			Feature2Distribution feature2Distribution = new Feature2DistributionBuilder()
					.setLeftEndpoint("(name=" + m_feature + ")")
					.setRightEndpoint("(name=" + m_distribution + ")").build();
			m_workspace.createResource(feature2Distribution);
		}
	}

	private void createDefaultDistribution() throws AceClientException {
		if (!defaultDistributionExisists()) {
			Distribution dist = new DistributionBuilder().setName(
					m_distribution).build();
			m_workspace.createResource(dist);

			createTarget();
			createDistribution2Target();

		}
	}

	private void createTarget() throws AceClientException {
		Target target = new TargetBuilder().setId(m_target).build();
		try {
			m_workspace.createResource(target);
		} catch (AceClientException ex) {
			// ignore, this happens when target already exists...
		}
	}

	private void createDistribution2Target() throws AceClientException {
		if (!defaultDistribution2TargetExists()) {
			Distribution2Target distribution2Target = new Distribution2TargetBuilder()
					.setLeftEndpoint("(name=" + m_distribution + ")")
					.setRightEndpoint("(id=" + m_target + ")").build();
			m_workspace.createResource(distribution2Target);
		}
	}

	private boolean defaultFeatureExisists() throws AceClientException {
		Feature[] features = m_workspace.getResources(Feature.class);
		for (Feature f : features) {
			if (f.getName().equals(m_feature)) {
				return true;
			}
		}

		return false;
	}

	private boolean artifact2FeatureExists(Artifact artifact)
			throws AceClientException {
		Artifact2Feature[] links = m_workspace
				.getResources(Artifact2Feature.class);
		for (Artifact2Feature a2f : links) {

			if (a2f.getLeftEndpoint().equals(
					"(&(Bundle-SymbolicName="
							+ artifact.getBundleSymbolicName()
							+ ")(Bundle-Version=" + artifact.getBundleVersion()
							+ "))")
					&& a2f.getRightEndpoint()
							.equals("(name=" + m_feature + ")")) {
				return true;
			}
		}

		return false;
	}

	private boolean defaultFeature2DistributionExisists()
			throws AceClientException {
		Feature2Distribution[] features = m_workspace
				.getResources(Feature2Distribution.class);
		for (Feature2Distribution f : features) {
			if (f.getLeftEndpoint().equals("(name=" + m_feature + ")")
					&& f.getRightEndpoint().equals(
							"(name=" + m_distribution + ")")) {
				return true;
			}
		}

		return false;
	}

	private boolean defaultDistributionExisists() throws AceClientException {
		Distribution[] distributions = m_workspace
				.getResources(Distribution.class);
		for (Distribution d : distributions) {
			if (d.getName().equals(m_distribution)) {
				return true;
			}
		}

		return false;
	}

	private boolean defaultDistribution2TargetExists()
			throws AceClientException {
		Distribution2Target[] features = m_workspace
				.getResources(Distribution2Target.class);
		for (Distribution2Target f : features) {
			if (f.getLeftEndpoint().equals("(name=" + m_distribution + ")")
					&& f.getRightEndpoint().equals("(id=" + m_target + ")")) {
				return true;
			}
		}

		return false;
	}

}
