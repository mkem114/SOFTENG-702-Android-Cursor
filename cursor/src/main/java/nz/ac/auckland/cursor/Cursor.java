package nz.ac.auckland.cursor;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class Cursor {

    private Drawable cursor;
    private int width;
    private int height;
    private int centerX;
    private int centerY;

    public Cursor(Drawable drawable, int width, int height, int centerX, int centerY) {
        cursor = drawable;
        this.width = width;
        this.height = height;
        this.centerX = centerX;
        this.centerY = centerY;
        Log.e("Cursor", String.format("Width: %d, Height: %d", width, height));

    }

    public Drawable getDrawable() {
        return cursor;
    }

    public void updateLocation(int x, int y) {
        Rect bounds = cursor.copyBounds();
        //   Log.e("Cursor", String.format("Top: %s, Left: %s, Right: %s, Bottom: %s", bounds.top, bounds.left, bounds.right, bounds.bottom));
        bounds.left = x - centerX;
        bounds.top = y - centerY;
        bounds.right = x + width - centerX;
        bounds.bottom = y + height - centerY;
        cursor.setBounds(bounds);
        cursor.invalidateSelf();
    }
}
