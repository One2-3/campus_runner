package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Redesigned finale: real movement/jump input, including branch return loops. No position edits. */
public final class SeniorSemester2Verification {
    private static boolean left,right,jump,enter,escape,spaceHeld,cameraAudit;
    private static float dt;
    private static int frames,seenHud,branchMask;
    private static final Method UPDATE=method("update",float.class),LOAD=method("loadStage",int.class);

    public static void verifyConnected(Main game,int fps) throws Exception {
        Input previous=Gdx.input;
        try {
            dt=1f/fps; left=right=jump=enter=escape=spaceHeld=cameraAudit=false;
            Gdx.input=(Input)Proxy.newProxyInstance(Input.class.getClassLoader(),new Class<?>[]{Input.class},
                (proxy,method,arguments)->{
                    int key=arguments==null?-1:(Integer)arguments[0];
                    if(method.getName().equals("isKeyPressed")) return (key==Input.Keys.RIGHT&&right)||(key==Input.Keys.LEFT&&left);
                    if(method.getName().equals("isKeyJustPressed")) return (key==Input.Keys.SPACE&&jump)||(key==Input.Keys.ENTER&&enter)||(key==Input.Keys.ESCAPE&&escape);
                    throw new UnsupportedOperationException(method.getName());
                });
            verifyDefinitions();
            StageResult[] prior=results(game).clone();
            for(int i=0;i<7;i++) check(prior[i].getItemScore()==20+i*5,"Incoming real semester "+i);
            tick(game);land(game,300,280);move(game,340);jump=true;move(game,480);
            waitFor(game,()->LifeVerificationSupport.died(game),3,"Real missed descending ledge fall");
            restart(game);
            route(game,0,true);
            for(int i=0;i<7;i++) check(results(game)[i]==prior[i],"Finale preserves semester "+i);
            check(results(game)[7].getItemScore()==60&&total(game) == LifeVerificationSupport.expectedTotal(game,305),"Final record and total 305");
            StageResult last=results(game)[7];
            for(int i=0;i<30;i++) tick(game);
            check(state(game)==GameState.GAME_COMPLETE&&number(game,"currentStageNumber")==8&&results(game)[7]==last&&total(game) == LifeVerificationSupport.expectedTotal(game,305),"No Stage 9 advance");
            try{StageFactory.createStage(9);throw new AssertionError("Stage 9 must be rejected");}catch(IllegalArgumentException expected){}
            enter=true;tick(game);
            check(state(game)==GameState.GAME_COMPLETE&&results(game)[7]==last,"Report Enter preserves results");
            jump=true;tick(game);
            check(state(game)==GameState.GRADUATION_ENDING&&results(game)[7]==last,"Space opens ending with final record");
            enter=true;tick(game);
            check(number(game,"currentStageNumber")==1&&state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game),"Ending Enter restarts Stage 1");
            for(StageResult r:results(game))check(r==null,"Existing new-game result reset");
            route(newGame(),1,false); // Upper choice can finish without lower items.
            route(newGame(),2,false); // Lower choice can finish without upper items.
            route(newGame(),3,false); // Starting lower can return and collect upper items too.
            verifyContacts();
            System.out.printf("%d fps: redesigned Stage 8 passed: descent, both branches/rejoin/return loops, tower switchback, "
                +"broken bridge, graduation tower, 12/60 and both partial routes, camera, retry/pause and GAME_COMPLETE%n",fps);
        }finally{cameraAudit=false;Gdx.input=previous;}
    }
    private static int total(Main game)throws Exception{Method m=Main.class.getDeclaredMethod("getTotalScore");m.setAccessible(true);return(Integer)m.invoke(game);}
    private static void verifyDefinitions(){
        StageDefinition s=StageFactory.createStage(8);
        check(s.getWorldWidth()==4500&&s.getStartX()==80&&s.getStartY()==360&&s.getYear()==4&&s.getSemester()==2,"New world and elevated spawn");
        check(s.getBlocks().size==22&&s.getGradeItems().size==12&&s.getMovingPlatforms().size==2&&s.getEnemies().size==3&&s.getTimedTraps().size==5,"Redesigned contents");
        check(s.getFlag().equals(new Rectangle(4440,480,20,100)),"High graduation flag");
        int maximum=0;
        for(GradeItem item:s.getGradeItems()){
            check(item.getScore()==5,"Five points each");maximum+=item.getScore();
            for(Rectangle b:s.getBlocks())check(!item.getBounds().overlaps(b),"Item outside solids");
            for(TimedTrap t:s.getTimedTraps())check(!item.getBounds().overlaps(t.getBounds()),"Item outside hazards");
        }
        check(maximum==60,"Maximum 60 unchanged");
        Array<PatrollingEnemy> enemies=s.getEnemies();
        float[][] patrols={{1320,1420,85,60,28,32},{2670,2700,90,320,28,32},{3520,3580,95,260,30,32}};
        int[] floors={8,14,18},reversals=new int[3],seen=new int[2],transitions=new int[2];
        boolean[] directions={enemies.get(0).isMovingRight(),enemies.get(1).isMovingRight(),enemies.get(2).isMovingRight()};
        Array<TimedTrap> traps=s.getTimedTraps();TrapState[] before={traps.get(3).getState(),traps.get(4).getState()};
        double[][] durations={{1.3,.7,1.8},{1.1,.7,1.6}};
        Array<MovingPlatform> platforms=s.getMovingPlatforms();
        for(int f=0;f<16/dt;f++){
            for(int i=0;i<3;i++){
                PatrollingEnemy e=enemies.get(i);float old=e.getBounds().x;e.update(dt);Rectangle b=e.getBounds(),floor=s.getBlocks().get(floors[i]);float[] p=patrols[i];
                check(b.x>=p[0]&&b.x<=p[1]&&b.y==p[3]&&b.width==p[4]&&b.height==p[5],"Patrol geometry "+i);
                check(Math.abs(b.x-old)<=p[2]*dt+.01f,"Continuous patrol "+i);
                check(b.x>=floor.x&&b.x+b.width<=floor.x+floor.width&&b.y==floor.y+floor.height,"Supported patrol "+i);
                if(directions[i]!=e.isMovingRight()){reversals[i]++;directions[i]=e.isMovingRight();}
            }
            for(TimedTrap t:traps)t.update(dt);
            for(int i=0;i<3;i++)check(traps.get(i).isFixed()&&traps.get(i).getState()==TrapState.ACTIVE,"Fixed spikes stay active");
            for(int i=0;i<2;i++){
                TimedTrap t=traps.get(i+3);TrapState current=t.getState();seen[i]|=1<<current.ordinal();
                if(before[i]!=current){check((before[i]==TrapState.SAFE&&current==TrapState.WARNING)||(before[i]==TrapState.WARNING&&current==TrapState.ACTIVE)||(before[i]==TrapState.ACTIVE&&current==TrapState.SAFE),"Ordered cycle");transitions[i]++;before[i]=current;}
                check(t.getStateElapsedTime()>=0&&t.getStateElapsedTime()<durations[i][current.ordinal()],"Phase duration");
            }
            for(int i=0;i<2;i++){
                MovingPlatform p=platforms.get(i);p.update(dt);Rectangle b=p.getBounds(),rider=new Rectangle(b.x,b.y+b.height,b.width,44);
                if(i==0)check(b.x==2190&&b.y>=120&&b.y<=320,"Vertical tower range");
                else check(b.y==400&&b.x>=4060&&b.x<=4140,"Final bridge range");
                for(Rectangle floor:s.getBlocks())check(!b.overlaps(floor)&&!rider.overlaps(floor),"No platform crush");
                for(TimedTrap t:traps)check(!b.overlaps(t.getBounds())&&!rider.overlaps(t.getBounds()),"Platform clear of hazards");
            }
        }
        for(int n:reversals)check(n>=4,"Repeated patrol reversals");
        for(int i=0;i<2;i++)check(seen[i]==7&&transitions[i]>=6,"Repeated timed cycle");
        check(360-200>650f*650/(2*1700),"Tower elevator cannot be replaced by direct low-to-high jump");
        check(4300-4030>260*2*650f/1700+32,"Graduation moving bridge needed");
    }
    private static void descend(Main game)throws Exception{
        tick(game);land(game,300,280);
        move(game,340);leap(game,560,200);
        move(game,620);leap(game,800,120);
        move(game,840);leap(game,1020,120);
        check(x(game)>=1000&&x(game)<1160,"Descent reaches fork entry");
    }
    private static void merged(Main game,int branch)throws Exception{
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)&&x(game)>=1700&&x(game)<1960&&player(game).getY()==120,"Both paths merge on same ledge");branchMask|=branch;
    }
    private static void upper(Main game)throws Exception{
        move(game,1120);leap(game,1230,220);move(game,1250);leap(game,1390,280);
        move(game,1410);leap(game,1550,240);move(game,1570);leap(game,1730,120);merged(game,1);
    }
    private static void lowerEntry(Main game)throws Exception{move(game,1120);land(game,1240,60);move(game,1260);}
    private static void lowerForward(Main game)throws Exception{
        waitFor(game,()->!enemy(game,0).isMovingRight()&&enemy(game,0).getBounds().x<=1360,4,"Lower route enemy return");
        leap(game,1450,60);check(x(game)>enemy(game,0).getBounds().x+28,"Lower enemy evasion");
        move(game,1470);crossTimed(game,3,1650);leap(game,1730,120);merged(game,2);
    }
    private static void lowerReverse(Main game)throws Exception{
        leap(game,1640,60);crossTimed(game,3,1470);
        waitFor(game,()->enemy(game,0).isMovingRight()&&enemy(game,0).getBounds().x>=1380,4,"Return enemy window");
        leap(game,1260,60);check(x(game)+32<enemy(game,0).getBounds().x,"Reverse lower enemy evasion");
    }
    private static void branch(Main game,int mode)throws Exception{
        if(mode==0){upper(game);lowerReverse(game);lowerForward(game);}
        if(mode==1)upper(game);
        if(mode==2){lowerEntry(game);lowerForward(game);}
        if(mode==3){lowerEntry(game);lowerForward(game);lowerReverse(game);move(game,1220);leap(game,1100,120);upper(game);}
        int expected=(mode==1?1:mode==2?2:3);check(branchMask==expected,"Selected paths actually traversed and rejoined");
    }
    private static void tower(Main game,boolean exercise)throws Exception{
        move(game,1940);leap(game,2080,200);move(game,2070);
        waitFor(game,()->moving(game,0).getBounds().y<=160,7,"Tower lift low window");jumpToSupport(game,2240,moving(game,0));
        if(exercise){verifyRide(game,0,8,65);verifyPause(game);}
        move(game,2200);waitFor(game,()->moving(game,0).isMovingForward()&&moving(game,0).getBounds().y>=310,7,"Tower high window");
        leap(game,2050,360);check(support(game)==null,"Switchback leaves lift to left");
        move(game,2090);leap(game,2280,440);land(game,2410,400);
        check(player(game).getY()==400,"Vertical tower complete after left/right fixed-platform changes");
    }
    private static void bridgeEnemy(Main game)throws Exception{
        leap(game,2620,320);
        waitFor(game,()->!enemy(game,1).isMovingRight()&&enemy(game,1).getBounds().x<=2690,3,"First broken-bridge enemy window");
        leap(game,2740,320);check(x(game)>enemy(game,1).getBounds().x+28,"Bridge enemy evasion");
    }
    private static void bridgeTimed(Main game)throws Exception{bridgeEnemy(game);leap(game,2840,360);}
    private static void bridgeLast(Main game)throws Exception{
        bridgeTimed(game);crossTimed(game,4,2950);leap(game,3100,280);leap(game,3290,340);leap(game,3470,260);
        check(x(game)+32<3520,"Protected far bridge island");
    }
    private static void graduation(Main game,boolean exercise)throws Exception{
        bridgeLast(game);
        waitFor(game,()->!enemy(game,2).isMovingRight()&&enemy(game,2).getBounds().x<=3550,3,"Far bridge enemy return");
        leap(game,3600,260);check(x(game)>enemy(game,2).getBounds().x+30,"Last enemy evasion");
        leap(game,3760,320);move(game,3820);leap(game,3960,400);move(game,3980);
        waitFor(game,()->moving(game,1).getBounds().x<=4080,4,"Graduation moving bridge approach");jumpToSupport(game,4110,moving(game,1));
        if(exercise){verifyRide(game,1,4,65);verifyPause(game);}
        waitFor(game,()->moving(game,1).isMovingForward()&&moving(game,1).getBounds().x>=4120,4,"Final high jump window");
        move(game,moving(game,1).getBounds().x+80);leap(game,4340,480);move(game,4440);
    }
    private static void route(Main game,int mode,boolean exercise)throws Exception{
        frames=seenHud=branchMask=0;cameraAudit=true;
        descend(game);branch(game,mode);tower(game,exercise);graduation(game,exercise);
        cameraAudit=false;int count=(mode==1||mode==2)?10:12;
        check(state(game)==GameState.GAME_COMPLETE&&number(game,"currentStageNumber")==8&&number(game,"collectedItemCount")==count
            &&results(game)[7].getItemScore()==count*5&&results(game)[7].getCollectedItemCount()==count,"Actual completion/score; count="+number(game,"collectedItemCount"));
        check(seenHud==(1<<(count+1))-1,"Every HUD count 0 through "+count);
        Array<?> items=(Array<?>)get(game,"gradeItems");
        for(int i=0;i<12;i++){boolean skipped=(mode==1&&(i==4||i==5))||(mode==2&&(i==2||i==3));check(((GradeItem)items.get(i)).isCollected()!=skipped,"Intended branch item "+i);}
        System.out.printf("%d fps: redesigned Stage 8 mode %d, %d/12, %d points, %.2fs%n",Math.round(1/dt),mode,count,count*5,frames*dt);
    }
    private static void crossTimed(Main game,int index,float target)throws Exception{
        check(!player(game).overlaps(trap(game,index).getBounds()),"Safe timed waiting point");
        waitFor(game,()->trap(game,index).getState()==TrapState.SAFE&&trap(game,index).getStateElapsedTime()<.08,5,"Fresh SAFE "+index);
        boolean touched=false;boolean forward=target>x(game);right=forward;left=!forward;int remaining=(int)(2/dt);
        while((forward?x(game)<target:x(game)>target)&&--remaining>0&&state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)){
            tick(game);if(player(game).overlaps(trap(game,index).getBounds())){touched=true;check(trap(game,index).getState()==TrapState.SAFE&&!trap(game,index).isDangerousTo(player(game)),"Actual SAFE passage "+index);}
        }
        right=left=false;check(remaining>0&&touched&&state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game),"Timed crossing both directions");
    }
    private static void verifyContacts()throws Exception{
        cameraAudit=false;
        Main first=newGame();tick(first);land(first,300,280);move(first,420);check(LifeVerificationSupport.died(first)&&LifeVerificationSupport.failedTrap(first,0).isDangerousTo(LifeVerificationSupport.failedPlayer(first)),"Bad descent landing contacts spikes");restart(first);
        Main second=newGame();tick(second);land(second,300,280);move(second,340);leap(second,560,200);move(second,620);leap(second,800,120);move(second,930);
        check(LifeVerificationSupport.died(second)&&LifeVerificationSupport.failedTrap(second,1).isDangerousTo(LifeVerificationSupport.failedPlayer(second)),"Lower terrace spikes");restart(second);
        for(int i=0;i<3;i++){
            Main game=newGame();descend(game);
            if(i==0){lowerEntry(game);move(game,1390);}
            else{upper(game);tower(game,false);if(i==1){leap(game,2620,320);move(game,2700);}else{bridgeLast(game);move(game,3560);}}
            check(LifeVerificationSupport.died(game)&&LifeVerificationSupport.failedEnemy(game,i).touches(LifeVerificationSupport.failedPlayer(game)),"Enemy hit "+i);restart(game);
        }
        Main fixed=newGame();descend(fixed);upper(fixed);tower(fixed,false);bridgeTimed(fixed);crossTimed(fixed,4,2950);leap(fixed,3100,280);move(fixed,3200);
        check(LifeVerificationSupport.died(fixed)&&LifeVerificationSupport.failedTrap(fixed,2).isDangerousTo(LifeVerificationSupport.failedPlayer(fixed)),"Broken bridge fixed spikes");restart(fixed);
        for(int index=3;index<5;index++){
            Main game=newGame();descend(game);
            if(index==3){lowerEntry(game);waitFor(game,()->!enemy(game,0).isMovingRight()&&enemy(game,0).getBounds().x<=1360,4,"Lower collision setup");leap(game,1450,60);move(game,1470);}
            else{upper(game);tower(game,false);bridgeTimed(game);}
            final int selected=index;waitFor(game,()->trap(game,selected).getState()==TrapState.ACTIVE&&trap(game,selected).getStateElapsedTime()<.08,5,"Fresh ACTIVE");
            move(game,trap(game,index).getBounds().x+30);check(LifeVerificationSupport.died(game)&&LifeVerificationSupport.failedTrap(game,index).isDangerousTo(LifeVerificationSupport.failedPlayer(game)),"Active timed collision "+index);restart(game);
        }
    }
    private static void jumpToSupport(Main game,float target,MovingPlatform p)throws Exception{
        check((Boolean)playerField(game,"grounded"),"Moving jump starts grounded");visible(game,target,p.getBounds().y+p.getBounds().height);
        jump=true;move(game,target);waitFor(game,()->support(game)==p||state(game)!=GameState.PLAYING,2,"Moving platform landing");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)&&support(game)==p,"Actual moving platform boarding");
    }
    private static void visible(Main game,float target,float height)throws Exception{
        OrthographicCamera c=(OrthographicCamera)get(game,"camera");
        check(target>=c.position.x-400&&target+32<=c.position.x+400&&height>=0&&height+44<600,"Next landing visible "+target);
        float low=Math.min(x(game),target),high=Math.max(x(game)+32,target+32);
        for(Object value:(Array<?>)get(game,"enemies")){Rectangle b=((PatrollingEnemy)value).getBounds();if(b.x<high&&b.x+b.width>low&&b.y<height+150)check(b.x>=c.position.x-400&&b.x+b.width<=c.position.x+400,"Path enemy visible");}
        for(Object value:(Array<?>)get(game,"timedTraps")){Rectangle b=((TimedTrap)value).getBounds();if(b.x<high&&b.x+b.width>low&&b.y<height+150)check(b.x>=c.position.x-400&&b.x+b.width<=c.position.x+400,"Path hazard visible");}
    }
    private static void verifyRide(Main game,int index,int seconds,float speed)throws Exception{
        MovingPlatform p=moving(game,index);float offset=x(game)-p.getBounds().x;boolean direction=p.isMovingForward();int reversals=0;
        float low=player(game).getY(),high=low;
        for(int i=0;i<seconds/dt;i++){tick(game);check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)&&support(game)==p&&Math.abs(x(game)-p.getBounds().x-offset)<.05f
            &&Math.abs(player(game).getY()-p.getBounds().y-p.getBounds().height)<.01f,"Stable rider");
            check(Math.abs(p.getDeltaX())+Math.abs(p.getDeltaY())<=speed*dt+.01f,"Continuous reversal");
            if(direction!=p.isMovingForward()){reversals++;direction=p.isMovingForward();}low=Math.min(low,player(game).getY());high=Math.max(high,player(game).getY());}
        check(reversals>=2,"Both endpoints with rider");if(index==0)check(high-low>195,"Full vertical tower ride");
    }
    private static void restart(Main game)throws Exception{
        check(LifeVerificationSupport.died(game),"Retry after real failure");LifeVerificationSupport.verifyAutomaticRetry(game);
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)&&number(game,"currentStageNumber")==8&&x(game)==80&&player(game).getY()==360
            &&number(game,"collectedItemCount")==0&&number(game,"stageScore")==0,"Elevated retry spawn/counters");
        StageDefinition initial=StageFactory.createStage(8);
        for(Object item:(Array<?>)get(game,"gradeItems"))check(!((GradeItem)item).isCollected(),"All twelve restored");
        for(int i=0;i<2;i++)check(moving(game,i).getBounds().equals(initial.getMovingPlatforms().get(i).getBounds()),"Platform reset");
        for(int i=0;i<3;i++){PatrollingEnemy e=initial.getEnemies().get(i);check(enemy(game,i).getBounds().equals(e.getBounds())&&enemy(game,i).isMovingRight()==e.isMovingRight(),"Enemy reset");}
        for(int i=0;i<5;i++){TimedTrap t=initial.getTimedTraps().get(i);check(trap(game,i).getBounds().equals(t.getBounds())&&trap(game,i).isFixed()==t.isFixed()
            &&trap(game,i).getState()==t.getState()&&trap(game,i).getStateElapsedTime()==t.getStateElapsedTime(),"Hazard reset");}
    }
    private static void verifyPause(Main game)throws Exception{
        Rectangle[] enemies=new Rectangle[3],platforms=new Rectangle[2];boolean[] directions=new boolean[3];TrapState[] states=new TrapState[2];double[] elapsed=new double[2];
        for(int i=0;i<3;i++){enemies[i]=enemy(game,i).getBounds();directions[i]=enemy(game,i).isMovingRight();}
        for(int i=0;i<2;i++){platforms[i]=moving(game,i).getBounds();states[i]=trap(game,i+3).getState();elapsed[i]=trap(game,i+3).getStateElapsedTime();}
        float px=x(game),py=player(game).getY();escape=true;tick(game);for(int i=0;i<30;i++)tick(game);
        check(state(game)==GameState.PAUSED&&x(game)==px&&player(game).getY()==py,"Pause player");
        for(int i=0;i<3;i++)check(enemy(game,i).getBounds().equals(enemies[i])&&enemy(game,i).isMovingRight()==directions[i],"Pause enemy");
        for(int i=0;i<2;i++)check(moving(game,i).getBounds().equals(platforms[i])&&trap(game,i+3).getState()==states[i]&&trap(game,i+3).getStateElapsedTime()==elapsed[i],"Pause platform/timer");
        escape=true;tick(game);check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game),"Resume");
    }
    private static MovingPlatform moving(Main game,int i)throws Exception{return(MovingPlatform)((Array<?>)get(game,"movingPlatforms")).get(i);}
    private static PatrollingEnemy enemy(Main game,int i)throws Exception{return(PatrollingEnemy)((Array<?>)get(game,"enemies")).get(i);}

    private static void leap(Main game, float target, float height) throws Exception {
        check((Boolean) playerField(game, "grounded"), "Jump starts grounded");
        visible(game,target,height);
        jump = true; move(game, target);
        waitFor(game, () -> (Boolean) playerField(game, "grounded") || state(game) != GameState.PLAYING, 2, "Landing");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game) && Math.abs(player(game).getY() - height) < .01f,
            "Land " + target + "/" + height + " actual " + x(game) + "/" + player(game).getY());
    }
    private static void move(Main game,float target)throws Exception{
        boolean forward=target>x(game);right=forward;left=!forward;
        waitFor(game,()->(forward?x(game)>=target:x(game)<=target)||state(game)!=GameState.PLAYING,12,"Move "+target);
        right=left=false;
    }
    private static void land(Main game,float target,float height)throws Exception{
        visible(game,target,height);move(game,target);
        waitFor(game,()->(Boolean)playerField(game,"grounded")||state(game)!=GameState.PLAYING,2,"Landing");
        check(state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)&&Math.abs(player(game).getY()-height)<.01f,"Land "+target+"/"+height+" actual "+x(game)+"/"+player(game).getY());
    }
    private interface Condition { boolean ready() throws Exception; }
    private static void waitFor(Main game, Condition condition, int seconds, String message) throws Exception {
        int remaining = (int) (seconds / dt) + 1;
        while (!condition.ready() && !LifeVerificationSupport.died(game) && --remaining > 0) tick(game);
        check(remaining > 0, message + " x=" + x(game) + " y=" + player(game).getY());
    }
    private static Main newGame() throws Exception {
        left = right = jump = enter = escape = spaceHeld = false;
        Main game = new Main();
        OrthographicCamera camera = new OrthographicCamera() {
            @Override public void update() { }
            @Override public void update(boolean updateFrustum) { }
        };
        field(Main.class, "camera").set(game, camera);
        field(Main.class, "viewport").set(game, new FitViewport(800, 600, camera));
        LOAD.invoke(game, 8); return game;
    }
    private static void tick(Main game) throws Exception {
        LifeVerificationSupport.update(game, UPDATE, dt); jump = enter = escape = false; frames++;
        if(cameraAudit && state(game) == GameState.PLAYING && !LifeVerificationSupport.died(game)){
            OrthographicCamera c=(OrthographicCamera)get(game,"camera");Rectangle b=player(game).getBounds();
            check(c.position.y==300 && c.position.x>=400 && c.position.x<=4100,"Camera remains inside world without Y jumps");
            check(b.y>=0 && b.y+b.height<=600 && b.x>=c.position.x-400 && b.x+b.width<=c.position.x+400,"Entire player remains visible");
        }
        if (number(game, "currentStageNumber") == 8) {
            int count = number(game, "collectedItemCount");
            check(count >= 0 && count <= 12 && ((Array<?>) get(game, "gradeItems")).size == 12
                && number(game, "stageScore") == count * 5, "Dynamic HUD 0-12 / score");
            seenHud |= 1 << count;
        }
    }
    private static Object get(Main game, String name) throws Exception { return field(Main.class, name).get(game); }
    private static Object playerField(Main game, String name) throws Exception { return field(Player.class, name).get(player(game)); }
    private static int number(Main game, String name) throws Exception { return (Integer) get(game, name); }
    private static Player player(Main game) throws Exception { return (Player) get(game, "player"); }
    private static float x(Main game) throws Exception { return player(game).getCenterX() - 16; }
    private static GameState state(Main game) throws Exception { return (GameState) get(game, "gameState"); }
    private static StageResult[] results(Main game) throws Exception { return (StageResult[]) get(game, "completedResults"); }
    private static MovingPlatform platform(Main game) throws Exception { return (MovingPlatform) ((Array<?>) get(game, "movingPlatforms")).first(); }
    private static TimedTrap trap(Main game, int index) throws Exception { return (TimedTrap) ((Array<?>) get(game, "timedTraps")).get(index); }
    private static Object support(Main game) throws Exception { return playerField(game, "supportingPlatform"); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static Method method(String name, Class<?> parameter) {
        try {
            Method method = Main.class.getDeclaredMethod(name, parameter); method.setAccessible(true); return method;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
