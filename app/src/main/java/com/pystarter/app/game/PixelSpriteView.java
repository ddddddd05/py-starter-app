package com.pystarter.app.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Map;

/**
 * 像素小人 / 怪物：用一组字符画出来，每个字符代表一个像素的颜色，'.' 是透明。
 * 不用图片素材，没有版权问题；放大时每个“像素”都是整数倍的方块，保持清晰的像素感。
 */
public class PixelSpriteView extends View {

    private final Paint paint = new Paint();
    private String[] rows = new String[0];
    private Map<Character, Integer> palette;
    private boolean flipped;

    public PixelSpriteView(Context context) {
        super(context);
    }

    public PixelSpriteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSprite(Sprites.Sprite sprite) {
        this.rows = sprite.rows;
        this.palette = sprite.palette;
        invalidate();
    }

    /** 水平翻转：让主角和怪物面对面。 */
    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rows.length == 0 || palette == null) {
            return;
        }
        int h = rows.length;
        int w = rows[0].length();
        int cell = Math.max(1, Math.min(getWidth() / w, getHeight() / h));    // 整数倍放大
        int left = (getWidth() - cell * w) / 2;
        int top = getHeight() - cell * h;                                       // 底部对齐，像站在地上
        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < w && x < row.length(); x++) {
                Integer color = palette.get(row.charAt(x));
                if (color == null) {
                    continue;
                }
                int dx = flipped ? (w - 1 - x) : x;
                paint.setColor(color);
                canvas.drawRect(left + dx * cell, top + y * cell,
                        left + (dx + 1) * cell, top + (y + 1) * cell, paint);
            }
        }
    }
}
