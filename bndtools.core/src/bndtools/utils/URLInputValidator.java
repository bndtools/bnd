package bndtools.utils;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IInputValidator;

public class URLInputValidator implements IInputValidator {
    public String isValid(String newText) {
        String s = null;
        boolean valid = true;
        try {
            s = new URL(newText).toString();
        } catch (MalformedURLException e) {
            s = "Invalid URL " + newText + ": " + e.getMessage();
            valid = false;
        }

        if (valid) {
            return null;
        }

        return s;
    }
}