package io.jbnu.test;

/** Verify count/format presentation without changing the existing report/ending calculations. */
public final class FinalPixelUiVerification {
    public static void main(String[] args) {
        int[] maxima={20,25,30,35,40,45,50,60};
        StageResult[] full=new StageResult[8], partial=new StageResult[8];
        int expectedItems=0, expectedDeaths=0;
        for(int i=0;i<8;i++) {
            full[i]=new StageResult(i+1,maxima[i],maxima[i]/GradeItem.DEFAULT_SCORE,0);
            int items=maxima[i]/GradeItem.DEFAULT_SCORE-1,deaths=i%5;
            partial[i]=new StageResult(i+1,items*GradeItem.DEFAULT_SCORE,items,deaths);
            expectedItems+=items; expectedDeaths+=deaths;
        }
        FinalReport best=new FinalReport(full,0), some=new FinalReport(partial,expectedDeaths);
        check(FinalReportRenderer.collectedItems(best)==61&&FinalReportRenderer.maximumItems(best)==61,"Actual full collected count and capacity");
        check(FinalReportRenderer.collectedItems(some)==expectedItems&&FinalReportRenderer.maximumItems(some)==61,"Partial collection counts are saved counts");
        check(FinalReportRenderer.gpaText(best).equals("GPA: 4.50 / 4.50"),"Maximum GPA format");
        check(best.getFinalScore()==465&&best.getTotalItemScore()==305&&best.getTotalSurvivalBonus()==160,"Existing maximum score unchanged");
        for(int score=0;score<=465;score++) {
            GraduationEnding expected=score<207?GraduationEnding.DEFERRED:score<310?GraduationEnding.BARELY_GRADUATED
                :score<362?GraduationEnding.GRADUATED:score<414?GraduationEnding.HONORS:GraduationEnding.HIGHEST_HONORS;
            check(GraduationEnding.fromScore(score)==expected,"All exact ending thresholds unchanged");
        }
        for(int i=0;i<8;i++)check(some.getSemester(i)==partial[i],"UI aggregation cannot replace results");
        check(some.getTotalDeathCount()==expectedDeaths,"Actual deaths preserved");
        System.out.println("Final pixel UI passed: saved count aggregation, 61 capacity, partial counts, GPA formatting, 465 maximum, exact ending thresholds and unchanged results.");
    }
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
}
