package org.bndtools.builder.handlers.baseline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BaselineErrorHandlerTest {

	@Test
	void replaceVersionInExportPackage_simpleEntry() {
		String content = "Export-Package: com.example;version=\"1.0.0\"";
		int searchFrom = content.indexOf("com.example") + "com.example".length();
		assertThat(BaselineErrorHandler.replaceVersionInExportPackage(content, searchFrom, "2.0.0"))
			.isEqualTo("Export-Package: com.example;version=\"2.0.0\"");
	}

	@Test
	void replaceVersionInExportPackage_spacesAroundEquals() {
		String content = "Export-Package: com.example ; version = \"1.0.0\"";
		int searchFrom = content.indexOf("com.example") + "com.example".length();
		assertThat(BaselineErrorHandler.replaceVersionInExportPackage(content, searchFrom, "2.0.0"))
			.isEqualTo("Export-Package: com.example ; version = \"2.0.0\"");
	}

	@Test
	void replaceVersionInExportPackage_multiplePackages_firstMatched() {
		String content = "Export-Package: com.a;version=\"1.0.0\",com.b;version=\"2.0.0\"";
		int searchFrom = content.indexOf("com.a") + "com.a".length();
		assertThat(BaselineErrorHandler.replaceVersionInExportPackage(content, searchFrom, "1.5.0"))
			.isEqualTo("Export-Package: com.a;version=\"1.5.0\",com.b;version=\"2.0.0\"");
	}

	@Test
	void replaceVersionInExportPackage_multiplePackages_secondMatched() {
		String content = "Export-Package: com.a;version=\"1.0.0\",com.b;version=\"2.0.0\"";
		int searchFrom = content.indexOf("com.b") + "com.b".length();
		assertThat(BaselineErrorHandler.replaceVersionInExportPackage(content, searchFrom, "3.0.0"))
			.isEqualTo("Export-Package: com.a;version=\"1.0.0\",com.b;version=\"3.0.0\"");
	}

	@Test
	void replaceVersionInExportPackage_withAdditionalAttributes() {
		String content = "Export-Package: com.example;uses:=\"other\";version=\"1.0.0\"";
		int searchFrom = content.indexOf("com.example") + "com.example".length();
		assertThat(BaselineErrorHandler.replaceVersionInExportPackage(content, searchFrom, "2.0.0"))
			.isEqualTo("Export-Package: com.example;uses:=\"other\";version=\"2.0.0\"");
	}

	@Test
	void replaceVersionInExportPackage_noVersionAttribute_returnsNull() {
		String content = "Export-Package: com.example";
		int searchFrom = content.indexOf("com.example") + "com.example".length();
		assertThat(BaselineErrorHandler.replaceVersionInExportPackage(content, searchFrom, "2.0.0"))
			.isNull();
	}

	@Test
	void replaceVersionInExportPackage_multilineWithContinuation() {
		String content = "Export-Package: \\\n    com.example;version=\"1.0.0\"";
		int searchFrom = content.indexOf("com.example") + "com.example".length();
		assertThat(BaselineErrorHandler.replaceVersionInExportPackage(content, searchFrom, "2.0.0"))
			.isEqualTo("Export-Package: \\\n    com.example;version=\"2.0.0\"");
	}
}
