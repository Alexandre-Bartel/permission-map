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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;


public class OutputTest extends TestCase {
  String filename = "./testOut/test.txt";
  String text = "test1234";
  Output o = new Output(filename);
  
  protected void setUp() throws Exception {
    File f = new File(filename);
    if (!f.exists())
      f.createNewFile();
  }
  
  public void testWrite() throws FileNotFoundException, IOException {   
    o.open();
    o.print(text);
    o.close();
    InputStreamReader in = new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(filename))));
    BufferedReader br = new BufferedReader(in);
    String s = br.readLine();
    assertEquals(s, text);
  }
  
  public void tearDown() {
    File f = new File(filename);
    f.delete();
  }
}
