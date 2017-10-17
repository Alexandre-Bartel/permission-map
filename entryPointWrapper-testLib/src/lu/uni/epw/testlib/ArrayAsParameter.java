package lu.uni.epw.testlib;

public class ArrayAsParameter {

  public int add (int a, int b, Simple[] s) {
    return s[1].add(a, b);
  }
  
  public int add (int[][][][][] a, int [][][] b, Simple[][][][][][][][][][][][][][][][][][] s) {
    return s[1][2][3][1000000000][0][0][0][0][0][0][0][0][0][0][0][0][0][0].add(a[0][1][2][3][4], b[10][20][30]);
  }
  
}
