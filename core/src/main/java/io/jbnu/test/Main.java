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
    private int collectedItemCount;
    private int stageScore;
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

        stage = StageFactory.createStage(1);
        // 실행용 사본을 한 번 받아 사용하며 스테이지 원본은 보존합니다.
        flag = stage.getFlag();
        blocks = stage.getBlocks();
        player = new Player(stage.getStartX(), stage.getStartY());
        restartStage();
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
        switch (gameState) {
            case PAUSED:
                title = "PAUSED";
                instruction = "Press ESC to Resume";
                break;
            case STAGE_CLEAR:
                title = "STAGE CLEAR";
                instruction = "Press ENTER to Restart";
                break;
            case GAME_OVER:
                title = "GAME OVER";
                instruction = "Press ENTER to Restart";
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
        textLayout.setText(font, instruction);
        font.draw(batch, textLayout,
            (uiViewport.getWorldWidth() - textLayout.width) / 2f,
            uiViewport.getWorldHeight() / 2f - 40f);
        batch.end();
    }

    private void restartStage() {
        player.reset(stage.getStartX(), stage.getStartY());
        gradeItems = stage.getGradeItems();
        collectedItemCount = 0;
        stageScore = 0;
        camera.position.set(getCameraX(),
            viewport.getWorldHeight() / 2f, 0);
        camera.update();
        gameState = GameState.PLAYING;
    }


    private void update(float dt) {
        // 상태 입력은 물리 갱신 전에 처리하여 정지 상태에서도 동작합니다.
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            restartStage();
            return;
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

        player.update(dt, blocks, stage.getWorldWidth());

        // PLAYING에서만 실행되며, 수집 성공 순간에만 각각 누적합니다.
        for (GradeItem item : gradeItems) {
            if (item.tryCollect(player)) {
                collectedItemCount++;
                stageScore += item.getScore();
            }
        }

        // 4. 낭떠러지에 떨어지면 실패 상태로 전환합니다.
        if (player.getY() < -100) {
            gameState = GameState.GAME_OVER;
        }

        // 5. 깃발에 닿으면 클리어 상태로 전환합니다.
        else if (player.overlaps(flag)) gameState = GameState.STAGE_CLEAR;

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
