package com.datonicgroup.narrate.app.util;

import android.graphics.Color;

/**
 * Created by timothymiko on 8/9/14.
 */
public class GraphicsUtil {

    /**
     * Blends the two provided colors according to the ratio
     *
     * @param color1 first color to blend
     * @param color2 second color to blend
     * @param ratio  ratio to blend colors. 0.0f results in color1, 1.0f results in color2
     * @return
     */
    public static int blendColors(int color1, int color2, float ratio) {

        float proportion = ratio;

        proportion = Math.min(1.0f, proportion);
        proportion = Math.max(0.0f, proportion);

        int[] color1Comps = {Color.alpha(color1), Color.red(color1), Color.green(color1), Color.blue(color1)};
        int[] color2Comps = {Color.alpha(color2), Color.red(color2), Color.green(color2), Color.blue(color2)};
        int[] newColorComps = {0, 0, 0, 0};

        for (int i = 0; i < 4; i++) {
            newColorComps[i] = Math.round(color1Comps[i] + ((color2Comps[i] - color1Comps[i]) * proportion));
        }

        return Color.argb(newColorComps[0], newColorComps[1], newColorComps[2], newColorComps[3]);
    }

    public static int darkenColor(int color, float ratio) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= ratio;
        return Color.HSVToColor(hsv);
    }
}
