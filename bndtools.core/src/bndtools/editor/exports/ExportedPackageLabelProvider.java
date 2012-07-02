package bndtools.editor.exports;

import org.eclipse.jface.viewers.StyledString;

import bndtools.UIConstants;
import bndtools.editor.pkgpatterns.HeaderClauseLabelProvider;
import bndtools.model.clauses.ExportedPackage;

public class ExportedPackageLabelProvider extends HeaderClauseLabelProvider<ExportedPackage> {

    @Override
    protected void decorate(StyledString label, ExportedPackage clause) {
        if (clause.isProvided())
            label.append(" <provide>", UIConstants.ITALIC_QUALIFIER_STYLER);
    }

}
