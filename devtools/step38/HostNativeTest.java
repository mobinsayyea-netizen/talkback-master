import com.luajava.*;
public class T {
  public static void main(String[] a) throws Exception {
    LuaState L = LuaStateFactory.newLuaState();
    L.openLibs();
    L.getGlobal("package"); L.pushString("/home/claude/host/lib?.so"); L.setField(-2, "cpath"); L.pop(1);
    String code =
      "local out={}\n"+
      "local function T(n,f) local ok,e=pcall(f) out[#out+1]=(ok and 'PASS ' or 'FAIL ')..n..(ok and '' or (' '..tostring(e))) end\n"+
      "T('cjson',function() local c=require('cjson') local d=c.decode(c.encode({a=1,b={1,2,3}})) assert(d.a==1 and d.b[3]==3) end)\n"+
      "T('bson',function() local b=require('bson') local d=b.decode(b.encode({x=5,y='z'})) assert(d.x==5 and d.y=='z') end)\n"+
      "T('crypt',function() local c=require('crypt') assert(#c.sha1('abc')==20) assert(c.hexencode(c.sha1('abc'))=='a9993e364706816aba3e25717850c26c9cd0d89d') assert(c.base64decode(c.base64encode('hello'))=='hello') end)\n"+
      "T('regex',function() local r=require('regex') assert(r.find('hello world','o w')==5) end)\n"+
      "return table.concat(out,'\\n')";
    int rc = L.LdoString(code);
    System.out.println(rc==0 ? L.toString(-1) : ("LUA ERROR: "+L.toString(-1)));
  }
}
