package org.bndtools.core.editors.pkginfo;

import org.eclipse.ui.editors.text.TextEditor;

public class PackageInfoEditor extends TextEditor {

    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        setDocumentProvider(new PackageInfoDocumentProvider());
        setRulerContextMenuId("#PackageInfoRuleContext");
        setSourceViewerConfiguration(new PackageInfoSourceViewerConfiguration());
    }

}
