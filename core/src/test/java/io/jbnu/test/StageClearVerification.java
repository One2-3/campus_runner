package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.*;
import java.util.*;

/** All eight confirmed reports, every survival bonus, frozen time and explicit continuation. */
public final class StageClearVerification {
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
                for(int n=1;n<=8;n++)for(int deaths=0;deaths<=4;deaths++) {
                    held.clear();edges.clear();Main game=new Main();
                    OrthographicCamera c=new OrthographicCamera(){@Override public void update(){} @Override public void update(boolean f){}};
                    field("camera").set(game,c);field("viewport").set(game,new FitViewport(800,600,c));call(game,"loadStage",n);
                    for(int i=0;i<deaths;i++){player(game).reset(80,-150);tick(game);}
                    Rectangle item=((GradeItem)((Array<?>)get(game,"gradeItems")).first()).getBounds();
                    player(game).reset(item.x,item.y);tick(game);
                    Rectangle flag=(Rectangle)get(game,"flag");player(game).reset(flag.x,flag.y);
                    held.add(Input.Keys.ENTER);edges.add(Input.Keys.ENTER);tick(game);edges.clear();
                    check(get(game,"gameState")==GameState.STAGE_CLEAR,"Every flag requires semester report, even flag-frame Enter");
                    StageResult result=((StageResult[])get(game,"completedResults"))[n-1];
                    check(result!=null&&result.getItemScore()==5&&result.getCollectedItemCount()==1
                        &&result.getDeathCount()==deaths&&result.getSurvivalBonus()==Math.max(0,20-deaths*5)
                        &&result.getFinalScore()==5+Math.max(0,20-deaths*5),"Confirmed report data and bonus exact");
                    check(StageClearRenderer.semesterLine(result).equals("YEAR "+((n+1)/2)+" / SEMESTER "+((n-1)%2+1)),"Report semester label");
                    String frozen=snapshot(game);held.add(Input.Keys.RIGHT);held.add(Input.Keys.SPACE);
                    for(int i=0;i<fps;i++)tick(game);
                    check(get(game,"gameState")==GameState.STAGE_CLEAR&&snapshot(game).equals(frozen),"Held Enter cannot advance; entire world frozen");
                    check(((StageResult[])get(game,"completedResults"))[n-1]==result,"No duplicate result/bonus while displayed");
                    held.clear();held.add(Input.Keys.ENTER);edges.add(Input.Keys.ENTER);edges.add(Input.Keys.SPACE);tick(game);edges.clear();
                    GameState expected=n==8?GameState.GAME_COMPLETE:VacationEventType.afterStage(n)!=null?GameState.VACATION_EVENT:GameState.PLAYING;
                    check(get(game,"gameState")==expected,"Enter reaches original semester/vacation/final-report destination");
                    check(((StageResult[])get(game,"completedResults"))[n-1]==result,"Continue preserves exact saved result");
                    if(expected==GameState.VACATION_EVENT){
                        VacationMinigame mini=(VacationMinigame)get(game,"vacationGame");
                        check(mini.getType()==VacationEventType.afterStage(n)&&mini.getPhase()==VacationMinigame.Phase.INTRO,"Correct minigame INTRO, no input leak");
                        for(int i=0;i<fps;i++)tick(game);
                        check(mini.getPhase()==VacationMinigame.Phase.INTRO&&mini.getTimeRemaining()==mini.getTimeLimit(),"Held Continue cannot start mini/timer");
                    }else if(n==8){
                        for(int i=0;i<fps;i++)tick(game);
                        check(get(game,"gameState")==GameState.GAME_COMPLETE&&(Integer)get(game,"currentStageNumber")==8,"Final report not skipped; no Stage 9");
                    }else check((Integer)get(game,"currentStageNumber")==n+1,"Normal next semester exactly once");
                }
                System.out.println(fps+" fps: semester reports passed: eight stages, 0-4 death bonuses, frozen worlds, held/simultaneous guards, vacation INTRO and final-report destinations.");
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
        s.append(((OrthographicCamera)get(g,"camera")).position);return s.toString();
    }
    private static void tick(Main g)throws Exception {Method m=Main.class.getDeclaredMethod("update",float.class);m.setAccessible(true);m.invoke(g,dt);}
    private static void call(Main g,String n,int a)throws Exception {Method m=Main.class.getDeclaredMethod(n,int.class);m.setAccessible(true);m.invoke(g,a);}
    private static Player player(Main g)throws Exception{return (Player)get(g,"player");}
    private static Object get(Main g,String n)throws Exception{return field(n).get(g);}
    private static Field field(String n)throws Exception{Field f=Main.class.getDeclaredField(n);f.setAccessible(true);return f;}
    private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
}
