package bndtools.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

/**
 * Helper to create menus with submenus from a list of
 * {@link HierarchicalLabel}s.
 */
public class HierarchicalMenu {

	private final List<HierarchicalLabel<Action>> labels = new ArrayList<>();

	public HierarchicalMenu() {

	}

	public HierarchicalMenu add(HierarchicalLabel<Action> label) {
		labels.add(label);
		return this;
	}

	public void build(IMenuManager root) {
		createMenu(labels, root);
	}


	public static void createMenu(List<HierarchicalLabel<Action>> labels, IMenuManager rootMenu) {

		Map<String, IMenuManager> menus = new LinkedHashMap<>();

		for (HierarchicalLabel<Action> hl : labels) {
			IMenuManager current = rootMenu;
			List<String> hlLabels = hl.getLabels();
			for (int i = 0; i < hlLabels.size(); i++) {
				String currentLabel = hlLabels.get(i);
				if (i == hlLabels.size() - 1) {
					// currentMenu = getOrCreateSubMenu(menuMap, currentMenu,
					// currentLabel);
					current.add(hl.getLeafAction());
				} else {
					current = getOrCreateSubMenu(menus, current, currentLabel);
				}
			}
		}

	}

	private static IMenuManager getOrCreateSubMenu(Map<String, IMenuManager> menus, IMenuManager parent, String label) {
		String key = parent.toString() + " -> " + label;

		if (!menus.containsKey(key)) {
			IMenuManager newMenu = new MenuManager(label, null);
			parent.add(newMenu);
			menus.put(key, newMenu);
		}

		return menus.get(key);
	}
}
