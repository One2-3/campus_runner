package io.jbnu.test;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.Locale;

/** All minigame drawing uses the fixed UI camera, never the paused platformer camera. */
public final class VacationMinigameRenderer {
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final FitViewport viewport;
    private final OrthographicCamera camera;

    public VacationMinigameRenderer(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font,
                                    FitViewport viewport, OrthographicCamera camera) {
        this.batch = batch; this.shapes = shapes; this.font = font;
        this.viewport = viewport; this.camera = camera;
    }

    public void render(VacationMinigame game) {
        boolean dark = game.getType() == VacationEventType.HACKATHON;
        viewport.apply();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (dark) shapes.setColor(.035f,.06f,.12f,1);
        else if (game.getType() == VacationEventType.CAFE) shapes.setColor(.98f,.94f,.85f,1);
        else shapes.setColor(.87f,.94f,.96f,1);
        shapes.rect(0,0,800,600);
        if (game.getPhase() == VacationMinigame.Phase.PLAYING) drawScene(game);
        else {
            shapes.setColor(dark ? new Color(.08f,.13f,.22f,1) : Color.WHITE);
            shapes.rect(30,130,740,340);
        }
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(dark ? Color.WHITE : new Color(.15f,.21f,.23f,1));
        center(game instanceof HackathonMinigame ? "SERVER CONNECTION" : game.getType().getTitle(),570,1.6f);
        if (game.getPhase() == VacationMinigame.Phase.INTRO) intro(game);
        else if (game.getPhase() == VacationMinigame.Phase.RESULT) result(game);
        else playText(game);
        batch.end();
        font.getData().setScale(2f);
        font.setColor(Color.BLACK);
    }

    private void intro(VacationMinigame game) {
        center("WINTER VACATION - HOW TO PLAY",510,1.2f);
        String[] lines;
        switch (game.getType()) {
            case CAFE: lines = new String[]{"LEFT / RIGHT ARROWS: move the tray", "Catch coffee cups: +10 points", "Avoid gray diamond trash: -10 (minimum 0)",
                "Missed coffee does not change your score", "Reach 100 points by the end of 30 seconds", "Keep playing even after reaching the target"}; break;
            case CERTIFICATION: lines = new String[]{"Memorize the arrow pattern while it is visible", "Wait for the pattern to hide, then repeat it", "Use UP / DOWN / LEFT / RIGHT, one press at a time",
                "Five rounds: pattern lengths 2, 3, 3, 4, 5", "A mistake replays the same round", "Finish within 40 seconds; 3 mistakes means failure"}; break;
            default: lines = new String[]{"ARROWS: move the cursor inside the grid", "SPACE: rotate a circuit tile 90 degrees clockwise", "ENTER: check the connection from PC to server",
                "Match both ends of each link; fixed tiles do not rotate", "Connect all 3 boards within 45 seconds", "3 incorrect connection checks means failure"}; break;
        }
        for (int i = 0; i < lines.length; i++) center(lines[i],440-i*37,1.05f);
        center("SUCCESS: " + game.getType().getSuccessReward(),194,1.05f);
        center("FAILURE: " + game.getType().getFailurePenalty(),163,1.05f);
        center("PRESS ENTER TO BEGIN",85,1.5f);
    }

    private void result(VacationMinigame game) {
        center("VACATION RESULT",510,1.2f);
        font.setColor(game.isSuccess() ? new Color(.12f,.65f,.38f,1) : new Color(.85f,.28f,.22f,1));
        center(game.isSuccess() ? "SUCCESS" : "FAILURE",430,2.8f);
        font.setColor(game.getType() == VacationEventType.HACKATHON ? Color.WHITE : Color.DARK_GRAY);
        String summary;
        if (game instanceof CafeMinigame) summary = "SCORE: " + ((CafeMinigame)game).getScore() + " / TARGET: 100";
        else if (game instanceof CertificationMinigame) summary = "ROUNDS CLEARED: " + ((CertificationMinigame)game).getCompletedRounds() + " / 5";
        else summary = "PUZZLES CLEARED: " + ((HackathonMinigame)game).getCompletedPuzzles() + " / 3";
        center(summary,355,1.4f);
        center("NEXT YEAR: " + (game.isSuccess() ? game.getType().getSuccessReward() : game.getType().getFailurePenalty()),280,1.15f);
        center("APPLIES TO BOTH SEMESTERS OF YEAR " + game.getType().getTargetYear(),235,1.05f);
        center("MINIGAME POINTS DO NOT CHANGE SEMESTER SCORES",177,1f);
        center("PRESS ENTER TO START THE NEXT YEAR",85,1.3f);
    }

    private void playText(VacationMinigame game) {
        String time = String.format(Locale.ROOT,"TIME: %.1f",game.getTimeRemaining());
        if (game instanceof CafeMinigame) {
            CafeMinigame cafe = (CafeMinigame)game;
            text("SCORE: "+cafe.getScore(),45,525,1.3f); text(time,330,525,1.3f); text("TARGET: 100",605,525,1.3f);
            center("Coffee cups +10  /  Gray diamond trash -10",492,1.05f);
            font.setColor(.98f,.94f,.85f,1);
            center("LEFT / RIGHT ARROWS - MOVE TRAY",35,1.1f);
        } else if (game instanceof CertificationMinigame) {
            CertificationMinigame study = (CertificationMinigame)game;
            text("ROUND: "+study.getRoundNumber()+" / 5",45,525,1.2f); text(time,330,525,1.2f);
            text("MISTAKES: "+study.getMistakes()+" / 3",545,525,1.2f);
            center(study.isShowingPattern() ? "MEMORIZE - INPUT IS LOCKED" : "REPEAT THE HIDDEN PATTERN",457,1.3f);
            center(study.isShowingPattern() ? String.format(Locale.ROOT,"VISIBLE FOR %.1f MORE SECONDS",study.getShowTimeRemaining())
                : "CORRECT INPUTS: "+study.getInputIndex()+" / "+study.getPatternLength(),405,1.05f);
            if (!study.isShowingPattern()) {
                for (int i = 0; i < study.getPatternLength(); i++) text("?",cardX(i,study.getPatternLength())-5,350,1.5f);
            }
            center("UP / DOWN / LEFT / RIGHT - PRESS ONCE PER ARROW",220,1.05f);
            center("Mistakes replay this round. Three mistakes end the exam.",175,1f);
        } else {
            HackathonMinigame hack = (HackathonMinigame)game;
            text("PUZZLE: "+hack.getPuzzleNumber()+" / 3",45,525,1.2f); text(time,330,525,1.2f);
            text("MISTAKES: "+hack.getMistakes()+" / 3",545,525,1.2f);
            center(hack.getLastCheck() == null ? "CONNECT THE CYAN PC TO THE GOLD SERVER"
                : "NOT CONNECTED - MATCH BOTH ENDS OF EACH LINK",477,1.05f);
            center("CYAN: PC    GOLD: SERVER    EMPTY: NO ROTATION",114,1f);
            center("ARROWS: MOVE    SPACE: ROTATE    ENTER: CHECK",70,1.1f);
        }
    }

    private void drawScene(VacationMinigame game) {
        if (game instanceof CafeMinigame) {
            CafeMinigame cafe = (CafeMinigame)game;
            shapes.setColor(.37f,.22f,.14f,1); shapes.rect(0,0,800,65);
            shapes.setColor(.65f,.38f,.18f,1); shapes.rect(cafe.getTrayX(),CafeMinigame.TRAY_Y,CafeMinigame.TRAY_WIDTH,CafeMinigame.TRAY_HEIGHT);
            for (CafeMinigame.Drop drop : cafe.getDrops()) {
                float x = drop.getX(), y = drop.getY();
                if (drop.isCoffee()) {
                    shapes.setColor(.55f,.24f,.08f,1); shapes.rect(x,y,20,24); shapes.rect(x+20,y+8,4,12);
                    shapes.setColor(.98f,.75f,.4f,1); shapes.rect(x,y+8,20,5); shapes.rect(x-1,y+24,22,4);
                } else {
                    shapes.setColor(.35f,.39f,.42f,1);
                    shapes.triangle(x,y+14,x+12,y+28,x+24,y+14);
                    shapes.triangle(x,y+14,x+24,y+14,x+12,y);
                    shapes.setColor(.8f,.84f,.85f,1); shapes.rect(x+5,y+12,14,3);
                }
            }
        } else if (game instanceof CertificationMinigame) {
            CertificationMinigame study = (CertificationMinigame)game;
            int[] visible = study.getVisiblePattern();
            for (int i = 0; i < study.getPatternLength(); i++) {
                float x = cardX(i,study.getPatternLength());
                shapes.setColor(.64f,.8f,.83f,1); shapes.rect(x-32,300,64,72);
                if (i < visible.length) { shapes.setColor(.08f,.36f,.3f,1); arrow(visible[i],x,336); }
            }
            shapes.setColor(.35f,.48f,.52f,1); shapes.rect(60,135,680,8);
        } else {
            HackathonMinigame hack = (HackathonMinigame)game;
            CircuitBoard board = hack.getBoard();
            float cell = Math.min(68f,300f/board.getRows()), left = (800-board.getColumns()*cell)/2f, bottom = 145f;
            for (int r = 0; r < board.getRows(); r++) for (int c = 0; c < board.getColumns(); c++) {
                float x = left+c*cell, y = bottom+(board.getRows()-1-r)*cell;
                shapes.setColor(r == hack.getCursorRow() && c == hack.getCursorColumn()
                    ? new Color(.65f,.95f,1f,1) : new Color(.12f,.2f,.3f,1));
                shapes.rect(x+1,y+1,cell-2,cell-2);
                shapes.setColor(.055f,.09f,.15f,1); shapes.rect(x+4,y+4,cell-8,cell-8);
                CircuitTile tile = board.getTile(r,c);
                boolean reached = hack.getLastCheck() != null && hack.getLastCheck().isReached(r,c);
                if (tile.getType() == CircuitTile.Type.START) shapes.setColor(.4f,.85f,1f,1);
                else if (tile.getType() == CircuitTile.Type.SERVER) shapes.setColor(.98f,.79f,.3f,1);
                else if (reached) shapes.setColor(.4f,1f,.6f,1);
                else shapes.setColor(.1f,.65f,.59f,1);
                float cx = x+cell/2, cy = y+cell/2;
                int ports = tile.getPorts();
                for (CircuitTile.Direction direction : CircuitTile.Direction.values()) {
                    if ((ports & direction.bit()) == 0) continue;
                    if (direction == CircuitTile.Direction.UP) shapes.rect(cx-4,cy,8,cell/2);
                    else if (direction == CircuitTile.Direction.DOWN) shapes.rect(cx-4,y,8,cell/2);
                    else if (direction == CircuitTile.Direction.RIGHT) shapes.rect(cx,cy-4,cell/2,8);
                    else shapes.rect(x,cy-4,cell/2,8);
                }
                if (tile.getType() == CircuitTile.Type.START) {
                    shapes.rect(cx-12,cy-7,24,17); shapes.rect(cx-3,cy-11,6,5); shapes.rect(cx-8,cy-14,16,3);
                    shapes.setColor(.05f,.2f,.32f,1); shapes.rect(cx-9,cy-4,18,11);
                } else if (tile.getType() == CircuitTile.Type.SERVER) {
                    shapes.rect(cx-10,cy-14,20,28);
                    shapes.setColor(.25f,.2f,.1f,1);
                    for (int stripe = 0; stripe < 3; stripe++) shapes.rect(cx-7,cy-9+stripe*8,14,4);
                } else if (tile.isRotatable()) shapes.circle(cx,cy,5,12);
            }
        }
    }

    private float cardX(int i, int count) { return 400+(i-(count-1)/2f)*84; }
    private void arrow(int key, float x, float y) {
        if (key == Input.Keys.UP) { shapes.rect(x-4,y-15,8,23); shapes.triangle(x-14,y+6,x+14,y+6,x,y+21); }
        else if (key == Input.Keys.DOWN) { shapes.rect(x-4,y-8,8,23); shapes.triangle(x-14,y-6,x,y-21,x+14,y-6); }
        else if (key == Input.Keys.LEFT) { shapes.rect(x-8,y-4,23,8); shapes.triangle(x-6,y-14,x-21,y,x-6,y+14); }
        else { shapes.rect(x-15,y-4,23,8); shapes.triangle(x+6,y-14,x+6,y+14,x+21,y); }
    }
    private void text(String value, float x, float y, float scale) { font.getData().setScale(scale); font.draw(batch,value,x,y); }
    private void center(String value, float y, float scale) {
        font.getData().setScale(scale); font.draw(batch,value,0,y,800,Align.center,false);
    }
}
