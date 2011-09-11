package bndtools.editor.imports;

import org.eclipse.jface.viewers.StyledString;

import bndtools.UIConstants;
import bndtools.editor.pkgpatterns.HeaderClauseLabelProvider;
import bndtools.model.clauses.ImportPattern;

public class ImportPatternLabelProvider extends HeaderClauseLabelProvider<ImportPattern> {

    @Override
    protected void decorate(StyledString label, ImportPattern pattern) {
        if (pattern.isOptional())
            label.append(" <optional>", UIConstants.ITALIC_QUALIFIER_STYLER);
    }

}
