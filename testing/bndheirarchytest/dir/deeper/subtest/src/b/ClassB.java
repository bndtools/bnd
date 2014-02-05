package b;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

@Component
public class ClassB {
  @Activate
  public void start() {
    System.out.println("ClassB started");
  }
}
