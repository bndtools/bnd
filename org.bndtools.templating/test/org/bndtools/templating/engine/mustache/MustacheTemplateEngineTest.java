package org.bndtools.templating.engine.mustache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.templating.FolderResource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.StringResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

import aQute.lib.io.IO;

public class MustacheTemplateEngineTest {

    @Test
    public void testBasic() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("{{srcDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/package-info.java", new StringResource("package {{packageName}};"));

        Map<String, List<Object>> params = new HashMap<>();
        params.put("srcDir", Collections.<Object> singletonList("src"));
        params.put("packageDir", Collections.<Object> singletonList("org/example/foo"));
        params.put("packageName", Collections.<Object> singletonList("org.example.foo"));
        ResourceMap output = engine.generateOutputs(input, params, new NullProgressMonitor());

        assertEquals(3, output.size());
        assertEquals("package org.example.foo;", IO.collect(output.get("src/org/example/foo/package-info.java")
            .getContent()));
    }

    @Test
    public void testIgnore() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("_template.properties", new StringResource("ignore=*/donotcopy.*"));
        input.put("{{srcDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/package-info.java", new StringResource("package {{packageName}};"));
        input.put("{{srcDir}}/{{packageDir}}/donotcopy.txt", new StringResource(""));

        Map<String, List<Object>> params = new HashMap<>();
        params.put("srcDir", Collections.<Object> singletonList("src"));
        params.put("packageDir", Collections.<Object> singletonList("org/example/foo"));
        params.put("packageName", Collections.<Object> singletonList("org.example.foo"));
        ResourceMap output = engine.generateOutputs(input, params, new NullProgressMonitor());

        assertEquals(3, output.size());
        assertEquals("package org.example.foo;", IO.collect(output.get("src/org/example/foo/package-info.java")
            .getContent()));
        assertNull(output.get("src/org/example/foo/donotcopy.txt"));
    }

    @Test
    public void testNoProcessDefaultPattern() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("{{srcDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/package-info.java", new StringResource("package {{packageName}};"));
        input.put("{{srcDir}}/{{packageDir}}/package-info.jpg", new StringResource("package {{packageName}};"));

        Map<String, List<Object>> params = new HashMap<>();
        params.put("srcDir", Collections.<Object> singletonList("src"));
        params.put("packageDir", Collections.<Object> singletonList("org/example/foo"));
        params.put("packageName", Collections.<Object> singletonList("org.example.foo"));
        ResourceMap output = engine.generateOutputs(input, params, new NullProgressMonitor());

        assertEquals(4, output.size());
        assertEquals("package org.example.foo;", IO.collect(output.get("src/org/example/foo/package-info.java")
            .getContent()));
        assertEquals("package {{packageName}};", IO.collect(output.get("src/org/example/foo/package-info.jpg")
            .getContent()));
    }

    @Test
    public void testNoProcessExtendedPattern() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("_template.properties", new StringResource("process.before=!*.java"));
        input.put("{{srcDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/", new FolderResource());
        input.put("{{srcDir}}/{{packageDir}}/package-info.java", new StringResource("package {{packageName}};"));

        Map<String, List<Object>> params = new HashMap<>();
        params.put("srcDir", Collections.<Object> singletonList("src"));
        params.put("packageDir", Collections.<Object> singletonList("org/example/foo"));
        params.put("packageName", Collections.<Object> singletonList("org.example.foo"));
        ResourceMap output = engine.generateOutputs(input, params, new NullProgressMonitor());

        assertEquals(3, output.size());
        assertEquals("package {{packageName}};", IO.collect(output.get("src/org/example/foo/package-info.java")
            .getContent()));
    }

    @Test
    public void testAlternativeDelimiters() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("readme.txt", new StringResource("{{=\u00ab \u00bb=}}Unprocessed: {{packageName}}. Processed: \u00abpackageName\u00bb"));

        Map<String, List<Object>> params = new HashMap<>();
        params.put("packageName", Collections.<Object> singletonList("org.example.foo"));
        ResourceMap output = engine.generateOutputs(input, params, new NullProgressMonitor());

        assertEquals(1, output.size());
        assertEquals("Unprocessed: {{packageName}}. Processed: org.example.foo", IO.collect(output.get("readme.txt")
            .getContent()));
    }

    @Test
    public void testAlternativeDelimiters2() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("_template.properties", new StringResource("leftDelim=_\nrightDelim=_"));
        input.put("readme.txt", new StringResource("Unprocessed: {{packageName}}. Processed: _packageName_"));

        Map<String, List<Object>> params = new HashMap<>();
        params.put("packageName", Collections.<Object> singletonList("org.example.foo"));
        ResourceMap output = engine.generateOutputs(input, params, new NullProgressMonitor());

        assertEquals(1, output.size());
        assertEquals("Unprocessed: {{packageName}}. Processed: org.example.foo", IO.collect(output.get("readme.txt")
            .getContent()));
    }

    @Test
    public void testGetParamNames() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("readme.txt", new StringResource("Blah {{fish}} blah {{battleship}} blah {{antidisestablishmentarianism}}"));

        Map<String, String> params = engine.getTemplateParameters(input, new NullProgressMonitor());
        assertTrue(params.containsKey("fish"));
        assertTrue(params.containsKey("battleship"));
        assertTrue(params.containsKey("antidisestablishmentarianism"));
    }

    @Test
    public void testGetDefaults() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("_defaults.properties", new StringResource("fish=carp\nbattleship=potemkin"));
        input.put("readme.txt", new StringResource("Blah {{fish}} blah {{battleship}} blah {{antidisestablishmentarianism}}"));

        Map<String, String> params = engine.getTemplateParameters(input, new NullProgressMonitor());
        assertEquals("carp", params.get("fish"));
        assertEquals("potemkin", params.get("battleship"));
        assertNull(params.get("antidisestablishmentarianism"));
    }

    @Test
    public void testApplyDefaults() throws Exception {
        MustacheTemplateEngine engine = new MustacheTemplateEngine();

        ResourceMap input = new ResourceMap();
        input.put("_defaults.properties", new StringResource("fish=carp\nbattleship=potemkin\nantidisestablishmentarianism=bigword"));
        input.put("readme.txt", new StringResource("Blah {{fish}} blah {{battleship}} blah {{antidisestablishmentarianism}}"));

        ResourceMap outputs = engine.generateOutputs(input, new HashMap<String, List<Object>>(), new NullProgressMonitor());
        assertEquals(1, outputs.size());
        assertEquals("Blah carp blah potemkin blah bigword", IO.collect(outputs.get("readme.txt")
            .getContent()));
    }

}
