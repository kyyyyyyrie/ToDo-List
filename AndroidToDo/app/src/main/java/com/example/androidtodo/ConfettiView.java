package com.example.androidtodo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Random;

public class ConfettiView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Piece> pieces = new ArrayList<>();
    private final Random random = new Random();
    private ObjectAnimator animator;
    private float progress = 0f;
    private boolean animating = false;
    private AnimationListener listener;

    public interface AnimationListener {
        void onCompleted();
    }

    private static class Piece {
        float x, y, vx, vy, rot, vr, scale;
        int color;
        int shape;
        long delay;
    }

    private static final int[] COLORS = {
        0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
        0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
        0xFF009688, 0xFF4CAF50, 0xFFCDDC39, 0xFFFFEB3B,
        0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF795548
    };

    public ConfettiView(Context c) {
        super(c);
        init();
    }

    public ConfettiView(Context c, AttributeSet attrs) {
        super(c, attrs);
        init();
    }

    public ConfettiView(Context c, AttributeSet attrs, int defStyleAttr) {
        super(c, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVisibility(GONE);
    }

    public void start() {
        if (animating) return;
        animating = true;
        setVisibility(VISIBLE);
        pieces.clear();
        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < 100; i++) {
            Piece p = new Piece();
            p.x = random.nextFloat() * (w > 0 ? w : 1080);
            p.y = -60 - random.nextInt(500);
            p.vx = (random.nextFloat() - 0.5f) * 12f;
            p.vy = 3f + random.nextFloat() * 6f;
            p.rot = random.nextInt(360);
            p.vr = (random.nextFloat() - 0.5f) * 800;
            p.scale = 0.4f + random.nextFloat() * 0.8f;
            p.color = COLORS[random.nextInt(COLORS.length)];
            p.shape = random.nextInt(3);
            p.delay = random.nextInt(400);
            pieces.add(p);
        }
        animator = ObjectAnimator.ofFloat(this, "progress", 0f, 1f);
        animator.setDuration(2200);
        animator.setInterpolator(new LinearInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                animating = false;
                setVisibility(GONE);
                if (listener != null) listener.onCompleted();
            }
        });
        animator.start();
    }

    public void setProgress(float v) { progress = v; invalidate(); }
    public float getProgress() { return progress; }

    public void setAnimationListener(AnimationListener l) { this.listener = l; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!animating) return;
        float p = progress;
        for (Piece piece : pieces) {
            float lp = Math.max(0, (p * 2200 - piece.delay) / 2200f);
            if (lp <= 0 || lp > 1) continue;
            piece.x += piece.vx * 0.016f;
            piece.y += piece.vy * lp * 0.016f;
            piece.vy += 0.3f;
            piece.rot += piece.vr * 0.016f;
            float alpha = Math.max(0, 1f - lp);
            paint.setAlpha((int)(alpha * 255));
            paint.setColor(piece.color);
            canvas.save();
            canvas.translate(piece.x, piece.y);
            canvas.rotate(piece.rot);
            canvas.scale(piece.scale, piece.scale);
            float size = 10f * (1f - lp * 0.3f);
            if (piece.shape == 0) {
                canvas.drawRect(-size, -size * 0.4f, size, size * 0.4f, paint);
            } else if (piece.shape == 1) {
                canvas.drawCircle(0, 0, size * 0.5f, paint);
            } else {
                float h2 = size * 0.7f;
                canvas.drawRoundRect(new RectF(-h2, -h2, h2, h2), 2f, 2f, paint);
            }
            canvas.restore();
        }
    }
}
