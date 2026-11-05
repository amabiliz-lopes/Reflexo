package com.dpdm.reflexoapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ReactionView extends View {
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GameViewModel.State state = GameViewModel.State.IDLE;
    private String message = "Mexa o celular para começar";
    private float pulse = 0f;
    private final ValueAnimator animator;

    public ReactionView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(12f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(52f);
        textPaint.setFakeBoldText(true);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(900);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(v -> { pulse = (float) v.getAnimatedValue(); invalidate(); });
    }

    public void setState(GameViewModel.State s, String msg) {
        this.state = s;
        this.message = msg;
        if (s == GameViewModel.State.WAITING) {
            if (!animator.isRunning()) {
                animator.start();
            }
        } else {
            animator.cancel();
        }
        invalidate();
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 3.5f;

        switch (state) {
            case WAITING:   circlePaint.setColor(Color.RED); break;
            case GO:        circlePaint.setColor(Color.rgb(0, 200, 83)); break;
            case TOO_EARLY: circlePaint.setColor(Color.rgb(255, 152, 0)); break;
            case RESULT:    circlePaint.setColor(Color.rgb(33, 150, 243)); break;
            default:        circlePaint.setColor(Color.GRAY);
        }
        canvas.drawCircle(cx, cy, r, circlePaint);

        if (state == GameViewModel.State.WAITING) {
            ringPaint.setColor(Color.argb((int) (255 * (1 - pulse)), 255, 255, 255));
            canvas.drawCircle(cx, cy, r + pulse * 80f, ringPaint);
        }
        canvas.drawText(message, cx, cy + 18f, textPaint);
    }
}
