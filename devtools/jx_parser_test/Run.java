import javax.tools.*;
import java.util.*;
public class Run {
  public static void main(String[] a) throws Exception {
    JavaCompiler c = ToolProvider.getSystemJavaCompiler();
    List<String> files = new ArrayList<>();
    for (String f : new java.io.File("com/google/android/accessibility/talkback/editor").list()) files.add("com/google/android/accessibility/talkback/editor/"+f);
    int r = c.run(null, null, null, concat(new String[]{"-d","out"}, files.toArray(new String[0])));
    System.out.println("compile rc="+r);
  }
  static String[] concat(String[] a, String[] b){ String[] r=Arrays.copyOf(a,a.length+b.length); System.arraycopy(b,0,r,a.length,b.length); return r; }
}
