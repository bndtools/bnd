package org.bndtools.refactor.types;

import org.bndtools.refactor.types.PackageInfoRefactorer.PackageEntry;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.junit.jupiter.api.Test;

class PackageInfoSourceTest {

	@Test
	public void testEnsureAnnotations() throws Exception {
		String source = """
				@org.osgi.annotation.versioning.ProviderType
				package foo;
				""";
		RefactorAssistant ass = new RefactorAssistant(source);
		PackageEntry pe = new PackageEntry("foo", true, "1.4.5", false);
		PackageInfoRefactorer.ensureThat(ass, pe, new NullProgressMonitor());
		TextEdit apply = ass.getTextEdit();
		System.out.println(apply);
		IDocument d = new Document(source);
		apply.apply(d);
		System.out.println(d.get());
	}

}
