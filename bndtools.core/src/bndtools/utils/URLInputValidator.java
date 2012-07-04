package bndtools.utils;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IInputValidator;

public class URLInputValidator implements IInputValidator {

    public String isValid(String newText) {
        String s = null;
        try {
            if (new URL(newText) == null)
                s = "Invalid URL " + newText;
        } catch (MalformedURLException e) {
            s = "Invalid URL " + newText + ": " + e.getMessage();
        }

        return s;
    }
}
