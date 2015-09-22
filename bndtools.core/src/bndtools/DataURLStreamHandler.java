package bndtools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import org.osgi.service.url.AbstractURLStreamHandlerService;

import aQute.lib.base64.Base64;

/**
 * Basic implementation of IETF RFC 2397, the "data" URL scheme (http://tools.ietf.org/html/rfc2397).
 */
public class DataURLStreamHandler extends AbstractURLStreamHandlerService {

    public final static String PROTOCOL = "data";

    @Override
    public URLConnection openConnection(URL u) throws IOException {
        if (!PROTOCOL.equals(u.getProtocol()))
            throw new MalformedURLException("Unsupported protocol");
        return new DataURLConnection(u);
    }

    static class ParseResult {
        String mediaType = "text/plain";
        String charset = "US-ASCII";
        byte[] data;
    }

    static final class DataURLConnection extends URLConnection {

        private ParseResult parsed = null;

        DataURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            parsed = parse(url.getPath());
        }

        static ParseResult parse(String ssp) throws IOException {
            int commaIndex = ssp.indexOf(',');
            if (commaIndex < 0)
                throw new MalformedURLException("missing comma");

            String paramSegment = ssp.substring(0, commaIndex);
            String dataSegment = ssp.substring(commaIndex + 1);

            String mediaType = null;
            boolean base64 = false;
            String charset = "US-ASCII";

            StringTokenizer tokenizer = new StringTokenizer(paramSegment, ";");
            boolean first = true;
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (first)
                    mediaType = token;
                else if ("base64".equals(token))
                    base64 = true;
                else if (token.startsWith("charset="))
                    charset = token.substring("charset=".length());
                first = false;
            }

            byte[] bytes;
            if (base64) {
                bytes = Base64.decodeBase64(dataSegment);
            } else {
                String decoded = URLDecoder.decode(dataSegment, charset);
                bytes = decoded.getBytes("UTF-8");
            }

            ParseResult parsed = new ParseResult();
            parsed.data = bytes;
            if (mediaType != null && !mediaType.isEmpty())
                parsed.mediaType = mediaType;
            if (charset != null && !charset.isEmpty())
                parsed.charset = charset;
            return parsed;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (parsed == null)
                connect();

            return new ByteArrayInputStream(parsed.data);
        }

        @Override
        public String getContentType() {
            return parsed != null ? parsed.mediaType : null;
        }

        @Override
        public String getContentEncoding() {
            return parsed != null ? parsed.charset : null;
        }

    }

}
