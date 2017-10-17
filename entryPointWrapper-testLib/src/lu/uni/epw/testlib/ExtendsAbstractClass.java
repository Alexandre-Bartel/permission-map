package lu.uni.epw.testlib;

public class ExtendsAbstractClass extends AbstractClass {

  public int add (int a, int b, Simple[] s) {
    return s[1].add(a, b);
  }
  
  public int addAbstract (int a, int b, Simple s) {
    return s.add(a, b);
  }
  
}
