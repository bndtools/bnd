package aQute.bnd.lsp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import aQute.bnd.lsp.BndLanguageServer;
import aQute.bnd.lsp.Constants;

class BndWizardServiceTest {
    @TempDir
    Path temporary;

    private JsonObject request(Map<String, Object> values) {
        return new Gson().toJsonTree(values).getAsJsonObject();
    }

    private BndWizardService.Catalog catalog(BndWizardService service, String resource) throws Exception {
        Path archive = temporary.resolve("template.zip");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("prefix/" + resource));
            zip.write("# generated\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Path index = temporary.resolve("index.bnd");
        Files.writeString(index, "-workspace-templates: " + archive.toUri() + ";name=Test\n");
        return service.list(request(Map.of("indexes", List.of(index.toUri().toString()))));
    }

    private JsonObject planRequest(BndWizardService.Catalog catalog, Path target) {
        return request(Map.of("catalogId", catalog.catalogId(), "targetUri", target.toUri().toString(),
            "templates", List.of(catalog.templates().get(0).id()), "trustedSources", true));
    }

    @Test
    void createsFromPreviewWithoutTouchingTargetEarly() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/build.bnd");
            Path target = temporary.resolve("workspace");
            var preview = service.plan(planRequest(catalog, target));
            assertThat(target).doesNotExist();
            assertThat(preview.files()).extracting(BndWizardService.PlannedFile::path).containsExactly("cnf/build.bnd");
            assertThatThrownBy(() -> service.apply(request(Map.of("planId", preview.planId())))).hasMessageContaining("Confirm");
            service.apply(request(Map.of("planId", preview.planId(), "confirmed", true)));
            assertThat(Files.readString(target.resolve("cnf/build.bnd"))).isEqualTo("# generated\n");
            assertThatThrownBy(() -> service.apply(request(Map.of("planId", preview.planId(), "confirmed", true))))
                .hasMessageContaining("expired");
        }
    }

    @Test
    void directCreationRequiresWarningConfirmationAndDoesNotCreateStagingDirectory() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/build.bnd");
            Path target = temporary.resolve("workspace");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "workspace");
            assertThatThrownBy(() -> service.create(request)).hasMessageContaining("warning");
            assertThat(target).doesNotExist();
            request.addProperty("confirmed", true);
            service.create(request);
            assertThat(Files.readString(target.resolve("cnf/build.bnd"))).isEqualTo("# generated\n");
            try (var entries = Files.list(temporary)) {
                assertThat(entries.map(path -> path.getFileName().toString()).toList()).noneMatch(name -> name.startsWith(".bnd-wizard-"));
            }
        }
    }

    @Test
    void directCreationRejectsDirtyDestinationsWithoutChangingFiles() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/build.bnd");
            Path target = temporary.resolve("workspace");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "workspace");
            request.addProperty("confirmed", true);
            request.add("dirtyUris", new Gson().toJsonTree(List.of(target.resolve("cnf/build.bnd").toUri().toString())));
            assertThatThrownBy(() -> service.create(request)).hasMessageContaining("unsaved");
            assertThat(target).doesNotExist();
        }
    }

    @Test
    void workspaceCatalogDefaultsToWorkspaceRepositoryNotFragments() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = service.list(request(Map.of("mode", "workspace")));
            assertThat(catalog.templates()).hasSize(1);
            assertThat(catalog.templates().get(0).name()).isEqualTo("bndtools/bndtools.workspace.min");
            assertThat(catalog.templates().get(0).id()).isEqualTo("https://github.com/bndtools/bndtools.workspace.min/tree/master/");
            assertThat(catalog.templates().get(0).tags()).containsExactly("workspace");
        }
    }

    @Test
    void customWorkspaceRepositoriesAndZipArchivesAreListedWithoutReadingFragmentIndexes() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = service.list(request(Map.of("mode", "workspace", "indexes", List.of(
                "https://github.com/example/custom-workspace/tree/main/template", "https://example.org/workspace.zip",
                temporary.resolve("local-workspace.zip").toUri().toString(),
                "https://github.com/example/workspace/archive/refs/heads/main.zip"))));
            assertThat(catalog.templates()).extracting(BndWizardService.Template::name)
                .containsExactly("example/custom-workspace", "workspace.zip", "local-workspace.zip", "main.zip");
            assertThat(catalog.templates().get(0).id()).isEqualTo("https://github.com/example/custom-workspace/tree/main/template");
        }
    }

    @Test
    void rejectsChangedOrNonemptyDestination() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/build.bnd");
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            var preview = service.plan(planRequest(catalog, target));
            Files.writeString(target.resolve("keep.txt"), "keep");
            assertThatThrownBy(() -> service.apply(request(Map.of("planId", preview.planId(), "confirmed", true))))
                .hasMessageContaining("not empty");
            assertThatThrownBy(() -> service.plan(planRequest(catalog, target))).hasMessageContaining("not empty");
            assertThat(Files.readString(target.resolve("keep.txt"))).isEqualTo("keep");
        }
    }

    @Test
    void closesPlansAndSupportsEmptyExistingDestination() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "readme.txt");
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            var canceled = service.plan(planRequest(catalog, target));
            service.dispose(request(Map.of("planId", canceled.planId())));
            assertThatThrownBy(() -> service.apply(request(Map.of("planId", canceled.planId(), "confirmed", true))))
                .hasMessageContaining("expired");
            var preview = service.plan(planRequest(catalog, target));
            service.apply(request(Map.of("planId", preview.planId(), "confirmed", true)));
            assertThat(target.resolve("cnf/build.bnd")).isRegularFile();
            assertThat(target.resolve("readme.txt")).isRegularFile();
        }
        try (var entries = Files.list(temporary)) {
            assertThat(entries.map(path -> path.getFileName().toString()).toList()).noneMatch(name -> name.startsWith(".bnd-wizard-"));
        }
    }

    @Test
    void rejectsUnconfirmedSourcesAndEscapingPaths() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/build.bnd");
            JsonObject request = planRequest(catalog, temporary.resolve("workspace"));
            request.addProperty("trustedSources", false);
            assertThatThrownBy(() -> service.plan(request)).hasMessageContaining("third-party");
            var escaping = catalog(service, "../../escape.txt");
            assertThatThrownBy(() -> service.plan(planRequest(escaping, temporary.resolve("workspace"))))
                .hasMessageContaining("outside");
            assertThat(temporary.getParent().resolve("escape.txt")).doesNotExist();
        }
    }

    @Test
    void preprocessesUsingFinalWorkspaceLocation() throws Exception {
        Path archive = temporary.resolve("macros.zip");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("prefix/tool.bnd"));
            zip.write("-tool: cnf/build.bnd;preprocess=true\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("prefix/cnf/build.bnd"));
            zip.write("root: ${workspace}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        Path index = temporary.resolve("macros-index.bnd");
        Files.writeString(index, "-workspace-templates: " + archive.toUri() + ";name=Macros\n");
        try (BndWizardService service = new BndWizardService()) {
            var catalog = service.list(request(Map.of("indexes", List.of(index.toUri().toString()))));
            Path target = temporary.resolve("workspace");
            var preview = service.plan(planRequest(catalog, target));
            String content = new String(java.util.Base64.getDecoder().decode(preview.files().get(0).contentBase64()),
                java.nio.charset.StandardCharsets.UTF_8);
            assertThat(content.replace('\\', '/')).contains(target.toString().replace('\\', '/'));
            assertThat(content).doesNotContain("bnd-wizard");
        }
    }

    @Test
    void createsInCurrentNonemptyFolderWithoutReplacingIt() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/build.bnd");
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            Files.writeString(target.resolve("keep.txt"), "keep");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "workspace");
            var preview = service.plan(request);
            service.apply(request(Map.of("planId", preview.planId(), "confirmed", true)));
            assertThat(Files.readString(target.resolve("keep.txt"))).isEqualTo("keep");
            assertThat(Files.readString(target.resolve("cnf/build.bnd"))).isEqualTo("# generated\n");
            assertThatThrownBy(() -> service.plan(request)).hasMessageContaining("already exists");
        }
    }

    @Test
    void addsFragmentsWithoutReplacingExistingConfiguration() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/ext/fragment.bnd");
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            Files.createDirectories(target.resolve("cnf"));
            Files.writeString(target.resolve("cnf/build.bnd"), "# keep\n");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "fragment");
            var preview = service.plan(request);
            assertThat(preview.files()).extracting(BndWizardService.PlannedFile::path).containsExactly("cnf/ext/fragment.bnd");
            service.apply(request(Map.of("planId", preview.planId(), "confirmed", true)));
            assertThat(Files.readString(target.resolve("cnf/build.bnd"))).isEqualTo("# keep\n");
            assertThat(target.resolve("cnf/ext/fragment.bnd")).isRegularFile();
        }
    }

    @Test
    void rejectsChangedTouchedFileBeforeAnyWrites() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/build.bnd");
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            Files.createDirectories(target.resolve("cnf"));
            Files.writeString(target.resolve("cnf/build.bnd"), "# keep\n");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "fragment");
            var preview = service.plan(request);
            assertThat(preview.files().get(0).exists()).isTrue();
            Files.writeString(target.resolve("cnf/build.bnd"), "# changed\n");
            assertThatThrownBy(() -> service.apply(request(Map.of("planId", preview.planId(), "confirmed", true))))
                .hasMessageContaining("changed since preview");
            assertThat(Files.readString(target.resolve("cnf/build.bnd"))).isEqualTo("# changed\n");
        }
    }

    @Test
    void protocolRequiresTrustAndAdvertisesCapabilities() throws Exception {
        BndLanguageServer server = new BndLanguageServer();
        try {
            var capabilities = server.initialize(new org.eclipse.lsp4j.InitializeParams()).get().getCapabilities();
            assertThat(capabilities.getExecuteCommandProvider().getCommands()).contains(Constants.COMMAND_WIZARD_LIST,
                Constants.COMMAND_WIZARD_PLAN, Constants.COMMAND_WIZARD_APPLY, Constants.COMMAND_WIZARD_DISPOSE);
            var result = server.getWorkspaceService().executeCommand(new org.eclipse.lsp4j.ExecuteCommandParams(
                Constants.COMMAND_WIZARD_LIST, List.of(Map.of()))).get();
            assertThat(result.toString()).contains("trusted");
        } finally {
            server.shutdown().get();
        }
    }

    private BndWizardService.Catalog catalog(BndWizardService service, Map<String, String> resources) throws Exception {
        Path archive = temporary.resolve("actions.zip");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(archive))) {
            for (var entry : resources.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry("prefix/" + entry.getKey()));
                zip.write(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        Path index = temporary.resolve("actions-index.bnd");
        Files.writeString(index, "-workspace-templates: " + archive.toUri() + ";name=Actions\n");
        return service.list(request(Map.of("indexes", List.of(index.toUri().toString()))));
    }

    @Test
    void appendRetainsExistingFileAndPreviewShowsFinalBytes() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, Map.of("tool.bnd", "-tool: cnf/build.bnd;append=true\n", "cnf/build.bnd", "# added\n"));
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            Files.createDirectories(target.resolve("cnf"));
            Files.writeString(target.resolve("cnf/build.bnd"), "# existing\n");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "fragment");
            var preview = service.plan(request);
            String previewText = new String(java.util.Base64.getDecoder().decode(preview.files().get(0).contentBase64()),
                java.nio.charset.StandardCharsets.UTF_8);
            assertThat(previewText).isEqualTo("# existing\n# added\n");
            service.apply(request(Map.of("planId", preview.planId(), "confirmed", true)));
            assertThat(Files.readString(target.resolve("cnf/build.bnd"))).isEqualTo(previewText);
        }
    }

    @Test
    void applicationFailureRollsBackOnlyTouchedFiles() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, Map.of("a.txt", "new", "z/block/file.txt", "blocked"));
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            Files.createDirectories(target.resolve("cnf"));
            Files.writeString(target.resolve("cnf/build.bnd"), "# keep\n");
            Files.writeString(target.resolve("a.txt"), "original");
            Files.writeString(target.resolve("keep.txt"), "keep");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "fragment");
            var preview = service.plan(request);
            Files.createDirectories(target.resolve("z"));
            Files.writeString(target.resolve("z/block"), "unrelated blocking file");
            assertThatThrownBy(() -> service.apply(request(Map.of("planId", preview.planId(), "confirmed", true))))
                .isInstanceOf(Exception.class);
            assertThat(Files.readString(target.resolve("a.txt"))).isEqualTo("original");
            assertThat(Files.readString(target.resolve("keep.txt"))).isEqualTo("keep");
            assertThat(Files.readString(target.resolve("z/block"))).isEqualTo("unrelated blocking file");
            assertThat(target.resolve("z/block/file.txt")).doesNotExist();
        }
    }

    @Test
    void fragmentOnUnconfiguredFolderCreatesConfigurationWithoutRemovingFiles() throws Exception {
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "cnf/ext/example.bnd");
            Path target = Files.createDirectory(temporary.resolve("workspace"));
            Files.writeString(target.resolve("keep.txt"), "keep");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "fragment");
            var preview = service.plan(request);
            service.apply(request(Map.of("planId", preview.planId(), "confirmed", true)));
            assertThat(target.resolve("cnf/build.bnd")).isRegularFile();
            assertThat(Files.readString(target.resolve("keep.txt"))).isEqualTo("keep");
        }
    }

    @Test
    void rejectsLinkedDestinationComponents() throws Exception {
        Path target = Files.createDirectory(temporary.resolve("workspace"));
        Path outside = Files.createDirectory(temporary.resolve("outside"));
        Path link = target.resolve("linked");
        if (aQute.lib.io.IO.isWindows()) {
            Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), outside.toString()).start();
            assertThat(process.waitFor()).isZero();
        } else {
            Files.createSymbolicLink(link, outside);
        }
        try (BndWizardService service = new BndWizardService()) {
            var catalog = catalog(service, "linked/file.bnd");
            JsonObject request = planRequest(catalog, target);
            request.addProperty("mode", "fragment");
            assertThatThrownBy(() -> service.plan(request)).hasMessageContaining("symbolic link");
            assertThat(outside.resolve("file.bnd")).doesNotExist();
        } finally {
            Files.delete(link);
        }
    }
}
