package io.jbnu.test;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.*;
import java.util.*;

/** Real Main input transitions; model snapshots ensure pause never resets or advances a vacation. */
public final class VacationPauseVerification {
    private static final Set<Integer> held=new HashSet<>(),edges=new HashSet<>();
    private static float dt;
    public static void main(String[] args)throws Exception {
        Input previous=Gdx.input;
        try {
            Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (p,m,a)->m.getName().equals("isKeyPressed")?held.contains((Integer)a[0]):m.getName().equals("isKeyJustPressed")&&edges.contains((Integer)a[0]));
            for(int fps:new int[]{30,60,120}) {
                dt=1f/fps;
                for(VacationEventType type:VacationEventType.values()) {
                    for(int phase=0;phase<4;phase++)checkPhase(type,phase,fps);
                    Main game=setup(type);press(game,Input.Keys.ESCAPE);press(game,Input.Keys.UP);press(game,Input.Keys.ENTER);
                    check(get(game,"gameState")==GameState.TITLE_SCREEN&&get(game,"vacationGame")==null,"Vacation QUIT clears pending game and opens title");
                    check((Integer)get(game,"currentStageNumber")==1&&(Integer)get(game,"lives")==5&&(Integer)get(game,"totalDeathCount")==0,"Full reset, not application exit");
                    for(StageResult r:(StageResult[])get(game,"completedResults"))check(r==null,"QUIT clears all scores/bonuses");
                    check(((VacationManager)get(game,"vacationManager")).getPending()==null,"QUIT clears vacation pending state");
                }
                System.out.println(fps+" fps: vacation pause passed: all three games/INTRO/play/both results, frozen timers/data, ESC/RESUME, held keys, safe restart, single reward, title reset.");
            }
        } finally {Gdx.input=previous;}
    }
    private static void checkPhase(VacationEventType type,int phase,int fps)throws Exception {
        Main game=setup(type);VacationMinigame mini=(VacationMinigame)get(game,"vacationGame");
        if(phase==1){mini.update(dt,MinigameTestDriver.key(Input.Keys.ENTER));for(int i=0;i<fps;i++)mini.update(dt,MinigameInput.NONE);}
        else if(phase>=2)MinigameTestDriver.finish(mini,phase==2,dt);
        StageResult[] results=((StageResult[])get(game,"completedResults")).clone();
        VacationManager manager=(VacationManager)get(game,"vacationManager");
        String frozen=snapshot(mini);Object player=get(game,"player");
        held.add(Input.Keys.ESCAPE);edges.add(Input.Keys.ESCAPE);tick(game);edges.clear();
        check(get(game,"gameState")==GameState.PAUSED&&get(game,"pauseReturnState")==GameState.VACATION_EVENT,"Pause remembers vacation context");
        held.add(Input.Keys.RIGHT);held.add(Input.Keys.SPACE);
        for(int i=0;i<fps*2;i++)tick(game);
        check(get(game,"gameState")==GameState.PAUSED&&get(game,"vacationGame")==mini&&snapshot(mini).equals(frozen),"Held ESC, movement, jump cannot alter model/time");
        check(!game.completeVacationEvent(type,phase==2),"Paused vacation cannot confirm a reward via callback");
        held.clear();press(game,Input.Keys.ESCAPE);
        check(get(game,"gameState")==GameState.VACATION_EVENT&&snapshot(mini).equals(frozen)&&get(game,"player")==player,"ESC resumes exact object/phase, not platformer");
        if(phase==1) {
            double time=mini.getTimeRemaining();tick(game);
            check(mini.getTimeRemaining()<time,"Next active Main update resumes the same minigame clock");
            frozen=snapshot(mini);
        }
        press(game,Input.Keys.ESCAPE);press(game,Input.Keys.ENTER);
        check(get(game,"gameState")==GameState.VACATION_EVENT&&get(game,"vacationGame")==mini&&snapshot(mini).equals(frozen),"RESUME preserves exact progress");
        press(game,Input.Keys.ESCAPE);press(game,Input.Keys.DOWN);
        held.add(Input.Keys.ENTER);edges.add(Input.Keys.ENTER);tick(game);edges.clear();
        VacationMinigame restarted=(VacationMinigame)get(game,"vacationGame");
        check(restarted!=mini&&restarted.getType()==type&&restarted.getPhase()==VacationMinigame.Phase.INTRO
            &&restarted.getTimeRemaining()==restarted.getTimeLimit(),"RESTART creates same game fresh at INTRO");
        check(get(game,"gameState")==GameState.VACATION_EVENT&&manager.getPending()==type&&manager.getResult(type)==null,"Restart neither confirms nor reapplies reward");
        for(int i=0;i<8;i++)check(((StageResult[])get(game,"completedResults"))[i]==results[i],"Restart preserves saved semester results");
        for(int i=0;i<fps;i++)tick(game);
        check(get(game,"vacationGame")==restarted&&restarted.getPhase()==VacationMinigame.Phase.INTRO,"Held Enter cannot repeat restart or skip INTRO");
        held.clear();tick(game);press(game,Input.Keys.ENTER);
        check(restarted.getPhase()==VacationMinigame.Phase.PLAYING&&restarted.getTimeRemaining()==restarted.getTimeLimit(),"Fresh input begins restarted game once");
        MinigameTestDriver.finish(restarted,phase!=3,dt);press(game,Input.Keys.ENTER);
        check(get(game,"gameState")==GameState.PLAYING&&(Integer)get(game,"currentStageNumber")==type.getCompletedStage()+1,"Normal completion still advances");
        VacationResult result=manager.getResult(type);
        check(result!=null&&result.isSuccess()==(phase!=3)&&!game.completeVacationEvent(type,phase!=3),"Reward confirmed once after real completion");
    }
    private static Main setup(VacationEventType type)throws Exception {
        held.clear();edges.clear();Main game=new Main();
        OrthographicCamera camera=new OrthographicCamera(){@Override public void update(){} @Override public void update(boolean f){}};
        field("camera").set(game,camera);field("viewport").set(game,new FitViewport(800,600,camera));
        Method load=Main.class.getDeclaredMethod("loadStage",int.class);load.setAccessible(true);load.invoke(game,type.getCompletedStage());
        Rectangle item=((GradeItem)((Array<?>)get(game,"gradeItems")).first()).getBounds();((Player)get(game,"player")).reset(item.x,item.y);tick(game);
        Rectangle flag=(Rectangle)get(game,"flag");((Player)get(game,"player")).reset(flag.x,flag.y);tick(game);press(game,Input.Keys.ENTER);
        check(get(game,"gameState")==GameState.VACATION_EVENT,"Real semester report enters vacation");return game;
    }
    private static String snapshot(VacationMinigame g) {
        StringBuilder s=new StringBuilder().append(g.getPhase()).append('/').append(g.getTimeRemaining());
        if(g instanceof CafeMinigame){CafeMinigame c=(CafeMinigame)g;s.append(c.getScore()).append('/').append(c.getTrayX()).append('/').append(c.getSpawnedCount()).append('/').append(c.getRemovedCount());for(CafeMinigame.Drop d:c.getDrops())s.append('/').append(d.getX()).append('/').append(d.getY());}
        else if(g instanceof CertificationMinigame){CertificationMinigame c=(CertificationMinigame)g;s.append(c.getRoundNumber()).append('/').append(c.getInputIndex()).append('/').append(c.getShowTimeRemaining()).append('/').append(c.getMistakes()).append(Arrays.toString(c.getVisiblePattern()));}
        else {HackathonMinigame h=(HackathonMinigame)g;s.append(h.getPuzzleNumber()).append('/').append(h.getMistakes()).append('/').append(h.getCursorRow()).append('/').append(h.getCursorColumn()).append('/').append(h.getLastCheck());if(h.getBoard()!=null)for(int r=0;r<h.getBoard().getRows();r++)for(int c=0;c<h.getBoard().getColumns();c++)s.append('/').append(h.getBoard().getTile(r,c).getPorts());}
        return s.toString();
    }
    private static void press(Main g,int key)throws Exception {held.add(key);edges.add(key);tick(g);held.clear();edges.clear();}
    private static void tick(Main g)throws Exception {Method m=Main.class.getDeclaredMethod("update",float.class);m.setAccessible(true);m.invoke(g,dt);}
    private static Object get(Main g,String n)throws Exception{return field(n).get(g);}
    private static Field field(String n)throws Exception{Field f=Main.class.getDeclaredField(n);f.setAccessible(true);return f;}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
}
