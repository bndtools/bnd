package org.bndtools.templating;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public final class Category implements Comparable<Category> {

	private final String name;
	private final SortedSet<Template> templates = new TreeSet<>(new Comparator<Template>() {
		@Override
		public int compare(Template t1, Template t2) {
			// The ranking is intentionally backwards, so we get the highest ranked templates first in the list
			int diff = t1.getRanking() - t2.getRanking();
			if (diff != 0) return diff;
			
			// Sort on name
			diff = t1.getName().compareTo(t2.getName());
			if (diff != 0) return diff;
			
			// Finally sort on version -- again, intentionally backwards
			return t1.getVersion().compareTo(t2.getVersion());
		}
	});
	private final String prefix;

	public static List<Category> categorise(Collection<Template> templates) {
		SortedMap<Category, Category> cats = new TreeMap<>();

		for (Template template : templates) {
			Category tmp = new Category(template.getCategory());
			Category category = cats.get(tmp);
			if (category == null) {
				category = tmp;
				cats.put(category, category);
			}
			category.add(template);
		}

		List<Category> rootList = new ArrayList<>(cats.size());
		rootList.addAll(cats.keySet());
		return rootList;
	}

	public Category(String fullName) {
		int slashIndex = fullName.indexOf('/');
		if (slashIndex >= 0) {
			this.prefix = fullName.substring(0, slashIndex);
			this.name = fullName.substring(slashIndex+1);
		} else {
			this.prefix = null;
			this.name = fullName;
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public String getName() {
		return name;
	}

	public Collection<Template> getTemplates() {
		return Collections.unmodifiableCollection(templates);
	}

	public void add(Template template) {
		templates.add(template);
	}
	
	@Override
	public int compareTo(Category o) {
		int diff;
		if (this.prefix == null)
			diff = o.prefix == null ? 0 : 1;
		else if (o.prefix == null)
			diff = -1;
		else
			diff = o.name.compareTo(this.name);
		return diff;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Category other = (Category) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		return true;
	}
	
	

}