package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.*;
import java.util.*;

/** Tests the actual fifth-death data and both game-over menu actions at every semester. */
public final class GameOverVerification {
    private static final Set<Integer> held=new HashSet<>(),edges=new HashSet<>();
    private static float dt;
    public static void main(String[] args)throws Exception {
        Input previous=Gdx.input;
        try {
            Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (p,m,a)->m.getName().equals("isKeyPressed")?held.contains((Integer)a[0])
                    :m.getName().equals("isKeyJustPressed")&&edges.contains((Integer)a[0]));
            for(int fps:new int[]{30,60,120}) {
                dt=1f/fps;
                for(int stage=1;stage<=8;stage++)for(boolean title:new boolean[]{false,true}) {
                    held.clear();edges.clear();Main game=new Main();
                    OrthographicCamera camera=new OrthographicCamera(){@Override public void update(){} @Override public void update(boolean f){}};
                    field("camera").set(game,camera);field("viewport").set(game,new FitViewport(800,600,camera));
                    call(game,"loadStage",1);call(game,"recordStageResult");call(game,"loadStage",stage);
                    for(int death=0;death<5;death++) {
                        Rectangle item=((GradeItem)((Array<?>)get(game,"gradeItems")).first()).getBounds();
                        player(game).reset(item.x,item.y);tick(game);
                        check(number(game,"stageScore")==5&&number(game,"collectedItemCount")==1,"Real collection data before death");
                        player(game).reset(80,-150);tick(game);
                    }
                    check(get(game,"gameState")==GameState.GAME_OVER&&number(game,"lives")==0,"Fifth death enters game over");
                    check(number(game,"stageScore")==5&&number(game,"collectedItemCount")==1
                        &&number(game,"semesterDeathCount")==5&&number(game,"totalDeathCount")==5,"Final attempt data retained exactly");
                    StageDefinition definition=(StageDefinition)get(game,"stage");
                    check(GameOverRenderer.semesterLine(definition.getYear(),definition.getSemester()).equals("YEAR "+((stage+1)/2)+" / SEMESTER "+((stage-1)%2+1)),"Correct failed semester label");
                    check(GameOverRenderer.scoreLine(number(game,"stageScore")).equals("SCORE: 5"),"Actual item score, no bonus");
                    check(GameOverRenderer.itemsLine(number(game,"collectedItemCount"),definition.getGradeItems().size).equals("ITEMS: 1 / "+definition.getGradeItems().size),"Dynamic denominator");
                    check(GameOverRenderer.deathsLine(number(game,"semesterDeathCount")).equals("SEMESTER DEATHS: 5"),"Actual semester deaths");
                    String world=snapshot(game);held.add(Input.Keys.RIGHT);held.add(Input.Keys.SPACE);held.add(Input.Keys.ESCAPE);
                    for(int i=0;i<fps;i++)tick(game);held.clear();
                    check(world.equals(snapshot(game))&&get(game,"gameState")==GameState.GAME_OVER,"No world/counter/timer progress");
                    held.add(Input.Keys.DOWN);edges.add(Input.Keys.DOWN);tick(game);edges.clear();
                    for(int i=0;i<fps;i++)tick(game);held.clear();
                    check(number(game,"gameOverSelection")==1,"Held navigation moves once");
                    press(game,Input.Keys.UP);check(number(game,"gameOverSelection")==0,"UP returns to default restart");
                    if(title)press(game,Input.Keys.DOWN);
                    held.add(Input.Keys.ENTER);edges.add(Input.Keys.ENTER);tick(game);edges.clear();
                    check(get(game,"gameState")== (title?GameState.TITLE_SCREEN:GameState.PLAYING),"Selected action state");
                    check(number(game,"currentStageNumber")==1&&number(game,"lives")==5&&number(game,"stageScore")==0
                        &&number(game,"collectedItemCount")==0&&number(game,"semesterDeathCount")==0&&number(game,"totalDeathCount")==0,"Both actions reuse full Stage 1 reset");
                    for(StageResult result:(StageResult[])get(game,"completedResults"))check(result==null,"All scores and bonuses cleared");
                    for(int deaths:(int[])get(game,"semesterDeaths"))check(deaths==0,"All semester death records cleared");
                    Object freshPlayer=player(game);
                    if(title) {
                        for(int i=0;i<fps;i++)tick(game);
                        check(get(game,"gameState")==GameState.TITLE_SCREEN,"Held Enter cannot skip title");
                        held.clear();tick(game);press(game,Input.Keys.ENTER);
                        check(get(game,"gameState")==GameState.PLAYING,"Fresh title Enter starts game");
                    } else {
                        tick(game);check(player(game)==freshPlayer,"Held Enter cannot initialize twice");held.clear();
                    }
                }
                System.out.println(fps+" fps: game-over passed: eight final-attempt values, frozen worlds/timers, edge menus, restart/title full reset and held-input guards.");
            }
        } finally {Gdx.input=previous;}
    }
    private static String snapshot(Main g)throws Exception {
        StringBuilder s=new StringBuilder().append(player(g).getBounds());
        for(Object e:(Array<?>)get(g,"enemies"))s.append(((PatrollingEnemy)e).getBounds()).append(((PatrollingEnemy)e).isMovingRight());
        for(Object p:(Array<?>)get(g,"movingPlatforms"))s.append(((MovingPlatform)p).getBounds());
        for(Object t:(Array<?>)get(g,"timedTraps"))s.append(((TimedTrap)t).getState()).append(((TimedTrap)t).getStateElapsedTime());
        for(Object i:(Array<?>)get(g,"gradeItems"))s.append(((GradeItem)i).isCollected());
        for(String n:new String[]{"stageScore","collectedItemCount","lives","semesterDeathCount","totalDeathCount"})s.append(get(g,n));
        return s.toString();
    }
    private static void press(Main g,int key)throws Exception{held.add(key);edges.add(key);tick(g);held.clear();edges.clear();}
    private static void tick(Main g)throws Exception{Method m=Main.class.getDeclaredMethod("update",float.class);m.setAccessible(true);m.invoke(g,dt);}
    private static void call(Main g,String n,Object...a)throws Exception{Method m=a.length==0?Main.class.getDeclaredMethod(n):Main.class.getDeclaredMethod(n,int.class);m.setAccessible(true);m.invoke(g,a);}
    private static Player player(Main g)throws Exception{return (Player)get(g,"player");}
    private static int number(Main g,String n)throws Exception{return (Integer)get(g,n);}
    private static Object get(Main g,String n)throws Exception{return field(n).get(g);}
    private static Field field(String n)throws Exception{Field f=Main.class.getDeclaredField(n);f.setAccessible(true);return f;}
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
}
