package android.test;
import android.content.Context;
public class C {

  public void m_3_1(){
    check_p1();
  }
  public void m_3_2(){
    m_4_1();
    check_p2();   
    m_4_2();
  }
  public void m_3_3(){
    check_p2();
    check_p7();
  }

  public void m_3_4(){
    check_p5();
  }
  public void m_3_5(){
    m_4_3();
    check_p6();
  }
  public void m_4_1(){
    check_p4();
    check_p3();
  }
  public void m_4_2(){
  }
  public void m_4_3(){
    check_p5();
  }
  public void check_p1(){
   Context.checkPermission("android.permission.P1"); 
  }
  public void check_p2(){
   Context.checkPermission("android.permission.P2"); 
  }
  public void check_p3(){
   Context.checkPermission("android.permission.P3"); 
  }
  public void check_p4(){
   Context.checkPermission("android.permission.P4"); 
  }
  public void check_p5(){
   Context.checkPermission("android.permission.P5"); 
  } 
  public void check_p6(){
   Context.checkPermission("android.permission.P6"); 
  }
  public void check_p7(){
   Context.checkPermission("android.permission.P7"); 
  }

}
