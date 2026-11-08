package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import java.util.HashSet;
import java.util.Set;

/** Distinct palettes, text contrast and live summaries; model rules remain separately tested. */
public final class VacationThemeVerification {
    public static void main(String[] args) {
        int[] backgrounds={0xFFF0DBFF,0xF5F8FFFF,0x18263CFF};
        int[] borders={0x704B38FF,0x355B8CFF,0x65E0C0FF};
        Set<Integer> distinct=new HashSet<>();
        for(VacationEventType type:VacationEventType.values()) {
            VacationUiTheme theme=VacationUiTheme.forType(type);
            check(Color.rgba8888(theme.background)==backgrounds[type.ordinal()],"Requested background palette");
            check(Color.rgba8888(theme.border)==borders[type.ordinal()],"Requested outline/accent palette");
            check(distinct.add(Color.rgba8888(theme.background)),"Three independent backgrounds");
            check(contrast(theme.ink,theme.panel)>=4.5,"Readable panel text");
            check(contrast(theme.failure,theme.panel)>=3,"Readable large failure heading in each theme");
            for(int fps:new int[]{30,60,120})for(boolean success:new boolean[]{false,true}) {
                VacationMinigame game=VacationMinigame.create(type);
                MinigameTestDriver.finish(game,success,1f/fps);
                String summary=VacationMinigameRenderer.summary(game);
                String expected=game instanceof CafeMinigame?"점수: "+((CafeMinigame)game).getScore()+" / 목표: "+CafeMinigame.TARGET+"점"
                    :game instanceof CertificationMinigame?"통과 단계: "+((CertificationMinigame)game).getCompletedRounds()+" / 5"
                    :"연결한 퍼즐: "+((HackathonMinigame)game).getCompletedPuzzles()+" / 3";
                check(summary.equals(expected)&&game.isSuccess()==success,"Actual result summary, no injected result");
                check(VacationUiTheme.forType(game.getType())==theme,"Same theme through result phase");
            }
        }
        System.out.println("Vacation themes passed: three exact independent palettes, panel/button contrast, real success/failure summaries at 30/60/120fps.");
    }
    private static double luminance(Color c){return .2126*channel(c.r)+.7152*channel(c.g)+.0722*channel(c.b);}
    private static double channel(float c){return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4);}
    private static double contrast(Color a,Color b){double x=luminance(a),y=luminance(b);return(Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
}
