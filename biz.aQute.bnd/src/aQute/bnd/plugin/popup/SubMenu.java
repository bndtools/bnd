package aQute.bnd.plugin.popup;

import java.util.*;

import org.eclipse.jface.action.*;

public class SubMenu extends MenuManager {
    Map<String, IContributionItem> items = new LinkedHashMap<String, IContributionItem>();

    SubMenu(String name) {
        super(name);
    }

    @Override
    public IContributionItem[] getItems() {
        return items.values().toArray(new IContributionItem[items.size()]);
    }

    void add(final Scripts script, final String s, final String full) {
        int n = s.indexOf(':');
        if (n < 0) {
            n = s.indexOf(">");
            if (n < 0) {
                items.put(s, new ActionContributionItem(new Action() {
                    {
                        setText(s);
                    }

                    public void run() {
                        script.exec(full);
                    }
                }));
            } else {
                String name = s.substring(0,n);
                String remainder = s.substring(n+1);
                IContributionItem ici = items.get(name);
                if (ici == null) {
                    ici = new SubMenu(name);
                    items.put(name, ici);
                }
                if (!(ici instanceof SubMenu)) {
                    // A leaf & node ... :-(
                } else {
                    SubMenu sub = (SubMenu) ici;
                    String parts[] = remainder.split(",");
                    for ( String part : parts ) {
                        sub.add(script, part, full);
                    }
                }
                
            }
        } else {
            String name = s.substring(0, n);
            IContributionItem ici = items.get(name);
            if (ici == null) {
                ici = new SubMenu(name);
                items.put(name, ici);
            }
            if (!(ici instanceof SubMenu)) {
                // A leaf & node ... :-(
            } else {
                SubMenu sub = (SubMenu) ici;
                sub.add(script, s.substring(n + 1), full);
            }
        }
    }
    
    void add(SubMenu subMenu) {
    	IContributionItem ici = items.get(subMenu.getMenuText());
        if (ici == null) {
            items.put(subMenu.getMenuText(), subMenu);
        }
   }
}
