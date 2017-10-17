package android.test;

import android.content.Context;

public class Ep1 {
  public void entryPoint1CallsA() {
    a();
  }
  public void entryPoint2CallsX() {
    x();
  }
  public void a() {
    b();
  }
  public void b() {
    x();
    c();
  }
  public void x() {
    a();
    check_p1();
  }
  public void c() {
    check_p2();
  }
  public void check_p1(){
   Context.checkPermission("android.permission.P1",0, 1); 
  }
  public void check_p2(){
   Context.checkPermission("android.permission.P2",0, 1); 
  }
}
