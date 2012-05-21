package bndtools.editor.model;

import java.util.Properties;

class PropertiesParser {

    /**
     * Get the key for the specified line of a properties file, as specified in the {@link Properties} specification.
     * @param line
     * @return
     */
    public static String getPropertyKey(String line) {
        char[] chars = line.toCharArray();
        StringBuilder result = new StringBuilder();

        boolean started = false;
        
        int index = 0;
        while (true) {
            if (index >= chars.length)
                break;
            char c = chars[index];
            
            if (c == '\\') {
                index++;
                result.append(chars[index]);
                index++;
                continue;
            }
            
            if (c == '=' || c == ':')
                break; // terminates key

            if (!started && (c == '#' || c == '!'))
                return null; // comment line
            
            if (Character.isWhitespace(c)) {
                if (started)
                    break; // whitespace terminates the key
            } else {
                started = true;
                result.append(c);
            }
            
            index++;
        }
        
        if (!started)
            return null; // empty line
        return result.toString();
    }

}
