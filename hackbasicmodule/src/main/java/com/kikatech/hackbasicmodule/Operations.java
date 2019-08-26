package com.kikatech.hackbasicmodule;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.kikatech.hackbasicmodule.utils.Logger;

/**
 * Created by xm on 17/4/14.
 */

public class Operations {

    public enum Action {
        DOWN(MotionEvent.ACTION_DOWN), UP(MotionEvent.ACTION_UP), MOVE(MotionEvent.ACTION_MOVE);

        private final int value;

        Action(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static void touchView(final View view, final String arg, final int x, final int y) {
        if (view == null) {
            Logger.e("[Operations-sendCode] view is null");
            return;
        }
        view.post(new Runnable() {
            @Override
            public void run() {
                int action = MotionEvent.ACTION_DOWN;
                switch (arg) {
                    case "d":
                        action = MotionEvent.ACTION_DOWN;
                        break;
                    case "u":
                        action = MotionEvent.ACTION_UP;
                        break;
                    case "m":
                        action = MotionEvent.ACTION_MOVE;
                        break;
                }
                Logger.v("action_value : " + arg);
                MotionEvent event = MotionEvent.obtain(
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        action,
                        x, y, 0);
                Logger.v("view dispatchTouchEvent");
                boolean isHanlded = view.dispatchTouchEvent(event);
                Logger.i(view.getClass().getSimpleName() + isHanlded);
            }
        });
    }
}
