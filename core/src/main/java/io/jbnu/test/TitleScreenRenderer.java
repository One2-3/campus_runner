package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Static campus front page. Borrowed art/resources never create or advance a gameplay world. */
public final class TitleScreenRenderer implements Disposable {
    static final String SUBTITLE="학점을 모아 졸업까지 달려라!";
    static final String[] MENU={"게임 시작","게임 방법","게임 종료"};
    static final String[] HELP={
        "이동: 왼쪽 / 오른쪽 방향키 · 점프: Space",
        "A+ 학점 별을 모아 학기 점수를 올리세요.",
        "과제 몬스터와 가시를 피하며 깃발에 도착하세요.",
        "총 8학기를 통과하면 최종 성적표를 확인합니다.",
        "방학에는 카페·자격증·서버 연결 미니게임이 열립니다.",
        "Esc: 일시정지 · 재개 (스테이지와 방학 미니게임)",
        "획득 점수와 생존 보너스가 최종 GPA에 반영됩니다.",
        "최종 GPA에 따라 다섯 가지 졸업 엔딩이 결정됩니다."
    };
    private static final Color NAVY=new Color(0x102C42FF), PANEL=new Color(0x193D52FF);
    private static final Color TEAL=new Color(0x74D3CBFF), GOLD=new Color(0xE4BD66FF);
    private static final Color TEXT=new Color(0xECF7F2FF), MUTED=new Color(0xBDDBD9FF);
    private final SeasonalBackgroundRenderer background;
    private final Texture pixel,star;
    private final BitmapFont english;
    private final BitmapFont korean=new BitmapFont(Gdx.files.internal("fonts/title-ko.fnt"));
    private final GlyphLayout layout=new GlyphLayout();
    private final Matrix4 projection=new Matrix4(),transform=new Matrix4(),screen=new Matrix4(),identity=new Matrix4();
    private final Color fontColor=new Color();

    public TitleScreenRenderer(SeasonalBackgroundRenderer background,Texture star,BitmapFont english) {
        this.background=background;this.pixel=background.getSolidTexture();this.star=star;this.english=english;
        for(TextureRegion region:korean.getRegions())region.getTexture().setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        korean.setUseIntegerPositions(true);
    }

    public void render(SpriteBatch batch,FitViewport viewport,int selection,boolean help) {
        projection.set(batch.getProjectionMatrix());transform.set(batch.getTransformMatrix());
        ShaderProgram shader=batch.getShader();float tint=batch.getPackedColor();
        boolean blend=batch.isBlendingEnabled();
        int src=batch.getBlendSrcFunc(),dst=batch.getBlendDstFunc(),srcA=batch.getBlendSrcFuncAlpha(),dstA=batch.getBlendDstFuncAlpha();
        float fontX=english.getData().scaleX,fontY=english.getData().scaleY;fontColor.set(english.getColor());
        try {
            batch.setTransformMatrix(identity);batch.setColor(Color.WHITE);
            // A fixed virtual background center, independent of the gameplay camera and stage data.
            background.render(batch,1,1600,800,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            batch.setShader(null);batch.enableBlending();batch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
            Gdx.gl.glViewport(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            batch.setProjectionMatrix(screen.setToOrtho2D(0,0,1,1));batch.begin();
            batch.setColor(NAVY.r,NAVY.g,NAVY.b,.56f);batch.draw(pixel,0,0,1,1);batch.end();
            viewport.apply();batch.setProjectionMatrix(viewport.getCamera().combined);batch.begin();
            float center=viewport.getWorldWidth()/2,top=viewport.getWorldHeight();
            if(help) {
                text(batch,korean,"CAMPUS RUNNER",center,top-48,1.35f,GOLD,560);
                float x=center-336,y=(top-414)/2;
                panel(batch,x,y,672,414);
                text(batch,korean,"게임 방법",center,y+382,1.15f,TEXT,600);
                rect(batch,x+24,y+358,624,2,TEAL);
                for(int i=0;i<HELP.length;i++)text(batch,korean,HELP[i],center,y+330-i*38,.78f,TEXT,600);
                rect(batch,center-180,y+12,360,32,NAVY);
                text(batch,korean,"Esc : 메인 메뉴로 돌아가기",center,y+28,.72f,TEAL,332);
            }else {
                cap(batch,center,top-57);
                text(batch,korean,"CAMPUS RUNNER",center+3,top-118,1.85f,NAVY,564);
                text(batch,korean,"CAMPUS RUNNER",center,top-115,1.85f,GOLD,564);
                batch.setColor(Color.WHITE);batch.draw(star,center-310,top-137,44,44);batch.draw(star,center+266,top-137,44,44);
                text(batch,korean,SUBTITLE,center,top-162,.9f,TEXT,640);
                panel(batch,center-180,top-446,360,218);
                for(int i=0;i<MENU.length;i++) {
                    float row=top-274-i*58;
                    if(i==selection) {
                        rect(batch,center-156,row-22,312,44,GOLD);
                        rect(batch,center-142,row-6,4,12,NAVY);rect(batch,center-138,row-4,4,8,NAVY);rect(batch,center-134,row-2,4,4,NAVY);
                    }
                    text(batch,korean,MENU[i],center,row,1.05f,i==selection?NAVY:TEXT,250);
                }
                text(batch,korean,"위 / 아래 : 선택 · Enter : 확인",center,top-490,.7f,TEXT,640);
                text(batch,korean,"8학기의 도전, 나만의 캠퍼스 이야기",center,top-524,.65f,MUTED,640);
            }
        }finally {
            if(batch.isDrawing())batch.end();viewport.apply();
            english.getData().setScale(fontX,fontY);english.setColor(fontColor);
            batch.setProjectionMatrix(projection);batch.setTransformMatrix(transform);batch.setShader(shader);batch.setPackedColor(tint);
            batch.setBlendFunctionSeparate(src,dst,srcA,dstA);if(blend)batch.enableBlending();else batch.disableBlending();
        }
    }
    private void panel(SpriteBatch b,float x,float y,float w,float h) {
        rect(b,x+4,y-4,w,h,NAVY);rect(b,x,y,w,h,TEAL);rect(b,x+3,y+3,w-6,h-6,PANEL);
    }
    private void cap(SpriteBatch b,float x,float y) {
        rect(b,x-20,y-10,40,14,NAVY);
        for(int i=0;i<5;i++){float half=10+8*(i<3?i:4-i);rect(b,x-half,y+18-i*5,half*2,5,GOLD);}
        rect(b,x+27,y-12,3,29,TEAL);rect(b,x+24,y-17,9,7,GOLD);
    }
    private void rect(SpriteBatch b,float x,float y,float w,float h,Color color){b.setColor(color);b.draw(pixel,x,y,w,h);}
    private void text(SpriteBatch b,BitmapFont font,String value,float x,float y,float scale,Color color,float maxWidth) {
        font.getData().setScale(scale);font.setColor(color);layout.setText(font,value);
        if(layout.width>maxWidth){font.getData().setScale(scale*maxWidth/layout.width);layout.setText(font,value);}
        b.setColor(Color.WHITE);font.draw(b,layout,Math.round(x-layout.width/2),Math.round(y+layout.height/2));
    }
    @Override public void dispose(){korean.dispose();} // All other resources are owned by Main.
}
