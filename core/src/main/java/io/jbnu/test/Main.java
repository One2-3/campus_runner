package io.jbnu.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.Arrays;
import java.util.Locale;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    /** Base starting lives; a semester may receive an extra vacation life. */
    public static final int MAX_LIVES = 5;

    private SpriteBatch batch;
    private Texture flagTexture;
    private FixedPlatformRenderer fixedPlatformRenderer;
    private Texture gradeItemTexture;
    private PlayerSprites playerSprites;
    private final HitFeedback hitFeedback = new HitFeedback();
    private final Matrix4 worldRenderProjection = new Matrix4();
    private boolean waitForHitKeyRelease;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private OrthographicCamera uiCamera;
    private FitViewport uiViewport;
    private BitmapFont font;
    private BitmapFont endingFont;
    private GlyphLayout textLayout;
    private ShapeRenderer itemRenderer;

    private Player player;
    private StageDefinition stage;
    private Rectangle flag;
    private Array<Rectangle> blocks;
    private Array<GradeItem> gradeItems;
    private Array<MovingPlatform> movingPlatforms;
    private Array<TimedTrap> timedTraps;
    private Array<PatrollingEnemy> enemies;
    private int collectedItemCount;
    private int stageScore;
    private int lives = MAX_LIVES;
    private int semesterMaxLives = MAX_LIVES;
    private final VacationManager vacationManager = new VacationManager();
    private VacationMinigame vacationGame;
    private VacationMinigameRenderer vacationRenderer;
    private int semesterDeathCount;
    private int totalDeathCount;
    private final int[] semesterDeaths = new int[StageFactory.STAGE_COUNT];
    private int currentStageNumber = 1;
    private final StageResult[] completedResults = new StageResult[StageFactory.STAGE_COUNT];
    private GameState gameState = GameState.TITLE_SCREEN;


    @Override
    public void create() {
        batch = new SpriteBatch();
        itemRenderer = new ShapeRenderer();
        playerSprites = new PlayerSprites();
        fixedPlatformRenderer = new FixedPlatformRenderer();
        flagTexture = new Texture(Gdx.files.internal("flag.png"));
        gradeItemTexture = new Texture(Gdx.files.internal("pixel/items/grade_a_plus_128.png"));
        gradeItemTexture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        viewport = new FitViewport(800, 600, camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        // UI는 월드 카메라와 분리하여 항상 화면 중앙에 표시합니다.
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(800, 600, uiCamera);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        font = new BitmapFont();
        font.setColor(Color.BLACK);
        font.getData().setScale(2f);
        endingFont = new BitmapFont(Gdx.files.internal("fonts/ending-ko.fnt"));
        textLayout = new GlyphLayout();
        vacationRenderer = new VacationMinigameRenderer(batch, itemRenderer, font, uiViewport, uiCamera);

    }


    @Override
    public void render() {

        // 큰 프레임 시간으로 인해 한 번에 너무 멀리 이동하는 것을 제한합니다.
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        update(dt);

        if (gameState == GameState.TITLE_SCREEN) {
            renderTitleScreen();
            return;
        }



        ScreenUtils.clear(1f,1f,1f,1f);

        if (gameState == GameState.GAME_COMPLETE) {
            renderFinalReport(getFinalReport());
            return;
        }
        if (gameState == GameState.GRADUATION_ENDING) {
            renderGraduationEnding(getFinalReport());
            return;
        }
        if (gameState == GameState.VACATION_EVENT) {
            renderVacationEvent();
            return;
        }

        viewport.apply();
        Matrix4 worldProjection = getWorldRenderProjection();
        boolean showHurt = gameState == GameState.DYING || gameState == GameState.GAME_OVER;
        batch.setProjectionMatrix(worldProjection);
        batch.begin();
        for (Rectangle block : blocks) {
            fixedPlatformRenderer.render(batch, block);
        }
        batch.draw(flagTexture, flag.x, flag.y, flag.width, flag.height);
        if (!showHurt) playerSprites.render(batch, player, false);
        batch.end();

        // SpriteBatch와 ShapeRenderer는 동시에 begin하지 않습니다.
        itemRenderer.setProjectionMatrix(worldProjection);
        itemRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (MovingPlatform platform : movingPlatforms) {
            platform.render(itemRenderer);
        }
        for (TimedTrap trap : timedTraps) {
            trap.render(itemRenderer);
        }
        for (PatrollingEnemy enemy : enemies) {
            enemy.render(itemRenderer);
        }
        itemRenderer.end();

        batch.begin();
        for (GradeItem item : gradeItems) {
            item.render(batch, gradeItemTexture);
        }
        if (showHurt) {
            playerSprites.render(batch, player, true);
        }
        batch.end();

        renderHud();
        renderStateUi();

    }

    private void renderTitleScreen() {
        ScreenUtils.clear(0.035f, 0.06f, 0.13f, 1f);
        uiViewport.apply();
        itemRenderer.setProjectionMatrix(uiCamera.combined);
        itemRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // A simple mortarboard and tassel, drawn entirely in UI coordinates.
        itemRenderer.setColor(0.98f, 0.81f, 0.36f, 1f);
        itemRenderer.triangle(344f, 530f, 400f, 550f, 456f, 530f);
        itemRenderer.triangle(344f, 530f, 456f, 530f, 400f, 510f);
        itemRenderer.rect(378f, 492f, 44f, 17f);
        itemRenderer.rect(453f, 495f, 3f, 35f);
        itemRenderer.circle(454.5f, 493f, 4f, 16);
        itemRenderer.setColor(0.085f, 0.13f, 0.22f, 1f);
        itemRenderer.rect(150f, 160f, 500f, 136f);
        // Campus silhouettes leave the central text area clear.
        itemRenderer.rect(28f, 0f, 92f, 82f);
        itemRenderer.triangle(18f, 82f, 74f, 112f, 130f, 82f);
        itemRenderer.rect(680f, 0f, 92f, 82f);
        itemRenderer.triangle(670f, 82f, 726f, 112f, 782f, 82f);
        itemRenderer.setColor(0.98f, 0.81f, 0.36f, 1f);
        itemRenderer.rect(180f, 295f, 440f, 2f);
        itemRenderer.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(0.98f, 0.81f, 0.36f, 1f);
        font.getData().setScale(3.2f);
        drawReportCentered("CAMPUS RUNNER", 460f);
        font.setColor(0.76f, 0.83f, 0.94f, 1f);
        font.getData().setScale(1.4f);
        drawReportCentered("8 SEMESTERS TO GRADUATION", 401f);
        font.getData().setScale(1.15f);
        drawReportCentered("Collect credits. Survive semesters. Graduate!", 354f);
        font.setColor(0.98f, 0.81f, 0.36f, 1f);
        font.getData().setScale(1.2f);
        drawReportCentered("CONTROLS", 279f);
        font.setColor(0.9f, 0.94f, 1f, 1f);
        font.getData().setScale(1.1f);
        drawReportCentered("LEFT / RIGHT ARROWS  -  MOVE", 245f);
        drawReportCentered("SPACE  -  JUMP", 217f);
        drawReportCentered("ESC  -  PAUSE / RESUME", 189f);
        font.getData().setScale(1.6f);
        drawReportCentered("PRESS ENTER TO START", 116f);
        batch.end();
        font.getData().setScale(2f);
        font.setColor(Color.BLACK);
    }

    private void renderHud() {
        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        float top = uiViewport.getWorldHeight() - 20f;
        font.draw(batch, "YEAR: " + stage.getYear(), 20f, top);
        font.draw(batch, "SEMESTER: " + stage.getSemester(), 20f, top - 30f);
        font.draw(batch, "ITEMS: " + collectedItemCount + " / " + gradeItems.size,
            20f, top - 60f);
        font.draw(batch, "SCORE: " + stageScore, 20f, top - 90f);
        font.draw(batch, "LIVES: " + lives + " / " + semesterMaxLives, 520f, top);
        batch.end();

        itemRenderer.setProjectionMatrix(uiCamera.combined);
        itemRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < semesterMaxLives; i++) {
            float x = 532f + i * 36f;
            float y = top - 50f;
            itemRenderer.setColor(i < lives ? Color.RED : Color.LIGHT_GRAY);
            itemRenderer.circle(x - 6f, y + 12f, 6f, 16);
            itemRenderer.circle(x + 6f, y + 12f, 6f, 16);
            itemRenderer.triangle(x - 12f, y + 12f, x + 12f, y + 12f, x, y);
        }
        itemRenderer.end();
    }

    private void renderVacationEvent() {
        vacationRenderer.render(vacationGame);
    }

    private void renderStateUi() {
        if (gameState == GameState.PLAYING) return;

        String title;
        String instruction;
        String detail = null;
        String restartNotice = null;
        switch (gameState) {
            case PAUSED:
                title = "PAUSED";
                instruction = "Press ESC to Resume";
                break;
            case STAGE_CLEAR:
                title = "YEAR " + stage.getYear() + " - SEMESTER " + stage.getSemester() + " CLEAR";
                instruction = "PRESS ENTER FOR NEXT SEMESTER";
                break;
            case GAME_OVER:
                title = "GAME OVER";
                detail = "ALL LIVES LOST";
                restartNotice = "RESTART FROM YEAR 1, SEMESTER 1";
                instruction = "PRESS ENTER TO RESTART";
                break;
            default:
                return;
        }

        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        textLayout.setText(font, title);
        font.draw(batch, textLayout,
            (uiViewport.getWorldWidth() - textLayout.width) / 2f,
            (uiViewport.getWorldHeight() + textLayout.height) / 2f);
        if (detail != null) {
            textLayout.setText(font, detail);
            font.draw(batch, textLayout,
                (uiViewport.getWorldWidth() - textLayout.width) / 2f,
                uiViewport.getWorldHeight() / 2f - 40f);
        }
        if (restartNotice != null) {
            textLayout.setText(font, restartNotice);
            font.draw(batch, textLayout,
                (uiViewport.getWorldWidth() - textLayout.width) / 2f,
                uiViewport.getWorldHeight() / 2f - 80f);
        }
        textLayout.setText(font, instruction);
        font.draw(batch, textLayout,
            (uiViewport.getWorldWidth() - textLayout.width) / 2f,
            uiViewport.getWorldHeight() / 2f - (restartNotice != null ? 120f : detail == null ? 40f : 80f));
        batch.end();
    }

    private FinalReport getFinalReport() {
        return gameState == GameState.GAME_COMPLETE || gameState == GameState.GRADUATION_ENDING
            ? new FinalReport(completedResults, totalDeathCount) : null;
    }

    private void renderFinalReport(FinalReport report) {
        uiViewport.apply();
        itemRenderer.setProjectionMatrix(uiCamera.combined);
        itemRenderer.begin(ShapeRenderer.ShapeType.Filled);
        itemRenderer.setColor(0.94f, 0.96f, 0.98f, 1f);
        itemRenderer.rect(32f, 96f, 736f, 426f);
        itemRenderer.setColor(0.82f, 0.88f, 0.93f, 1f);
        itemRenderer.rect(50f, 478f, 700f, 32f);
        for (int i = 0; i < report.getSemesterCount(); i += 2) {
            itemRenderer.setColor(0.88f, 0.92f, 0.95f, 1f);
            itemRenderer.rect(50f, 450f - i * 28f, 700f, 28f);
        }
        itemRenderer.setColor(0.3f, 0.4f, 0.5f, 1f);
        itemRenderer.rect(50f, 244f, 700f, 2f);
        itemRenderer.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(0.1f, 0.2f, 0.3f, 1f);
        font.getData().setScale(2f);
        drawReportCentered("FINAL ACADEMIC REPORT", 574f);
        font.getData().setScale(1.2f);
        drawReportCentered("CAMPUS RUNNER", 540f);
        font.getData().setScale(1.1f);
        font.draw(batch, "SEMESTER", 70f, 500f);
        drawReportRight("ITEM SCORE", 370f, 500f);
        drawReportRight("SURVIVAL BONUS", 570f, 500f);
        drawReportRight("FINAL", 730f, 500f);
        for (int i = 0; i < report.getSemesterCount(); i++) {
            StageResult result = report.getSemester(i);
            float y = 472f - i * 28f;
            font.draw(batch, (i / 2 + 1) + "-" + (i % 2 + 1), 95f, y);
            drawReportRight(result.getItemScore() + " / " + report.getItemMaximum(i), 370f, y);
            drawReportRight(result.getSurvivalBonus() + " / 20", 570f, y);
            drawReportRight(result.getFinalScore() + " / " + (report.getItemMaximum(i) + 20), 730f, y);
        }
        font.draw(batch, "TOTAL ITEM SCORE: " + report.getTotalItemScore() + " / " + FinalReport.MAX_ITEM_SCORE, 64f, 222f);
        font.draw(batch, "TOTAL SURVIVAL BONUS: " + report.getTotalSurvivalBonus() + " / " + FinalReport.MAX_SURVIVAL_BONUS, 64f, 194f);
        font.getData().setScale(1.4f);
        font.draw(batch, "FINAL SCORE: " + report.getFinalScore() + " / " + FinalReport.MAX_FINAL_SCORE, 64f, 150f);
        font.setColor(0.65f, 0.15f, 0.15f, 1f);
        font.getData().setScale(1.2f);
        font.draw(batch, "TOTAL DEATHS: " + report.getTotalDeathCount(), 490f, 222f);
        font.setColor(0.1f, 0.2f, 0.3f, 1f);
        font.draw(batch, String.format(Locale.ROOT, "GPA: %.2f / 4.50", report.getGpa()), 490f, 186f);
        font.getData().setScale(1.1f);
        font.draw(batch, String.format(Locale.ROOT, "SCORE RATE: %.2f%%", report.getScoreRate()), 490f, 150f);
        drawReportCentered("PRESS SPACE TO VIEW GRADUATION RESULT", 60f);
        batch.end();
        font.getData().setScale(2f);
        font.setColor(Color.BLACK);
    }

    private void renderGraduationEnding(FinalReport report) {
        GraduationEnding ending = report.getEnding();
        int rgb = ending.getColorRgb();
        Color accent = new Color(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, 1f);
        uiViewport.apply();
        itemRenderer.setProjectionMatrix(uiCamera.combined);
        itemRenderer.begin(ShapeRenderer.ShapeType.Filled);
        itemRenderer.setColor(0.94f, 0.96f, 0.98f, 1f);
        itemRenderer.rect(32f, 96f, 736f, 426f);
        itemRenderer.setColor(accent);
        itemRenderer.rect(32f, 514f, 736f, 8f);
        itemRenderer.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        font.setColor(0.1f, 0.2f, 0.3f, 1f);
        font.getData().setScale(1.2f);
        drawReportCentered("CAMPUS RUNNER", 570f);
        font.getData().setScale(2f);
        drawReportCentered("GRADUATION RESULT", 490f);
        endingFont.setColor(accent);
        endingFont.getData().setScale(1.5f);
        drawEndingCentered(ending.getTitle(), 432f);
        font.setColor(0.1f, 0.2f, 0.3f, 1f);
        font.getData().setScale(1.4f);
        drawReportCentered(String.format(Locale.ROOT, "GPA: %.2f / 4.50", report.getGpa()), 366f);
        drawReportCentered("FINAL SCORE: " + report.getFinalScore() + " / " + FinalReport.MAX_FINAL_SCORE, 328f);
        drawReportCentered("TOTAL DEATHS: " + report.getTotalDeathCount(), 290f);
        endingFont.setColor(0.1f, 0.2f, 0.3f, 1f);
        endingFont.getData().setScale(0.85f);
        drawEndingCentered(ending.getMessage(), 236f);
        drawEndingCentered(ending.getDetail(), 198f);
        endingFont.getData().setScale(0.65f);
        drawEndingCentered(GraduationEnding.PRECISION_NOTE, 142f);
        font.getData().setScale(1.1f);
        drawReportCentered("PRESS ENTER TO RESTART FROM YEAR 1, SEMESTER 1", 60f);
        batch.end();
        font.getData().setScale(2f);
        font.setColor(Color.BLACK);
    }

    private void drawEndingCentered(String text, float y) {
        textLayout.setText(endingFont, text);
        endingFont.draw(batch, textLayout, (uiViewport.getWorldWidth() - textLayout.width) / 2f, y);
    }

    private void drawReportRight(String text, float right, float y) {
        textLayout.setText(font, text);
        font.draw(batch, textLayout, right - textLayout.width, y);
    }

    private void drawReportCentered(String text, float y) {
        textLayout.setText(font, text);
        font.draw(batch, textLayout, (uiViewport.getWorldWidth() - textLayout.width) / 2f, y);
    }

    private void restartStage() {
        resetStageRuntime(currentStageNumber);
    }

    private void startNewGame() {
        vacationGame = null;
        vacationManager.reset();
        Arrays.fill(completedResults, null);
        Arrays.fill(semesterDeaths, 0);
        totalDeathCount = 0;
        loadStage(1);
    }

    private void loadStage(int stageNumber) {
        vacationManager.enterStage(stageNumber);
        semesterMaxLives = MAX_LIVES + vacationManager.modifierForStage(stageNumber).getExtraStartingLives();
        lives = semesterMaxLives;
        semesterDeathCount = 0;
        semesterDeaths[stageNumber - 1] = 0;
        resetStageRuntime(stageNumber);
    }

    private void resetStageRuntime(int stageNumber) {
        hitFeedback.reset();
        waitForHitKeyRelease = false;
        stage = StageFactory.createStage(stageNumber);
        currentStageNumber = stage.getStageNumber();
        // 이전 학기의 플레이어와 실행용 객체를 모두 교체합니다.
        player = new Player(stage.getStartX(), stage.getStartY(), vacationManager.modifierForStage(stageNumber));
        flag = stage.getFlag();
        blocks = stage.getBlocks();
        gradeItems = stage.getGradeItems();
        movingPlatforms = stage.getMovingPlatforms();
        timedTraps = stage.getTimedTraps();
        enemies = stage.getEnemies();
        collectedItemCount = 0;
        stageScore = 0;
        camera.position.set(getCameraX(),
            viewport.getWorldHeight() / 2f, 0);
        camera.update();
        gameState = GameState.PLAYING;
    }

    private void recordStageResult() {
        int index = currentStageNumber - 1;
        if (completedResults[index] == null) {
            completedResults[index] = new StageResult(currentStageNumber, stageScore, collectedItemCount,
                semesterDeathCount);
        }
    }

    private int getTotalScore() {
        int total = 0;
        for (StageResult result : completedResults) {
            if (result != null) total += result.getFinalScore();
        }
        return total;
    }


    /** Confirm the finished game's actual result on the game thread; early or stale callbacks are rejected. */
    public boolean completeVacationEvent(VacationEventType type, boolean success) {
        if (gameState != GameState.VACATION_EVENT || vacationGame == null || !vacationGame.isFinished()
            || vacationGame.getType() != type || vacationGame.isSuccess() != success
            || !vacationManager.complete(type, success)) return false;
        vacationGame = null;
        loadStage(currentStageNumber + 1);
        return true;
    }

    private void update(float dt) {
        if (gameState == GameState.DYING) {
            if (hitFeedback.advance(dt)) {
                boolean held = isGameplayKeyHeld();
                applyDeath();
                waitForHitKeyRelease = held;
            }
            return;
        }
        if (waitForHitKeyRelease) {
            if (!isGameplayKeyHeld()) waitForHitKeyRelease = false;
            return;
        }
        if (gameState == GameState.TITLE_SCREEN) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) startNewGame();
            return; // Do not also apply movement, jump, or ESC on the start frame.
        }
        if (gameState == GameState.VACATION_EVENT) {
            MinigameInput input = MinigameInput.read();
            if (vacationGame.isFinished()) {
                if (input.isPressed(Input.Keys.ENTER)) completeVacationEvent(vacationGame.getType(), vacationGame.isSuccess());
            } else vacationGame.update(dt,input);
            return; // Including after completion: no second input or world step on the transition frame.
        }
        // Consume report input separately, so simultaneous Space/Enter cannot also restart the game.
        if (gameState == GameState.GAME_COMPLETE) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) gameState = GameState.GRADUATION_ENDING;
            return;
        }
        // 상태 입력은 물리 갱신 전에 처리하여 정지 상태에서도 동작합니다.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            switch (gameState) {
                case GAME_OVER:
                    startNewGame();
                    return;
                case STAGE_CLEAR:
                    loadStage(currentStageNumber + 1);
                    return;
                case GRADUATION_ENDING:
                    startNewGame();
                    return;
                default:
                    // PLAYING/PAUSED에서는 Enter로 상태나 스테이지를 변경하지 않습니다.
                    break;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                return;
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
                return;
            }
        }
        if (gameState != GameState.PLAYING || dt <= 0) return;

        // 이동량을 먼저 확정한 뒤 플레이어의 탑승/밀림과 자체 이동을 처리합니다.
        for (MovingPlatform platform : movingPlatforms) platform.update(dt);
        for (TimedTrap trap : timedTraps) trap.update(dt);
        for (PatrollingEnemy enemy : enemies) enemy.update(dt);
        player.update(dt, blocks, movingPlatforms, stage.getWorldWidth());

        // PLAYING에서만 실행되며, 수집 성공 순간에만 각각 누적합니다.
        for (GradeItem item : gradeItems) {
            if (item.tryCollect(player)) {
                collectedItemCount++;
                stageScore += item.getScore();
            }
        }

        // 실패가 깃발보다 우선합니다. 같은 프레임의 동시 접촉도 GAME_OVER입니다.
        boolean trapHit = false;
        for (TimedTrap trap : timedTraps) {
            if (trap.isDangerousTo(player)) {
                trapHit = true;
                break;
            }
        }
        boolean enemyHit = false;
        for (PatrollingEnemy enemy : enemies) {
            if (enemy.touches(player)) {
                enemyHit = true;
                break;
            }
        }
        if (player.getY() < -100) {
            applyDeath(); // Off-screen falls retain the original immediate-death policy.
            return;
        }
        if (trapHit || enemyHit) {
            if (lives == 1) {
                hitFeedback.reset();
                applyDeath();
            } else {
                hitFeedback.start();
                gameState = GameState.DYING;
            }
            return;
        }

        // 마지막 학기도 결과를 저장한 뒤 전체 완료로 전환합니다.
        else if (player.overlaps(flag)) {
            recordStageResult();
            if (currentStageNumber == StageFactory.STAGE_COUNT) gameState = GameState.GAME_COMPLETE;
            else if (VacationEventType.afterStage(currentStageNumber) != null) {
                vacationManager.beginAfterStage(currentStageNumber);
                vacationGame = VacationMinigame.create(vacationManager.getPending());
                gameState = GameState.VACATION_EVENT;
            } else gameState = GameState.STAGE_CLEAR;
        }

        // 6. 카메라는 X축으로만 따라갑니다.
        camera.position.x = getCameraX();
        camera.update();
    }

    private void applyDeath() {
        semesterDeathCount++;
        totalDeathCount++;
        semesterDeaths[currentStageNumber - 1] = semesterDeathCount;
        lives--;
        if (lives == 0) gameState = GameState.GAME_OVER;
        else restartStage();
    }

    private boolean isGameplayKeyHeld() {
        return Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)
            || Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyPressed(Input.Keys.R) || Gdx.input.isKeyPressed(Input.Keys.ESCAPE);
    }

    private Matrix4 getWorldRenderProjection() {
        return worldRenderProjection.set(camera.combined).translate(-hitFeedback.getShakeX(),-hitFeedback.getShakeY(),0);
    }

    private float getCameraX() {
        float halfWidth = viewport.getWorldWidth() / 2f;
        if (stage.getWorldWidth() <= viewport.getWorldWidth()) {
            return stage.getWorldWidth() / 2f;
        }
        return MathUtils.clamp(player.getCenterX(), halfWidth,
            stage.getWorldWidth() - halfWidth);
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        uiViewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        playerSprites.dispose();
        fixedPlatformRenderer.dispose();
        flagTexture.dispose();
        gradeItemTexture.dispose();
        font.dispose();
        endingFont.dispose();
        itemRenderer.dispose();
    }
}
