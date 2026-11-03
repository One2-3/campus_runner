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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private static final float WORLD_WIDTH = 2600;
    private static final float MOVE_SPEED = 260;
    private static final float JUMP_SPEED = 650;
    private static final float GRAVITY = 1700;
    private static final float START_X = 80;
    private static final float START_Y = 60;

    private SpriteBatch batch;
    private Texture playerTexture, blockTexture, flagTexture;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private OrthographicCamera uiCamera;
    private FitViewport uiViewport;
    private BitmapFont font;
    private GlyphLayout textLayout;

    private final Rectangle player = new Rectangle(START_X, START_Y, 32, 44);
    private final Rectangle flag = new Rectangle(2420, 344, 20, 100);
    private final Array<Rectangle> blocks = new Array<>();
    private float velocityX, velocityY;
    private boolean grounded;
    private GameState gameState = GameState.PLAYING;


    @Override
    public void create() {
        batch = new SpriteBatch();
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

        // x, y, 너비, 높이. 바닥도 같은 충돌 블록입니다.
        blocks.add(new Rectangle(0, 0, 620, 60));
        blocks.add(new Rectangle(760, 0, 540, 60));
        blocks.add(new Rectangle(1440, 0, 1160, 60));
        blocks.add(new Rectangle(300, 60, 80, 64));
        blocks.add(new Rectangle(900, 60, 96, 80));
        blocks.add(new Rectangle(1080, 180, 160, 24));
        blocks.add(new Rectangle(1650, 60, 120, 70));
        blocks.add(new Rectangle(1790, 60, 120, 140));
        blocks.add(new Rectangle(1930, 60, 120, 210));
        blocks.add(new Rectangle(2100, 270, 110, 24));
        blocks.add(new Rectangle(2280, 320, 210, 24));
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
        batch.draw(playerTexture, player.x, player.y, player.width, player.height);
        batch.end();

        renderStateUi();

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
        player.setPosition(START_X, START_Y);
        velocityX = 0;
        velocityY = 0;
        grounded = false;
        camera.position.set(viewport.getWorldWidth() / 2f,
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

        // 1. 좌우 이동과 지상 점프
        velocityX = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) velocityX -= MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) velocityX += MOVE_SPEED;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && grounded) {
            velocityY = JUMP_SPEED;
            grounded = false;
        }

        // 2. 중력: 아래쪽 속도는 음수이며 최대 낙하 속도를 제한합니다.
        velocityY = Math.max(velocityY - GRAVITY * dt, -900);

        // 3. X축과 Y축을 나누어 이동하고 충돌을 보정합니다.
        moveX(dt);
        moveY(dt);

        // 4. 낭떠러지에 떨어지면 실패 상태로 전환합니다.
        if (player.y < -100) {
            gameState = GameState.GAME_OVER;
        }

        // 5. 깃발에 닿으면 클리어 상태로 전환합니다.
        else if (player.overlaps(flag)) gameState = GameState.STAGE_CLEAR;

        // 6. 카메라는 X축으로만 따라갑니다.
        camera.position.x = MathUtils.clamp(player.x + player.width / 2, 400, WORLD_WIDTH - 400);
        camera.update();
    }

    private void moveX(float dt) {
        player.x += velocityX * dt;
        for (Rectangle block : blocks) {
            if (!player.overlaps(block)) continue;
            if (velocityX > 0) player.x = block.x - player.width;
            else if (velocityX < 0) player.x = block.x + block.width;
        }
        player.x = MathUtils.clamp(player.x, 0, WORLD_WIDTH - player.width);
    }

    private void moveY(float dt) {
        float dy = velocityY * dt;
        player.y += dy;
        grounded = false;

        for (Rectangle block : blocks) {
            if (!player.overlaps(block)) continue;
            if (dy < 0) { // 내려오던 중: 블록 위에 착지
                player.y = block.y + block.height;
                velocityY = 0;
                grounded = true;
            } else if (dy > 0) { // 올라가던 중: 블록 아래에 머리 충돌
                player.y = block.y - player.height;
                velocityY = 0;
            }
        }
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
    }
}
