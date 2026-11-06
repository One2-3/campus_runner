package io.jbnu.test;

/** Graduation classification uses exact integer score comparisons, never the displayed GPA. */
public enum GraduationEnding {
    HIGHEST_HONORS(8, 0xB8860B, "최우수 졸업", "완벽한 캠퍼스 생활!", "8학기 동안의 노력이 최고의 결과로 이어졌습니다."),
    HONORS(7, 0x2389CF, "우수 졸업", "노력의 결실!", "훌륭한 성적으로 캠퍼스를 떠납니다."),
    GRADUATED(6, 0x26834A, "일반 졸업", "졸업 성공!", "수많은 도전을 이겨내고 마침내 졸업했습니다."),
    BARELY_GRADUATED(4, 0xB85B16, "간신히 졸업", "아슬아슬한 졸업!", "쉽지 않았지만 끝까지 포기하지 않았습니다."),
    DEFERRED(0, 0x9B4141, "졸업 유예", "아직 졸업할 수 없습니다.", "학점이 부족합니다. 다시 도전해 보세요!");

    public static final String PRECISION_NOTE = "등급은 반올림 전 GPA 기준입니다.";
    private final int minimumGpaTwice;
    private final int colorRgb;
    private final String title, message, detail;

    GraduationEnding(int minimumGpaTwice, int colorRgb, String title, String message, String detail) {
        this.minimumGpaTwice = minimumGpaTwice;
        this.colorRgb = colorRgb;
        this.title = title;
        this.message = message;
        this.detail = detail;
    }

    public static GraduationEnding fromScore(int finalScore) {
        if (finalScore < 0 || finalScore > FinalReport.MAX_FINAL_SCORE) {
            throw new IllegalArgumentException("Final score outside 0..465");
        }
        // GPA = score * 9 / 930. Compare against twice each GPA threshold without rounding.
        for (GraduationEnding ending : values()) {
            if (finalScore * 9 >= ending.minimumGpaTwice * FinalReport.MAX_FINAL_SCORE) return ending;
        }
        throw new AssertionError("Unreachable graduation classification");
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getDetail() { return detail; }
    public int getColorRgb() { return colorRgb; }
    public boolean isGraduated() { return this != DEFERRED; }
}
