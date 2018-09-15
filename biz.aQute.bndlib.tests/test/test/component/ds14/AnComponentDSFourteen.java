package test.component.ds14;

import org.osgi.service.component.annotations.Component;

@Component
public class AnComponentDSFourteen implements MyInterface {
    public String getName() {
        return "Cristiano";
    }
}
