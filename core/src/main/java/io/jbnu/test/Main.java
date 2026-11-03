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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.Arrays;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture playerTexture, blockTexture, flagTexture;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private OrthographicCamera uiCamera;
    private FitViewport uiViewport;
    private BitmapFont font;
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
    private int currentStageNumber = 1;
    private final StageResult[] completedResults = new StageResult[StageFactory.STAGE_COUNT];
    private GameState gameState = GameState.PLAYING;


    @Override
    public void create() {
        batch = new SpriteBatch();
        itemRenderer = new ShapeRenderer();
        playerTexture = new Texture(Gdx.files.internal("player.png"));
        blockTexture = new Texture(Gdx.files.internal("block.png"));
        flagTexture = new Texture(Gdx.files.internal("flag.png"));

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
        textLayout = new GlyphLayout();

        startNewGame();
    }


    @Override
    public void render() {

        // 큰 프레임 시간으로 인해 한 번에 너무 멀리 이동하는 것을 제한합니다.
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        update(dt);



        ScreenUtils.clear(1f,1f,1f,1f);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Rectangle block : blocks) {
            batch.draw(blockTexture, block.x, block.y, block.width, block.height);
        }
        batch.draw(flagTexture, flag.x, flag.y, flag.width, flag.height);
        player.render(batch, playerTexture);
        batch.end();

        // SpriteBatch와 ShapeRenderer는 동시에 begin하지 않습니다.
        itemRenderer.setProjectionMatrix(camera.combined);
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
        for (GradeItem item : gradeItems) {
            item.render(itemRenderer);
        }
        itemRenderer.end();

        renderHud();
        renderStateUi();

    }

    private void renderHud() {
        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        float top = uiViewport.getWorldHeight() - 20f;
        font.draw(batch, "YEAR: " + stage.getStageNumber(), 20f, top);
        font.draw(batch, "ITEMS: " + collectedItemCount + " / " + gradeItems.size,
            20f, top - 30f);
        font.draw(batch, "SCORE: " + stageScore, 20f, top - 60f);
        batch.end();
    }

    private void renderStateUi() {
        if (gameState == GameState.PLAYING) return;

        String title;
        String instruction;
        String detail = null;
        switch (gameState) {
            case PAUSED:
                title = "PAUSED";
                instruction = "Press ESC to Resume";
                break;
            case STAGE_CLEAR:
                title = "YEAR " + currentStageNumber + " CLEAR";
                instruction = "PRESS ENTER FOR NEXT YEAR";
                break;
            case GAME_OVER:
                title = "GAME OVER";
                instruction = "PRESS ENTER TO RETRY";
                break;
            case GAME_COMPLETE:
                title = "GRADUATION COMPLETE";
                detail = "TOTAL SCORE: " + getTotalScore();
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
        textLayout.setText(font, instruction);
        font.draw(batch, textLayout,
            (uiViewport.getWorldWidth() - textLayout.width) / 2f,
            uiViewport.getWorldHeight() / 2f - (detail == null ? 40f : 80f));
        batch.end();
    }

    private void restartStage() {
        loadStage(currentStageNumber);
    }

    private void startNewGame() {
        Arrays.fill(completedResults, null);
        loadStage(1);
    }

    private void loadStage(int stageNumber) {
        stage = StageFactory.createStage(stageNumber);
        currentStageNumber = stage.getStageNumber();
        // 이전 학년의 플레이어와 실행용 객체를 모두 교체합니다.
        player = new Player(stage.getStartX(), stage.getStartY());
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
            completedResults[index] = new StageResult(currentStageNumber, stageScore, collectedItemCount);
        }
    }

    private int getTotalScore() {
        int total = 0;
        for (StageResult result : completedResults) {
            if (result != null) total += result.getScore();
        }
        return total;
    }


    private void update(float dt) {
        // 상태 입력은 물리 갱신 전에 처리하여 정지 상태에서도 동작합니다.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            switch (gameState) {
                case GAME_OVER:
                    restartStage();
                    return;
                case STAGE_CLEAR:
                    loadStage(currentStageNumber + 1);
                    return;
                case GAME_COMPLETE:
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
        if (player.getY() < -100 || trapHit || enemyHit) {
            gameState = GameState.GAME_OVER;
        }

        // 마지막 학년도 결과를 저장한 뒤 전체 완료로 전환합니다.
        else if (player.overlaps(flag)) {
            recordStageResult();
            gameState = currentStageNumber == StageFactory.STAGE_COUNT
                ? GameState.GAME_COMPLETE : GameState.STAGE_CLEAR;
        }

        // 6. 카메라는 X축으로만 따라갑니다.
        camera.position.x = getCameraX();
        camera.update();
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
        playerTexture.dispose();
        blockTexture.dispose();
        flagTexture.dispose();
        font.dispose();
        itemRenderer.dispose();
    }
}
