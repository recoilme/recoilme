package ru.recoilme.imageloader;

import android.graphics.*;

/**
 * Created with IntelliJ IDEA.
 * User: recoilme
 * Date: 09.10.12
 * Time: 15:50
 * To change this template use File | Settings | File Templates.
 */
public class ShadowImageProcessor implements ImageProcessor {


    public Bitmap processImage(Bitmap bitmap) {

        /*Paint mShadow = new Paint();
        Rect rect = new Rect(0,0,bitmap.getWidth(), bitmap.getHeight());

        mShadow.setAntiAlias(true);
        mShadow.setShadowLayer(5.5f, 4.0f, 4.0f, Color.BLACK);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(rect, mShadow);        */
        //Paint mShadow = new Paint();
        //Canvas canvas = new Canvas(bitmap);
// radius=10, y-offset=2, color=black
        ///mShadow.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
// in onDraw(Canvas)

        //canvas.drawBitmap(bitmap, 0.0f, 0.0f, mShadow);
        //return bitmap;
        int leftRightThk = 5,padTop=5,bottomThk=5;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int newW = w - (leftRightThk * 2);
        int newH = h - (bottomThk + padTop);

        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(w, h, conf);
        Bitmap sbmp = Bitmap.createScaledBitmap(bitmap, newW, newH, false);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Canvas c = new Canvas(bmp);

        // Left
        int leftMargin = (leftRightThk + 7)/2;
        Shader lshader = new LinearGradient(0, 0, leftMargin, 0, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
        paint.setShader(lshader);
        c.drawRect(0, padTop, leftMargin, newH, paint);

        // Right
        Shader rshader = new LinearGradient(w - leftMargin, 0, w, 0, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP);
        paint.setShader(rshader);
        c.drawRect(newW, padTop, w, newH, paint);

        // Bottom
        Shader bshader = new LinearGradient(0, newH, 0, bitmap.getHeight(), Color.BLACK, Color.TRANSPARENT, Shader.TileMode.CLAMP);
        paint.setShader(bshader);
        c.drawRect(leftMargin -3, newH, newW + leftMargin + 3, bitmap.getHeight(), paint);
        c.drawBitmap(sbmp, leftRightThk, 0, null);

        return bmp;

    }
}
