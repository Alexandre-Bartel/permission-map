package lu.uni.epw.testlib;

public class TakeObjectAsParameter {

  public int add (int a, int b, Simple s) {
    return s.add(a, b);
  }
  
  private int addPrivate (int a, int b, Simple s) {
    return s.add(a, s.add(a, b));
  }
  
  protected int addProtected (int a, int b, Simple s) {
    return s.add(a, s.add(a, s.add(a, b)));
  }
  
}
