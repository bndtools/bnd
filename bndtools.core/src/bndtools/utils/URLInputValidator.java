package bndtools.utils;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IInputValidator;

public class URLInputValidator implements IInputValidator {

    public String isValid(String newText) {
        try {
            @SuppressWarnings("unused")
            URL url = new URL(newText);
            return null;
        } catch (MalformedURLException e) {
            return "Invalid URL: " + e.getMessage();
        }
    }

}
