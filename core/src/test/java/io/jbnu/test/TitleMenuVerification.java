package io.jbnu.test;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Real title input paths, plus resource-free font coverage and exit request verification. */
public final class TitleMenuVerification {
    private static final Set<Integer> held=new HashSet<>(),edges=new HashSet<>();
    private static int exits;
    private static float dt;
    public static void main(String[] args)throws Exception {
        Input oldInput=Gdx.input;Application oldApp=Gdx.app;
        try {
            Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (p,m,a)->m.getName().equals("isKeyPressed")?held.contains((Integer)a[0]):m.getName().equals("isKeyJustPressed")&&edges.contains((Integer)a[0]));
            Gdx.app=(Application)Proxy.newProxyInstance(Application.class.getClassLoader(),new Class<?>[]{Application.class},
                (p,m,a)->{if(m.getName().equals("exit"))exits++;return null;});
            glyphs();
            for(int fps:new int[]{30,60,120}) {
                dt=1f/fps;exits=0;held.clear();edges.clear();Main game=new Main();
                OrthographicCamera camera=new OrthographicCamera(){@Override public void update(){} @Override public void update(boolean f){}};
                field("camera").set(game,camera);field("viewport").set(game,new FitViewport(800,600,camera));
                check(number(game,"titleSelection")==0&&!bool(game,"titleHelpVisible"),"Default game start selection");
                held.add(Input.Keys.DOWN);edges.add(Input.Keys.DOWN);tick(game);edges.clear();
                for(int i=0;i<fps;i++)tick(game);
                check(number(game,"titleSelection")==1&&get(game,"stage")==null,"Held DOWN moves once, no world");held.clear();
                press(game,Input.Keys.ENTER);check(bool(game,"titleHelpVisible")&&state(game)==GameState.TITLE_SCREEN,"Help is title UI, not new game state");
                for(int key:new int[]{Input.Keys.UP,Input.Keys.DOWN,Input.Keys.SPACE,Input.Keys.ENTER})press(game,key);
                check(bool(game,"titleHelpVisible")&&get(game,"player")==null&&exits==0,"Help cannot start or exit the game");
                held.add(Input.Keys.ESCAPE);held.add(Input.Keys.ENTER);edges.addAll(held);tick(game);edges.clear();
                check(!bool(game,"titleHelpVisible")&&state(game)==GameState.TITLE_SCREEN&&get(game,"stage")==null,"ESC+Enter only closes help");
                for(int i=0;i<fps;i++)tick(game);held.clear();
                check(number(game,"titleSelection")==1,"Held ESC does not alter menu selection");
                press(game,Input.Keys.UP);check(number(game,"titleSelection")==0,"UP selects start");
                held.add(Input.Keys.DOWN);held.add(Input.Keys.ENTER);edges.addAll(held);tick(game);held.clear();edges.clear();
                check(number(game,"titleSelection")==1&&!bool(game,"titleHelpVisible"),"Navigation/Enter same frame only navigates");
                press(game,Input.Keys.UP);press(game,Input.Keys.ENTER);
                check(state(game)==GameState.PLAYING&&number(game,"currentStageNumber")==1&&number(game,"lives")==5
                    &&number(game,"stageScore")==0&&number(game,"totalDeathCount")==0,"Start reuses clean Stage 1 initialization");
                check(!bool(game,"titleHelpVisible")&&number(game,"titleSelection")==0,"New game clears title screen state");
                Object player=get(game,"player");for(int i=0;i<3;i++)press(game,Input.Keys.ENTER);
                check(get(game,"player")==player,"Repeated Enter cannot reinitialize playing world");
                Method load=Main.class.getDeclaredMethod("loadStage",int.class);load.setAccessible(true);load.invoke(game,4);
                for(int i=0;i<5;i++){((Player)get(game,"player")).reset(80,-150);tick(game);}
                check(state(game)==GameState.GAME_OVER,"Existing fifth-death condition");
                press(game,Input.Keys.DOWN);press(game,Input.Keys.ENTER);tick(game);
                check(state(game)==GameState.TITLE_SCREEN&&number(game,"titleSelection")==0&&!bool(game,"titleHelpVisible"),"GAME_OVER TITLE returns to default menu");
                for(StageResult r:(StageResult[])get(game,"completedResults"))check(r==null,"Title return clears scores/bonuses");
                press(game,Input.Keys.UP);check(number(game,"titleSelection")==2,"UP wraps to exit");
                held.add(Input.Keys.ENTER);edges.add(Input.Keys.ENTER);tick(game);edges.clear();
                for(int i=0;i<fps;i++)tick(game);held.clear();
                check(exits==1&&state(game)==GameState.TITLE_SCREEN,"One LibGDX exit request, no stage updates");
                System.out.println(fps+" fps: title menu passed: edge navigation, help/ESC, simultaneous/held keys, clean start, GAME_OVER title reset, exact one application exit.");
            }
        } finally {Gdx.input=oldInput;Gdx.app=oldApp;}
    }
    private static void glyphs()throws Exception {
        Path root=Files.isDirectory(Paths.get("assets"))?Paths.get("."):Paths.get("..");
        String source=new String(Files.readAllBytes(root.resolve("core/src/main/java/io/jbnu/test/TitleScreenRenderer.java")),StandardCharsets.UTF_8);
        String fnt=new String(Files.readAllBytes(root.resolve("assets/fonts/title-ko.fnt")),StandardCharsets.UTF_8);
        Set<Integer> glyphs=new HashSet<>();Matcher ids=Pattern.compile("char id=(\\d+)").matcher(fnt);while(ids.find())glyphs.add(Integer.parseInt(ids.group(1)));
        Matcher strings=Pattern.compile("\"([^\"\\r\\n]*)\"").matcher(source);
        while(strings.find())for(char c:strings.group(1).toCharArray())check(glyphs.contains((int)c),"Title font includes "+c);
        check(Files.size(root.resolve("assets/fonts/title-ko.png"))>0&&Files.exists(root.resolve("assets/fonts/title-ko-OFL.txt")),"Existing OFL font source reused with notice");
    }
    private static void press(Main g,int k)throws Exception{held.add(k);edges.add(k);tick(g);held.clear();edges.clear();}
    private static void tick(Main g)throws Exception{Method m=Main.class.getDeclaredMethod("update",float.class);m.setAccessible(true);m.invoke(g,dt);}
    private static Object get(Main g,String n)throws Exception{return field(n).get(g);}
    private static int number(Main g,String n)throws Exception{return(Integer)get(g,n);}
    private static boolean bool(Main g,String n)throws Exception{return(Boolean)get(g,n);}
    private static GameState state(Main g)throws Exception{return(GameState)get(g,"gameState");}
    private static Field field(String n)throws Exception{Field f=Main.class.getDeclaredField(n);f.setAccessible(true);return f;}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
}
