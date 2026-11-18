package com.pystarter.app.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 像素风迷宫格子地图 View：直接绘制地板、石墙、Bug 怪、宝石、终点旗帜和朝向不同的小骑士。
 */
public class MazeView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<String> grid = new ArrayList<>();
    private final Set<String> gems = new HashSet<>();
    private int knightX = 0;
    private int knightY = 0;
    private int knightDir = 0; // 0=右, 1=下, 2=左, 3=上
    private boolean hitState = false;

    public MazeView(Context context) {
        super(context);
    }

    public MazeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMaze(List<String> rows, int x, int y, int dir, Set<String> initialGems) {
        grid.clear();
        grid.addAll(rows);
        gems.clear();
        gems.addAll(initialGems);
        knightX = x;
        knightY = y;
        knightDir = dir;
        hitState = false;
        invalidate();
    }

    public void updateFrame(int x, int y, int dir, Set<String> currentGems, boolean isHit) {
        knightX = x;
        knightY = y;
        knightDir = dir;
        gems.clear();
        gems.addAll(currentGems);
        hitState = isHit;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (grid.isEmpty()) {
            return;
        }
        int rows = grid.size();
        int cols = grid.get(0).length();
        float cell = Math.min((float) getWidth() / cols, (float) getHeight() / rows);
        float offsetX = (getWidth() - cell * cols) / 2f;
        float offsetY = (getHeight() - cell * rows) / 2f;

        for (int y = 0; y < rows; y++) {
            String row = grid.get(y);
            for (int x = 0; x < cols; x++) {
                float left = offsetX + x * cell;
                float top = offsetY + y * cell;
                char ch = row.charAt(x);

                // 1. 地板棋盘格
                paint.setColor(((x + y) % 2 == 0) ? 0xFF1B2838 : 0xFF16202E);
                canvas.drawRect(left + 1, top + 1, left + cell - 1, top + cell - 1, paint);

                if (ch == '#') {
                    // 石墙
                    paint.setColor(0xFF475569);
                    canvas.drawRect(left + 2, top + 2, left + cell - 2, top + cell - 2, paint);
                    paint.setColor(0xFF64748B);
                    canvas.drawRect(left + 2, top + 2, left + cell - 2, top + cell * 0.25f, paint);
                } else if (ch == 'E') {
                    // 终点旗帜
                    paint.setColor(0xFF14532D);
                    canvas.drawRect(left + 3, top + 3, left + cell - 3, top + cell - 3, paint);
                    paint.setColor(0xFFE2E8F0);
                    canvas.drawRect(left + cell * 0.28f, top + cell * 0.2f, left + cell * 0.36f, top + cell * 0.85f, paint);
                    paint.setColor(0xFF22C55E);
                    canvas.drawRect(left + cell * 0.36f, top + cell * 0.2f, left + cell * 0.78f, top + cell * 0.48f, paint);
                } else if (ch == 'B') {
                    // Bug 怪
                    paint.setColor(0xFFEF4444);
                    canvas.drawRect(left + cell * 0.22f, top + cell * 0.26f, left + cell * 0.78f, top + cell * 0.76f, paint);
                    paint.setColor(0xFFFFFFFF);
                    canvas.drawRect(left + cell * 0.30f, top + cell * 0.38f, left + cell * 0.42f, top + cell * 0.50f, paint);
                    canvas.drawRect(left + cell * 0.58f, top + cell * 0.38f, left + cell * 0.70f, top + cell * 0.50f, paint);
                }

                // 宝石
                if (gems.contains(x + "," + y)) {
                    paint.setColor(0xFFFACC15);
                    float p = cell * 0.28f;
                    canvas.drawRect(left + p, top + p, left + cell - p, top + cell - p, paint);
                    paint.setColor(0xFFFEF08A);
                    canvas.drawRect(left + cell * 0.36f, top + cell * 0.36f, left + cell * 0.52f, top + cell * 0.52f, paint);
                }
            }
        }

        // 绘制小骑士
        float kl = offsetX + knightX * cell;
        float kt = offsetY + knightY * cell;
        paint.setColor(hitState ? 0xFFEF4444 : 0xFF3B82F6);
        canvas.drawRect(kl + cell * 0.18f, kt + cell * 0.22f, kl + cell * 0.82f, kt + cell * 0.84f, paint);
        // 银色头盔 + 金色皇冠
        paint.setColor(0xFFCBD5E1);
        canvas.drawRect(kl + cell * 0.22f, kt + cell * 0.24f, kl + cell * 0.78f, kt + cell * 0.54f, paint);
        paint.setColor(0xFFFFD43B);
        canvas.drawRect(kl + cell * 0.30f, kt + cell * 0.14f, kl + cell * 0.70f, kt + cell * 0.24f, paint);
        // 方向指示眼（朝向哪边就靠哪边画亮蓝面甲）
        paint.setColor(0xFF0F172A);
        float vx = kl + cell * 0.32f;
        float vy = kt + cell * 0.36f;
        if (knightDir == 0) vx = kl + cell * 0.48f;
        else if (knightDir == 2) vx = kl + cell * 0.22f;
        else if (knightDir == 1) vy = kt + cell * 0.42f;
        else if (knightDir == 3) vy = kt + cell * 0.28f;
        canvas.drawRect(vx, vy, vx + cell * 0.28f, vy + cell * 0.12f, paint);
        paint.setColor(0xFF38BDF8);
        canvas.drawRect(vx + cell * 0.06f, vy + cell * 0.03f, vx + cell * 0.22f, vy + cell * 0.09f, paint);
    }
}
