package com.datonicgroup.narrate.app.util;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.graphics.Palette;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.codehaus.jackson.map.util.LRUMap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A class that can apply colors to Views lazily.
 * Can be used in ListViews.
 *
 * Created by Freddie (Musenkishi) Lust-Hed on 2014-10-21.
 */
public class PaletteLoader {

    private static final int MSG_RENDER_PALETTE = 4194;
    private static final int MSG_DISPLAY_PALETTE = 4195;

    private static final int MAX_ITEMS_IN_CACHE = 100;
    private static final int MAX_CONCURRENT_THREADS = 5;

    private static int TRUE = 1;
    private static int FALSE = 0;

    private static Handler uiHandler;
    private static Handler backgroundHandler;

    private static Map<String, Palette> colorSchemeCache;
    private static ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS);

    private PaletteLoader(){
        //Don't use
    }

    public static PaletteBuilder with(Context context, String string) {
        if (colorSchemeCache == null){
            colorSchemeCache = Collections.synchronizedMap(
                    new LRUMap<String, Palette>(0, MAX_ITEMS_IN_CACHE)
            );
        }
        if (uiHandler == null || backgroundHandler == null) {
            setupHandlers(context);
        }
        return new PaletteBuilder(string);
    }

    private static void setupHandlers(Context context) {
        HandlerThread handlerThread = new HandlerThread("palette-loader-background");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper(), sCallback);
        uiHandler = new Handler(context.getMainLooper(), sCallback);
    }

    private static Handler.Callback sCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {

            switch (message.what) {

                case MSG_RENDER_PALETTE:
                    Pair<Bitmap, PaletteTarget> pair = (Pair<Bitmap, PaletteTarget>) message.obj;
                    if (pair != null && !pair.first.isRecycled()) {
                        Palette palette = Palette.generate(pair.first);

                        colorSchemeCache.put(pair.second.getPath(), palette);

                        Message uiMessage = uiHandler.obtainMessage();
                        uiMessage.what = MSG_DISPLAY_PALETTE;
                        uiMessage.obj = new Pair<Palette, PaletteTarget>(palette, pair.second);
                        uiMessage.arg1 = FALSE;

                        uiHandler.sendMessage(uiMessage);
                    }
                    break;

                case MSG_DISPLAY_PALETTE:
                    Pair<Palette, PaletteTarget> pairDisplay = (Pair<Palette, PaletteTarget>) message.obj;
                    boolean fromCache = message.arg1 == TRUE;
                    applyColorToView(pairDisplay.second, pairDisplay.first, fromCache);

                    break;

            }

            return false;
        }
    };

    public static class PaletteBuilder {

        private String string;
        private Bitmap bitmap;
        private boolean maskDrawable;
        private int fallbackColor = Color.TRANSPARENT;
        private PaletteRequest paletteRequest = new PaletteRequest(PaletteRequest.SwatchType.REGULAR_VIBRANT, PaletteRequest.SwatchColor.BACKGROUND);
        private Palette palette;

        public PaletteBuilder(String string) {
            this.string = string;
        }

        public PaletteBuilder load(Bitmap bitmap) {
            this.bitmap = bitmap;
            return this;
        }

        public PaletteBuilder load(Palette colorScheme) {
            this.palette = colorScheme;
            return this;
        }

        public PaletteBuilder mask() {
            maskDrawable = true;
            return this;
        }

        public PaletteBuilder fallbackColor(int fallbackColor) {
            this.fallbackColor = fallbackColor;
            return this;
        }

        public PaletteBuilder setPaletteRequest(PaletteRequest paletteRequest) {
            this.paletteRequest = paletteRequest;
            return this;
        }

        public void into(View view) {
            final PaletteTarget paletteTarget = new PaletteTarget(string, paletteRequest, view, maskDrawable, fallbackColor);
            if (palette != null) {
                colorSchemeCache.put(paletteTarget.getPath(), palette);
                applyColorToView(paletteTarget, palette, false);
            } else {
                if (colorSchemeCache.get(string) != null) {
                    Palette palette = colorSchemeCache.get(string);
                    applyColorToView(paletteTarget, palette, true);
                } else {
                    if (Build.VERSION.SDK_INT >= 21) {
                        executorService.submit(new PaletteRenderer(bitmap, paletteTarget));
                    } else {
                        Message bgMessage = backgroundHandler.obtainMessage();
                        bgMessage.what = MSG_RENDER_PALETTE;
                        bgMessage.obj = new Pair<Bitmap, PaletteTarget>(bitmap, paletteTarget);
                        backgroundHandler.sendMessage(bgMessage);
                    }
                }
            }
        }
    }

    private static void applyColorToView(PaletteTarget target, Palette palette, boolean fromCache) {
        if (!isViewRecycled(target)) {
            applyColorToView(target, target.getPaletteRequest().getColor(palette), fromCache);
        }
    }

    private static void applyColorToView(final PaletteTarget target, int color, boolean fromCache) {
        if (target.getView() instanceof TextView) {
            applyColorToView((TextView) target.getView(), color, fromCache);
            return;
        }
        if (fromCache) {
            if (target.getView() instanceof ImageView && target.shouldMaskDrawable()) {
                ((ImageView) target.getView()).getDrawable().mutate()
                        .setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            } else {
                target.getView().setBackgroundColor(color);
            }
        } else {
            if (target.getView() instanceof ImageView && target.shouldMaskDrawable()) {
                Integer colorFrom;
                ValueAnimator.AnimatorUpdateListener imageAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ((ImageView) target.getView()).getDrawable().mutate()
                                .setColorFilter((Integer) valueAnimator
                                        .getAnimatedValue(), PorterDuff.Mode.MULTIPLY);
                    }
                };
                ValueAnimator.AnimatorUpdateListener animatorUpdateListener;

                PaletteTag paletteTag = (PaletteTag) target.getView().getTag();
                animatorUpdateListener = imageAnimatorUpdateListener;
                colorFrom = paletteTag.getColor();
                target.getView().setTag(new PaletteTag(paletteTag.getPath(), color));

                Integer colorTo = color;
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                colorAnimation.addUpdateListener(animatorUpdateListener);
                colorAnimation.setDuration(300);
                colorAnimation.start();
            } else {

                Drawable preDrawable;

                if (target.getView().getBackground() == null) {
                    preDrawable = new ColorDrawable(Color.TRANSPARENT);
                } else {
                    preDrawable = target.getView().getBackground();
                }

                TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{
                        preDrawable,
                        new ColorDrawable(color)
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    target.getView().setBackground(transitionDrawable);
                } else {
                    target.getView().setBackgroundDrawable(transitionDrawable);
                }
                transitionDrawable.startTransition(300);
            }
        }
    }

    private static void applyColorToView(final TextView textView, int color, boolean fromCache) {
        if (fromCache) {
            textView.setTextColor(color);
        } else {
            Integer colorFrom = textView.getCurrentTextColor();
            Integer colorTo = color;
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    textView.setTextColor((Integer) animator.getAnimatedValue());
                }
            });
            colorAnimation.start();
        }
    }

    /**
     * Is it null? Is that null? What about that one? Is that null too? What about this one?
     * And this one? Is this null? Is null even null? How can null be real if our eyes aren't real?
     *
     * <p>Checks whether the view has been recycled or not.</p>
     *
     * @param target A {@link com.datonicgroup.narrate.app.util.PaletteLoader.PaletteTarget} to check
     * @return true is view has been recycled, otherwise false.
     */
    private static boolean isViewRecycled(PaletteTarget target) {

        if (target != null && target.getPath() != null && target.getView() != null
                && target.getView().getTag() != null) {
            if (target.getView().getTag() instanceof PaletteTag) {
                return !target.getPath().equals(((PaletteTag) target.getView().getTag()).getPath());
            } else {
                throw new NoPaletteTagFoundException("PaletteLoader couldn't determine whether" +
                        " a View has been reused or not. PaletteLoader uses View.setTag() and " +
                        "View.getTag() for keeping check if a View has been reused and it's " +
                        "recommended to refrain from setting tags to Views PaletteLoader is using."
                );
            }
        } else {
            return false;
        }
    }

    private static class PaletteRenderer implements Runnable {

        private Bitmap bitmap;
        private PaletteTarget paletteTarget;

        private PaletteRenderer(Bitmap bitmap, PaletteTarget paletteTarget) {
            this.bitmap = bitmap;
            this.paletteTarget = paletteTarget;
        }

        @Override
        public void run() {
            if (bitmap != null && !bitmap.isRecycled()) {
                Palette colorScheme = Palette.generate(bitmap);
                colorSchemeCache.put(paletteTarget.getPath(), colorScheme);

                PalettePresenter palettePresenter = new PalettePresenter(
                        paletteTarget,
                        colorScheme,
                        false
                );
                uiHandler.post(palettePresenter);
            }
        }
    }

    private static class PalettePresenter implements Runnable {

        private PaletteTarget paletteTarget;
        private Palette palette;
        private boolean fromCache;

        private PalettePresenter(PaletteTarget paletteTarget, Palette palette, boolean fromCache) {
            this.paletteTarget = paletteTarget;
            this.palette = palette;
            this.fromCache = fromCache;
        }

        @Override
        public void run() {
            applyColorToView(paletteTarget, palette, fromCache);
        }
    }

    private static class PaletteTarget {
        private String string;
        private PaletteRequest paletteRequest;
        private View view;
        private boolean maskDrawable;

        private PaletteTarget(String string, PaletteRequest paletteRequest, View view, boolean maskDrawable, int fallbackColor) {
            this.string = string;
            this.paletteRequest = paletteRequest;
            this.view = view;
            this.view.setTag(new PaletteTag(this.string, fallbackColor));
            this.maskDrawable = maskDrawable;
        }

        public String getPath() {
            return string;
        }

        public PaletteRequest getPaletteRequest() {
            return paletteRequest;
        }

        public View getView() {
            return view;
        }

        public boolean shouldMaskDrawable() {
            return maskDrawable;
        }
    }

    private static class PaletteTag {
        private String string;
        private Integer color;

        private PaletteTag(String string, Integer color) {
            this.string = string;
            this.color = color;
        }

        public String getPath() {
            return string;
        }

        public void setPath(String string) {
            this.string = string;
        }

        public Integer getColor() {
            return color;
        }

        public void setColor(Integer color) {
            this.color = color;
        }
    }

    public static class NoPaletteTagFoundException extends NullPointerException {
        public NoPaletteTagFoundException() { super(); }
        public NoPaletteTagFoundException(String message) { super(message); }
    }

}
