package io.jbnu.test;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Bundled font coverage for every UI literal and existing-effect Korean formatting. */
public final class VacationKoreanVerification {
    public static void main(String[] args)throws Exception {
        Path root=Files.isDirectory(Paths.get("assets"))?Paths.get("."):Paths.get("..");
        String fnt=read(root.resolve("assets/fonts/vacation-ko.fnt"));
        Set<Integer> glyphs=new HashSet<>();Map<Integer,Integer> advance=new HashMap<>();
        Matcher ids=Pattern.compile("char id=(\\d+).*?xadvance=(\\d+)").matcher(fnt);
        while(ids.find()){int id=Integer.parseInt(ids.group(1));glyphs.add(id);advance.put(id,Integer.parseInt(ids.group(2)));}
        for(String file:new String[]{"VacationMinigameRenderer.java","VacationUiTheme.java"}) {
            String source=read(root.resolve("core/src/main/java/io/jbnu/test/"+file));
            Matcher literals=Pattern.compile("\"([^\"\\r\\n]*)\"").matcher(source);
            while(literals.find())for(char c:literals.group(1).toCharArray())check(glyphs.contains((int)c),"Font includes UI character: "+c);
        }
        check(advance.get((int)' ')>0,"Korean word spacing is visible");
        check(Files.size(root.resolve("assets/fonts/vacation-ko.png"))>0,"Atlas bundled");
        check(read(root.resolve("assets/fonts/vacation-ko-OFL.txt")).contains("SIL OPEN FONT LICENSE"),"Font license bundled");
        check(fnt.contains("Campus Vacation Korean")&&!fnt.contains("face=\"Nanum"),"Derivative has distinct name");
        check(VacationMinigameRenderer.effectText(VacationEventType.CAFE,true).equals("시작 목숨 +1 (6개)"),"Actual extra starting life");
        check(VacationMinigameRenderer.effectText(VacationEventType.CAFE,false).equals("이동 속도 -5%"),"Cafe penalty");
        check(VacationMinigameRenderer.effectText(VacationEventType.CERTIFICATION,true).equals("점프력 +10%"),"Study reward");
        check(VacationMinigameRenderer.effectText(VacationEventType.CERTIFICATION,false).equals("점프력 -5%"),"Study penalty");
        check(VacationMinigameRenderer.effectText(VacationEventType.HACKATHON,true).equals("이동 속도 +10%"),"Server reward");
        check(VacationMinigameRenderer.effectText(VacationEventType.HACKATHON,false).equals("이동 속도 -5%"),"Server penalty");
        System.out.println("Vacation Korean passed: every UI literal glyph, nonzero spaces, OFL notice, distinct font name, exact existing reward/penalty copy.");
    }
    private static String read(Path p)throws Exception{return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
}
