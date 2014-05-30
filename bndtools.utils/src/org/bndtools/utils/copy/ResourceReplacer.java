package org.bndtools.utils.copy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public class ResourceReplacer extends Thread {
    private PipedInputStream in = null;
    private PipedOutputStream out = null;
    private Map<String,String> replaceRegularExpressions = null;
    private URL url = null;
    private IOException result = null;

    public ResourceReplacer(Map<String,String> replaceRegularExpressions, URL url) throws IOException {
        if ((replaceRegularExpressions == null) || (replaceRegularExpressions.isEmpty())) {
            this.replaceRegularExpressions = Collections.emptyMap();
        } else {
            this.replaceRegularExpressions = replaceRegularExpressions;
        }
        this.url = url;
        in = new PipedInputStream();
        out = new PipedOutputStream(in);
    }

    @Override
    public void run() {
        String line;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName("UTF-8")));
            writer = new BufferedWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
            while ((line = reader.readLine()) != null) {
                for (Map.Entry<String,String> replaceRegularExpression : replaceRegularExpressions.entrySet()) {
                    line = line.replaceAll(replaceRegularExpression.getKey(), replaceRegularExpression.getValue());
                }
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            result = e;

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    /* swallow */
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    /* swallow */
                }
            }
        }
    }

    public PipedInputStream getStream() {
        return in;
    }

    public IOException getResult() {
        return result;
    }
}
