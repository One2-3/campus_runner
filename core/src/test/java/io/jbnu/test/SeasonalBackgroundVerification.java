package io.jbnu.test;

import com.badlogic.gdx.math.Rectangle;

public final class SeasonalBackgroundVerification {
    public static void main(String[] args) {
        String[] expected = {"year1_spring_day", "year1_spring_night", "year2_summer_day", "year2_summer_night",
            "year3_fall_day", "year3_fall_night", "year4_winter_day", "year4_winter_night"};
        for (int i=0;i<8;i++) check(SeasonalBackgroundRenderer.pathForStage(i+1).equals(
            "pixel/backgrounds/"+expected[i]+".png"),"Exact semester mapping");
        for(int i=0;i<8;i++)for(boolean left:new boolean[]{true,false})check(
            SeasonalBackgroundRenderer.extensionPathForStage(i+1,left).equals("pixel/backgrounds/"+expected[i]+(left?"_left.png":"_right.png")),
            "Gate-free extension mapping");
        for (int stage:new int[]{0,9}) {
            try { SeasonalBackgroundRenderer.pathForStage(stage);throw new AssertionError("Invalid stage accepted"); }
            catch (IllegalArgumentException expectedError) { }
        }
        Rectangle gate=new Rectangle(),view=new Rectangle(),movedGate=new Rectangle(),movedView=new Rectangle();
        for(int stage=1;stage<=8;stage++){
            float world=StageFactory.createStage(stage).getWorldWidth();
            for(int[] screen:new int[][]{{800,600},{1280,720},{1920,1080}}){
                SeasonalBackgroundRenderer.layout(2048,768,world,world/2,screen[0],screen[1],gate,view);
                check(gate.width==1600&&gate.height==600&&gate.y==0,"Uniform world size, no white vertical margins");
                check(gate.x+gate.width/2==world/2,"Exactly one central landmark placement");
                check(Math.abs(view.width/view.height-(float)screen[0]/screen[1])<.00001f,"Full-window background aspect");
                SeasonalBackgroundRenderer.layout(2048,768,world,world/2+100,screen[0],screen[1],movedGate,movedView);
                check(gate.equals(movedGate)&&Math.abs(movedView.x-view.x-30)<.001f,"Camera 100 -> one shared layer moves 30");
                for(int fps:new int[]{30,60,120}){
                    float previous=Float.NaN;
                    for(float camera=400;camera<=world-400;camera+=260f/fps){
                        SeasonalBackgroundRenderer.layout(2048,768,world,camera,screen[0],screen[1],movedGate,movedView);
                        check(gate.equals(movedGate)&&movedView.height==600&&movedView.y==0,"Gate stays fixed in shared world at all fps");
                        if(!Float.isNaN(previous))check(Math.abs((movedView.x-previous)-260f/fps*.3f)<.001f,"Continuous parallax");
                        previous=movedView.x;
                    }
                }
            }
        }
        System.out.println("Seasonal backgrounds passed: eight mappings, gate-free extension paths, one central 1600x600 landmark, .3 parallax, full-window coverage at 800/1280/1920 and 30/60/120fps.");
    }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
