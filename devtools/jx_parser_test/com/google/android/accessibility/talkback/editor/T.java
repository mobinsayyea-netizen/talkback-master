package com.google.android.accessibility.talkback.editor;
import java.util.*;
public class T {
  static List<String> log = new ArrayList<>();
  static Set<String> screen = new LinkedHashSet<>();
  static long clock = 0;
  static Set<String> clicked = new HashSet<>();
  static JxClickScript.Env env = new JxClickScript.Env() {
    boolean m(String p, String t){ return java.util.regex.Pattern.compile(String.join(".*", Arrays.stream(p.split("\\*",-1)).map(java.util.regex.Pattern::quote).toArray(String[]::new)), java.util.regex.Pattern.DOTALL).matcher(t).matches(); }
    public List<Object> findAll(String p, boolean l){ List<Object> r=new ArrayList<>(); for(String s:screen) if(m(p,s)) r.add(s); return r; }
    public boolean click(Object n){ log.add("click:"+n); return true; }
    public boolean locate(Object n){ log.add("locate:"+n); return true; }
    public boolean rawTap(Object n,long ms){ log.add("rawtap:"+n); return true; }
    public boolean screenContains(String p){ for(String s:screen) if(m(p,s)) return true; return false; }
    public boolean focusIs(String t){ return t.equals("可编辑"); }
    public boolean inApp(String n){ return n.equals("QQ"); }
    public boolean openApp(String n){ log.add("open:"+n); return true; }
    public boolean command(String n){ log.add("cmd:"+n); return true; }
    public Boolean callFunction(String n,String a){ if(n.equals("fn")){ log.add("fn("+a+")"); return true;} if(n.equals("stop")){log.add("stop"); return false;} return null; }
    public boolean tap(double x,double y,long ms){ log.add("tap:"+x+","+y+","+ms); return true; }
    public boolean swipe(double a,double b,double c,double d,long ms){ log.add("swipe:"+a+","+b+","+c+","+d+","+ms); return true; }
    public boolean tapFocus(long ms){ log.add("tapfocus:"+ms); return true; }
    public void sleep(long ms){ clock+=ms; }
    public long now(){ return clock; }
  };
  static int fails=0;
  static void t(String name, List<?> script, boolean expect, String expLog){
    log.clear(); clock=0;
    boolean r = new JxClickScript(env).run(script);
    String got = String.join(" ", log);
    boolean ok = r==expect && got.equals(expLog);
    if(!ok) fails++;
    System.out.println((ok?"PASS ":"FAIL ")+name+" -> "+r+" ["+got+"]"+(ok?"":"  expected "+expect+" ["+expLog+"]"));
  }
  static List<Object> L(Object... a){ return new ArrayList<>(Arrays.asList(a)); }
  public static void main(String[] x){
    screen.addAll(Arrays.asList("完成","群资料卡","文件","确定","确定","下一曲","解说读屏交流群 99条未读","退出确定吗"));
    t("simple", L("确定"), true, "click:确定");
    t("alt first missing", L("取消","完成"), true, "click:完成");
    t("or-bar", L("取消|完成"), true, "click:完成");
    t("none", L("取消"), false, "");
    t("sequence", L(L("群资料卡","文件")), true, "click:群资料卡 click:文件");
    t("seq stop", L(L("群资料卡","不存在","文件")), false, "click:群资料卡");
    t("seq fail goes to next alt", L(L("不存在","文件"), "完成"), true, "click:完成");
    t("wildcard", L("解说读屏交流群*"), true, "click:解说读屏交流群 99条未读");
    t("index1", L("确定#1"), true, "click:确定");
    t("index -1", L("确定#-1"), true, "click:确定");
    t("index 3 missing", L("确定#3"), false, "");
    t("repeat", L("下一曲<3"), true, "click:下一曲 click:下一曲 click:下一曲");
    t("locate only", L("<确定"), true, "locate:确定");
    t("cond and yes", L("确定&*退出*"), true, "click:确定");
    t("cond and no", L("确定&*不在*"), false, "");
    t("cond not yes", L("确定&!*不在*"), true, "click:确定");
    t("cond not no", L("确定&!*退出*"), false, "");
    t("focus type seq", L(L("&可编辑","%复制")), true, "cmd:复制");
    t("focus type no", L(L("&列表","%复制")), false, "");
    t("open app", L(L(">QQ","解说读屏交流群*","群资料卡","文件")), true, "open:QQ click:解说读屏交流群 99条未读 click:群资料卡 click:文件");
    t("branch present", L(L(">QQ",L("我已阅读群公告|确定"),"群资料卡")), true, "open:QQ click:确定 click:群资料卡");
    t("branch absent", L(L(">QQ",L("我已阅读群公告|我知道了"),"群资料卡")), true, "open:QQ click:群资料卡");
    t("nested branches", L(L(">QQ",L(L("我已阅读群公告","确定"),L("我知道了","确定")),"群资料卡")), true, "open:QQ click:群资料卡");
    t("func ok", L(L("%fn(1,2)","确定")), true, "fn(1,2) click:确定");
    t("func stop", L(L("%stop","确定")), false, "stop");
    t("cmd", L("%复制"), true, "cmd:复制");
    t("direct xy", L("[300,500]"), true, "tap:300.0,500.0,50");
    t("direct rel", L("[0.3,0.5]"), true, "tap:0.3,0.5,50");
    t("direct press", L("[300,500,1000]"), true, "tap:300.0,500.0,1000");
    t("direct longfocus", L("[6000]"), true, "tapfocus:6000");
    t("direct focus", L("[%点击]"), true, "tapfocus:50");
    t("direct name", L("[确定]"), true, "rawtap:确定");
    t("direct swipe", L("[300,500,300,800,1000]"), true, "swipe:300.0,500.0,300.0,800.0,1000");
    t("direct swipe n+app", L("[300,500,300,800,1000,2,QQ]"), true, "swipe:300.0,500.0,300.0,800.0,1000 swipe:300.0,500.0,300.0,800.0,1000");
    t("direct swipe wrong app", L("[300,500,300,800,1000,2,抖音]"), false, "");
    t("direct tap n app", L("[300,500,2,QQ]"), true, "tap:300.0,500.0,50 tap:300.0,500.0,50");
    t("color unsupported", L("[1,2,3,4,5,6,7,8]"), false, "");
    t("delay", L(L("群资料卡$2000","文件")), true, "click:群资料卡 click:文件");
    t("bad number", L("确定#x"), false, "");
    t("only app no", L("确定@抖音"), false, "");
    t("only app yes", L("确定@QQ"), true, "click:确定");
    t("wait count timeout", L("不存在>2"), false, "");
    System.out.println(clock);
    System.out.println(fails==0?"ALL PASS":"FAILS="+fails);
  }
}
