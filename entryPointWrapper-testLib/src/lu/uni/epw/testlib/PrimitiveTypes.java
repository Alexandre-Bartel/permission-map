package lu.uni.epw.testlib;

public class PrimitiveTypes {
  
  public int add (boolean b, int i, double d, float f, long l, short sh, char c, byte by, String str, Simple s) {
    return s.add(i, s.add((int)d, s.add((int)f, s.add((int)l, s.add((int)sh, c))))) + by + str.length();
  }
  
}
