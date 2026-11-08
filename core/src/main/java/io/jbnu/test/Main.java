package io.jbnu.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.Arrays;



/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    /** Base starting lives; a semester may receive an extra vacation life. */
    public static final int MAX_LIVES = 5;

    private SpriteBatch batch;
    private Texture flagTexture;
    private FixedPlatformRenderer fixedPlatformRenderer;
    private MovingPlatformRenderer movingPlatformRenderer;
    private Texture gradeItemTexture;
    private PixelHudRenderer pixelHudRenderer;
    private PauseMenuRenderer pauseMenuRenderer;
    private int pauseSelection;
    private GameState pauseReturnState = GameState.PLAYING;
    private GameOverRenderer gameOverRenderer;
    private StageClearRenderer stageClearRenderer;
    private int gameOverSelection;
    private PlayerSprites playerSprites;
    private EnemySprites enemySprites;
    private TrapSprites trapSprites;
    private SeasonalBackgroundRenderer seasonalBackgroundRenderer;
    private final HitFeedback hitFeedback = new HitFeedback();
    private final Matrix4 worldRenderProjection = new Matrix4();
    private boolean waitForHitKeyRelease;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private OrthographicCamera uiCamera;
    private FitViewport uiViewport;
    private BitmapFont font;
    private TitleScreenRenderer titleScreenRenderer;
    private int titleSelection;
    private boolean titleHelpVisible;
    private PixelResultCanvas resultCanvas;
    private FinalReportRenderer finalReportRenderer;
    private EndingRenderer endingRenderer;
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
        enemySprites = new EnemySprites();
        trapSprites = new TrapSprites();
        seasonalBackgroundRenderer = new SeasonalBackgroundRenderer();
        fixedPlatformRenderer = new FixedPlatformRenderer();
        movingPlatformRenderer = new MovingPlatformRenderer();
        flagTexture = new Texture(Gdx.files.internal("flag.png"));
        gradeItemTexture = new Texture(Gdx.files.internal("pixel/items/grade_a_plus_128.png"));
        gradeItemTexture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        pixelHudRenderer = new PixelHudRenderer(gradeItemTexture);
        pauseMenuRenderer = new PauseMenuRenderer();
        gameOverRenderer = new GameOverRenderer();
        stageClearRenderer = new StageClearRenderer();

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
        titleScreenRenderer = new TitleScreenRenderer(seasonalBackgroundRenderer, gradeItemTexture, font);
        resultCanvas = new PixelResultCanvas();
        finalReportRenderer = new FinalReportRenderer(resultCanvas);
        endingRenderer = new EndingRenderer(resultCanvas);
        vacationRenderer = new VacationMinigameRenderer(batch, itemRenderer, uiViewport, uiCamera);

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

        if (gameState == GameState.GAME_COMPLETE || gameState == GameState.GRADUATION_ENDING) {
            seasonalBackgroundRenderer.render(batch, currentStageNumber, stage.getWorldWidth(), camera.position.x,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            if (gameState == GameState.GAME_COMPLETE) renderFinalReport(getFinalReport());
            else renderGraduationEnding(getFinalReport());
            return;
        }
        if (gameState == GameState.VACATION_EVENT
            || (gameState == GameState.PAUSED && pauseReturnState == GameState.VACATION_EVENT)) {
            renderVacationEvent();
            if (gameState == GameState.PAUSED) pauseMenuRenderer.render(batch, uiViewport, pauseSelection);
            return;
        }

        seasonalBackgroundRenderer.render(batch, currentStageNumber, stage.getWorldWidth(), camera.position.x,
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        viewport.apply();
        Matrix4 worldProjection = getWorldRenderProjection();
        fixedPlatformRenderer.setPixelsPerWorldUnit(Math.abs(worldProjection.val[Matrix4.M11]) * viewport.getScreenHeight()
            * Gdx.graphics.getBackBufferHeight() / Gdx.graphics.getHeight() / 2f);
        boolean showHurt = gameState == GameState.DYING || gameState == GameState.GAME_OVER;
        batch.setProjectionMatrix(worldProjection);
        batch.begin();
        for (Rectangle block : blocks) {
            fixedPlatformRenderer.render(batch, block);
        }
        for (MovingPlatform platform : movingPlatforms) {
            movingPlatformRenderer.render(batch, platform);
        }
        batch.draw(flagTexture, flag.x, flag.y, flag.width, flag.height);
        if (!showHurt) playerSprites.render(batch, player, false);

        for (TimedTrap trap : timedTraps) {
            trapSprites.render(batch, trap);
        }
        for (PatrollingEnemy enemy : enemies) {
            enemySprites.render(batch, enemy);
        }
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
        titleScreenRenderer.render(batch, uiViewport, titleSelection, titleHelpVisible);
    }

    private void renderHud() {
        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        pixelHudRenderer.render(batch, uiViewport.getWorldWidth(), uiViewport.getWorldHeight(),
            stage.getYear(), stage.getSemester(), stageScore, collectedItemCount, gradeItems.size,
            lives, semesterMaxLives);
    }
    private void renderVacationEvent() {
        vacationRenderer.render(vacationGame);
    }

    private void renderStateUi() {
        if (gameState == GameState.PLAYING) return;
        if (gameState == GameState.GAME_OVER) {
            gameOverRenderer.render(batch, uiViewport, gameOverSelection, stage.getYear(), stage.getSemester(),
                stageScore, collectedItemCount, gradeItems.size, semesterDeathCount);
            return;
        }
        if (gameState == GameState.PAUSED) {
            pauseMenuRenderer.render(batch, uiViewport, pauseSelection);
            return;
        }

        if (gameState == GameState.STAGE_CLEAR) {
            stageClearRenderer.render(batch, uiViewport, completedResults[currentStageNumber - 1], gradeItems.size);
        }
    }

    private FinalReport getFinalReport() {
        return gameState == GameState.GAME_COMPLETE || gameState == GameState.GRADUATION_ENDING
            ? new FinalReport(completedResults, totalDeathCount) : null;
    }

    private void renderFinalReport(FinalReport report) {
        finalReportRenderer.render(batch, uiViewport, report);
    }

    private void renderGraduationEnding(FinalReport report) {
        endingRenderer.render(batch, uiViewport, report);
    }
    private void restartStage() {
        resetStageRuntime(currentStageNumber);
    }

    private void startNewGame() {
        titleSelection = 0;
        titleHelpVisible = false;
        pauseReturnState = GameState.PLAYING;
        gameOverSelection = 0;
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
        pauseReturnState = GameState.PLAYING;
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
            updateTitleMenu();
            return; // Do not also apply movement, jump, or ESC on the start frame.
        }
        if (gameState == GameState.PAUSED) {
            updatePauseMenu();
            return; // No physics, item collection or world timers on menu-action frames.
        }
        if (gameState == GameState.GAME_OVER) {
            updateGameOverMenu();
            return;
        }
        if (gameState == GameState.VACATION_EVENT) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                pauseSelection = 0;
                pauseReturnState = GameState.VACATION_EVENT;
                gameState = GameState.PAUSED;
                return;
            }
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
                case STAGE_CLEAR:
                    continueAfterStageClear();
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
                pauseSelection = 0;
                pauseReturnState = GameState.PLAYING;
                gameState = GameState.PAUSED;
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
            gameState = GameState.STAGE_CLEAR;
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
        if (lives == 0) {
            gameOverSelection = 0;
            gameState = GameState.GAME_OVER;
        }
        else restartStage();
    }

    private void continueAfterStageClear() {
        // Results were confirmed once at the flag; only the existing destination moves behind Enter.
        if (currentStageNumber == StageFactory.STAGE_COUNT) {
            gameState = GameState.GAME_COMPLETE;
        } else if (VacationEventType.afterStage(currentStageNumber) != null) {
            vacationManager.beginAfterStage(currentStageNumber);
            vacationGame = VacationMinigame.create(vacationManager.getPending());
            gameState = GameState.VACATION_EVENT;
        } else {
            loadStage(currentStageNumber + 1);
        }
    }

    private void updateGameOverMenu() {
        boolean up = Gdx.input.isKeyJustPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyJustPressed(Input.Keys.DOWN);
        if (up != down) {
            gameOverSelection = 1 - gameOverSelection;
            return;
        }
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) return;
        boolean title = gameOverSelection == 1;
        startNewGame(); // Existing GAME_OVER reset: always Stage 1, never the failed semester.
        if (title) {
            gameState = GameState.TITLE_SCREEN;
            waitForHitKeyRelease = true;
        }
    }

    private void updatePauseMenu() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            gameState = pauseReturnState;
            return;
        }
        boolean up = Gdx.input.isKeyJustPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyJustPressed(Input.Keys.DOWN);
        if (up || down) {
            if (up != down) pauseSelection = (pauseSelection + (down ? 1 : 2)) % 3;
            return; // Navigation and confirmation never execute together.
        }
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) return;
        switch (pauseSelection) {
            case 0:
                gameState = pauseReturnState;
                break;
            case 1:
                if (pauseReturnState == GameState.VACATION_EVENT) {
                    // Pending vacation and saved semester results remain untouched; reward is not confirmed here.
                    vacationGame = VacationMinigame.create(vacationManager.getPending());
                    gameState = GameState.VACATION_EVENT;
                } else restartStage(); // Same stage reset as a retry, without charging a life/death.
                waitForHitKeyRelease = true;
                break;
            case 2:
                startNewGame(); // Clear scores, deaths and vacation history before returning.
                gameState = GameState.TITLE_SCREEN;
                waitForHitKeyRelease = true;
                break;
            default:
                throw new IllegalStateException("Unknown pause selection: " + pauseSelection);
        }
        pauseSelection = 0;
    }

    private void updateTitleMenu() {
        if (titleHelpVisible) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) titleHelpVisible = false;
            return; // Help never starts or exits the game, including simultaneous ESC/Enter.
        }
        boolean up = Gdx.input.isKeyJustPressed(Input.Keys.UP);
        boolean down = Gdx.input.isKeyJustPressed(Input.Keys.DOWN);
        if (up != down) {
            titleSelection = (titleSelection + (down ? 1 : 2)) % 3;
            return;
        }
        if (!Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) return;
        switch (titleSelection) {
            case 0: startNewGame(); break;
            case 1: titleHelpVisible = true; break;
            case 2: Gdx.app.exit(); break;
            default: throw new IllegalStateException("Unknown title selection: " + titleSelection);
        }
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
        titleScreenRenderer.dispose();
        vacationRenderer.dispose();
        batch.dispose();
        playerSprites.dispose();
        enemySprites.dispose();
        trapSprites.dispose();
        seasonalBackgroundRenderer.dispose();
        fixedPlatformRenderer.dispose();
        movingPlatformRenderer.dispose();
        flagTexture.dispose();
        pixelHudRenderer.dispose();
        pauseMenuRenderer.dispose();
        gameOverRenderer.dispose();
        stageClearRenderer.dispose();
        gradeItemTexture.dispose();
        font.dispose();
        resultCanvas.dispose();
        itemRenderer.dispose();
    }
}
