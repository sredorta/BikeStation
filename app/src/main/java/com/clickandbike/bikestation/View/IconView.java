package com.clickandbike.bikestation.View;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;


/**
 * Created by sredorta on 12/15/2016.
 * This class contains an special ImageView with all animations and things
 * The only thing required is set the result before the animation ends
 */
public class IconView extends ImageView {
    private static Boolean DEBUG_MODE = true;
    private Context mContext;
    private static final String TAG ="IconView::";
    private static final String IMAGE_ICONS_FOLDER = "checkerImages";
    private static final int FINAL_LANDING_Y_COORDINATE = 250; //450 for the snapdragon
    private Bitmap mOriginalBitmap;
    private Bitmap mRedBitmap;
    private Bitmap mGreenBitmap;
    private Bitmap mSepiaBitmap;
    private AssetManager mAssets;
    private String mName = "";
    private IconView mIconView;
    private int mIterations = 4;        // Number of iterations
    private long mDelay = 0;            // Delay for the animation
    private boolean mResult = false;    //Stores the result so that it turns red/green depending on boolean

    //Default constructor
    public IconView(Context context) {
        super(context);
        mContext = context;
        mAssets = context.getAssets();

    }
    //Default constructor
    public IconView(Context context, AttributeSet atts) {
        super(context,atts);
        mContext = context;
        mAssets = context.getAssets();
        mIconView = this;
    }

    public IconView(Context context, AttributeSet atts, int defStyleAttr) {
        super(context,atts,defStyleAttr);
        mContext = context;
        mAssets = context.getAssets();
        mIconView = this;
    }


    //Handle Logs in Debug mode
    public static void setDebugMode(Boolean mode) {
        DEBUG_MODE = mode;
        if (DEBUG_MODE) Log.i(TAG, "Debug mode enabled !");
    }


    public void setResult(Boolean result) {
        Log.i(TAG,"Setting Result to : " + result);
        mResult = result;
    }
    public Boolean getResult() {
        return mResult;
    }

    public void setIterations(int iterations ) {
        mIterations = iterations;
    }
    public void setDelay(long delay ) {
        mDelay = delay;
    }
    public int getIterations() {
        return mIterations;
    }

    public Bitmap getRedBitmap() {
        return mRedBitmap;
    }

    public void setImageColor(String color) {
        switch (color) {
            case "red":
                this.setImageBitmap(mRedBitmap);
                break;
            case "green":
                this.setImageBitmap(mGreenBitmap);
                break;
            case "sepia":
                this.setImageBitmap(mSepiaBitmap);
                break;
            default:
                this.setImageBitmap(mOriginalBitmap);
        }
        if (color.equals("red")) {
            this.setImageBitmap(mRedBitmap);
        }
    }
    public Bitmap getOriginalBitmap() {
        return mOriginalBitmap;
    }
    public Bitmap getGreenBitmap() {
        return mGreenBitmap;
    }
    public Bitmap getSepiaBitmap() {
        return mSepiaBitmap;
    }


    public void loadBitmapAsset(String asset) {
        if (DEBUG_MODE) Log.i(TAG,"loadBitmapAsset:");
        try {
            for (String filename : mAssets.list(IMAGE_ICONS_FOLDER)) {
                if (DEBUG_MODE) Log.i(TAG,"Found asset: " + filename);
                if (filename.equals(asset)) {
                    if (DEBUG_MODE) Log.i(TAG,"Loading asset image: " + filename);
                    String assetPath = IMAGE_ICONS_FOLDER + "/" + filename;
                    mOriginalBitmap = loadBitmap(assetPath);
                    mRedBitmap = colorizeBitmap("red",mOriginalBitmap);
                    mGreenBitmap = colorizeBitmap("green",mOriginalBitmap);
                    mSepiaBitmap = colorizeBitmap("sepia",mOriginalBitmap);
                    //Set the name to the asset name
                    break;
                }
            }
        } catch (IOException ioe) {
            Log.i(TAG, "Caught exception: " + ioe);
        }
        //By default we want sepia color
        this.setImageColor("sepia");
        //Store mName so that we can do the correct check
        mName = asset;
    }


    private Bitmap loadBitmap(String asset) throws IOException {
        InputStream ims;

        //Bitmap loader for the original image
        Bitmap bitmap;
        ims = mAssets.open(asset);
        if (DEBUG_MODE) Log.i(TAG,"Loading bitmap :" + asset);
        bitmap = BitmapFactory.decodeStream(ims);
        ims.close();
        return bitmap;
    }

    //Colors a bitmap to generate sepia/bw/green/red flavours
    private Bitmap colorizeBitmap(String color, Bitmap src) {
        //Apply coloring filter
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrix colorScale = new ColorMatrix();
        switch (color) {
            case "red":    colorScale.setScale(4f,0.8f,0.8f,2f); break;
            case "green":  colorScale.setScale(0.1f,0.9f,0.1f,2f); break;
            case "blue":   colorScale.setScale(0.3f,0.3f,1,1); break;
            case "sepia":  colorScale.setScale(1, 1, 0.8f, 1); break;
            case "bw":     colorScale.setScale(1,1,1,1); break;
            default:       colorScale.setScale(1,1,1,1); break;
        }

        cm.postConcat(colorScale);
        ColorFilter cf = new ColorMatrixColorFilter(cm);

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(cf);
        canvas.drawBitmap(src, 0, 0, paint);

        return bitmap;
    }

    private ObjectAnimator startFallDownAnimation() {
        if (DEBUG_MODE) Log.i(TAG,"Started initial anim");
        //Turn on visibility
        this.setVisibility(View.VISIBLE);
        ObjectAnimator fallDownAnimator = ObjectAnimator.ofFloat(this, "y", -100, FINAL_LANDING_Y_COORDINATE).setDuration(1000);
        return fallDownAnimator;
    }
    private ObjectAnimator startFadeInOutAnimation() {
        if (DEBUG_MODE) Log.i(TAG,"Started fadeInOut anim");
        ObjectAnimator fadeInOutAnimator = ObjectAnimator.ofFloat(this, "alpha", 1, 0, 1).setDuration(2000);
        fadeInOutAnimator.setRepeatCount(mIconView.getIterations());
        return fadeInOutAnimator;
    }
    private ObjectAnimator startFadeOutAnimation() {
        if (DEBUG_MODE) Log.i(TAG,"Started fadeOut anim");
        ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(this, "alpha", 1, 0).setDuration(1000);
        fadeOutAnimator.setRepeatCount(0);
        return fadeOutAnimator;
    }
    private ObjectAnimator startFadeInAnimation() {
        if (DEBUG_MODE) Log.i(TAG,"Started fadeIn anim");
        ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(this, "alpha", 0, 1).setDuration(1000);
        fadeInAnimator.setRepeatCount(0);
        fadeInAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                //final Locker myLocker = Locker.getLocker();
                if (DEBUG_MODE) Log.i(TAG,"Checking for " + mName + "mResult is: " + mResult );
                if (mResult) {
                    mIconView.setImageColor("green");
                } else {
                    mIconView.setImageColor("red");
                }
            }
            @Override
            public void onAnimationEnd(Animator animator) {}
            @Override
            public void onAnimationCancel(Animator animator) {}
            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
        //fadeInAnimator.start();
        return fadeInAnimator;
    }

    public AnimatorSet startAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(startFallDownAnimation(),startFadeInOutAnimation(),startFadeOutAnimation(),startFadeInAnimation());
        animatorSet.setStartDelay(this.mDelay);
        Log.i(TAG, "Animation delay is : " + mDelay);
        animatorSet.start();
        return animatorSet;
    }

}
