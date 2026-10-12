import java.text.BreakIterator; import java.util.Locale;
public class Seg {
  enum Unit { CHAR, WORD, SENTENCE, PARAGRAPH }
  /** Next segment that starts at or after {@code pos}; null at the end. */
  static int[] nextSegment(String text, int pos, Unit unit) {
    int len = text.length();
    if (pos >= len) {
      return null;
    }
    if (unit == Unit.CHAR) {
      int e = text.offsetByCodePoints(pos, 1);
      return new int[] {pos, e};
    }
    if (unit == Unit.PARAGRAPH) {
      int s = pos;
      while (s < len && text.charAt(s) == '\n') {
        s++;
      }
      if (s >= len) {
        return null;
      }
      int e = text.indexOf('\n', s);
      return new int[] {s, e < 0 ? len : e};
    }
    BreakIterator it = unit == Unit.WORD ? BreakIterator.getWordInstance(Locale.getDefault())
        : BreakIterator.getSentenceInstance(Locale.getDefault());
    it.setText(text);
    int start = it.isBoundary(pos) ? pos : it.following(pos);
    while (start != BreakIterator.DONE && start < len) {
      int end = it.following(start);
      if (end == BreakIterator.DONE) {
        end = len;
      }
      if (hasLetterOrDigit(text, start, end)) {
        return new int[] {start, end};
      }
      start = end;
    }
    return null;
  }

  /** Previous segment that ends at or before {@code pos}; null at the start. */
  static int[] previousSegment(String text, int pos, Unit unit) {
    if (pos <= 0) {
      return null;
    }
    int len = text.length();
    pos = Math.min(pos, len);
    if (unit == Unit.CHAR) {
      int s = text.offsetByCodePoints(pos, -1);
      return new int[] {s, pos};
    }
    if (unit == Unit.PARAGRAPH) {
      int e = pos;
      while (e > 0 && text.charAt(e - 1) == '\n') {
        e--;
      }
      if (e <= 0) {
        return null;
      }
      int s = text.lastIndexOf('\n', e - 1) + 1;
      return new int[] {s, e};
    }
    BreakIterator it = unit == Unit.WORD ? BreakIterator.getWordInstance(Locale.getDefault())
        : BreakIterator.getSentenceInstance(Locale.getDefault());
    it.setText(text);
    int end = it.isBoundary(pos) ? pos : it.preceding(pos);
    while (end != BreakIterator.DONE && end > 0) {
      int start = it.preceding(end);
      if (start == BreakIterator.DONE) {
        start = 0;
      }
      if (hasLetterOrDigit(text, start, end)) {
        return new int[] {start, end};
      }
      end = start;
    }
    return null;
  }

  private static boolean hasLetterOrDigit(String t, int s, int e) {
    for (int i = s; i < e; i++) {
      if (Character.isLetterOrDigit(t.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  static String s(String t,int[] r){return r==null?"null":"["+t.substring(r[0],r[1]).replace("\n","\\n")+"]@"+r[0]+"-"+r[1];}
  static int fails=0;
  static void eq(String name,String got,String want){ boolean ok=got.equals(want); if(!ok)fails++; System.out.println((ok?"PASS ":"FAIL ")+name+" -> "+got+(ok?"":"  (wanted "+want+")")); }
  public static void main(String[] a){
    String t="Hello, big world. Second one!\nThird line";
    eq("char next @0", s(t,nextSegment(t,0,Unit.CHAR)), "[H]@0-1");
    eq("char prev @5", s(t,previousSegment(t,5,Unit.CHAR)), "[o]@4-5");
    eq("char next at end", s(t,nextSegment(t,t.length(),Unit.CHAR)), "null");
    eq("char prev at 0", s(t,previousSegment(t,0,Unit.CHAR)), "null");
    eq("word next @0", s(t,nextSegment(t,0,Unit.WORD)), "[Hello]@0-5");
    eq("word next @5 skips comma+space", s(t,nextSegment(t,5,Unit.WORD)), "[big]@7-10");
    eq("word prev @10", s(t,previousSegment(t,10,Unit.WORD)), "[big]@7-10");
    eq("word prev @7 skips space+comma", s(t,previousSegment(t,7,Unit.WORD)), "[Hello]@0-5");
    eq("sentence next @0", s(t,nextSegment(t,0,Unit.SENTENCE)).replaceAll("\\s+\\]","]"), "[Hello, big world.]@0-18");
    eq("paragraph next @0", s(t,nextSegment(t,0,Unit.PARAGRAPH)), "[Hello, big world. Second one!]@0-29");
    eq("paragraph next @29", s(t,nextSegment(t,29,Unit.PARAGRAPH)), "[Third line]@30-40");
    eq("paragraph prev @40", s(t,previousSegment(t,40,Unit.PARAGRAPH)), "[Third line]@30-40");
    eq("paragraph prev @30", s(t,previousSegment(t,30,Unit.PARAGRAPH)), "[Hello, big world. Second one!]@0-29");
    eq("paragraph prev @0", s(t,previousSegment(t,0,Unit.PARAGRAPH)), "null");
    String hi="\u0928\u092e\u0938\u094d\u0924\u0947 \u0926\u0941\u0928\u093f\u092f\u093e";
    eq("hindi word next", s(hi,nextSegment(hi,0,Unit.WORD)), "["+hi.substring(0,6)+"]@0-6");
    String emoji="a\uD83D\uDE00b";
    eq("char next keeps surrogate pair", s(emoji,nextSegment(emoji,1,Unit.CHAR)), "["+emoji.substring(1,3)+"]@1-3");
    System.out.println(fails==0?"ALL PASS":("FAILS: "+fails)); System.exit(fails==0?0:1);
  }
}
