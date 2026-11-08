package io.jbnu.test;

import com.badlogic.gdx.graphics.Color;

/** Presentation palette only; each activity keeps its own identity in every phase. */
enum VacationUiTheme {
    CAFE(0xFFF0DBFF,0xFFF8EDFF,0x704B38FF,0x704B38FF,0xC98A53FF,0xE9B86CFF,
        "카페 아르바이트","아르바이트 정산표",0xB8564DFF),
    STUDY(0xF5F8FFFF,0xFFFFFFFF,0x355B8CFF,0x355B8CFF,0x82A9D9FF,0x76B9ADFF,
        "자격증 공부","시험 결과표",0xB85870FF),
    SERVER(0x18263CFF,0x263E54FF,0x65E0C0FF,0xE6F4F5FF,0xA4DCE8FF,0x65E0C0FF,
        "서버 연결","서버 연결 보고서",0xF19EA5FF);

    final Color background,panel,border,ink,secondary,accent,failure;
    final String title,resultTitle;
    VacationUiTheme(int bg,int panel,int border,int ink,int secondary,int accent,String title,String resultTitle,int failure) {
        this.background=new Color(bg);this.panel=new Color(panel);this.border=new Color(border);
        this.ink=new Color(ink);this.secondary=new Color(secondary);this.accent=new Color(accent);
        this.failure=new Color(failure);
        this.title=title;this.resultTitle=resultTitle;
    }
    static VacationUiTheme forType(VacationEventType type) {
        switch(type){case CAFE:return CAFE;case CERTIFICATION:return STUDY;default:return SERVER;}
    }
}
