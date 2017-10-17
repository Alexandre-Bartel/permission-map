// 
// (c) 2012 University of Luxembourg - Interdisciplinary Centre for 
// Security Reliability and Trust (SnT) - All rights reserved
//
// Author: Alexandre Bartel
//
// This library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>. 
//

package lu.uni.fpc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class Output {

  private File targetFile = null;
  private BufferedOutputStream out = null;
  
  public Output(String filename) {
    if (filename == null) {
      System.err.println("error: filename is null when creating Output");
      System.err.println(Thread.getAllStackTraces());
      return;
    }
    if (filename.equals("")) {
      return;
    }
    File f = new File(filename);
    f.delete();
    
    targetFile = f;
  }
  
  public void open() {
    try {
      if (targetFile == null) {
        out = new BufferedOutputStream(System.out);
      } else {
    	  if (!targetFile.exists()) {
    		  targetFile.getParentFile().mkdirs();
    		  targetFile.createNewFile();
    	  }
        out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(targetFile)));
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("error when opening file '"+ targetFile +"'");
      System.exit(-1);
    }
  }
  
  public void println() {
    print("\n");
  }
  
  public void println(String s) {
    print(s);
    println();
  }
  
  public void print(String s) {
    try {
      out.write(s.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("error when writing to file '"+ targetFile +"'");
      System.exit(-1);
    }
  }
  
  public void close() {
    try {
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println("error when closin file '"+ targetFile +"'");
      System.exit(-1);
    }
  }

  public void flush() throws IOException {
    out.flush();
  }
  
}
