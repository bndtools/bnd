package test;

import java.io.*;
import java.util.*;

import junit.framework.*;
import aQute.lib.collections.*;
import aQute.lib.osgi.*;
import static aQute.lib.collections.Logic.*;

public class GroupsTest extends TestCase{

	static class Group implements Comparable<Group>{
		final Set<String>    name;
		final Set<String>		refs;
		
		public Group(Set<String> key, Set<String> value) {
			
			name = key;
			refs = value;
		}

		public int compareTo(Group o) {
			int n = o.name.size()-name.size();
			if ( n != 0) 
				return n;

			n = o.refs.size() - refs.size();
			
			if ( n != 0) 
				return n;
			
			String s1 = name.iterator().next();
			String s2 = o.name.iterator().next();
			return s1.compareTo(s2);
		}
		
		public String toString() {
			return name.toString();
		}
	}
	
	@SuppressWarnings("unchecked") public void testSimple() throws Exception {
		Builder	b = new Builder();
//		b.addClasspath(new File("../biz.aQute.bnd/tmp/biz.aQute.bnd.jar"));
		b.addClasspath(new File("jar/spring.jar"));
//		b.addClasspath(new File("/Ws/aQute.old/aQute.google/lib/gwt-dev-windows.jar"));
		b.setProperty("Private-Package", "*");
		b.build();
			
		System.out.println(b.getUses());
		
        MultiMap<Set<String>, String> x = b.getGroups();
        SortedSet<Group> groups = new TreeSet<Group>();
        
        for ( Map.Entry<Set<String>,Set<String>> entry : x.entrySet()) {
        	groups.add( new Group(entry.getKey(), entry.getValue()));
        }

        Iterator<Group> i = groups.iterator();
        Group main = i.next();
        System.out.println("Main " + main);

        while (  i.hasNext() ) {
        	Group g = i.next();
        	System.out.println("Trying " + g);
        	if ( !retain(main.refs,g.name).isEmpty()) {        		
        		Collection<String> newReferences = remove(g.refs, main.refs, main.name, g.name);
        		if ( newReferences.isEmpty()) {
        			System.out.println("merging " + g);
        			main.name.addAll(g.name);
        			i.remove();
        		}
            	else System.out.println("  would add " + newReferences);
        	}
        	else System.out.println("  main does not refer");
        }
        
        for ( Group g : groups) {
        	System.out.println(g);
        }
	}
}
