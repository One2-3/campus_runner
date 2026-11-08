package io.jbnu.test;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.Locale;

/** Displays immutable confirmed data; aggregation here is limited to item counts for the UI. */
public final class FinalReportRenderer {
    private final PixelResultCanvas ui;
    FinalReportRenderer(PixelResultCanvas ui) { this.ui = ui; }

    static int collectedItems(FinalReport report) {
        int count = 0;
        for (int i=0;i<report.getSemesterCount();i++) count += report.getSemester(i).getCollectedItemCount();
        return count;
    }
    static int maximumItems(FinalReport report) {
        int count = 0;
        // Existing item maxima and the existing five-point item value; no new scoring rule.
        for (int i=0;i<report.getSemesterCount();i++) count += report.getItemMaximum(i) / GradeItem.DEFAULT_SCORE;
        return count;
    }
    static String gpaText(FinalReport report) { return String.format(Locale.ROOT,"GPA: %.2f / 4.50",report.getGpa()); }

    public void render(SpriteBatch batch,FitViewport viewport,FinalReport report) {
        float w = Math.min(704,viewport.getWorldWidth()-48), h = Math.min(536,viewport.getWorldHeight()-40);
        float x = (viewport.getWorldWidth()-w)/2, y = (viewport.getWorldHeight()-h)/2, top = y+h, center = x+w/2;
        try {
            ui.begin(batch,viewport);
            ui.panel(batch,x,y,w,h,PixelResultCanvas.YELLOW);
            ui.center(batch,"FINAL REPORT",center,top-25,2f);
            ui.center(batch,"CAMPUS RUNNER / 8 SEMESTERS",center,top-53,.9f);
            ui.rect(batch,x+18,top-95,w-36,24,PixelResultCanvas.YELLOW);
            left(batch,"SEMESTER",x+28,top-84,.8f);
            right(batch,"ITEM SCORE",x+300,top-84,.8f);
            right(batch,"SURVIVAL BONUS",x+476,top-84,.8f);
            right(batch,"FINAL",x+w-28,top-84,.8f);
            int maxBonus = FinalReport.MAX_SURVIVAL_BONUS / report.getSemesterCount();
            for(int i=0;i<report.getSemesterCount();i++) {
                StageResult result = report.getSemester(i);
                float row = top-109-i*24;
                if(i%2==0)ui.rect(batch,x+18,row-11,w-36,23,PixelResultCanvas.PALE);
                int number = result.getStageNumber();
                left(batch,((number+1)/2)+"-"+((number-1)%2+1),x+48,row,1f);
                right(batch,result.getItemScore()+" / "+report.getItemMaximum(i),x+300,row,1f);
                right(batch,result.getSurvivalBonus()+" / "+maxBonus,x+476,row,1f);
                right(batch,result.getFinalScore()+" / "+(report.getItemMaximum(i)+maxBonus),x+w-28,row,1f);
            }
            ui.rect(batch,x+18,top-292,w-36,2,PixelResultCanvas.BORDER);
            left(batch,"TOTAL ITEM SCORE: "+report.getTotalItemScore()+" / "+FinalReport.MAX_ITEM_SCORE,x+28,top-314,.9f);
            left(batch,"TOTAL SURVIVAL BONUS: "+report.getTotalSurvivalBonus()+" / "+FinalReport.MAX_SURVIVAL_BONUS,x+28,top-337,.9f);
            left(batch,String.format(Locale.ROOT,"SCORE RATE: %.2f%%",report.getScoreRate()),x+28,top-360,.9f);
            right(batch,"CREDITS: "+collectedItems(report)+" / "+maximumItems(report),x+w-28,top-314,.9f);
            right(batch,"TOTAL DEATHS: "+report.getTotalDeathCount(),x+w-28,top-337,1f);
            right(batch,gpaText(report),x+w-28,top-360,1f);
            ui.rect(batch,x+18,top-410,w-36,36,PixelResultCanvas.YELLOW);
            left(batch,"FINAL SCORE",x+30,top-392,1.3f);
            right(batch,report.getFinalScore()+" / "+FinalReport.MAX_FINAL_SCORE,x+w-30,top-392,1.3f);
            ui.text(batch,ui.korean,report.getEnding().getTitle(),center,top-435,1f,PixelResultCanvas.INK,0);
            ui.text(batch,ui.korean,GraduationEnding.PRECISION_NOTE,center,top-460,.55f,PixelResultCanvas.INK,0);
            ui.rect(batch,x+100,top-511,w-200,28,PixelResultCanvas.PALE);
            ui.center(batch,"SPACE : GRADUATION RESULT",center,top-497,1f);
        } finally { ui.end(batch,viewport); }
    }
    private void left(SpriteBatch b,String text,float x,float y,float scale) { ui.text(b,ui.english,text,x,y,scale,PixelResultCanvas.INK,-1); }
    private void right(SpriteBatch b,String text,float x,float y,float scale) { ui.text(b,ui.english,text,x,y,scale,PixelResultCanvas.INK,1); }
}
