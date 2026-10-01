/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pianoparts;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Like HyperOS, slides a small pen battery pill in from the top right corner
 * of the screen and out again a few seconds later.
 */
class PenPopup {

    private static final long SHOW_MS = 3000;
    private static final long ANIMATION_MS = 300;

    private final Context mContext;
    private final WindowManager mWindowManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mHideRunnable = this::hide;

    private LinearLayout mView;
    private TextView mText;

    PenPopup(Context context) {
        mContext = context;
        mWindowManager = context.getSystemService(WindowManager.class);
    }

    void show(int level) {
        mHandler.post(() -> {
            if (mView == null) {
                createView();
            }
            mText.setText(mContext.getString(R.string.pen_battery_level, level));
            mHandler.removeCallbacks(mHideRunnable);
            if (!mView.isAttachedToWindow()) {
                mWindowManager.addView(mView, createLayoutParams());
                mView.setTranslationX(dp(240));
            }
            mView.animate().translationX(0).alpha(1f).setDuration(ANIMATION_MS)
                    .setInterpolator(new DecelerateInterpolator()).withEndAction(null).start();
            mHandler.postDelayed(mHideRunnable, SHOW_MS);
        });
    }

    private void hide() {
        if (mView == null || !mView.isAttachedToWindow()) {
            return;
        }
        mView.animate().translationX(dp(240)).alpha(0f).setDuration(ANIMATION_MS)
                .withEndAction(() -> {
                    if (mView.isAttachedToWindow()) {
                        mWindowManager.removeView(mView);
                    }
                }).start();
    }

    private void createView() {
        mView = new LinearLayout(mContext);
        mView.setOrientation(LinearLayout.HORIZONTAL);
        mView.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(16);
        int padV = dp(10);
        mView.setPadding(padH, padV, padH, padV);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xE6202124);
        background.setCornerRadius(dp(24));
        mView.setBackground(background);
        mView.setElevation(dp(6));

        ImageView icon = new ImageView(mContext);
        icon.setImageResource(R.drawable.ic_pen);
        icon.setColorFilter(0xFFFFFFFF);
        mView.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        mText = new TextView(mContext);
        mText.setTextColor(0xFFFFFFFF);
        mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.setMarginStart(dp(10));
        mView.addView(mText, textParams);
    }

    private WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(16);
        params.y = dp(48);
        params.setTitle("PianoPenPopup");
        return params;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                mContext.getResources().getDisplayMetrics()));
    }
}
