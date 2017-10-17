package lu.uni.epw.testlib;

public class PrivateConstructor {

  private PrivateConstructor(){
  }
  
  public static PrivateConstructor getPrivateConstructor () {
    return new PrivateConstructor();
  }
  
  public int add (int a, Simple s) {
    return s.add(a, a);
  }
  
}
