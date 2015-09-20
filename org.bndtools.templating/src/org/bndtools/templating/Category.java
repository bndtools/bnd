package org.bndtools.templating;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Category {

	private final String name;
	private final SortedSet<Template> templates = new TreeSet<>();

	public static List<Category> categorise(Collection<Template> templates) {
		SortedMap<String, Category> categories = new TreeMap<>();

		Category uncategorised = new Category(null);
		for (Template template : templates) {
			String categoryName = template.getCategory();
			if (categoryName == null)
				uncategorised.add(template);
			else {
				Category category = categories.get(categoryName);
				if (category == null) {
					category = new Category(categoryName);
					categories.put(categoryName, category);
				}
				category.add(template);
			}
		}

		List<Category> rootList = new ArrayList<>(categories.size());
		// add the special category "Core" first
		Category coreCategory = categories.remove("Core");
		if (coreCategory != null)
			rootList.add(coreCategory);
		// add the rest in alphabetical order
		rootList.addAll(categories.values());
		// lastly add the uncategorised stuff
		if (!uncategorised.getTemplates().isEmpty())
			rootList.add(uncategorised);
		return rootList;
	}

	public Category(String name) {
		this.name = name;
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

}