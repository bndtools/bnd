package aQute.lib.osgi;

import java.lang.annotation.*;
import java.util.*;

public class Annotation {
    String              name;
    Map<String, Object> elements;
    ElementType member;
    RetentionPolicy policy;
    
    public Annotation(String name, Map<String, Object> elements, ElementType member, RetentionPolicy policy ) {
        this.name = name;
        this.elements = elements;
        this.member = member;
        this.policy = policy;
    }
    
    
    public String getName() {
        return name;
    }
    public String toString() {
        return name  + ":" + member + ":" + policy +":" +elements;
    }


    @SuppressWarnings("unchecked")
    public <T> T get(String string) {
        if ( elements == null )
            return null;
        
        return (T) elements.get(string);
    }
    
    public <T> void put(String string, Object v) {
        if ( elements == null )
            return;
        
        elements.put(string,v);
    }
}
