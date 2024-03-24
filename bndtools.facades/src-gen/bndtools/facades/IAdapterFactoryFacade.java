package bndtools.facades;


import bndtools.facades.util.FacadeUtil;
import java.lang.Class;
import java.lang.Object;
import java.util.function.Function;
import java.util.function.Supplier;
import org.eclipse.core.runtime.IAdapterFactory;


public class IAdapterFactoryFacade extends FacadeUtil  {
  public interface Delegate extends IAdapterFactory  {
    
  }
  

  public IAdapterFactoryFacade(){ super(Delegate.class);}

  public static class Facade implements IAdapterFactory  {
    final Supplier<Delegate> bind;
    @SuppressWarnings({ "unchecked","rawtypes" }) Facade(Function<Object,Supplier<Object>> binding) { this.bind = (Supplier) binding.apply(this) /* I know :-( */; }

    @Override public <T> T getAdapter(Object arg0, Class<T> arg1)  {
      return bind.get().getAdapter(arg0,arg1);
      
    }
    
    @Override public Class<?>[] getAdapterList()  {
      return bind.get().getAdapterList();
      
    }
    
    
  }
  
  public Facade createFacade(Function<Object,Supplier<Object>> binder) { return new Facade(binder); }
}
