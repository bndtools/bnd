package aQute.bnd.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ConfigurableFileCollection;
import javax.inject.Inject;

import org.gradle.api.file.FileCollection;
import java.util.Objects;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.Internal;


/**
 * Gradle 9.x compatible Bundle task
 */
@CacheableTask
public abstract class Bundle extends DefaultTask {

    // Public properties
    public final Property<String> bndFile;
    public final DirectoryProperty outputDir;
    public final ConfigurableFileCollection classpath;

    private final BundleTaskExtension bundleExtension;

    @Inject
    public Bundle(ObjectFactory objects) {
        this.bndFile = objects.property(String.class);
        this.outputDir = objects.directoryProperty();
        this.classpath = objects.fileCollection();

        // Register an internal Jar task that does the actual packaging work
        var jarTaskProvider = getProject().getTasks().register(getName() + "Jar", Jar.class, jar -> {
            jar.from(classpath);
            jar.getDestinationDirectory().set(outputDir);
            jar.getArchiveBaseName().set(getName());
        });

        // ✅ Pass the actual Jar to the extension
        this.bundleExtension = getExtensions().create(
                BundleTaskExtension.NAME,
                BundleTaskExtension.class,
                jarTaskProvider.get()
        );
    }

    @TaskAction
    public void buildBundle() {
        // Run the build action defined by the extension
        bundleExtension.buildAction().execute(this);
    }

	@Internal
    public BundleTaskExtension getBundleExtension() {
        return bundleExtension;
    }

	public void from(Object... paths) {
		var jarTask = getProject().getTasks().named(getName() + "Jar", Jar.class).get();
		jarTask.from((Object[]) Objects.requireNonNull(paths));
	}

	public void into(Object destDir) {
		var jarTask = getProject().getTasks().named(getName() + "Jar", Jar.class).get();
		jarTask.into(destDir);
	}


	public void setArchiveBaseName(String name) {
		getProject().getTasks()
			.named(getName() + "Jar", Jar.class)
			.configure(jar -> jar.getArchiveBaseName().set(name));
	}

	public void setArchiveVersion(String version) {
		getProject().getTasks()
			.named(getName() + "Jar", Jar.class)
			.configure(jar -> jar.getArchiveVersion().set(version));
	}

	public void setArchiveClassifier(String classifier) {
		getProject().getTasks()
			.named(getName() + "Jar", Jar.class)
			.configure(jar -> jar.getArchiveClassifier().set(classifier));
	}


	@Internal
	public Provider<RegularFile> getArchiveFile() {
		return getProject().getTasks()
			.named(getName() + "Jar", Jar.class)
			.flatMap(Jar::getArchiveFile);
	}

	@OutputFile
	public Provider<RegularFile> getBundleArtifact() {
		return getArchiveFile();
	}
}
