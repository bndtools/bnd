package aQute.launchpad;

import static aQute.bnd.osgi.Constants.EXPORT_CONTENTS;
import static aQute.bnd.osgi.Constants.SERVICE_COMPONENT;
import static aQute.lib.exceptions.Exceptions.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.Glob;

public class StandaloneLaunchpadBuilder extends AbstractLaunchpadBuilder<StandaloneLaunchpadBuilder> {

	private final Run run;

	public StandaloneLaunchpadBuilder(File bndrunFile) {
		this(bndrunFile, unchecked(() -> Run.createRun(null, bndrunFile)));
	}

	StandaloneLaunchpadBuilder(File bndrunFile, Run run) {
		super(bndrunFile.getParentFile(), new AbstractRemoteWorkspace(run));
		this.run = run;
		RunSpecification analyzedTestSetup = remoteWorkspace.analyzeTestSetup(projectDir.getAbsolutePath());
		analyzedTestSetup.mergeWith(run.getSpecification());
		local.mergeWith(analyzedTestSetup);
	}

	public Run getRun() {
		return run;
	}

	static class AbstractRemoteWorkspace implements RemoteWorkspace {

		private final Run run;

		public AbstractRemoteWorkspace(Run run) {
			this.run = run;
		}

		@Override
		public String getBndVersion() {
			return About.CURRENT.toString();
		}

		@Override
		public RunSpecification getRun(String pathToBndOrBndrun) {
			return unchecked(() -> {
				File file = new File(pathToBndOrBndrun);

				if (file.isDirectory()) {
					Project project = run.getWorkspace()
						.getProjectFromFile(file);
					if (project == null)
						throw new IllegalArgumentException(
							"No such project " + pathToBndOrBndrun + " in workspace " + run);

					return project.getSpecification();

				} else {
					try (Run run = new Run(AbstractRemoteWorkspace.this.run.getWorkspace(), file)) {
						run.addIncluded(run.getPropertiesFile());
						return run.getSpecification();
					}
				}
			});
		}

		@Override
		public List<String> getLatestBundles(String projectDir, String specification) {
			return unchecked(() -> Container.toPaths(null, run.getBundles(Strategy.HIGHEST, specification, "remote")));
		}

		@Override
		public RunSpecification analyzeTestSetup(String projectDir) {
			return unchecked(() -> {
				RunSpecification r = run.getSpecification();

				try (Builder a = new Builder()) {
					a.setJar(run.getTestOutput());
					a.setConditionalPackage("!java.*,*");
					a.setProperty(EXPORT_CONTENTS, "*");

					a.addClasspath(run.getOutput());

					for (Container c : run.getTestpath()) {
						if (c.getError() != null)
							r.errors.add("Not a valid testpath entry " + c + " " + c.getError());
						else
							a.addClasspath(c.getFile());
					}

					for (Container c : run.getBuildpath()) {
						if (c.getError() != null)
							r.errors.add("Not a valid buildpath entry " + c + " " + c.getError());
						else
							a.addClasspath(c.getFile());
					}

					a.build();
					r.errors.addAll(a.getErrors());

					String clauses = Processor.printClauses(a.getExports());

					Parameters extraPackages = new Parameters(clauses);

					r.extraSystemPackages.putAll(extraPackages.toBasic());
					return r;
				}
			});
		}

		@Override
		public byte[] build(String projectPath, BuilderSpecification spec) {
			return unchecked(() -> {
				File projectDir = new File(projectPath);

				try (Builder builder = getBuilder(run, spec.parent)) {
					builder.setBase(run.getBase());

					Jar build = builder.from(spec)
						.build();

					if (!builder.isOk()) {
						throw new IllegalStateException(builder.getErrors()
							.stream()
							.collect(Collectors.joining("\n")));
					}

					if (spec.testBundle != null) {
						File[] buildFiles = run.getBuildFiles(false);
						if (buildFiles == null)
							throw new IllegalStateException(projectPath + ": merge requested but has no build JAR");

						Glob glob = new Glob(spec.testBundle);
						boolean found = false;
						buildfiles: for (File f : buildFiles) {
							if (glob.matches(f.getName())) {
								found = true;

								Jar jar = new Jar(buildFiles[0]);
								builder.addClose(jar);
								jar.addAll(build);
								Manifest manifest = jar.getManifest();
								manifest.getMainAttributes()
									.putValue("DynamicImport-Package", "*");
								merge(manifest, build.getManifest(), SERVICE_COMPONENT);
								jar.setManifest(manifest);
								build = jar;
								break buildfiles;
							}
						}
					}
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					build.write(bout);
					return bout.toByteArray();
				}
			});
		}

		private void merge(Manifest a, Manifest b, String hdr) {
			if (a == null)
				return;

			if (b == null)
				return;

			String aa = a.getMainAttributes()
				.getValue(hdr);
			String bb = b.getMainAttributes()
				.getValue(hdr);
			if (aa == null && bb == null)
				return;

			if (bb == null)
				return;

			if (aa == null) {
				a.getMainAttributes()
					.putValue(hdr, bb);
				return;
			}

			String cc = aa + "," + bb;
			a.getMainAttributes()
				.putValue(hdr, cc);
		}

		/**
		 * Calculate the builder with the properly inheritance structure.
		 *
		 * @param project The context project
		 * @param parent The comma separated parent string. The last entry may
		 *            be either PROJECT or WORKSPACE
		 */
		private Builder getBuilder(Project ignored, List<String> parent) throws Exception {
			if (parent == null || parent.isEmpty())
				return new Builder();

			Builder builder = new Builder();
			builder.setBase(run.getBase());

			List<String> paths = new ArrayList<>(parent);
			String last = paths.get(paths.size() - 1);

			boolean workspaceParent = BuilderSpecification.WORKSPACE.equals(last);
			boolean projectParent = BuilderSpecification.PROJECT.equals(last);
			if (workspaceParent || projectParent) {

				paths.remove(paths.size() - 1);

				if (projectParent)
					builder.setParent(run);
				else if (workspaceParent)
					builder.setParent(run.getWorkspace());
			}

			workspaceParent = paths.remove(BuilderSpecification.WORKSPACE);
			projectParent = paths.remove(BuilderSpecification.PROJECT);

			if (workspaceParent || projectParent) {
				builder.error("PROJECT or WORKSPACE parent can only be specified as the last entry");
			}

			// earlier entries are prioritized
			Collections.reverse(paths);

			paths.forEach(path -> {
				File file = new File(path);
				try {
					if (file.isFile()) {
						UTF8Properties p = new UTF8Properties();
						p.load(file, builder);
						builder.setProperties(p);
					} else
						builder.error("specified file %s as parent for build but no such file exist", file.toString());
				} catch (Exception e) {
					builder.exception(e, "Reading properties %s", file);
				}
			});
			return builder;
		}

		void doPackage(Parameters extraPackages, PackageRef p, Attrs a) {
			Attrs attrs = new Attrs(a);

			String v = attrs.getVersion();

			if (v != null) {
				VersionRange vr = VersionRange.parseOSGiVersionRange(v);
				Version version = vr.getLow();
				attrs.put("version", version.toString());
			}

			extraPackages.put(p.getFQN(), attrs);
		}

		@Override
		public List<String> getProjects() {
			return Collections.singletonList(run.toString());
		}

		@Override
		public void close() throws IOException {}

	}

}
