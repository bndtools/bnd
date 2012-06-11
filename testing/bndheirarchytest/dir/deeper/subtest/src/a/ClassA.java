package a;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

@Component
public class ClassA {
  @Activate
  public void start() {
    System.out.println("ClassA started");
  }
}
