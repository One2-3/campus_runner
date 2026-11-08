package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.*;
import java.util.*;

/** One Main instance per complete run. Stage contact fixtures are not movement-route proofs. */
public final class FinalIntegrationVerification {
    private static final Set<Integer> held = new HashSet<>(), edges = new HashSet<>();
    private static final int[] COUNTS = {4,5,6,7,8,9,10,12};
    private static final int[] ACTIONS = {Input.Keys.UP,Input.Keys.DOWN,Input.Keys.LEFT,
        Input.Keys.RIGHT,Input.Keys.SPACE,Input.Keys.ENTER};
    private static float dt;

    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input) Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (p,m,a)->m.getName().equals("isKeyPressed") ? held.contains((Integer)a[0])
                    : m.getName().equals("isKeyJustPressed") && edges.contains((Integer)a[0]));
            int count = 0;
            for (int stage=1;stage<=8;stage++) {
                check(StageFactory.createStage(stage).getGradeItems().size == COUNTS[stage-1],"Semester item capacity");
                count += COUNTS[stage-1];
            }
            check(count==61 && count*GradeItem.DEFAULT_SCORE==305,"61 credits / 305 item points");
            try { StageFactory.createStage(9); throw new AssertionError("Stage 9 must be rejected"); }
            catch (IllegalArgumentException expected) { }
            for(int fps:new int[]{30,60,120}) {
                dt=1f/fps;
                run(fps,0,false);
                run(fps,1,false);
                run(fps,1,true);
            }
        } finally { held.clear();edges.clear();Gdx.input=previous; }
    }

    private static void run(int fps,int deaths,boolean partial) throws Exception {
        held.clear();edges.clear();Main game=new Main();
        OrthographicCamera c=new OrthographicCamera(){@Override public void update(){} @Override public void update(boolean f){}};
        field("camera").set(game,c);field("viewport").set(game,new FitViewport(800,600,c));
        check(state(game)==GameState.TITLE_SCREEN,"Real initial state");
        press(game,Input.Keys.ENTER);
        StringBuilder trace=new StringBuilder("TITLE");
        StageResult[] saved=new StageResult[8];
        int totalItems=0,totalBonus=0,totalDeaths=0;
        for(int stage=1;stage<=8;stage++) {
            trace.append(" -> ").append(stage);
            check(state(game)==GameState.PLAYING && number(game,"currentStageNumber")==stage,"Exact next semester");
            check(number(game,"semesterDeathCount")==0 && number(game,"totalDeathCount")==totalDeaths
                && number(game,"lives")==number(game,"semesterMaxLives"),"New semester resets only semester counters");
            for(int i=0;i<stage-1;i++)check(results(game)[i]==saved[i],"Prior immutable result survives all transitions");
            if(deaths>0) {
                player(game).reset(80,-150);tick(game);totalDeaths++;
                check(state(game)==GameState.PLAYING && number(game,"semesterDeathCount")==1
                    && number(game,"totalDeathCount")==totalDeaths,"Fall charges once, retries same semester");
            }
            Array<?> items=(Array<?>)get(game,"gradeItems");
            int collect=items.size-(partial?1:0);
            for(int i=0;i<collect;i++) {
                Rectangle item=((GradeItem)items.get(i)).getBounds();
                player(game).reset(item.x,item.y);tick(game);
                check(state(game)==GameState.PLAYING,"Item contact fixture must be safe");
                check(number(game,"stageScore")==5*(i+1),"Each item collected exactly once");
                check(!((GradeItem)items.get(i)).tryCollect(player(game)),"Duplicate collection rejected");
            }
            Rectangle flag=(Rectangle)get(game,"flag");player(game).reset(flag.x,flag.y);tick(game);
            check(state(game)==GameState.STAGE_CLEAR,"All eight semesters show clear report");
            StageResult result=results(game)[stage-1];saved[stage-1]=result;
            check(result.getItemScore()==collect*5 && result.getCollectedItemCount()==collect
                && result.getDeathCount()==deaths && result.getSurvivalBonus()==20-deaths*5,"Saved actual scores/deaths/bonus");
            totalItems+=collect;totalBonus+=result.getSurvivalBonus();
            for(int i=0;i<fps/2;i++)tick(game);
            check(results(game)[stage-1]==result,"Clear screen cannot duplicate result");
            press(game,Input.Keys.ENTER);
            if(stage==2||stage==4||stage==6) {
                check(state(game)==GameState.VACATION_EVENT,"Required winter vacation");
                VacationMinigame mini=(VacationMinigame)get(game,"vacationGame");
                VacationEventType type=VacationEventType.afterStage(stage);
                check(mini.getType()==type && mini.getPhase()==VacationMinigame.Phase.INTRO,"Correct INTRO after confirmation");
                // Deterministic puzzles/drops, but no score, outcome, or completion injection.
                mini=type==VacationEventType.CAFE ? new CafeMinigame(new Random(17))
                    : type==VacationEventType.CERTIFICATION ? new CertificationMinigame(new Random(17))
                    : new HackathonMinigame(new Random(17));
                field("vacationGame").set(game,mini);
                boolean success=!partial || type!=VacationEventType.CERTIFICATION;
                playVacation(game,mini,success);
                trace.append(" -> ").append(type).append(success?"+":"-");
                check(results(game)[stage-1]==result,"Vacation cannot change confirmed semester result");
                press(game,Input.Keys.ENTER);
                check(((VacationManager)get(game,"vacationManager")).getResult(type).isSuccess()==success,"Actual result confirmed once");
                check(!game.completeVacationEvent(type,success),"Stale completion rejected");
            }
        }
        check(state(game)==GameState.GAME_COMPLETE && number(game,"currentStageNumber")==8,"Report instead of Stage 9");
        FinalReport report=(FinalReport)call(game,"getFinalReport");
        int expected=totalItems*5+totalBonus;
        check(report.getTotalItemScore()==totalItems*5 && report.getTotalSurvivalBonus()==totalBonus
            && report.getFinalScore()==expected && report.getTotalDeathCount()==totalDeaths,"Whole-run accounting exact");
        check(FinalReportRenderer.collectedItems(report)==totalItems && FinalReportRenderer.maximumItems(report)==61,"Final saved collection count");
        check(Math.abs(report.getGpa()-expected/465.0*4.5)<1e-12,"GPA uses actual total");
        held.add(Input.Keys.SPACE);held.add(Input.Keys.ENTER);edges.addAll(held);tick(game);edges.clear();
        for(int i=0;i<fps;i++)tick(game);
        check(state(game)==GameState.GRADUATION_ENDING && number(game,"totalDeathCount")==totalDeaths,"Held Space/Enter cannot skip ending or clear deaths");
        held.clear();press(game,Input.Keys.ENTER);
        check(state(game)==GameState.PLAYING && number(game,"currentStageNumber")==1 && number(game,"lives")==5
            && number(game,"stageScore")==0 && number(game,"totalDeathCount")==0,"Ending starts clean Stage 1");
        for(StageResult r:results(game))check(r==null,"All results cleared");
        for(int d:(int[])get(game,"semesterDeaths"))check(d==0,"All semester death records cleared");
        for(VacationEventType t:VacationEventType.values())check(((VacationManager)get(game,"vacationManager")).getResult(t)==null,"All vacation effects/history cleared");
        System.out.println(fps+" fps integration: "+trace+" -> REPORT -> ENDING -> NEW GAME; items="+totalItems+", deaths="+totalDeaths+", final="+expected);
    }

    private static void playVacation(Main game,VacationMinigame mini,boolean success) throws Exception {
        MinigameTestDriver driver=new MinigameTestDriver(success);
        int budget=(int)((mini.getTimeLimit()+3)/dt)*2;
        while(!mini.isFinished() && --budget>0) {
            MinigameInput next=driver.next(mini);held.clear();edges.clear();
            if(next.isLeftHeld())held.add(Input.Keys.LEFT);
            if(next.isRightHeld())held.add(Input.Keys.RIGHT);
            for(int key:ACTIONS)if(next.isPressed(key)){held.add(key);edges.add(key);}
            boolean action=!edges.isEmpty();tick(game);held.clear();edges.clear();
            // Release action keys between fresh presses, including repeated identical arrows.
            if(action && !mini.isFinished())tick(game);
        }
        check(budget>0 && mini.isSuccess()==success && state(game)==GameState.VACATION_EVENT,"Actual Main inputs reach desired vacation result");
    }
    private static void press(Main g,int key)throws Exception{held.add(key);edges.add(key);tick(g);held.clear();edges.clear();}
    private static void tick(Main g)throws Exception{Method m=Main.class.getDeclaredMethod("update",float.class);m.setAccessible(true);m.invoke(g,dt);}
    private static Object call(Main g,String n)throws Exception{Method m=Main.class.getDeclaredMethod(n);m.setAccessible(true);return m.invoke(g);}
    private static Field field(String n)throws Exception{Field f=Main.class.getDeclaredField(n);f.setAccessible(true);return f;}
    private static Object get(Main g,String n)throws Exception{return field(n).get(g);}
    private static int number(Main g,String n)throws Exception{return (Integer)get(g,n);}
    private static GameState state(Main g)throws Exception{return (GameState)get(g,"gameState");}
    private static Player player(Main g)throws Exception{return (Player)get(g,"player");}
    private static StageResult[] results(Main g)throws Exception{return (StageResult[])get(g,"completedResults");}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
