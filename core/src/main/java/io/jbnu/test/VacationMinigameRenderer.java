package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.Locale;

/** Fixed UI coordinates, independent palettes, and read-only access to gameplay models. */
public final class VacationMinigameRenderer implements Disposable {
    private static final Color TRASH=new Color(0x66727AFF);
    private static final Color INACTIVE=new Color(0x49677CFF), AMBER=new Color(0xE9B86CFF);
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font=new BitmapFont(Gdx.files.internal("fonts/vacation-ko.fnt"));
    private final GlyphLayout layout=new GlyphLayout();
    private final FitViewport viewport;
    private final OrthographicCamera camera;
    private final Matrix4 projection=new Matrix4(),transform=new Matrix4();
    private final Matrix4 shapeProjection=new Matrix4(),shapeTransform=new Matrix4(),identity=new Matrix4();
    private final Color shapeColor=new Color();
    private VacationUiTheme theme;
    private float textMaxWidth;
    // Inside the cafe panel, above the decorative counter strip. Physics coordinates are unchanged.
    private final Rectangle cafeClipArea=new Rectangle(30,65,740,409);
    private final Rectangle cafeScissors=new Rectangle();

    public VacationMinigameRenderer(SpriteBatch batch,ShapeRenderer shapes,FitViewport viewport,OrthographicCamera camera) {
        this.batch=batch;this.shapes=shapes;this.viewport=viewport;this.camera=camera;
        for(TextureRegion region:font.getRegions())region.getTexture().setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        font.setUseIntegerPositions(true);
    }

    public void render(VacationMinigame game) {
        theme=VacationUiTheme.forType(game.getType());
        textMaxWidth=game.getPhase()==VacationMinigame.Phase.PLAYING?736:584;
        float color=batch.getPackedColor();ShaderProgram shader=batch.getShader();boolean blending=batch.isBlendingEnabled();
        int src=batch.getBlendSrcFunc(),dst=batch.getBlendDstFunc(),srcA=batch.getBlendSrcFuncAlpha(),dstA=batch.getBlendDstFuncAlpha();
        projection.set(batch.getProjectionMatrix());transform.set(batch.getTransformMatrix());
        shapeProjection.set(shapes.getProjectionMatrix());shapeTransform.set(shapes.getTransformMatrix());shapeColor.set(shapes.getColor());
        try {
            ScreenUtils.clear(theme.background);viewport.apply();
            shapes.setProjectionMatrix(camera.combined);shapes.setTransformMatrix(identity);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            if(game.getPhase()==VacationMinigame.Phase.PLAYING)playPanels(game);else sheet(game);
            shapes.end();
            batch.setProjectionMatrix(camera.combined);batch.setTransformMatrix(identity);batch.setShader(null);
            batch.enableBlending();batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.begin();batch.setColor(Color.WHITE);font.setColor(theme.ink);
            center(theme.title,576,1.6f);
            center(game.getPhase()==VacationMinigame.Phase.RESULT?theme.resultTitle
                :game.getPhase()==VacationMinigame.Phase.INTRO?"겨울방학 / 시작 안내":"겨울방학 / 활동 진행 중",550,.85f);
            if(game.getPhase()==VacationMinigame.Phase.INTRO)intro(game);
            else if(game.getPhase()==VacationMinigame.Phase.RESULT)result(game);else playText(game);
        } finally {
            if(shapes.isDrawing())shapes.end();if(batch.isDrawing())batch.end();
            shapes.setProjectionMatrix(shapeProjection);shapes.setTransformMatrix(shapeTransform);shapes.setColor(shapeColor);
            batch.setProjectionMatrix(projection);batch.setTransformMatrix(transform);batch.setShader(shader);batch.setPackedColor(color);
            batch.setBlendFunctionSeparate(src,dst,srcA,dstA);if(blending)batch.enableBlending();else batch.disableBlending();
        }
    }

    private void sheet(VacationMinigame game) {
        panel(80,96,640,430);
        if(theme==VacationUiTheme.CAFE)for(int x=96;x<708;x+=16)rect(x,112,8,2,theme.secondary);
        else if(theme==VacationUiTheme.STUDY){
            for(int y=144;y<480;y+=38)rect(124,y,570,1,theme.background);
        }
        else {rect(96,118,4,382,theme.accent);rect(700,118,4,382,theme.secondary);}
        if(game.getPhase()==VacationMinigame.Phase.INTRO){panel(104,146,592,76);button(180,38,440,38);}
        else {panel(112,228,576,94);button(140,38,520,38);}
    }
    private void intro(VacationMinigame game) {
        String[] lines;
        switch(game.getType()) {
            case CAFE:lines=new String[]{"왼쪽 / 오른쪽 방향키로 쟁반을 움직이세요.","커피를 받으면 +10점입니다.",
                "회색 마름모 쓰레기는 -10점입니다. (최소 0점)","커피를 놓쳐도 점수는 줄어들지 않습니다.",
                (int)game.getTimeLimit()+"초가 끝날 때 "+CafeMinigame.TARGET+"점 이상이면 성공합니다.","목표 점수를 넘어도 끝까지 플레이하세요."};break;
            case CERTIFICATION:lines=new String[]{"화면에 보이는 방향 순서를 외우세요.","방향이 숨겨지면 같은 순서로 입력하세요.",
                "위 / 아래 / 왼쪽 / 오른쪽 방향키를 한 번씩 누르세요.","총 5단계: 순서 길이는 2, 3, 3, 4, 5입니다.",
                "틀리면 같은 단계를 다시 외웁니다.",(int)game.getTimeLimit()+"초 안에 완료하세요. 3번 틀리면 실패합니다."};break;
            default:lines=new String[]{"방향키로 회로 칸을 선택하세요.","Space : 회로를 시계 방향으로 90도 회전합니다.",
                "Enter : 컴퓨터와 서버의 연결을 검사합니다.","양쪽 연결 방향을 맞추세요. 고정 칸은 회전하지 않습니다.",
                (int)game.getTimeLimit()+"초 안에 퍼즐 3개를 연결하세요.","연결 검사에 3번 실패하면 종료됩니다."};break;
        }
        for(int i=0;i<lines.length;i++)center(lines[i],482-i*40,1f);
        center("성공: "+effectText(game.getType(),true),195,1f);
        center("실패: "+effectText(game.getType(),false),167,1f);
        font.setColor(theme.ink);
        center("Enter : 시작",57,1.2f);
    }
    private void result(VacationMinigame game) {
        font.setColor(game.isSuccess()?(theme==VacationUiTheme.SERVER?theme.accent:theme.border):theme.failure);
        center(game.isSuccess()?"성공":"실패",454,2.5f);font.setColor(theme.ink);
        center(summary(game),396,1.3f);
        center(String.format(Locale.ROOT,"남은 시간: %.1f / %.0f초",game.getTimeRemaining(),game.getTimeLimit()),360,1f);
        center("다음 학년: "+effectText(game.getType(),game.isSuccess()),289,1.05f);
        center(game.getType().getTargetYear()+"학년 1·2학기 적용",258,1f);
        center("미니게임 결과는 학기 점수에 반영되지 않습니다.",182,.95f);
        font.setColor(theme.ink);
        center("Enter : 다음 학년 시작",57,1.15f);
    }
    static String summary(VacationMinigame game) {
        if(game instanceof CafeMinigame)return "점수: "+((CafeMinigame)game).getScore()+" / 목표: "+CafeMinigame.TARGET+"점";
        if(game instanceof CertificationMinigame)return "통과 단계: "+((CertificationMinigame)game).getCompletedRounds()+" / 5";
        return "연결한 퍼즐: "+((HackathonMinigame)game).getCompletedPuzzles()+" / 3";
    }

    static String effectText(VacationEventType type,boolean success) {
        PlayerModifier modifier=PlayerModifier.fromVacation(type,success);
        if(modifier.getExtraStartingLives()>0)return "시작 목숨 +"+modifier.getExtraStartingLives()+" ("+(Main.MAX_LIVES+modifier.getExtraStartingLives())+"개)";
        boolean jump=type==VacationEventType.CERTIFICATION;
        int percent=Math.round(((jump?modifier.getJumpPowerMultiplier():modifier.getMoveSpeedMultiplier())-1)*100);
        return (jump?"점프력 ":"이동 속도 ")+String.format(Locale.ROOT,"%+d%%",percent);
    }

    private void playPanels(VacationMinigame game) {
        for(int i=0;i<3;i++)panel(28+i*256,504,232,34);
        if(game instanceof CafeMinigame) {
            panel(28,60,744,416);for(int y=120;y<450;y+=64)rect(32,y,736,1,theme.background);rect(30,62,740,3,theme.secondary);
            CafeMinigame cafe=(CafeMinigame)game;
            rect(cafe.getTrayX(),CafeMinigame.TRAY_Y,CafeMinigame.TRAY_WIDTH,CafeMinigame.TRAY_HEIGHT,theme.border);
            rect(cafe.getTrayX()+3,CafeMinigame.TRAY_Y+3,CafeMinigame.TRAY_WIDTH-6,CafeMinigame.TRAY_HEIGHT-6,theme.secondary);
            shapes.flush(); // Submit the frame/tray before applying a clip to falling items only.
            viewport.calculateScissors(shapes.getTransformMatrix(),cafeClipArea,cafeScissors);
            if(ScissorStack.pushScissors(cafeScissors))try {
              for(CafeMinigame.Drop drop:cafe.getDrops()) {
                float x=drop.getX(),y=drop.getY();
                if(drop.isCoffee()) {
                    rect(x,y,20,24,theme.border);rect(x+20,y+8,4,12,theme.border);
                    rect(x+2,y+3,16,18,theme.secondary);rect(x,y+8,20,5,theme.accent);rect(x-1,y+24,22,4,theme.accent);
                }else {
                    shapes.setColor(TRASH);shapes.triangle(x,y+14,x+12,y+28,x+24,y+14);shapes.triangle(x,y+14,x+24,y+14,x+12,y);
                    rect(x+5,y+12,14,3,theme.panel);
                }
              }
            } finally {
                shapes.flush(); // Submit all clipped geometry before restoring the previous scissors.
                ScissorStack.popScissors();
            }
            panel(100,10,600,30);
        }else if(game instanceof CertificationMinigame) {
            CertificationMinigame study=(CertificationMinigame)game;panel(40,284,720,192);panel(80,140,640,124);
            int[] visible=study.getVisiblePattern();
            for(int i=0;i<study.getPatternLength();i++) {
                float x=cardX(i,study.getPatternLength());rect(x-32,300,64,72,theme.border);
                boolean accepted=!study.isShowingPattern()&&i<study.getInputIndex();
                rect(x-29,303,58,66,accepted?theme.panel:theme.background);
                if(accepted)rect(x-29,303,58,6,theme.accent);
                if(i<visible.length){shapes.setColor(theme.border);arrow(visible[i],x,336);}
            }
            for(int i=0;i<5;i++){rect(320+i*34,157,24,9,theme.border);rect(322+i*34,159,20,5,i<study.getCompletedRounds()?theme.accent:theme.background);}
        }else {
            HackathonMinigame hack=(HackathonMinigame)game;CircuitBoard board=hack.getBoard();
            float cell=Math.min(68f,300f/board.getRows()),left=(800-board.getColumns()*cell)/2f,bottom=145;
            panel(left-10,bottom-10,board.getColumns()*cell+20,board.getRows()*cell+20);
            for(int r=0;r<board.getRows();r++)for(int c=0;c<board.getColumns();c++) {
                float x=left+c*cell,y=bottom+(board.getRows()-1-r)*cell;
                boolean cursor=r==hack.getCursorRow()&&c==hack.getCursorColumn();
                rect(x+1,y+1,cell-2,cell-2,cursor?theme.secondary:theme.panel);rect(x+4,y+4,cell-8,cell-8,theme.background);
                CircuitTile tile=board.getTile(r,c);boolean reached=hack.getLastCheck()!=null&&hack.getLastCheck().isReached(r,c);
                Color link=tile.getType()==CircuitTile.Type.START?theme.secondary:tile.getType()==CircuitTile.Type.SERVER?AMBER:reached?theme.accent:INACTIVE;
                float cx=x+cell/2,cy=y+cell/2;int ports=tile.getPorts();
                for(CircuitTile.Direction dir:CircuitTile.Direction.values())if((ports&dir.bit())!=0) {
                    if(dir==CircuitTile.Direction.UP)rect(cx-4,cy,8,cell/2,link);
                    else if(dir==CircuitTile.Direction.DOWN)rect(cx-4,y,8,cell/2,link);
                    else if(dir==CircuitTile.Direction.RIGHT)rect(cx,cy-4,cell/2,8,link);else rect(x,cy-4,cell/2,8,link);
                }
                if(tile.getType()==CircuitTile.Type.START) {
                    rect(cx-12,cy-7,24,17,link);rect(cx-3,cy-11,6,5,link);rect(cx-8,cy-14,16,3,link);rect(cx-9,cy-4,18,11,theme.panel);
                }else if(tile.getType()==CircuitTile.Type.SERVER) {
                    rect(cx-10,cy-14,20,28,link);for(int stripe=0;stripe<3;stripe++)rect(cx-7,cy-9+stripe*8,14,4,theme.panel);
                }else if(tile.isRotatable())rect(cx-4,cy-4,8,8,link);
            }
            panel(100,44,600,44);
        }
    }

    private void playText(VacationMinigame game) {
        String time=String.format(Locale.ROOT,"남은 시간: %.1f초",game.getTimeRemaining());
        if(game instanceof CafeMinigame) {
            CafeMinigame cafe=(CafeMinigame)game;stat("점수: "+cafe.getScore(),0);stat(time,1);stat("목표: "+CafeMinigame.TARGET+"점",2);
            center("주문 안내 / 커피 +10점 · 회색 쓰레기 -10점",490,.9f);center("왼쪽 / 오른쪽 방향키 : 쟁반 이동",25,1f);
        }else if(game instanceof CertificationMinigame) {
            CertificationMinigame study=(CertificationMinigame)game;
            stat("단계: "+study.getRoundNumber()+" / 5",0);stat(time,1);stat("실수: "+study.getMistakes()+" / 3",2);
            center(study.isShowingPattern()?"암기 중 / 아직 입력할 수 없습니다":"입력 차례 / 외운 순서를 입력하세요",452,1.2f);
            center(study.isShowingPattern()?String.format(Locale.ROOT,"공개 시간: %.1f초 남음",study.getShowTimeRemaining()):"맞춘 입력: "+study.getInputIndex()+" / "+study.getPatternLength(),409,1f);
            if(!study.isShowingPattern())for(int i=0;i<study.getPatternLength();i++) {
                font.setColor(i<study.getInputIndex()?theme.border:theme.secondary);centered(i<study.getInputIndex()?"정답":"?",cardX(i,study.getPatternLength()),336,1.2f,54);
            }
            font.setColor(theme.ink);center("위 / 아래 / 왼쪽 / 오른쪽 방향키를 한 번씩 누르세요.",232,.95f);
            center("틀리면 같은 단계를 다시 진행합니다. 실수 3회 시 종료됩니다.",204,.9f);center("학습 기록: "+study.getCompletedRounds()+" / 5단계 통과",181,.85f);
        }else {
            HackathonMinigame hack=(HackathonMinigame)game;stat("퍼즐: "+hack.getPuzzleNumber()+" / 3",0);stat(time,1);stat("실수: "+hack.getMistakes()+" / 3",2);
            center(hack.getLastCheck()==null?"연결 상태: 검사 대기 / 컴퓨터와 서버":"연결 상태: 미완성 / 도달 노드: "+hack.getLastCheck().getVisitedCount(),484,.95f);
            center("하늘색: 컴퓨터 · 노란색: 서버 · 민트색: 도달 노드",114,.9f);center("방향키: 이동 · Space: 회전 · Enter: 검사",66,1f);
        }
    }
    private void panel(float x,float y,float w,float h) {
        rect(x+2,y-2,w,h,theme.border);rect(x,y,w,h,theme.border);rect(x+2,y+2,w-4,h-4,theme.panel);rect(x+2,y+h-4,w-4,2,theme.secondary);
    }
    private void button(float x,float y,float w,float h){
        panel(x,y,w,h);
        // Keep text on the readable panel color; accent rails identify the action consistently.
        rect(x+6,y+6,5,h-12,theme.accent);rect(x+w-11,y+6,5,h-12,theme.accent);
    }
    private void rect(float x,float y,float w,float h,Color c){shapes.setColor(c);shapes.rect(x,y,w,h);}
    private void stat(String text,int column){centered(text,144+column*256,521,1.1f,212);}
    private float cardX(int i,int count){return 400+(i-(count-1)/2f)*84;}
    private void arrow(int key,float x,float y) {
        for(int row=0;row<3;row++) {
            float span=4+row*8;
            if(key==Input.Keys.UP)shapes.rect(x-span/2,y+16-row*6,span,6);
            else if(key==Input.Keys.DOWN)shapes.rect(x-span/2,y-22+row*6,span,6);
            else if(key==Input.Keys.LEFT)shapes.rect(x-22+row*6,y-span/2,6,span);else shapes.rect(x+16-row*6,y-span/2,6,span);
        }
        if(key==Input.Keys.UP)shapes.rect(x-4,y-16,8,22);else if(key==Input.Keys.DOWN)shapes.rect(x-4,y-6,8,22);
        else if(key==Input.Keys.LEFT)shapes.rect(x-6,y-4,22,8);else shapes.rect(x-16,y-4,22,8);
    }
    private void center(String value,float y,float scale){centered(value,400,y,scale);}
    private void centered(String value,float x,float y,float scale) {
        centered(value,x,y,scale,textMaxWidth);
    }
    private void centered(String value,float x,float y,float scale,float maxWidth) {
        font.getData().setScale(scale*.75f);layout.setText(font,value);
        if(layout.width>maxWidth){font.getData().setScale(font.getData().scaleX*maxWidth/layout.width);layout.setText(font,value);}
        font.draw(batch,layout,Math.round(x-layout.width/2),Math.round(y+layout.height/2));
    }
    @Override public void dispose(){font.dispose();}
}
