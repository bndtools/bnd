package aQute.libg.glob;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Glob {

    private final String glob;
    private final Pattern pattern;

    public Glob(String globString) {
        this.glob = globString;
        this.pattern = Pattern.compile(convertGlobToRegEx(globString));
    }

    public Matcher matcher(CharSequence input) {
        return pattern.matcher(input);
    }

    @Override
    public String toString() {
        return glob;
    }

    private static String convertGlobToRegEx(String line) {
        String linei = line.trim();
        int strLen = linei.length();
        StringBuilder sb = new StringBuilder(strLen);
        // Remove beginning and ending * globs because they're useless
        if (linei.startsWith("*")) {
            linei = linei.substring(1);
            strLen--;
        }
        if (linei.endsWith("*")) {
            linei = linei.substring(0, strLen - 1);
            strLen--;
        }
        boolean escaping = false;
        int inCurlies = 0;
        for (char currentChar : linei.toCharArray()) {
            switch (currentChar) {
            case '*' :
                if (escaping)
                    sb.append("\\*");
                else
                    sb.append(".*");
                escaping = false;
                break;
            case '?' :
                if (escaping)
                    sb.append("\\?");
                else
                    sb.append('.');
                escaping = false;
                break;
            case '.' :
            case '(' :
            case ')' :
            case '+' :
            case '|' :
            case '^' :
            case '$' :
            case '@' :
            case '%' :
                sb.append('\\');
                sb.append(currentChar);
                escaping = false;
                break;
            case '\\' :
                if (escaping) {
                    sb.append("\\\\");
                    escaping = false;
                } else
                    escaping = true;
                break;
            case '{' :
                if (escaping) {
                    sb.append("\\{");
                } else {
                    sb.append('(');
                    inCurlies++;
                }
                escaping = false;
                break;
            case '}' :
                if (inCurlies > 0 && !escaping) {
                    sb.append(')');
                    inCurlies--;
                } else if (escaping)
                    sb.append("\\}");
                else
                    sb.append("}");
                escaping = false;
                break;
            case ',' :
                if (inCurlies > 0 && !escaping) {
                    sb.append('|');
                } else if (escaping)
                    sb.append("\\,");
                else
                    sb.append(",");
                break;
            default :
                escaping = false;
                sb.append(currentChar);
            }
        }
        return sb.toString();
    }
}
