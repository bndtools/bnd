package aQute.bnd.lsp.services;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.wstemplates.FragmentTemplateEngine;
import aQute.bnd.wstemplates.FragmentTemplateEngine.Action;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateInfo;
import aQute.bnd.wstemplates.FragmentTemplateEngine.Update;
import aQute.bnd.wstemplates.TemplateID;
import aQute.lib.io.IO;

public class BndWizardService implements AutoCloseable {
    private static final int MAX_FILES = 5000;
    private static final long MAX_BYTES = 32L * 1024 * 1024;
    private static final long LIFETIME_SECONDS = 15 * 60;

    public record Template(String id, String name, String description, boolean archived, boolean official,
        List<String> requires, List<String> tags) {}
    public record Catalog(String catalogId, List<Template> templates) {}
    public record PlannedFile(String path, String contentBase64, long size, boolean executable, boolean exists) {}
    public record Preview(String planId, String targetUri, List<Template> templates, List<PlannedFile> files) {}
    public record Applied(String targetUri, int files) {}

    private record CatalogState(List<TemplateInfo> templates, Instant expires) {}
    private record Original(String contentBase64, boolean executable) {}
    private record Plan(Path target, Path staging, boolean existed, boolean overlay, Map<String, Original> originals,
        List<PlannedFile> files, Instant expires) {}

    private final Map<String, CatalogState> catalogs = new LinkedHashMap<>();
    private final Map<String, Plan> plans = new LinkedHashMap<>();

    private static Template describe(TemplateInfo info) {
        return new Template(info.id().repoUrl(), info.name(), info.description(), info.archived(), info.isOfficial(),
            List.of(info.require()).stream().map(TemplateID::from).map(TemplateID::repoUrl).toList(), List.of(info.tag()));
    }

    private static TemplateInfo workspaceTemplate(URI uri) {
        if (uri.getPath() != null && uri.getPath().endsWith(".zip")) {
            TemplateID id = TemplateID.from(uri.toString());
            String name = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
            return new TemplateInfo(id, name, "Workspace template from " + uri, new String[0], "workspace");
        }
        if ("github.com".equalsIgnoreCase(uri.getHost())) {
            String[] parts = uri.getPath().replaceAll("^/|/$", "").split("/");
            if (parts.length < 2 || (parts.length > 2 && (parts.length < 4 || !parts[2].equals("tree")))) {
                throw new IllegalArgumentException("Use a GitHub repository URL or a repository/tree/ref/path URL.");
            }
            String repository = parts[1].replaceAll("\\.git$", "");
            String ref = parts.length >= 4 ? parts[3] : uri.getFragment() == null ? "master" : uri.getFragment();
            String subfolder = parts.length > 4 ? String.join("/", java.util.Arrays.copyOfRange(parts, 4, parts.length)) : "";
            TemplateID id = new TemplateID(parts[0], repository, subfolder, ref, null);
            return new TemplateInfo(id, parts[0] + "/" + repository,
                "Workspace template from " + uri, new String[0], "workspace");
        }
        throw new IllegalArgumentException("Workspace template locations require a GitHub repository, ZIP archive, or .bnd catalog URL.");
    }

    public synchronized Catalog list(JsonObject request) throws Exception {
        expire();
        if (catalogs.size() >= 10) throw new IllegalStateException("Too many open template catalogs. Close a wizard first.");
        Path bootstrap = Files.createTempDirectory("bnd-wizard-catalog-");
        try {
            Files.createDirectories(bootstrap.resolve("cnf"));
            Files.writeString(bootstrap.resolve("cnf/build.bnd"), "");
            try (Workspace workspace = new Workspace(bootstrap.toFile())) {
                FragmentTemplateEngine engine = new FragmentTemplateEngine(workspace);
                List<String> indexes = new ArrayList<>();
                boolean workspaceTemplates = request.has("mode") && request.get("mode").getAsString().equals("workspace");
                if (request.has("indexes")) {
                    request.getAsJsonArray("indexes").forEach(value -> indexes.add(value.getAsString()));
                } else {
                    indexes.add(workspaceTemplates ? "https://github.com/bndtools/bndtools.workspace.min" : FragmentTemplateEngine.DEFAULT_INDEX);
                }
                if (indexes.isEmpty() || indexes.size() > 10) throw new IllegalArgumentException("Select between one and ten template indexes.");
                for (String index : indexes) {
                    URI uri = URI.create(index);
                    if (!List.of("https", "http", "file").contains(uri.getScheme())) {
                        throw new IllegalArgumentException("Template indexes require an HTTP(S) or file URI.");
                    }
                    if (workspaceTemplates && (uri.getPath() == null || !uri.getPath().endsWith(".bnd"))) {
                        engine.add(workspaceTemplate(uri));
                    } else {
                        engine.read(uri.toURL()).unwrap().forEach(engine::add);
                    }
                }
                List<TemplateInfo> templates = engine.getAvailableTemplates();
                String id = UUID.randomUUID().toString();
                catalogs.put(id, new CatalogState(templates, Instant.now().plusSeconds(LIFETIME_SECONDS)));
                return new Catalog(id, templates.stream().map(BndWizardService::describe).toList());
            }
        } finally {
            IO.delete(bootstrap.toFile());
        }
    }

    private List<TemplateInfo> selection(JsonObject request) {
        CatalogState catalog = catalogs.get(request.get("catalogId").getAsString());
        if (catalog == null) throw new IllegalStateException("Template catalog expired. Start the wizard again.");
        List<TemplateInfo> selected = new ArrayList<>();
        request.getAsJsonArray("templates").forEach(value -> selected.add(catalog.templates.stream()
            .filter(info -> info.id().repoUrl().equals(value.getAsString())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown template: " + value.getAsString()))));
        if (selected.isEmpty()) throw new IllegalArgumentException("Select at least one workspace fragment.");
        return selected;
    }

    private static void requireDependencies(FragmentTemplateEngine engine, List<TemplateInfo> selected) {
        for (TemplateInfo info : selected) {
            for (String required : info.require()) {
                if (engine.getAvailableTemplates().stream().noneMatch(candidate -> candidate.id()
                    .equals(aQute.bnd.wstemplates.TemplateID.from(required.trim())))) {
                    throw new IllegalArgumentException("Missing required fragment: " + required);
                }
            }
        }
    }

    private static Path target(JsonObject request) throws Exception {
        URI uri = URI.create(request.get("targetUri").getAsString());
        if (!"file".equals(uri.getScheme())) throw new IllegalArgumentException("Creation requires a local file URI.");
        Path target = Path.of(uri).toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent)) throw new IllegalArgumentException("Select an existing parent folder.");
        return parent.toRealPath().resolve(target.getFileName());
    }

    private static void checkEmpty(Path target) throws Exception {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(target) || !Files.isDirectory(target) || !target.toRealPath().equals(target)) {
            throw new IllegalArgumentException("Destination must be a new or empty directory, not a symbolic link.");
        }
        try (var entries = Files.list(target)) {
            if (entries.findAny().isPresent()) throw new IllegalArgumentException("Destination is not empty. Existing workspaces must not be replaced.");
        }
    }

    private static void checkDirectory(Path target) throws Exception {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(target) || !Files.isDirectory(target) || !target.toRealPath().equals(target)) {
            throw new IllegalArgumentException("Destination must be a local directory, not a symbolic link.");
        }
    }

    private static Path destination(Path target, String relative) throws Exception {
        Path file = target.resolve(relative).normalize();
        if (!file.startsWith(target) || file.equals(target)) throw new SecurityException("Template path escapes its destination.");
        for (Path current = file; current != null && current.startsWith(target); current = current.getParent()) {
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                && (Files.isSymbolicLink(current) || !current.toRealPath().equals(current))) {
                throw new SecurityException("Template destination contains a symbolic link: " + current);
            }
        }
        return file;
    }

    private static Original original(Path file) throws Exception {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return new Original(null, false);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Template would replace a directory: " + file);
        if (Files.size(file) > MAX_BYTES) throw new IllegalArgumentException("Existing file exceeds 32 MiB: " + file);
        return new Original(Base64.getEncoder().encodeToString(Files.readAllBytes(file)), Files.isExecutable(file));
    }

    public synchronized Preview plan(JsonObject request) throws Exception {
        return prepare(request, false);
    }

    public synchronized Applied create(JsonObject request) throws Exception {
        if (!request.has("confirmed") || !request.get("confirmed").getAsBoolean()) {
            throw new IllegalArgumentException("Confirm the template warning before creating a workspace.");
        }
        if (!request.has("mode")) request.addProperty("mode", "workspace");
        if (!List.of("workspace", "fragment").contains(request.get("mode").getAsString())) {
            throw new IllegalArgumentException("Direct creation requires workspace or fragment mode.");
        }
        Preview generated = prepare(request, true);
        JsonObject applyRequest = new JsonObject();
        applyRequest.addProperty("planId", generated.planId());
        applyRequest.addProperty("confirmed", true);
        try {
            return apply(applyRequest);
        } finally {
            dispose(applyRequest);
        }
    }

    private Preview prepare(JsonObject request, boolean direct) throws Exception {
        expire();
        if (plans.size() >= 10) throw new IllegalStateException("Too many pending workspace plans. Close a wizard first.");
        Path target = target(request);
        String mode = request.has("mode") ? request.get("mode").getAsString() : "legacy";
        if (!List.of("legacy", "workspace", "fragment").contains(mode)) throw new IllegalArgumentException("Unknown workspace wizard mode.");
        boolean overlay = !mode.equals("legacy");
        if (overlay) checkDirectory(target);
        else checkEmpty(target);
        if (mode.equals("workspace") && Files.exists(target.resolve("cnf/build.bnd"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("A bnd workspace already exists here. Use Add Fragment to (New) Workspace instead.");
        }
        destination(target, "cnf/build.bnd");
        boolean existed = Files.exists(target);
        CatalogState catalog = catalogs.get(request.get("catalogId").getAsString());
        List<TemplateInfo> selected = selection(request);
        Path staging = direct ? null : Files.createTempDirectory(target.getParent(), ".bnd-wizard-").toRealPath();
        Path bootstrap = Files.createTempDirectory("bnd-wizard-context-");
        boolean retained = false;
        Map<String, Original> originals = new LinkedHashMap<>();
        List<PlannedFile> generated = new ArrayList<>();
        java.util.Set<Path> dirtyFiles = new java.util.HashSet<>();
        if (request.has("dirtyUris")) request.getAsJsonArray("dirtyUris").forEach(value -> {
            URI uri = URI.create(value.getAsString());
            if ("file".equals(uri.getScheme())) dirtyFiles.add(Path.of(uri).toAbsolutePath().normalize());
        });
        try {
            Files.createDirectories(bootstrap.resolve("cnf"));
            Files.writeString(bootstrap.resolve("cnf/build.bnd"), "");
            try (Workspace workspace = new Workspace(mode.equals("fragment") && Files.isRegularFile(target.resolve("cnf/build.bnd"))
                ? target.toFile() : bootstrap.toFile())) {
                workspace.setBase(target.toFile());
                FragmentTemplateEngine engine = new FragmentTemplateEngine(workspace);
                catalog.templates.forEach(engine::add);
                List<TemplateInfo> resolved = engine.resolveRequirements(selected);
                requireDependencies(engine, resolved);
                if (resolved.stream().anyMatch(info -> !info.isOfficial())
                    && (!request.has("trustedSources") || !request.get("trustedSources").getAsBoolean())) {
                    throw new IllegalArgumentException("Confirm trust in all third-party fragments and their dependencies before preview.");
                }
                try (var updater = engine.updater(target.toFile(), selected)) {
                    if (updater.updaters().size() > MAX_FILES) throw new IllegalArgumentException("Template output exceeds 5000 files.");
                    long originalBytes = 0;
                    for (var entry : updater.updaters().entrySet()) {
                        Path destination = entry.getKey().toPath().toAbsolutePath().normalize();
                        if (!destination.startsWith(target) || destination.equals(target)) {
                            throw new SecurityException("Template path escapes its destination: " + destination);
                        }
                        if (entry.getValue().stream().anyMatch(update -> update.actions().contains(Action.delete))) {
                            throw new IllegalArgumentException("New-workspace templates cannot delete files.");
                        }
                        if (entry.getValue().size() > 1 && (!request.has("compose") || !request.get("compose").getAsBoolean())) {
                            throw new IllegalArgumentException("Fragments compose the same file. Confirm source composition before preview.");
                        }
                        String relative = target.relativize(destination).toString().replace(File.separatorChar, '/');
                        if (dirtyFiles.contains(destination)) throw new IllegalArgumentException("Save or revert unsaved changes before applying templates: " + relative);
                        Original previous = original(destination(target, relative));
                        originalBytes += previous.contentBase64 == null ? 0 : previous.contentBase64.length();
                        if (originalBytes > MAX_BYTES * 4 / 3 + 4) throw new IllegalArgumentException("Existing files to update exceed 32 MiB.");
                        originals.put(relative, previous);
                    }
                    try (Processor processor = new Processor(workspace)) {
                        processor.setBase(target.toFile());
                        long generatedBytes = 0;
                        for (var entry : updater.updaters().entrySet()) {
                            Path relative = target.relativize(entry.getKey().toPath().toAbsolutePath().normalize());
                            Original previous = originals.get(relative.toString().replace(File.separatorChar, '/'));
                            boolean executable = previous.executable;
                            try (var stream = new java.io.ByteArrayOutputStream()) {
                                if (previous.contentBase64 != null && entry.getValue().stream().anyMatch(update -> update.actions().contains(Action.append))) {
                                    stream.write(Base64.getDecoder().decode(previous.contentBase64));
                                }
                                for (Update update : entry.getValue()) {
                                    if (update.actions().contains(Action.preprocess)) {
                                        String text = IO.collect(update.from().openInputStream());
                                        String expanded = processor.getReplacer().process(text);
                                        if (!processor.isOk()) throw new IllegalArgumentException("Template macro evaluation failed: " + processor.getErrors());
                                        stream.write(expanded.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                    } else {
                                        try (var input = update.from().openInputStream()) {
                                            byte[] content = input.readNBytes((int) MAX_BYTES + 1);
                                            if (content.length > MAX_BYTES) throw new IllegalArgumentException("Template output exceeds 32 MiB.");
                                            stream.write(content);
                                        }
                                    }
                                    executable |= update.actions().contains(Action.exec);
                                    if (stream.size() > MAX_BYTES) throw new IllegalArgumentException("Template output exceeds 32 MiB.");
                                }
                                byte[] content = stream.toByteArray();
                                generatedBytes += content.length;
                                if (generatedBytes > MAX_BYTES) throw new IllegalArgumentException("Template output exceeds 32 MiB.");
                                String name = relative.toString().replace(File.separatorChar, '/');
                                generated.add(new PlannedFile(name, Base64.getEncoder().encodeToString(content), content.length,
                                    executable, previous.contentBase64 != null));
                                if (staging != null) {
                                    Path output = staging.resolve(relative);
                                    Files.createDirectories(output.getParent());
                                    Files.write(output, content);
                                    if (executable && !IO.isWindows()) output.toFile().setExecutable(true);
                                }
                            }
                        }
                    }
                }
                if (generated.stream().noneMatch(file -> file.path.equals("cnf/build.bnd")) && !Files.isRegularFile(destination(target, "cnf/build.bnd"))) {
                    if (dirtyFiles.contains(target.resolve("cnf/build.bnd"))) throw new IllegalArgumentException("Save or revert unsaved changes before applying templates: cnf/build.bnd");
                    generated.add(new PlannedFile("cnf/build.bnd", "", 0, false, false));
                    if (staging != null) {
                        Files.createDirectories(staging.resolve("cnf"));
                        Files.writeString(staging.resolve("cnf/build.bnd"), "");
                    }
                    originals.put("cnf/build.bnd", original(destination(target, "cnf/build.bnd")));
                }
                if (generated.stream().noneMatch(file -> file.path.equals("cnf/build.bnd")) && !Files.isRegularFile(destination(target, "cnf/build.bnd"))) {
                    throw new IllegalArgumentException("Templates did not produce a valid cnf/build.bnd file.");
                }
                if (generated.size() > MAX_FILES) throw new IllegalArgumentException("Template output exceeds 5000 files.");
                List<PlannedFile> files = staging == null ? generated.stream().sorted(java.util.Comparator.comparing(PlannedFile::path)).toList()
                    : snapshot(staging, originals);
                String id = UUID.randomUUID().toString();
                plans.put(id, new Plan(target, staging, existed, overlay, originals, files, Instant.now().plusSeconds(LIFETIME_SECONDS)));
                retained = true;
                return new Preview(id, target.toUri().toString(), resolved.stream().map(BndWizardService::describe).toList(), files);
            }
        } finally {
            IO.delete(bootstrap.toFile());
            if (!retained && staging != null) IO.delete(staging.toFile());
        }
    }

    private static List<PlannedFile> snapshot(Path staging, Map<String, Original> originals) throws Exception {
        List<PlannedFile> files = new ArrayList<>();
        long bytes = 0;
        try (var walk = Files.walk(staging)) {
            for (Path file : walk.sorted().toList()) {
                if (Files.isSymbolicLink(file) || !file.toRealPath().startsWith(staging)) {
                    throw new SecurityException("Template produced a path outside its destination.");
                }
                if (Files.isDirectory(file)) continue;
                long size = Files.size(file);
                bytes += size;
                if (bytes > MAX_BYTES || files.size() >= MAX_FILES) throw new IllegalArgumentException("Template output exceeds 32 MiB or 5000 files.");
                String relative = staging.relativize(file).toString().replace(File.separatorChar, '/');
                files.add(new PlannedFile(relative, Base64.getEncoder().encodeToString(Files.readAllBytes(file)), size,
                    Files.isExecutable(file), originals.containsKey(relative) && originals.get(relative).contentBase64 != null));
            }
        }
        return files;
    }

    public synchronized Applied apply(JsonObject request) throws Exception {
        expire();
        String id = request.get("planId").getAsString();
        Plan plan = plans.get(id);
        if (plan == null) throw new IllegalStateException("Workspace plan expired. Start the wizard again.");
        if (!request.has("confirmed") || !request.get("confirmed").getAsBoolean()) {
            throw new IllegalArgumentException("Confirm the workspace preview before applying.");
        }
        if (!plan.target.getParent().toRealPath().equals(plan.target.getParent())) {
            throw new SecurityException("Destination parent changed since preview.");
        }
        if (Files.exists(plan.target, LinkOption.NOFOLLOW_LINKS) != plan.existed) {
            throw new IllegalStateException("Destination changed since preview.");
        }
        if (plan.staging != null && !snapshot(plan.staging, plan.originals).equals(plan.files)) throw new IllegalStateException("Staged files changed since preview.");
        if (plan.overlay) {
            applyOverlay(plan);
            plans.remove(id);
            if (plan.staging != null) IO.delete(plan.staging.toFile());
            return new Applied(plan.target.toUri().toString(), plan.files.size());
        }
        checkEmpty(plan.target);
        Path backup = null;
        if (plan.existed) {
            backup = Files.createTempDirectory(plan.target.getParent(), ".bnd-wizard-empty-");
            Files.delete(backup);
            Files.move(plan.target, backup, StandardCopyOption.ATOMIC_MOVE);
        }
        try {
            Files.move(plan.staging, plan.target, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception error) {
            if (backup != null) Files.move(backup, plan.target, StandardCopyOption.ATOMIC_MOVE);
            throw error;
        }
        plans.remove(id);
        if (backup != null) Files.delete(backup);
        return new Applied(plan.target.toUri().toString(), plan.files.size());
    }

    private static void checkOriginals(Plan plan) throws Exception {
        checkDirectory(plan.target);
        for (var entry : plan.originals.entrySet()) {
            if (!original(destination(plan.target, entry.getKey())).equals(entry.getValue())) {
                throw new IllegalStateException("Destination changed since preview: " + entry.getKey());
            }
        }
    }

    private static void createDirectories(Path directory, List<Path> created) throws Exception {
        if (Files.isDirectory(directory)) return;
        createDirectories(directory.getParent(), created);
        Files.createDirectory(directory);
        created.add(directory);
    }

    private static void applyOverlay(Plan plan) throws Exception {
        checkOriginals(plan);
        List<Path> createdDirectories = new ArrayList<>();
        List<String> written = new ArrayList<>();
        try {
            for (PlannedFile file : plan.files) {
                Path output = destination(plan.target, file.path);
                if (!original(output).equals(plan.originals.get(file.path))) {
                    throw new IllegalStateException("Destination changed since preview: " + file.path);
                }
                createDirectories(output.getParent(), createdDirectories);
                Path pending = Files.createTempFile(output.getParent(), ".bnd-wizard-", ".tmp");
                try {
                    Files.write(pending, Base64.getDecoder().decode(file.contentBase64));
                    if (!IO.isWindows()) pending.toFile().setExecutable(file.executable);
                    Files.move(pending, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    written.add(file.path);
                } finally {
                    Files.deleteIfExists(pending);
                }
            }
        } catch (Exception error) {
            for (int index = written.size() - 1; index >= 0; index--) {
                String relative = written.get(index);
                Original previous = plan.originals.get(relative);
                try {
                    Path output = destination(plan.target, relative);
                    if (previous.contentBase64 == null) Files.deleteIfExists(output);
                    else {
                        Files.write(output, Base64.getDecoder().decode(previous.contentBase64));
                        if (!IO.isWindows()) output.toFile().setExecutable(previous.executable);
                    }
                } catch (Exception rollback) { error.addSuppressed(rollback); }
            }
            for (int index = createdDirectories.size() - 1; index >= 0; index--) {
                try { Files.delete(createdDirectories.get(index)); }
                catch (Exception rollback) { error.addSuppressed(rollback); }
            }
            throw error;
        }
    }

    public synchronized Map<String, Boolean> dispose(JsonObject request) {
        Plan plan = request.has("planId") ? plans.remove(request.get("planId").getAsString()) : null;
        if (plan != null && plan.staging != null) IO.delete(plan.staging.toFile());
        if (request.has("catalogId")) catalogs.remove(request.get("catalogId").getAsString());
        return Map.of("disposed", true);
    }

    private void expire() {
        Instant now = Instant.now();
        catalogs.values().removeIf(catalog -> catalog.expires.isBefore(now));
        plans.values().removeIf(plan -> {
            if (!plan.expires.isBefore(now)) return false;
            if (plan.staging != null) IO.delete(plan.staging.toFile());
            return true;
        });
    }

    @Override
    public synchronized void close() {
        plans.values().forEach(plan -> { if (plan.staging != null) IO.delete(plan.staging.toFile()); });
        plans.clear();
        catalogs.clear();
    }
}
