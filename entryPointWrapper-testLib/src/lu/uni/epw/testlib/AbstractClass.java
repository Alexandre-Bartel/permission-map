package lu.uni.epw.testlib;

public abstract class AbstractClass {

  public int add (int a, int b, Simple[] s) {
    return s[1].add(a, b);
  }
  
  public abstract int addAbstract (int a, int b, Simple s);
  
}
