package com.prismpop.game;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.Random;

/**
 * Prism Pop — a fully offline, original match-three arcade game.
 * All game art is rendered with Android Canvas primitives; no external art pack is required.
 */
public class MainActivity extends Activity {
    private PrismView game;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16, 13, 39));
        getWindow().setNavigationBarColor(Color.rgb(16, 13, 39));
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        game = new PrismView(this);
        setContentView(game);
    }

    @Override protected void onResume() {
        super.onResume();
        if (game != null) game.resumeGameAudio();
    }

    @Override protected void onPause() {
        if (game != null) game.pauseGameAudio();
        super.onPause();
    }

    private static final class PrismView extends View {
        private static final int SIZE = 8;
        private static final int TYPES = 6;
        private static final int HOME = 0, PLAYING = 1, RESULT = 2, PAUSED = 3, SETTINGS = 4, PRIVACY = 5;
        private static final int NONE = 0, ROCKET_H = 1, ROCKET_V = 2, NOVA = 3, PRISM = 4, CROSS = 5;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Typeface bold = Typeface.create("sans-serif-black", Typeface.BOLD);
        private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        private final Random random = new Random();
        private final SharedPreferences prefs;
        private final int[][] tiles = new int[SIZE][SIZE];
        private final int[][] specials = new int[SIZE][SIZE];
        private final int[][] ice = new int[SIZE][SIZE];
        private final ArrayList<Particle> particles = new ArrayList<>();
        private final RectF pauseRect = new RectF();
        private final RectF[] boosterRects = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};
        private final int[] boosterCounts = {3, 2, 2, 2, 1, 1};
        private final int[] gemColors = {
                Color.rgb(255, 83, 117), Color.rgb(255, 181, 62),
                Color.rgb(80, 224, 158), Color.rgb(56, 200, 229),
                Color.rgb(84, 132, 255), Color.rgb(183, 112, 255)
        };
        private final int[] gemLight = {
                Color.rgb(255, 174, 190), Color.rgb(255, 220, 139),
                Color.rgb(172, 255, 214), Color.rgb(163, 246, 255),
                Color.rgb(184, 207, 255), Color.rgb(225, 191, 255)
        };
        private final int[] gemDark = {
                Color.rgb(177, 35, 82), Color.rgb(194, 100, 22),
                Color.rgb(20, 145, 104), Color.rgb(15, 117, 163),
                Color.rgb(40, 68, 171), Color.rgb(105, 51, 177)
        };

        private int screen = HOME;
        private int level = 1;
        private int bestScore = 0;
        private int score = 0;
        private int targetScore = 4200;
        private int moves = 25;
        private int iceRemaining = 0;
        private int stars = 0;
        private int selectedX = -1, selectedY = -1;
        private int activeBooster = -1;
        private int hintAX = -1, hintAY = -1, hintBX = -1, hintBY = -1;
        private int resultBest = 0;
        private boolean won = false;
        private boolean musicEnabled = true;
        private boolean soundEnabled = true;
        private boolean tutorialPending = false;
        private long lastActionAt = 0L;
        private long lastFrameAt = 0L;
        private long lastHintCheck = 0L;
        private float touchDownX, touchDownY;
        private SwapFx swapFx;
        private float width, height, scale, boardX, boardY, boardSize, cell, sideX, sideW;
        private boolean wideLayout;
        private RectF playRect = new RectF();
        private RectF settingsRect = new RectF();
        private RectF resultRect = new RectF();
        private RectF homeRect = new RectF();
        private ToneGenerator tone;
        private MusicEngine music;

        PrismView(Context context) {
            super(context);
            setFocusable(true);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            prefs = context.getSharedPreferences("prism_pop_save", Context.MODE_PRIVATE);
            level = Math.max(1, prefs.getInt("level", 1));
            bestScore = Math.max(0, prefs.getInt("best", 0));
            musicEnabled = prefs.getBoolean("music", true);
            soundEnabled = prefs.getBoolean("sound", true);
            try { tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 55); } catch (Throwable ignored) {}
            music = new MusicEngine();
            textPaint.setTypeface(medium);
            lastActionAt = SystemClock.uptimeMillis();
            if (musicEnabled) music.start();
            setContentDescription("Prism Pop match three puzzle game. Tap Play to begin.");
        }

        void resumeGameAudio() { if (musicEnabled && music != null) music.start(); }
        void pauseGameAudio() { if (music != null) music.stop(); }

        private void save() {
            prefs.edit().putInt("level", level).putInt("best", bestScore)
                    .putBoolean("music", musicEnabled).putBoolean("sound", soundEnabled).apply();
        }

        private void startLevel() {
            score = 0;
            moves = 25;
            targetScore = 3900 + Math.min(5200, (level - 1) * 260);
            iceRemaining = 0;
            selectedX = selectedY = -1;
            activeBooster = -1;
            hintAX = hintAY = hintBX = hintBY = -1;
            particles.clear();
            int[] startingBoosters = {3, 2, 2, 2, 1, 1};
            System.arraycopy(startingBoosters, 0, boosterCounts, 0, boosterCounts.length);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    specials[y][x] = NONE;
                    ice[y][x] = 0;
                    int value;
                    int guard = 0;
                    do {
                        value = random.nextInt(TYPES);
                        guard++;
                    } while (guard < 30 && createsInitialMatch(x, y, value));
                    tiles[y][x] = value;
                }
            }
            int iceCount = level < 3 ? 0 : Math.min(18, 4 + level * 2);
            int placed = 0, guard = 0;
            while (placed < iceCount && guard++ < 500) {
                int x = random.nextInt(SIZE), y = random.nextInt(SIZE);
                if (ice[y][x] == 0) {
                    ice[y][x] = 1;
                    placed++;
                }
            }
            iceRemaining = placed;
            screen = PLAYING;
            tutorialPending = level == 1 && !prefs.getBoolean("tutorial_seen", false);
            if (tutorialPending) prefs.edit().putBoolean("tutorial_seen", true).apply();
            lastActionAt = SystemClock.uptimeMillis();
            playTone(ToneGenerator.TONE_PROP_ACK, 90);
            announce("Level " + level + ". Make matches and clear the crystal frost.");
            invalidate();
        }

        private boolean createsInitialMatch(int x, int y, int type) {
            return (x >= 2 && tiles[y][x - 1] == type && tiles[y][x - 2] == type)
                    || (y >= 2 && tiles[y - 1][x] == type && tiles[y - 2][x] == type);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (getWidth() <= 0 || getHeight() <= 0) return;
            scale = Math.min(getWidth() / 420f, getHeight() / 780f);
            if (scale <= 0f) scale = 1f;
            width = getWidth() / scale;
            height = getHeight() / scale;
            wideLayout = width > height * 1.12f;
            canvas.save();
            canvas.scale(scale, scale);
            drawBackground(canvas);
            if (screen == HOME) drawHome(canvas);
            else if (screen == PLAYING) drawGame(canvas);
            else if (screen == RESULT) drawResult(canvas);
            else if (screen == PAUSED) drawPause(canvas);
            else if (screen == SETTINGS) drawSettings(canvas);
            else if (screen == PRIVACY) drawPrivacy(canvas);
            drawParticles(canvas);
            canvas.restore();
            if (!particles.isEmpty() || screen == PLAYING || swapFx != null) postInvalidateDelayed(16);
        }

        private void drawBackground(Canvas c) {
            paint.setShader(new LinearGradient(0, 0, width, height,
                    new int[]{Color.rgb(21, 16, 54), Color.rgb(35, 22, 78), Color.rgb(12, 28, 58)},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, width, height, paint);
            paint.setShader(null);
            for (int i = 0; i < 16; i++) {
                float x = (i * 89f + 31f) % Math.max(1f, width);
                float y = (i * 137f + 47f) % Math.max(1f, height);
                paint.setColor(Color.argb((i % 3 == 0) ? 95 : 45, 206, 207, 255));
                c.drawCircle(x, y, (i % 4 == 0) ? 2.2f : 1.1f, paint);
            }
            glow(c, width * 0.12f, height * 0.16f, 150, Color.rgb(103, 70, 220), 60);
            glow(c, width * 0.91f, height * 0.77f, 170, Color.rgb(24, 174, 190), 45);
        }

        private void glow(Canvas c, float x, float y, float radius, int color, int alpha) {
            paint.setShader(new RadialGradient(x, y, radius,
                    new int[]{Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, radius, paint);
            paint.setShader(null);
        }

        private void drawHome(Canvas c) {
            float cx = width / 2f;
            float top = Math.max(58, height * 0.12f);
            drawBrand(c, cx, top + 12);
            drawFestivalBadge(c, cx, top + 126);
            text(c, "A LITTLE MATCH. A BIG MAGIC.", cx, top + 185, 13, Color.rgb(191, 190, 231), true, Paint.Align.CENTER);
            text(c, "Your next favourite", cx, top + 238, 25, Color.WHITE, true, Paint.Align.CENTER);
            text(c, "five-minute adventure.", cx, top + 270, 25, Color.WHITE, true, Paint.Align.CENTER);
            float cardY = top + 310;
            rounded(c, cx - 142, cardY, cx + 142, cardY + 80, 22, Color.argb(210, 255, 255, 255));
            text(c, "WORLD  " + String.format("%02d", level), cx - 105, cardY + 31, 12, Color.rgb(98, 86, 145), true, Paint.Align.LEFT);
            text(c, "BEST " + compact(bestScore), cx - 105, cardY + 56, 17, Color.rgb(36, 28, 79), true, Paint.Align.LEFT);
            drawMiniGem(c, cx + 92, cardY + 40, 18, 3);
            float bw = 264;
            playRect.set(cx - bw / 2, cardY + 111, cx + bw / 2, cardY + 177);
            button(c, playRect, "PLAY", Color.rgb(255, 190, 73), Color.rgb(71, 42, 108), 23);
            rounded(c, cx - 90, playRect.bottom + 17, cx + 90, playRect.bottom + 63, 18, Color.argb(80, 255, 255, 255));
            text(c, "HOW TO PLAY", cx, playRect.bottom + 46, 13, Color.WHITE, true, Paint.Align.CENTER);
            settingsRect.set(cx - 28, height - 58, cx + 28, height - 10);
            rounded(c, settingsRect.left, settingsRect.top, settingsRect.right, settingsRect.bottom, 18, Color.argb(70, 255, 255, 255));
            drawGear(c, cx, settingsRect.centerY(), 10, Color.WHITE);
            text(c, "OFFLINE • NO ENERGY TIMER", cx, height - 77, 10, Color.argb(165, 255, 255, 255), true, Paint.Align.CENTER);
        }

        private void drawBrand(Canvas c, float cx, float y) {
            float s = 37;
            drawMiniGem(c, cx - 126, y, s * 0.53f, 4);
            drawMiniGem(c, cx + 126, y, s * 0.53f, 1);
            text(c, "PRISM", cx, y + 2, 43, Color.WHITE, true, Paint.Align.CENTER);
            text(c, "POP", cx, y + 43, 43, Color.rgb(255, 201, 94), true, Paint.Align.CENTER);
            text(c, "CRYSTAL CARNIVAL", cx, y + 68, 10, Color.rgb(190, 174, 255), true, Paint.Align.CENTER);
        }

        private void drawFestivalBadge(Canvas c, float cx, float y) {
            glow(c, cx, y, 75, Color.rgb(255, 126, 98), 65);
            for (int i = 0; i < 7; i++) {
                double a = Math.PI * 2 * i / 7.0;
                drawMiniGem(c, cx + (float)Math.cos(a) * 56, y + (float)Math.sin(a) * 31, 10, i % TYPES);
            }
            rounded(c, cx - 58, y - 25, cx + 58, y + 25, 16, Color.argb(190, 255, 255, 255));
            text(c, "LEVEL " + level, cx, y + 6, 18, Color.rgb(69, 43, 117), true, Paint.Align.CENTER);
        }

        private void drawGame(Canvas c) {
            computeBoardLayout();
            long now = SystemClock.uptimeMillis();
            if (screen == PLAYING && selectedX < 0 && activeBooster < 0
                    && now - lastActionAt > 6500 && now - lastHintCheck > 2500 && hintAX < 0) {
                lastHintCheck = now;
                findHint();
            }
            drawHeader(c);
            drawBoard(c);
            drawBoosters(c);
            if (tutorialPending) drawTutorial(c);
            if (SystemClock.uptimeMillis() - lastActionAt > 8500 && hintAX >= 0 && selectedX < 0 && activeBooster < 0) {
                rounded(c, width / 2f - 110, boardY + boardSize + 9, width / 2f + 110, boardY + boardSize + 39, 14, Color.argb(190, 255, 255, 255));
                text(c, "TRY THE GLOWING SWAP", width / 2f, boardY + boardSize + 29, 11, Color.rgb(70, 48, 117), true, Paint.Align.CENTER);
            }
        }

        private void computeBoardLayout() {
            if (wideLayout) {
                boardSize = Math.min(360, Math.min(height - 94, width * 0.47f));
                boardX = 28;
                boardY = (height - boardSize) / 2f;
                sideX = boardX + boardSize + 28;
                sideW = Math.max(170, width - sideX - 28);
            } else {
                boardSize = Math.min(390, Math.min(width - 30, height - 300));
                boardX = (width - boardSize) / 2f;
                boardY = Math.max(176, Math.min(216, height - boardSize - 205));
                sideX = 0;
                sideW = width;
            }
            cell = boardSize / SIZE;
        }

        private void drawHeader(Canvas c) {
            if (wideLayout) {
                text(c, "PRISM POP", 28, 34, 21, Color.WHITE, true, Paint.Align.LEFT);
                text(c, "LEVEL " + level, sideX, 42, 17, Color.rgb(255, 210, 107), true, Paint.Align.LEFT);
                rounded(c, sideX, 58, sideX + sideW - 8, 126, 19, Color.argb(80, 255, 255, 255));
                text(c, "SCORE", sideX + 15, 82, 10, Color.rgb(197, 191, 229), true, Paint.Align.LEFT);
                text(c, compact(score), sideX + 15, 111, 22, Color.WHITE, true, Paint.Align.LEFT);
                text(c, "MOVES  " + moves, sideX + sideW - 18, 96, 15, Color.WHITE, true, Paint.Align.RIGHT);
                drawProgress(c, sideX, 145, sideW - 12, 9);
                pauseRect.set(sideX + sideW - 48, 12, sideX + sideW - 4, 51);
                drawPauseIcon(c, pauseRect);
            } else {
                text(c, "PRISM POP", 20, 37, 20, Color.WHITE, true, Paint.Align.LEFT);
                text(c, "LEVEL " + level, width - 20, 37, 14, Color.rgb(255, 211, 117), true, Paint.Align.RIGHT);
                pauseRect.set(width - 62, 49, width - 20, 90);
                drawPauseIcon(c, pauseRect);
                rounded(c, 18, 54, width - 75, 119, 16, Color.argb(75, 255, 255, 255));
                text(c, "SCORE", 33, 76, 10, Color.rgb(197, 191, 229), true, Paint.Align.LEFT);
                text(c, compact(score), 33, 105, 22, Color.WHITE, true, Paint.Align.LEFT);
                text(c, "GOAL", width * 0.53f, 76, 10, Color.rgb(197, 191, 229), true, Paint.Align.LEFT);
                text(c, compact(targetScore), width * 0.53f, 105, 17, Color.WHITE, true, Paint.Align.LEFT);
                rounded(c, width - 74, 54, width - 18, 119, 16, Color.rgb(255, 196, 82));
                text(c, "MOVES", width - 46, 76, 9, Color.rgb(106, 60, 92), true, Paint.Align.CENTER);
                text(c, String.valueOf(moves), width - 46, 106, 24, Color.rgb(67, 39, 89), true, Paint.Align.CENTER);
                drawProgress(c, 20, 133, width - 40, 7);
            }
        }

        private void drawProgress(Canvas c, float x, float y, float w, float h) {
            rounded(c, x, y, x + w, y + h, h / 2, Color.argb(55, 255, 255, 255));
            float f = Math.min(1f, score / (float)Math.max(1, targetScore));
            rounded(c, x, y, x + Math.max(h, w * f), y + h, h / 2, Color.rgb(255, 200, 89));
        }

        private void drawPauseIcon(Canvas c, RectF r) {
            rounded(c, r.left, r.top, r.right, r.bottom, 13, Color.argb(80, 255, 255, 255));
            paint.setColor(Color.WHITE);
            c.drawRoundRect(r.centerX() - 7, r.centerY() - 8, r.centerX() - 2, r.centerY() + 8, 2, 2, paint);
            c.drawRoundRect(r.centerX() + 2, r.centerY() - 8, r.centerX() + 7, r.centerY() + 8, 2, 2, paint);
        }

        private void drawBoard(Canvas c) {
            rounded(c, boardX - 8, boardY - 8, boardX + boardSize + 8, boardY + boardSize + 8, 23, Color.argb(95, 3, 5, 31));
            rounded(c, boardX - 4, boardY - 4, boardX + boardSize + 4, boardY + boardSize + 4, 20, Color.rgb(91, 70, 142));
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    float l = boardX + x * cell + 2;
                    float t = boardY + y * cell + 2;
                    float r = boardX + (x + 1) * cell - 2;
                    float b = boardY + (y + 1) * cell - 2;
                    rounded(c, l, t, r, b, Math.max(5, cell * 0.16f),
                            ((x + y) & 1) == 0 ? Color.rgb(49, 43, 103) : Color.rgb(43, 39, 94));
                    boolean swapCell = swapFx != null && swapFx.isCell(x, y);
                    if (tiles[y][x] >= 0 && !swapCell) drawGem(c, boardX + (x + 0.5f) * cell, boardY + (y + 0.5f) * cell, cell * 0.36f, tiles[y][x], specials[y][x]);
                    if (ice[y][x] > 0) drawIce(c, boardX + (x + 0.5f) * cell, boardY + (y + 0.5f) * cell, cell * 0.45f);
                    if (selectedX == x && selectedY == y) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(Math.max(2.5f, cell * 0.065f));
                        paint.setColor(Color.rgb(255, 226, 126));
                        c.drawRoundRect(l + 1, t + 1, r - 1, b - 1, 7, 7, paint);
                        paint.setStyle(Paint.Style.FILL);
                    }
                    if ((x == hintAX && y == hintAY) || (x == hintBX && y == hintBY)) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2f);
                        paint.setColor(Color.argb(170, 255, 255, 255));
                        c.drawRoundRect(l + 3, t + 3, r - 3, b - 3, 8, 8, paint);
                        paint.setStyle(Paint.Style.FILL);
                    }
                }
            }
            drawSwapAnimation(c);
            if (iceRemaining > 0) {
                text(c, "FROST " + iceRemaining, boardX + boardSize - 6, boardY + boardSize + 22, 10, Color.rgb(187, 231, 255), true, Paint.Align.RIGHT);
            }
        }

        private void drawSwapAnimation(Canvas c) {
            if (swapFx == null) return;
            float p = Math.min(1f, (SystemClock.uptimeMillis() - swapFx.startedAt) / 190f);
            if (p >= 1f) { swapFx = null; return; }
            float eased = 1f - (1f - p) * (1f - p);
            float ax = boardX + (swapFx.ax + 0.5f) * cell;
            float ay = boardY + (swapFx.ay + 0.5f) * cell;
            float bx = boardX + (swapFx.bx + 0.5f) * cell;
            float by = boardY + (swapFx.by + 0.5f) * cell;
            float firstX = ax + (bx - ax) * eased;
            float firstY = ay + (by - ay) * eased;
            float secondX = bx + (ax - bx) * eased;
            float secondY = by + (ay - by) * eased;
            drawGem(c, firstX, firstY, cell * 0.36f, swapFx.firstGem, swapFx.firstSpecial);
            drawGem(c, secondX, secondY, cell * 0.36f, swapFx.secondGem, swapFx.secondSpecial);
        }

        private void drawGem(Canvas c, float cx, float cy, float r, int type, int special) {
            type = Math.max(0, Math.min(TYPES - 1, type));
            int color = gemColors[type];
            glow(c, cx, cy, r * 1.45f, color, 32);
            Path path = new Path();
            switch (type) {
                case 0:
                    path.addCircle(cx, cy, r * 0.94f, Path.Direction.CW);
                    break;
                case 1:
                    path.moveTo(cx, cy - r * 1.12f); path.lineTo(cx + r * 0.9f, cy);
                    path.lineTo(cx, cy + r * 1.12f); path.lineTo(cx - r * 0.9f, cy); path.close();
                    break;
                case 2:
                    path.moveTo(cx - r * .78f, cy - r * .92f); path.lineTo(cx + r * .78f, cy - r * .92f);
                    path.lineTo(cx + r, cy - r * .18f); path.lineTo(cx + r * .58f, cy + r * .92f);
                    path.lineTo(cx - r * .58f, cy + r * .92f); path.lineTo(cx - r, cy - r * .18f); path.close();
                    break;
                case 3:
                    path.moveTo(cx, cy - r * 1.08f); path.cubicTo(cx + r * 1.2f, cy - r * .45f, cx + r * .9f, cy + r, cx, cy + r);
                    path.cubicTo(cx - r * .9f, cy + r, cx - r * 1.2f, cy - r * .45f, cx, cy - r * 1.08f); path.close();
                    break;
                case 4:
                    path.addRoundRect(new RectF(cx - r, cy - r * .82f, cx + r, cy + r * .82f), r * .35f, r * .35f, Path.Direction.CW);
                    break;
                default:
                    for (int i = 0; i < 6; i++) {
                        double a = Math.PI / 3 * i - Math.PI / 2;
                        float px = cx + (float)Math.cos(a) * r;
                        float py = cy + (float)Math.sin(a) * r;
                        if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
                    }
                    path.close();
                    break;
            }
            paint.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r,
                    new int[]{gemLight[type], color, gemDark[type]}, null, Shader.TileMode.CLAMP));
            paint.setShadowLayer(5, 0, 3, Color.argb(120, 0, 0, 0));
            c.drawPath(path, paint);
            paint.clearShadowLayer();
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, r * .055f));
            paint.setColor(Color.argb(145, 255, 255, 255));
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(cx - r, cy - r, cx + r * .5f, cy,
                    Color.argb(140, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP));
            c.drawCircle(cx - r * .2f, cy - r * .34f, r * .23f, paint);
            paint.setShader(null);
            if (special != NONE) drawSpecial(c, cx, cy, r, special);
        }

        private void drawSpecial(Canvas c, float cx, float cy, float r, int type) {
            paint.setColor(Color.argb(235, 255, 255, 255));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.8f, r * .13f));
            if (type == ROCKET_H || type == CROSS) {
                c.drawLine(cx - r * .58f, cy, cx + r * .58f, cy, paint);
                c.drawLine(cx - r * .28f, cy - r * .25f, cx - r * .58f, cy, paint);
                c.drawLine(cx - r * .28f, cy + r * .25f, cx - r * .58f, cy, paint);
            }
            if (type == ROCKET_V || type == CROSS) {
                c.drawLine(cx, cy - r * .58f, cx, cy + r * .58f, paint);
                c.drawLine(cx - r * .25f, cy - r * .28f, cx, cy - r * .58f, paint);
                c.drawLine(cx + r * .25f, cy - r * .28f, cx, cy - r * .58f, paint);
            }
            if (type == NOVA) {
                c.drawCircle(cx, cy, r * .52f, paint);
                c.drawLine(cx - r * .7f, cy, cx + r * .7f, cy, paint);
                c.drawLine(cx, cy - r * .7f, cx, cy + r * .7f, paint);
            }
            if (type == PRISM) {
                Path star = new Path();
                star.moveTo(cx, cy - r * .72f); star.lineTo(cx + r * .18f, cy - r * .16f);
                star.lineTo(cx + r * .72f, cy); star.lineTo(cx + r * .18f, cy + r * .16f);
                star.lineTo(cx, cy + r * .72f); star.lineTo(cx - r * .18f, cy + r * .16f);
                star.lineTo(cx - r * .72f, cy); star.lineTo(cx - r * .18f, cy - r * .16f); star.close();
                c.drawPath(star, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawIce(Canvas c, float cx, float cy, float r) {
            paint.setColor(Color.argb(95, 182, 231, 255));
            c.drawCircle(cx, cy, r, paint);
            paint.setColor(Color.argb(180, 232, 250, 255));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.4f);
            c.drawLine(cx - r * .48f, cy - r * .35f, cx + r * .42f, cy + r * .4f, paint);
            c.drawLine(cx + r * .38f, cy - r * .5f, cx - r * .35f, cy + r * .48f, paint);
            c.drawCircle(cx, cy, r * .7f, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawBoosters(Canvas c) {
            String[] labels = {"HAMMER", "PRISM", "MIX", "+5", "ROW", "COLUMN"};
            int[] colors = {
                    Color.rgb(255, 122, 142), Color.rgb(164, 117, 255),
                    Color.rgb(77, 202, 215), Color.rgb(255, 191, 81),
                    Color.rgb(88, 190, 255), Color.rgb(104, 220, 170)
            };
            if (wideLayout) {
                text(c, "POWER UPS", sideX, 184, 12, Color.rgb(198, 188, 244), true, Paint.Align.LEFT);
                float bw = Math.min(135, sideW - 8);
                float bh = 51;
                for (int i = 0; i < 6; i++) {
                    float x = sideX + (sideW - bw) / 2;
                    float y = 198 + i * 58;
                    boosterRects[i].set(x, y, x + bw, y + bh);
                    drawBoosterButton(c, boosterRects[i], i, labels[i], colors[i]);
                }
                text(c, activeBooster >= 0 ? "TAP A GEM TO USE" : "SWIPE OR TAP TO MATCH", sideX + sideW / 2, 565, 9, Color.rgb(202, 196, 232), true, Paint.Align.CENTER);
            } else {
                text(c, "POWER UPS", 20, height - 157, 11, Color.rgb(200, 191, 238), true, Paint.Align.LEFT);
                float gap = 7;
                float bw = (width - 40 - gap * 2) / 3f;
                float bh = 48;
                float startY = height - 150;
                for (int i = 0; i < 6; i++) {
                    int col = i % 3;
                    int row = i / 3;
                    float x = 20 + col * (bw + gap);
                    float y = startY + row * 54;
                    boosterRects[i].set(x, y, x + bw, y + bh);
                    drawBoosterButton(c, boosterRects[i], i, labels[i], colors[i]);
                }
                text(c, activeBooster >= 0 ? "CHOOSE A GEM ON THE BOARD" : "SWIPE • MATCH • CASCADE", width / 2, height - 12, 9, Color.rgb(199, 192, 231), true, Paint.Align.CENTER);
            }
        }

        private void drawBoosterButton(Canvas c, RectF r, int index, String label, int color) {
            int bg = activeBooster == index ? Color.rgb(255, 235, 174) : Color.argb(220, 255, 255, 255);
            rounded(c, r.left, r.top + 3, r.right, r.bottom, 15, bg);
            drawBoosterIcon(c, r.centerX(), r.top + (wideLayout ? 18 : 15), Math.min(13, r.width() * .15f), index, color);
            text(c, label, r.centerX(), r.bottom - 7, wideLayout ? 9 : 8, Color.rgb(72, 53, 119), true, Paint.Align.CENTER);
            paint.setColor(color);
            c.drawCircle(r.right - 7, r.top + 7, 10, paint);
            text(c, String.valueOf(boosterCounts[index]), r.right - 7, r.top + 11, 10, Color.WHITE, true, Paint.Align.CENTER);
        }

        private void drawBoosterIcon(Canvas c, float x, float y, float r, int index, int color) {
            paint.setColor(color);
            paint.setStrokeWidth(3.2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            if (index == 0) {
                c.save(); c.rotate(-35, x, y);
                c.drawRoundRect(x - r * .25f, y - r, x + r * .25f, y + r * .35f, 3, 3, paint);
                c.drawRoundRect(x - r * .75f, y + r * .15f, x + r * .75f, y + r * .55f, 3, 3, paint);
                c.restore();
            } else if (index == 1) {
                Path path = new Path(); path.moveTo(x, y - r); path.lineTo(x + r, y);
                path.lineTo(x, y + r); path.lineTo(x - r, y); path.close();
                c.drawPath(path, paint);
                paint.setColor(Color.WHITE); c.drawCircle(x, y, r * .22f, paint);
            } else if (index == 2) {
                c.drawLine(x - r, y - r * .6f, x + r, y + r * .6f, paint);
                c.drawLine(x - r, y + r * .6f, x + r, y - r * .6f, paint);
                c.drawCircle(x, y, r * .24f, paint);
            } else if (index == 3) {
                text(c, "+5", x, y + r * .38f, r * 1.2f, Color.rgb(100, 57, 108), true, Paint.Align.CENTER);
            } else if (index == 4) {
                c.drawLine(x - r, y, x + r, y, paint);
                c.drawLine(x + r * .35f, y - r * .45f, x + r, y, paint);
                c.drawLine(x + r * .35f, y + r * .45f, x + r, y, paint);
            } else {
                c.drawLine(x, y - r, x, y + r, paint);
                c.drawLine(x - r * .45f, y + r * .35f, x, y + r, paint);
                c.drawLine(x + r * .45f, y + r * .35f, x, y + r, paint);
            }
        }

        private void drawTutorial(Canvas c) {
            paint.setColor(Color.argb(145, 9, 8, 30));
            c.drawRect(0, 0, width, height, paint);
            float x = width / 2;
            float y = Math.min(height - 170, boardY + boardSize + 65);
            rounded(c, x - 145, y - 45, x + 145, y + 65, 20, Color.WHITE);
            text(c, "SWAP TWO NEIGHBOURING GEMS", x, y - 8, 13, Color.rgb(75, 53, 120), true, Paint.Align.CENTER);
            text(c, "Match 3 • make special gems •", x, y + 16, 12, Color.rgb(96, 83, 134), false, Paint.Align.CENTER);
            text(c, "clear the frost before moves run out.", x, y + 36, 12, Color.rgb(96, 83, 134), false, Paint.Align.CENTER);
            rounded(c, x - 52, y + 45, x + 52, y + 75, 12, Color.rgb(255, 195, 79));
            text(c, "GOT IT", x, y + 65, 11, Color.rgb(72, 47, 104), true, Paint.Align.CENTER);
        }

        private void drawResult(Canvas c) {
            drawGame(c);
            paint.setColor(Color.argb(185, 10, 8, 34));
            c.drawRect(0, 0, width, height, paint);
            float cx = width / 2, cy = height / 2;
            rounded(c, cx - 155, cy - 180, cx + 155, cy + 168, 27, Color.rgb(250, 247, 255));
            if (won) {
                text(c, "LEVEL COMPLETE!", cx, cy - 125, 25, Color.rgb(77, 47, 135), true, Paint.Align.CENTER);
                text(c, "YOU MADE A SPECTACLE.", cx, cy - 91, 11, Color.rgb(125, 103, 158), true, Paint.Align.CENTER);
                for (int i = 0; i < 3; i++) drawStar(c, cx + (i - 1) * 53, cy - 42, 19, i < stars ? Color.rgb(255, 190, 61) : Color.rgb(216, 208, 224));
                text(c, compact(score) + " POINTS", cx, cy + 3, 20, Color.rgb(66, 49, 113), true, Paint.Align.CENTER);
                text(c, "WORLD " + level + " CLEARED", cx, cy + 31, 12, Color.rgb(129, 109, 158), true, Paint.Align.CENTER);
                resultRect.set(cx - 118, cy + 63, cx + 118, cy + 121);
                button(c, resultRect, "NEXT LEVEL", Color.rgb(255, 190, 73), Color.rgb(73, 45, 111), 16);
            } else {
                text(c, "ONE MORE TRY?", cx, cy - 116, 25, Color.rgb(77, 47, 135), true, Paint.Align.CENTER);
                text(c, "The gems are waiting for you.", cx, cy - 78, 12, Color.rgb(125, 103, 158), false, Paint.Align.CENTER);
                text(c, compact(score) + " / " + compact(targetScore), cx, cy - 23, 20, Color.rgb(66, 49, 113), true, Paint.Align.CENTER);
                resultRect.set(cx - 118, cy + 22, cx + 118, cy + 80);
                button(c, resultRect, "RETRY", Color.rgb(255, 190, 73), Color.rgb(73, 45, 111), 16);
            }
            homeRect.set(cx - 76, cy + 133, cx + 76, cy + 166);
            text(c, "HOME", cx, cy + 155, 11, Color.rgb(109, 88, 147), true, Paint.Align.CENTER);
        }

        private void drawPause(Canvas c) {
            drawGame(c);
            paint.setColor(Color.argb(175, 10, 8, 34)); c.drawRect(0, 0, width, height, paint);
            float cx = width / 2, cy = height / 2;
            rounded(c, cx - 140, cy - 135, cx + 140, cy + 132, 25, Color.rgb(250, 247, 255));
            text(c, "TAKE A BREATHER", cx, cy - 83, 22, Color.rgb(77, 47, 135), true, Paint.Align.CENTER);
            text(c, "Your progress is safe.", cx, cy - 50, 12, Color.rgb(125, 103, 158), false, Paint.Align.CENTER);
            RectF resume = new RectF(cx - 105, cy - 15, cx + 105, cy + 42);
            button(c, resume, "KEEP PLAYING", Color.rgb(255, 190, 73), Color.rgb(73, 45, 111), 13);
            RectF home = new RectF(cx - 105, cy + 57, cx + 105, cy + 105);
            button(c, home, "BACK TO HOME", Color.rgb(224, 217, 241), Color.rgb(73, 58, 115), 12);
        }

        private void drawSettings(Canvas c) {
            drawHome(c);
            paint.setColor(Color.argb(185, 10, 8, 34)); c.drawRect(0, 0, width, height, paint);
            float cx = width / 2, cy = height / 2;
            rounded(c, cx - 150, cy - 145, cx + 150, cy + 145, 25, Color.rgb(250, 247, 255));
            text(c, "GAME SETTINGS", cx, cy - 98, 22, Color.rgb(77, 47, 135), true, Paint.Align.CENTER);
            RectF musicR = new RectF(cx - 112, cy - 61, cx + 112, cy - 7);
            RectF soundR = new RectF(cx - 112, cy + 3, cx + 112, cy + 57);
            RectF privacyR = new RectF(cx - 112, cy + 67, cx + 112, cy + 121);
            button(c, musicR, "MUSIC   " + (musicEnabled ? "ON" : "OFF"), musicEnabled ? Color.rgb(255, 218, 143) : Color.rgb(224, 217, 241), Color.rgb(73, 58, 115), 14);
            button(c, soundR, "SOUND   " + (soundEnabled ? "ON" : "OFF"), soundEnabled ? Color.rgb(255, 218, 143) : Color.rgb(224, 217, 241), Color.rgb(73, 58, 115), 14);
            button(c, privacyR, "PRIVACY POLICY", Color.rgb(224, 217, 241), Color.rgb(73, 58, 115), 13);
            text(c, "CLOSE", cx, cy + 174, 12, Color.WHITE, true, Paint.Align.CENTER);
        }

        private void drawPrivacy(Canvas c) {
            drawHome(c);
            paint.setColor(Color.argb(190, 10, 8, 34)); c.drawRect(0, 0, width, height, paint);
            float cx = width / 2f, cy = height / 2f;
            rounded(c, cx - 158, cy - 196, cx + 158, cy + 184, 24, Color.rgb(250, 247, 255));
            text(c, "PRIVACY POLICY", cx, cy - 151, 20, Color.rgb(77, 47, 135), true, Paint.Align.CENTER);
            text(c, "PRISM POP IS OFFLINE", cx, cy - 111, 11, Color.rgb(115, 87, 160), true, Paint.Align.CENTER);
            String[] lines = {
                    "No account is needed.",
                    "No ads or analytics are included.",
                    "No personal data is collected or sent.",
                    "Your level and score stay in this app",
                    "on this device. Uninstalling removes",
                    "the local save. No sensitive permissions",
                    "or online services are used."
            };
            for (int i = 0; i < lines.length; i++)
                text(c, lines[i], cx, cy - 62 + i * 25, 12, Color.rgb(90, 78, 121), false, Paint.Align.CENTER);
            text(c, "PRIVACY QUESTIONS", cx, cy + 126, 10, Color.rgb(115, 87, 160), true, Paint.Align.CENTER);
            text(c, "github.com/souravmahato2011371/Game_Engine", cx, cy + 150, 8, Color.rgb(90, 78, 121), false, Paint.Align.CENTER);
            text(c, "TAP TO CLOSE", cx, cy + 213, 11, Color.WHITE, true, Paint.Align.CENTER);
        }

        private void drawStar(Canvas c, float cx, float cy, float r, int color) {
            Path path = new Path();
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * i / 5 - Math.PI / 2;
                float rr = (i & 1) == 0 ? r : r * .43f;
                float x = cx + (float)Math.cos(a) * rr;
                float y = cy + (float)Math.sin(a) * rr;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close(); paint.setColor(color); c.drawPath(path, paint);
        }

        private void drawMiniGem(Canvas c, float cx, float cy, float r, int type) {
            int col = gemColors[Math.max(0, Math.min(TYPES - 1, type))];
            paint.setColor(col);
            Path p = new Path();
            p.moveTo(cx, cy - r); p.lineTo(cx + r * .83f, cy);
            p.lineTo(cx, cy + r); p.lineTo(cx - r * .83f, cy); p.close();
            c.drawPath(p, paint);
            paint.setColor(Color.argb(150, 255, 255, 255));
            c.drawCircle(cx - r * .18f, cy - r * .2f, r * .18f, paint);
        }

        private void drawGear(Canvas c, float x, float y, float r, int color) {
            paint.setColor(color); paint.setStrokeWidth(3); paint.setStyle(Paint.Style.STROKE);
            c.drawCircle(x, y, r * .72f, paint);
            c.drawCircle(x, y, r * .25f, paint);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * i / 4;
                c.drawLine(x + (float)Math.cos(a) * r * .82f, y + (float)Math.sin(a) * r * .82f,
                        x + (float)Math.cos(a) * r * 1.15f, y + (float)Math.sin(a) * r * 1.15f, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        private void button(Canvas c, RectF r, String label, int bg, int fg, float size) {
            rounded(c, r.left, r.top + 4, r.right, r.bottom, 18, Color.argb(85, 0, 0, 0));
            rounded(c, r.left, r.top, r.right, r.bottom - 4, 18, bg);
            text(c, label, r.centerX(), r.centerY() + size * .35f - 2, size, fg, true, Paint.Align.CENTER);
        }

        private void rounded(Canvas c, float l, float t, float r, float b, float radius, int color) {
            paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.clearShadowLayer();
            c.drawRoundRect(l, t, r, b, radius, radius, paint);
        }

        private void text(Canvas c, String value, float x, float y, float size, int color, boolean heavy, Paint.Align align) {
            textPaint.setTypeface(heavy ? bold : medium);
            textPaint.setTextSize(size);
            textPaint.setColor(color);
            textPaint.setTextAlign(align);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setShader(null);
            c.drawText(value, x, y, textPaint);
        }

        private String compact(int value) {
            if (value >= 1000000) return String.format(java.util.Locale.US, "%.1fM", value / 1000000f);
            if (value >= 10000) return String.format(java.util.Locale.US, "%.1fk", value / 1000f);
            return String.valueOf(value);
        }

        private void drawParticles(Canvas c) {
            long now = SystemClock.uptimeMillis();
            float dt = lastFrameAt == 0 ? .016f : Math.min(.04f, (now - lastFrameAt) / 1000f);
            lastFrameAt = now;
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle a = particles.get(i);
                a.life -= dt;
                if (a.life <= 0) { particles.remove(i); continue; }
                a.x += a.vx * dt;
                a.y += a.vy * dt;
                a.vy += 65 * dt;
                paint.setColor(Color.argb((int)(255 * Math.max(0, a.life / a.maxLife)), a.red, a.green, a.blue));
                c.drawCircle(a.x, a.y, a.radius * Math.max(.15f, a.life / a.maxLife), paint);
            }
        }

        private void burst(float x, float y, int color, int count) {
            for (int i = 0; i < count && particles.size() < 220; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                float speed = 35 + random.nextFloat() * 100;
                int col = color;
                particles.add(new Particle(x, y, (float)Math.cos(a) * speed, (float)Math.sin(a) * speed - 15,
                        1.8f + random.nextFloat() * 3f, 0.38f + random.nextFloat() * .38f,
                        Color.red(col), Color.green(col), Color.blue(col)));
            }
        }

        private void playTone(int toneId, int duration) {
            if (!soundEnabled || tone == null) return;
            try { tone.startTone(toneId, duration); } catch (Throwable ignored) {}
        }

        private boolean inside(RectF r, float x, float y) { return r.contains(x, y); }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchDownX = event.getX() / Math.max(0.01f, scale);
                touchDownY = event.getY() / Math.max(0.01f, scale);
                return true;
            }
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            float x = event.getX() / scale;
            float y = event.getY() / scale;
            if (screen == HOME) {
                if (inside(playRect, x, y)) startLevel();
                else if (inside(settingsRect, x, y)) screen = SETTINGS;
                else if (y > playRect.bottom + 12 && y < playRect.bottom + 70) {
                    startLevel();
                    tutorialPending = true;
                }
                invalidate();
                return true;
            }
            if (screen == SETTINGS) {
                float cy = height / 2f;
                if (y > cy - 65 && y < cy - 5) {
                    musicEnabled = !musicEnabled;
                    if (musicEnabled) music.start(); else music.stop();
                    save();
                } else if (y > cy && y < cy + 62) {
                    soundEnabled = !soundEnabled; save(); playTone(ToneGenerator.TONE_PROP_ACK, 70);
                } else if (y > cy + 65 && y < cy + 128) {
                    screen = PRIVACY;
                } else { screen = HOME; }
                invalidate(); return true;
            }
            if (screen == PRIVACY) {
                screen = SETTINGS;
                invalidate();
                return true;
            }
            if (screen == RESULT) {
                if (inside(resultRect, x, y)) {
                    if (won) level++;
                    save();
                    startLevel();
                } else if (inside(homeRect, x, y)) screen = HOME;
                invalidate(); return true;
            }
            if (screen == PAUSED) {
                float cx = width / 2f, cy = height / 2f;
                if (y > cy - 25 && y < cy + 48) screen = PLAYING;
                else if (y >= cy + 48) screen = HOME;
                invalidate(); return true;
            }
            if (screen != PLAYING) return true;
            if (tutorialPending) {
                tutorialPending = false;
                invalidate();
                return true;
            }
            if (inside(pauseRect, x, y)) { screen = PAUSED; invalidate(); return true; }
            for (int i = 0; i < boosterRects.length; i++) {
                if (inside(boosterRects[i], x, y)) {
                    activateBooster(i);
                    invalidate();
                    return true;
                }
            }
            int gx = (int)((x - boardX) / cell);
            int gy = (int)((y - boardY) / cell);
            boolean inBoard = gx >= 0 && gx < SIZE && gy >= 0 && gy < SIZE
                    && x >= boardX && y >= boardY && x < boardX + boardSize && y < boardY + boardSize;
            if (inBoard && activeBooster < 0) {
                float dx = x - touchDownX;
                float dy = y - touchDownY;
                int startX = (int)((touchDownX - boardX) / cell);
                int startY = (int)((touchDownY - boardY) / cell);
                if (startX >= 0 && startX < SIZE && startY >= 0 && startY < SIZE
                        && (Math.abs(dx) > cell * 0.18f || Math.abs(dy) > cell * 0.18f)) {
                    int targetX = startX;
                    int targetY = startY;
                    if (Math.abs(dx) > Math.abs(dy)) targetX += dx > 0 ? 1 : -1;
                    else targetY += dy > 0 ? 1 : -1;
                    if (targetX >= 0 && targetX < SIZE && targetY >= 0 && targetY < SIZE
                            && Math.abs(targetX - startX) + Math.abs(targetY - startY) == 1) {
                        performSwap(startX, startY, targetX, targetY);
                        selectedX = selectedY = -1;
                        invalidate();
                        return true;
                    }
                }
            }
            if (inBoard) {
                if (activeBooster >= 0) useBooster(gx, gy);
                else handleTileTap(gx, gy);
                invalidate();
                return true;
            }
            selectedX = selectedY = -1;
            invalidate();
            return true;
        }

        private void activateBooster(int index) {
            if (index < 0 || index >= boosterCounts.length) return;
            if (boosterCounts[index] <= 0) {
                announce("No charges left for that power up.");
                playTone(ToneGenerator.TONE_PROP_NACK, 80);
                return;
            }
            if (index == 2) {
                boosterCounts[index]--;
                shuffleBoard();
                activeBooster = -1;
                announce("Board remixed!");
                playTone(ToneGenerator.TONE_PROP_ACK, 100);
            } else if (index == 3) {
                boosterCounts[index]--;
                moves += 5;
                activeBooster = -1;
                announce("Five extra moves!");
                playTone(ToneGenerator.TONE_PROP_ACK, 120);
            } else {
                activeBooster = (activeBooster == index) ? -1 : index;
                selectedX = selectedY = -1;
                String[] instructions = {
                        "Hammer selected. Tap one gem to smash it.",
                        "Prism selected. Tap a gem colour to sweep.",
                        "Mix the board.",
                        "Add five moves.",
                        "Row blast selected. Tap a row target.",
                        "Column blast selected. Tap a column target."
                };
                announce(instructions[index]);
            }
            lastActionAt = SystemClock.uptimeMillis();
        }

        private void useBooster(int x, int y) {
            if (activeBooster < 0 || activeBooster >= boosterCounts.length || boosterCounts[activeBooster] <= 0) return;
            int used = activeBooster;
            boosterCounts[used]--;
            if (used == 0) {
                clearSingleAndResolve(x, y, false);
                announce("Hammer smash!");
                playTone(ToneGenerator.TONE_PROP_NACK, 100);
            } else if (used == 1) {
                int type = tiles[y][x];
                boolean[][] clear = new boolean[SIZE][SIZE];
                for (int yy = 0; yy < SIZE; yy++) for (int xx = 0; xx < SIZE; xx++)
                    if (tiles[yy][xx] == type) clear[yy][xx] = true;
                resolveFromClear(clear, -1);
                announce("Prism sweep!");
                playTone(ToneGenerator.TONE_PROP_ACK, 140);
            } else if (used == 4) {
                boolean[][] clear = new boolean[SIZE][SIZE];
                for (int xx = 0; xx < SIZE; xx++) clear[y][xx] = true;
                resolveFromClear(clear, -1);
                announce("Row blast!");
                playTone(ToneGenerator.TONE_PROP_ACK, 140);
            } else if (used == 5) {
                boolean[][] clear = new boolean[SIZE][SIZE];
                for (int yy = 0; yy < SIZE; yy++) clear[yy][x] = true;
                resolveFromClear(clear, -1);
                announce("Column blast!");
                playTone(ToneGenerator.TONE_PROP_ACK, 140);
            }
            activeBooster = -1;
            selectedX = selectedY = -1;
            lastActionAt = SystemClock.uptimeMillis();
            checkLevelState();
        }

        private void clearSingleAndResolve(int x, int y, boolean consumesMove) {
            boolean[][] clear = new boolean[SIZE][SIZE];
            clear[y][x] = true;
            if (specials[y][x] != NONE) expandSpecial(clear, x, y, tiles[y][x]);
            resolveFromClear(clear, -1);
            if (consumesMove) moves--;
        }

        private void handleTileTap(int x, int y) {
            if (selectedX < 0) {
                selectedX = x; selectedY = y;
                lastActionAt = SystemClock.uptimeMillis();
                playTone(ToneGenerator.TONE_PROP_BEEP, 45);
                return;
            }
            if (selectedX == x && selectedY == y) {
                selectedX = selectedY = -1; return;
            }
            if (Math.abs(selectedX - x) + Math.abs(selectedY - y) == 1) {
                performSwap(selectedX, selectedY, x, y);
                selectedX = selectedY = -1;
            } else {
                selectedX = x; selectedY = y;
                lastActionAt = SystemClock.uptimeMillis();
                playTone(ToneGenerator.TONE_PROP_BEEP, 45);
            }
        }

        private void performSwap(int ax, int ay, int bx, int by) {
            int a = tiles[ay][ax], b = tiles[by][bx];
            int sa = specials[ay][ax], sb = specials[by][bx];
            swap(ax, ay, bx, by);
            MatchInfo info = findMatches();
            boolean powerCombo = sa != NONE || sb != NONE;
            if (!info.any() && !powerCombo) {
                swap(ax, ay, bx, by);
                playTone(ToneGenerator.TONE_PROP_NACK, 90);
                lastActionAt = SystemClock.uptimeMillis();
                return;
            }
            moves--;
            swapFx = new SwapFx(ax, ay, bx, by, a, b, sa, sb, SystemClock.uptimeMillis());
            hintAX = hintAY = hintBX = hintBY = -1;
            if (powerCombo) {
                boolean[][] first = buildSpecialCombo(ax, ay, bx, by, a, b, sa, sb);
                resolveFromClear(first, -1);
            } else {
                resolveMatches(info, b);
            }
            playTone(ToneGenerator.TONE_PROP_ACK, 100);
            lastActionAt = SystemClock.uptimeMillis();
            checkLevelState();
        }

        private boolean[][] buildSpecialCombo(int ax, int ay, int bx, int by, int a, int b, int sa, int sb) {
            boolean[][] clear = new boolean[SIZE][SIZE];
            if (sa == PRISM && sb == PRISM) {
                for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) clear[y][x] = true;
                return clear;
            }
            if (sa == PRISM || sb == PRISM) {
                int targetColor = sa == PRISM ? b : a;
                int pairedPower = sa == PRISM ? sb : sa;
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        if (tiles[y][x] != targetColor) continue;
                        if (pairedPower == ROCKET_H) {
                            for (int xx = 0; xx < SIZE; xx++) clear[y][xx] = true;
                        } else if (pairedPower == ROCKET_V) {
                            for (int yy = 0; yy < SIZE; yy++) clear[yy][x] = true;
                        } else if (pairedPower == NOVA) {
                            for (int yy = Math.max(0, y - 1); yy <= Math.min(SIZE - 1, y + 1); yy++)
                                for (int xx = Math.max(0, x - 1); xx <= Math.min(SIZE - 1, x + 1); xx++) clear[yy][xx] = true;
                        } else {
                            clear[y][x] = true;
                        }
                        if (specials[y][x] != NONE) expandSpecial(clear, x, y, targetColor);
                    }
                }
                return clear;
            }
            boolean horizontal = sa == ROCKET_H || sb == ROCKET_H || sa == CROSS || sb == CROSS;
            boolean vertical = sa == ROCKET_V || sb == ROCKET_V || sa == CROSS || sb == CROSS;
            boolean nova = sa == NOVA || sb == NOVA;
            if (horizontal) {
                for (int x = 0; x < SIZE; x++) { clear[ay][x] = true; clear[by][x] = true; }
            }
            if (vertical) {
                for (int y = 0; y < SIZE; y++) { clear[y][ax] = true; clear[y][bx] = true; }
            }
            if (nova || (!horizontal && !vertical)) {
                for (int cy : new int[]{ay, by}) for (int cx : new int[]{ax, bx})
                    for (int y = Math.max(0, cy - 1); y <= Math.min(SIZE - 1, cy + 1); y++)
                        for (int x = Math.max(0, cx - 1); x <= Math.min(SIZE - 1, cx + 1); x++) clear[y][x] = true;
            }
            clear[ay][ax] = true;
            clear[by][bx] = true;
            return clear;
        }

        private void swap(int ax, int ay, int bx, int by) {
            int t = tiles[ay][ax]; tiles[ay][ax] = tiles[by][bx]; tiles[by][bx] = t;
            int s = specials[ay][ax]; specials[ay][ax] = specials[by][bx]; specials[by][bx] = s;
            int i = ice[ay][ax]; ice[ay][ax] = ice[by][bx]; ice[by][bx] = i;
        }

        private void resolveMatches(MatchInfo first, int prismTarget) {
            MatchInfo info = first;
            int cascade = 0;
            while (info != null && info.any() && cascade < 14) {
                boolean[][] clear = info.cells;
                int keepX = -1, keepY = -1, newSpecial = NONE;
                if (cascade == 0 && info.bestLength >= 4) {
                    keepX = info.bestX; keepY = info.bestY;
                    newSpecial = info.bestType;
                }
                if (cascade == 0 && info.intersectionX >= 0) {
                    keepX = info.intersectionX; keepY = info.intersectionY; newSpecial = NOVA;
                }
                expandSpecialChain(clear, prismTarget, keepX, keepY);
                int removed = applyClear(clear, keepX, keepY, newSpecial);
                if (removed <= 0) break;
                score += removed * (55 + cascade * 22);
                cascade++;
                gravityAndRefill();
                info = findMatches();
            }
            if (cascade > 1) playTone(ToneGenerator.TONE_PROP_ACK, 130);
            if (score > bestScore) { bestScore = score; save(); }
            if (!hasPossibleMove()) shuffleBoard();
        }

        private void resolveFromClear(boolean[][] firstClear, int prismTarget) {
            boolean[][] clear = firstClear;
            int cascade = 0;
            while (clear != null && cascade < 14) {
                expandSpecialChain(clear, prismTarget, -1, -1);
                int removed = applyClear(clear, -1, -1, NONE);
                if (removed <= 0) break;
                score += removed * (58 + cascade * 20);
                cascade++;
                gravityAndRefill();
                MatchInfo next = findMatches();
                if (!next.any()) break;
                clear = next.cells;
            }
            if (score > bestScore) { bestScore = score; save(); }
            if (!hasPossibleMove()) shuffleBoard();
        }

        private void expandSpecialChain(boolean[][] clear, int targetColor, int keepX, int keepY) {
            boolean[][] triggered = new boolean[SIZE][SIZE];
            boolean found;
            do {
                found = false;
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        if (clear[y][x] && !triggered[y][x] && specials[y][x] != NONE
                                && !(x == keepX && y == keepY)) {
                            triggered[y][x] = true;
                            expandSpecial(clear, x, y, targetColor);
                            found = true;
                        }
                    }
                }
            } while (found);
        }

        private int applyClear(boolean[][] clear, int keepX, int keepY, int newSpecial) {
            int removed = 0;
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!clear[y][x]) continue;
                    if (x == keepX && y == keepY) {
                        specials[y][x] = newSpecial;
                        continue;
                    }
                    if (tiles[y][x] >= 0) {
                        int col = gemColors[tiles[y][x]];
                        burst(boardX + (x + .5f) * cell, boardY + (y + .5f) * cell, col, 5);
                        removed++;
                        if (ice[y][x] > 0) { ice[y][x] = 0; iceRemaining--; }
                        tiles[y][x] = -1;
                        specials[y][x] = NONE;
                    }
                }
            }
            return removed;
        }

        private void expandSpecial(boolean[][] clear, int x, int y, int targetColor) {
            int type = specials[y][x];
            if (type == ROCKET_H) {
                for (int xx = 0; xx < SIZE; xx++) clear[y][xx] = true;
            } else if (type == ROCKET_V) {
                for (int yy = 0; yy < SIZE; yy++) clear[yy][x] = true;
            } else if (type == NOVA) {
                for (int yy = Math.max(0, y - 1); yy <= Math.min(SIZE - 1, y + 1); yy++)
                    for (int xx = Math.max(0, x - 1); xx <= Math.min(SIZE - 1, x + 1); xx++) clear[yy][xx] = true;
            } else if (type == PRISM) {
                int color = targetColor;
                if (color < 0 || color >= TYPES) color = tiles[y][x];
                for (int yy = 0; yy < SIZE; yy++) for (int xx = 0; xx < SIZE; xx++)
                    if (tiles[yy][xx] == color) clear[yy][xx] = true;
            } else if (type == CROSS) {
                for (int xx = 0; xx < SIZE; xx++) clear[y][xx] = true;
                for (int yy = 0; yy < SIZE; yy++) clear[yy][x] = true;
            }
            clear[y][x] = true;
        }

        private MatchInfo findMatches() {
            MatchInfo info = new MatchInfo();
            boolean[][] horizontal = new boolean[SIZE][SIZE];
            boolean[][] vertical = new boolean[SIZE][SIZE];
            for (int y = 0; y < SIZE; y++) {
                int start = 0;
                while (start < SIZE) {
                    int value = tiles[y][start], end = start + 1;
                    if (value < 0) { start++; continue; }
                    while (end < SIZE && tiles[y][end] == value) end++;
                    int len = end - start;
                    if (len >= 3) {
                        for (int x = start; x < end; x++) { info.cells[y][x] = true; horizontal[y][x] = true; }
                        if (len > info.bestLength) {
                            info.bestLength = len; info.bestType = len >= 5 ? PRISM : ROCKET_H;
                            info.bestX = start + len / 2; info.bestY = y;
                        }
                    }
                    start = end;
                }
            }
            for (int x = 0; x < SIZE; x++) {
                int start = 0;
                while (start < SIZE) {
                    int value = tiles[start][x], end = start + 1;
                    if (value < 0) { start++; continue; }
                    while (end < SIZE && tiles[end][x] == value) end++;
                    int len = end - start;
                    if (len >= 3) {
                        for (int y = start; y < end; y++) { info.cells[y][x] = true; vertical[y][x] = true; }
                        if (len > info.bestLength) {
                            info.bestLength = len; info.bestType = len >= 5 ? PRISM : ROCKET_V;
                            info.bestX = x; info.bestY = start + len / 2;
                        }
                    }
                    start = end;
                }
            }
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
                if (horizontal[y][x] && vertical[y][x]) {
                    info.intersectionX = x; info.intersectionY = y;
                }
            }
            if (info.intersectionX >= 0) {
                info.bestX = info.intersectionX; info.bestY = info.intersectionY; info.bestType = NOVA;
            }
            return info;
        }

        private void gravityAndRefill() {
            for (int x = 0; x < SIZE; x++) {
                int write = SIZE - 1;
                for (int y = SIZE - 1; y >= 0; y--) {
                    if (tiles[y][x] >= 0) {
                        int value = tiles[y][x], sp = specials[y][x], frost = ice[y][x];
                        tiles[y][x] = -1; specials[y][x] = NONE; ice[y][x] = 0;
                        tiles[write][x] = value; specials[write][x] = sp; ice[write][x] = frost;
                        write--;
                    }
                }
                while (write >= 0) {
                    tiles[write][x] = random.nextInt(TYPES);
                    specials[write][x] = NONE;
                    ice[write][x] = 0;
                    write--;
                }
            }
        }

        private boolean hasPossibleMove() {
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
                if (x + 1 < SIZE) {
                    swap(x, y, x + 1, y);
                    boolean ok = findMatches().any() || specials[y][x] != NONE || specials[y][x + 1] != NONE;
                    swap(x, y, x + 1, y);
                    if (ok) return true;
                }
                if (y + 1 < SIZE) {
                    swap(x, y, x, y + 1);
                    boolean ok = findMatches().any() || specials[y][x] != NONE || specials[y + 1][x] != NONE;
                    swap(x, y, x, y + 1);
                    if (ok) return true;
                }
            }
            return false;
        }

        private void shuffleBoard() {
            int[] values = new int[SIZE * SIZE];
            int count = 0;
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) values[count++] = tiles[y][x];
            for (int i = values.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1), t = values[i]; values[i] = values[j]; values[j] = t;
            }
            count = 0;
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
                tiles[y][x] = values[count++]; specials[y][x] = NONE;
            }
            for (int attempt = 0; attempt < 8 && (!hasPossibleMove() || findMatches().any()); attempt++) {
                for (int i = values.length - 1; i > 0; i--) {
                    int j = random.nextInt(i + 1), t = values[i]; values[i] = values[j]; values[j] = t;
                }
                count = 0;
                for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) tiles[y][x] = values[count++];
            }
            selectedX = selectedY = -1;
        }

        private void checkLevelState() {
            if (score > bestScore) { bestScore = score; save(); }
            boolean objectiveComplete = score >= targetScore && iceRemaining <= 0;
            if (objectiveComplete) {
                won = true;
                stars = score >= targetScore * 1.65f ? 3 : score >= targetScore * 1.28f ? 2 : 1;
                screen = RESULT;
                resultBest = score;
                playTone(ToneGenerator.TONE_PROP_ACK, 180);
                burst(width / 2, height / 2, Color.rgb(255, 210, 96), 48);
                announce("Level complete. " + stars + " stars!");
                return;
            }
            if (moves <= 0) {
                won = false;
                screen = RESULT;
                announce("Out of moves. Try again.");
                playTone(ToneGenerator.TONE_PROP_NACK, 170);
            }
        }

        private void announce(String message) {
            setContentDescription(message);
            sendAccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        }

        private void findHint() {
            hintAX = hintAY = hintBX = hintBY = -1;
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
                if (x + 1 < SIZE) {
                    swap(x, y, x + 1, y);
                    boolean ok = findMatches().any();
                    swap(x, y, x + 1, y);
                    if (ok) { hintAX = x; hintAY = y; hintBX = x + 1; hintBY = y; return; }
                }
                if (y + 1 < SIZE) {
                    swap(x, y, x, y + 1);
                    boolean ok = findMatches().any();
                    swap(x, y, x, y + 1);
                    if (ok) { hintAX = x; hintAY = y; hintBX = x; hintBY = y + 1; return; }
                }
            }
        }

        private static final class SwapFx {
            final int ax, ay, bx, by, firstGem, secondGem, firstSpecial, secondSpecial;
            final long startedAt;
            SwapFx(int ax, int ay, int bx, int by, int firstGem, int secondGem,
                   int firstSpecial, int secondSpecial, long startedAt) {
                this.ax = ax; this.ay = ay; this.bx = bx; this.by = by;
                this.firstGem = firstGem; this.secondGem = secondGem;
                this.firstSpecial = firstSpecial; this.secondSpecial = secondSpecial;
                this.startedAt = startedAt;
            }
            boolean isCell(int x, int y) {
                return (x == ax && y == ay) || (x == bx && y == by);
            }
        }

        private static final class MatchInfo {
            final boolean[][] cells = new boolean[SIZE][SIZE];
            int bestLength = 0, bestType = NONE, bestX = -1, bestY = -1;
            int intersectionX = -1, intersectionY = -1;
            boolean any() {
                for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) if (cells[y][x]) return true;
                return false;
            }
        }

        private static final class Particle {
            float x, y, vx, vy, radius, life, maxLife;
            int red, green, blue;
            Particle(float x, float y, float vx, float vy, float radius, float life, int red, int green, int blue) {
                this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.radius = radius;
                this.life = life; this.maxLife = life; this.red = red; this.green = green; this.blue = blue;
            }
        }

        private static final class MusicEngine {
            private volatile boolean running = false;
            private AudioTrack track;
            private Thread thread;

            synchronized void start() {
                if (running) return;
                try {
                    int rate = 22050;
                    int min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
                    AudioFormat format = new AudioFormat.Builder().setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
                    track = new AudioTrack(attributes, format, Math.max(min * 2, 4096),
                            AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
                    track.play();
                    running = true;
                    thread = new Thread(() -> {
                        short[] buffer = new short[1024];
                        long frame = 0;
                        int[] notes = {261, 329, 392, 523, 392, 349, 293, 349, 440, 523, 440, 392, 329, 392, 349, 293};
                        while (running) {
                            for (int i = 0; i < buffer.length; i++, frame++) {
                                double seconds = frame / 22050.0;
                                int beat = (int)(seconds / .44) % notes.length;
                                double phase = seconds * notes[beat] * Math.PI * 2;
                                double bass = Math.sin(seconds * (notes[beat] * .5) * Math.PI * 2) * .15;
                                double shimmer = Math.sin(phase * 2.0) * .07;
                                double beatPhase = (seconds % .44) / .44;
                                double envelope = Math.min(1.0, beatPhase * 8) * Math.min(1.0, (1.0 - beatPhase) * 5);
                                double sample = (Math.sin(phase) * .12 + bass + shimmer) * envelope;
                                buffer[i] = (short)(Math.max(-1, Math.min(1, sample)) * 32767);
                            }
                            if (!running || track == null) break;
                            try { track.write(buffer, 0, buffer.length); } catch (Throwable ignored) { break; }
                        }
                    }, "PrismPop-Music");
                    thread.setDaemon(true);
                    thread.start();
                } catch (Throwable ignored) { running = false; }
            }

            synchronized void stop() {
                running = false;
                AudioTrack old = track;
                track = null;
                if (old != null) {
                    try { old.pause(); old.flush(); old.release(); } catch (Throwable ignored) {}
                }
                thread = null;
            }
        }
    }
}
