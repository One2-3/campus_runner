package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** The existing five endings and Korean messages; only the decoration changes. */
public final class EndingRenderer {
    private final PixelResultCanvas ui;
    private final Color accent = new Color();
    EndingRenderer(PixelResultCanvas ui) { this.ui = ui; }

    public void render(SpriteBatch batch,FitViewport viewport,FinalReport report) {
        GraduationEnding ending = report.getEnding();
        Color.rgba8888ToColor(accent,(ending.getColorRgb()<<8)|255);
        float w = Math.min(640,viewport.getWorldWidth()-48), h = Math.min(480,viewport.getWorldHeight()-64);
        float x = (viewport.getWorldWidth()-w)/2, y = (viewport.getWorldHeight()-h)/2, top = y+h, center = x+w/2;
        try {
            ui.begin(batch,viewport);
            ui.panel(batch,x,y,w,h,accent);
            ui.center(batch,"CAMPUS RUNNER",center,top-25,1f);
            ui.center(batch,"GRADUATION RESULT",center,top-58,1.7f);
            cap(batch,center,top-112,ending.isGraduated());
            ui.text(batch,ui.korean,ending.getTitle(),center,top-160,1.35f,accent,0);
            ui.text(batch,ui.korean,ending.getMessage(),center,top-208,.85f,PixelResultCanvas.INK,0);
            ui.text(batch,ui.korean,ending.getDetail(),center,top-240,.75f,PixelResultCanvas.INK,0);
            ui.rect(batch,x+48,top-352,w-96,88,PixelResultCanvas.PALE);
            ui.center(batch,FinalReportRenderer.gpaText(report),center,top-282,1.25f);
            ui.center(batch,"FINAL SCORE: "+report.getFinalScore()+" / "+FinalReport.MAX_FINAL_SCORE,center,top-310,1.1f);
            ui.center(batch,"TOTAL DEATHS: "+report.getTotalDeathCount(),center,top-336,1.1f);
            ui.text(batch,ui.korean,GraduationEnding.PRECISION_NOTE,center,top-386,.6f,PixelResultCanvas.INK,0);
            ui.rect(batch,x+48,y+22,w-96,30,PixelResultCanvas.YELLOW);
            ui.center(batch,"ENTER : RESTART FROM YEAR 1, SEMESTER 1",center,y+37,.95f);
        } finally { ui.end(batch,viewport); }
    }

    private void cap(SpriteBatch b,float x,float y,boolean graduated) {
        // Stepped mortarboard; deferred graduation uses a closed notebook instead of celebration.
        if(!graduated) {
            ui.rect(b,x-24,y-14,48,32,PixelResultCanvas.BORDER);
            ui.rect(b,x-20,y-10,40,24,accent);
            ui.rect(b,x-12,y+2,24,3,PixelResultCanvas.CREAM);
            ui.rect(b,x-12,y-5,18,3,PixelResultCanvas.CREAM);
            return;
        }
        ui.rect(b,x-22,y-16,44,16,PixelResultCanvas.BORDER);
        for(int row=0;row<5;row++) {
            int half=12+8*(row<3?row:4-row);
            ui.rect(b,x-half,y+12-row*5,half*2,5,accent);
        }
        ui.rect(b,x+28,y-18,3,30,PixelResultCanvas.BORDER);
        ui.rect(b,x+25,y-22,9,7,PixelResultCanvas.YELLOW);
        ui.rect(b,x-55,y+4,5,5,accent); ui.rect(b,x+50,y-6,5,5,accent);
    }
}
