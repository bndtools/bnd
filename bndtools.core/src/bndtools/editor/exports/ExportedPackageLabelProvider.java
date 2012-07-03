package bndtools.editor.exports;

import org.eclipse.jface.viewers.StyledString;

import aQute.bnd.build.model.clauses.ExportedPackage;
import bndtools.UIConstants;
import bndtools.editor.pkgpatterns.HeaderClauseLabelProvider;

public class ExportedPackageLabelProvider extends HeaderClauseLabelProvider<ExportedPackage> {

    @Override
    protected void decorate(StyledString label, ExportedPackage clause) {
        if (clause.isProvided())
            label.append(" <provide>", UIConstants.ITALIC_QUALIFIER_STYLER);
    }

}
