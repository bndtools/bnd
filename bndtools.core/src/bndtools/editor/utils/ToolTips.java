package bndtools.editor.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.help.Syntax;
import bndtools.Logger;
import bndtools.api.ILogger;

public class ToolTips {
    private static final ILogger logger = Logger.getLogger();

    static private String getStrippedExample(Syntax syntax, String constant) {
        String example = syntax.getExample();
        Pattern p = Pattern.compile("^(\\s*" + Pattern.quote(constant.trim()) + "\\s*)(:|=|:=)(\\s*)(.*?)\\s*$");
        Matcher matcher = p.matcher(example);
        if (matcher.matches()) {
            return example.substring(matcher.start(4), matcher.end(4));
        }

        return example;
    }

    static public void setupHeaderMessageAndToolTip(Control control, String constant) {
        Syntax syntax = Syntax.HELP.get(constant);
        if (syntax == null) {
            logger.logError("No bnd syntax found for " + constant, null);
            syntax = new Syntax(constant, "Description of " + constant, constant + ": Example for " + constant, null, null);
        }
        String se = getStrippedExample(syntax, constant);

        if (control instanceof Text) {
            Text text = (Text) control;
            text.setToolTipText(syntax.getLead() + "\n\nExample:\n" + se);
            text.setMessage(se);
        }
    }
}
