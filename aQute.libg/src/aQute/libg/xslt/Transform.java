package aQute.libg.xslt;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

public class Transform {
    static TransformerFactory transformerFactory = TransformerFactory
                                                         .newInstance();

    static Map<URL, Templates> cache          = new ConcurrentHashMap<URL, Templates>();

    public static void transform(TransformerFactory transformerFactory, URL xslt, InputStream in, OutputStream out)
            throws Exception {
        if ( xslt == null ) 
            throw new IllegalArgumentException("No source template specified");
        
        Templates templates = cache.get(xslt);
        if (templates == null) {
            InputStream xsltIn = xslt.openStream();
            try {
                templates = transformerFactory
                        .newTemplates(new StreamSource(xsltIn));

                cache.put(xslt, templates);
            } finally {
                in.close();
            }
        }
        Result xmlResult = new StreamResult(out);
        Source xmlSource = new StreamSource(in);
        Transformer t = templates.newTransformer();
        t.transform(xmlSource, xmlResult);
        out.flush();
    }

    public static void transform(URL xslt, InputStream in, OutputStream out) throws Exception {
        transform(transformerFactory,xslt, in, out);
    }
}
