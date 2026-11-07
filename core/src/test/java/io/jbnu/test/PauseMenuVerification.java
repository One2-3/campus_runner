package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

/** Input edges drive the real Main pause actions; no renderer/GL is needed. */
public final class PauseMenuVerification {
    private static final Set<Integer> held = new HashSet<>(), edges = new HashSet<>();
    private static float dt;
    public static void main(String[] args) throws Exception {
        Input previous = Gdx.input;
        try {
            Gdx.input = (Input)Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[]{Input.class},
                (p,m,a) -> m.getName().equals("isKeyPressed") ? held.contains((Integer)a[0])
                    : m.getName().equals("isKeyJustPressed") && edges.contains((Integer)a[0]));
            for (int fps : new int[]{30,60,120}) {
                dt = 1f / fps; held.clear(); edges.clear();
                Main game = new Main();
                OrthographicCamera camera = new OrthographicCamera() {
                    @Override public void update() { }
                    @Override public void update(boolean updateFrustum) { }
                };
                field("camera").set(game,camera);
                field("viewport").set(game,new FitViewport(800,600,camera));
                invoke(game,"loadStage",1);
                invoke(game,"recordStageResult");
                StageResult prior = ((StageResult[])get(game,"completedResults"))[0];
                VacationManager vacation = (VacationManager)get(game,"vacationManager");
                vacation.beginAfterStage(6);
                check(vacation.complete(VacationEventType.HACKATHON,true),"Existing vacation success fixture");
                invoke(game,"loadStage",7);
                PlayerModifier modifier = vacation.modifierForStage(7);
                player(game).reset(80,-150); tick(game); // Existing real fall/death path.
                Rectangle item = ((GradeItem)((Array<?>)get(game,"gradeItems")).first()).getBounds();
                player(game).reset(item.x,item.y); tick(game);
                check(number(game,"stageScore") == 5 && number(game,"lives") == 4,"Meaningful retry fixture");
                press(game,Input.Keys.ESCAPE);
                check(get(game,"gameState") == GameState.PAUSED && number(game,"pauseSelection") == 0,"ESC enters at RESUME");
                String frozen = snapshot(game);
                held.add(Input.Keys.RIGHT); held.add(Input.Keys.SPACE);
                for (int i=0;i<fps*3;i++) tick(game);
                held.clear();
                check(snapshot(game).equals(frozen),"All player/enemy/platform/trap/item/camera state frozen");
                press(game,Input.Keys.ESCAPE);
                check(get(game,"gameState") == GameState.PLAYING && snapshot(game).equals(frozen),"ESC resumes exact world");
                press(game,Input.Keys.ESCAPE); press(game,Input.Keys.ENTER);
                check(get(game,"gameState") == GameState.PLAYING && snapshot(game).equals(frozen),"Enter RESUME preserves world");
                press(game,Input.Keys.ESCAPE); press(game,Input.Keys.UP);
                check(number(game,"pauseSelection") == 2,"UP wraps to QUIT");
                press(game,Input.Keys.DOWN);
                check(number(game,"pauseSelection") == 0,"DOWN wraps to RESUME");
                held.add(Input.Keys.DOWN); edges.add(Input.Keys.DOWN); tick(game); edges.clear();
                for(int i=0;i<fps;i++) tick(game);
                held.clear(); check(number(game,"pauseSelection") == 1,"Held arrow moves once");
                Object oldPlayer = player(game), oldPlatforms = get(game,"movingPlatforms");
                held.add(Input.Keys.ENTER); edges.add(Input.Keys.ENTER); tick(game); edges.clear();
                Object restarted = player(game);
                check(restarted != oldPlayer && get(game,"movingPlatforms") != oldPlatforms,"RESTART replaces runtime");
                check(number(game,"currentStageNumber")==7 && number(game,"lives")==4
                    && number(game,"semesterDeathCount")==1 && number(game,"totalDeathCount")==1,"Retry preserves stage/lives/deaths");
                check(number(game,"stageScore")==0 && number(game,"collectedItemCount")==0
                    && ((StageResult[])get(game,"completedResults"))[0] == prior,"Retry resets items but keeps previous result");
                check(vacation.modifierForStage(7)==modifier && modifier!=PlayerModifier.NONE,
                    "Retry keeps active vacation modifier without stacking");
                StageDefinition fresh = StageFactory.createStage(7);
                check(player(game).getBounds().x==fresh.getStartX() && player(game).getY()==fresh.getStartY(),"Retry restores spawn");
                String reset = snapshot(game);
                for(int i=0;i<fps;i++) tick(game);
                check(player(game)==restarted && snapshot(game).equals(reset),"Held Enter cannot repeat action or progress reset world");
                held.clear(); tick(game);
                press(game,Input.Keys.ESCAPE);
                held.add(Input.Keys.DOWN); held.add(Input.Keys.ENTER); edges.addAll(held); tick(game); held.clear(); edges.clear();
                check(get(game,"gameState")==GameState.PAUSED && number(game,"pauseSelection")==1,"Simultaneous navigation/Enter only navigates");
                press(game,Input.Keys.DOWN);
                held.add(Input.Keys.ENTER); edges.add(Input.Keys.ENTER); tick(game); edges.clear();
                check(get(game,"gameState")==GameState.TITLE_SCREEN && number(game,"currentStageNumber")==1,"QUIT returns to title, not application exit");
                check(number(game,"lives")==5 && number(game,"stageScore")==0 && number(game,"semesterDeathCount")==0
                    && number(game,"totalDeathCount")==0,"QUIT resets scores/lives/deaths");
                for(StageResult result:(StageResult[])get(game,"completedResults"))check(result==null,"QUIT clears all results");
                for(VacationEventType type:VacationEventType.values())check(vacation.getResult(type)==null,"QUIT clears vacation history");
                check(vacation.modifierForStage(7)==PlayerModifier.NONE,"QUIT clears vacation effect");
                for(int i=0;i<fps;i++)tick(game);
                check(get(game,"gameState")==GameState.TITLE_SCREEN,"Held confirmation cannot skip title");
                held.clear(); tick(game); press(game,Input.Keys.ENTER);
                check(get(game,"gameState")==GameState.PLAYING && number(game,"currentStageNumber")==1,"Fresh Enter starts Stage 1");
                System.out.println(fps+" fps: pause menu passed: frozen world/timers, ESC/RESUME, edge navigation/wrapping, retry preservation, title reset, held/simultaneous guards.");
            }
        } finally { Gdx.input = previous; }
    }
    private static String snapshot(Main game)throws Exception {
        StringBuilder s=new StringBuilder().append(player(game).getBounds()).append(player(game).isFacingRight());
        for(Object p:(Array<?>)get(game,"movingPlatforms"))s.append(((MovingPlatform)p).getBounds());
        for(Object e:(Array<?>)get(game,"enemies"))s.append(((PatrollingEnemy)e).getBounds()).append(((PatrollingEnemy)e).isMovingRight());
        for(Object t:(Array<?>)get(game,"timedTraps"))s.append(((TimedTrap)t).getState()).append(((TimedTrap)t).getStateElapsedTime());
        for(Object i:(Array<?>)get(game,"gradeItems"))s.append(((GradeItem)i).isCollected());
        for(String n:new String[]{"stageScore","collectedItemCount","lives","semesterDeathCount","totalDeathCount"})s.append(get(game,n));
        s.append(((OrthographicCamera)get(game,"camera")).position);
        return s.toString();
    }
    private static void press(Main g,int key)throws Exception {held.add(key);edges.add(key);tick(g);edges.clear();held.clear();}
    private static void tick(Main g)throws Exception {Method m=Main.class.getDeclaredMethod("update",float.class);m.setAccessible(true);m.invoke(g,dt);}
    private static void invoke(Main g,String n,Object...args)throws Exception {Method m=args.length==0?Main.class.getDeclaredMethod(n):Main.class.getDeclaredMethod(n,int.class);m.setAccessible(true);m.invoke(g,args);}
    private static Player player(Main g)throws Exception{return (Player)get(g,"player");}
    private static int number(Main g,String n)throws Exception{return (Integer)get(g,n);}
    private static Object get(Main g,String n)throws Exception{return field(n).get(g);}
    private static Field field(String n)throws Exception{Field f=Main.class.getDeclaredField(n);f.setAccessible(true);return f;}
    private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
}
