package lu.uni.epw.testlib;

public class UseAbstractClass {

  public int add (int a, int b, AbstractClass[] ac) {
    return ac[1].addAbstract(a,b,new Simple());
  }
  
}
