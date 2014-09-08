package com.mhk.android.croplib;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.Log;
import android.view.View;

public class CropView {

	private static final int OUTLINE_COLOR=0xFF33B5E5;
	private static final float OUTLINE_WIDTH=2F;
	
	RectF imageRect, cropRect;
	Rect drawRect;
	private Matrix matrix, prevMatrix;
	
	float changedWidth, changedHeight;
	
	private final Paint outsidePaint = new Paint();
    private final Paint outlinePaint = new Paint();
    float outlineWidth;
    
    private View imageView;
    
    public CropView(View image)
    {
    	this.imageView=image;
    }
    
    public void setup(Matrix m, Rect imageRect, RectF cropRect) {
        matrix = new Matrix(m);

        this.cropRect = cropRect;
        this.imageRect = new RectF(imageRect);
       
        Log.d("SETUP", "crop: "+cropRect.left+", "+cropRect.top+", "+cropRect.right+", "+cropRect.bottom);
        Log.d("SETUP", "image: "+imageRect.left+", "+imageRect.top+", "+imageRect.right+", "+imageRect.bottom);
        
        drawRect = computeDrawRect();
        
        outsidePaint.setARGB(160, 50, 50, 50);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setAntiAlias(true);
        outlineWidth = dpToPx(OUTLINE_WIDTH);
    }
    
    protected void draw(Canvas canvas) {
        canvas.save();
        Path path = new Path();
        outlinePaint.setStrokeWidth(outlineWidth);
        
        Rect viewDrawingRect = new Rect();
        imageView.getDrawingRect(viewDrawingRect);

        path.addRect(new RectF(drawRect), Path.Direction.CW);
        outlinePaint.setColor(OUTLINE_COLOR);

        if (isClipPathSupported(canvas)) {
            canvas.clipPath(path, Region.Op.DIFFERENCE);
            canvas.drawRect(viewDrawingRect, outsidePaint);
        } else {
            drawOutsideFallback(canvas);
        }

        canvas.restore();
        canvas.drawPath(path, outlinePaint);
    }
    
    public void setMatrix(Matrix mat)
    {
    	prevMatrix = matrix;
    	matrix = mat;
    }
    
    private void drawOutsideFallback(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), drawRect.top, outsidePaint);
        canvas.drawRect(0, drawRect.bottom, canvas.getWidth(), canvas.getHeight(), outsidePaint);
        canvas.drawRect(0, drawRect.top, drawRect.left, drawRect.bottom, outsidePaint);
        canvas.drawRect(drawRect.right, drawRect.top, canvas.getWidth(), drawRect.bottom, outsidePaint);
    }
    
    public Rect getScaledCropRect(float scale) {
    	return new Rect((int) (cropRect.left * scale), (int) (cropRect.top * scale),
                (int) (cropRect.right * scale), (int) (cropRect.bottom * scale));
    }
    
    private RectF computeLayout(Matrix mat) {
        RectF r = new RectF(cropRect.left, cropRect.top,
                            cropRect.right, cropRect.bottom);
        mat.mapRect(r);
        return r;
    }
     
    private Rect computeDrawRect()
    {
    	RectF r = new RectF(cropRect.left, cropRect.top,
                cropRect.right, cropRect.bottom);
		matrix.mapRect(r);
		return new Rect(Math.round(r.left), Math.round(r.top),
		            Math.round(r.right), Math.round(r.bottom));
    }

   public void handlePan(float dx, float dy)
   {
	   RectF r = computeLayout(matrix);
	   
	   float xDelta = -(dx * (cropRect.width() / r.width()));
       float yDelta = -(dy * (cropRect.height() / r.height()));
	   cropRect.offset(xDelta, yDelta);

       // Put the cropping rectangle inside image rectangle
       cropRect.offset(
               Math.max(0, imageRect.left - cropRect.left),
               Math.max(0, imageRect.top  - cropRect.top));

       cropRect.offset(
               Math.min(0, imageRect.right  - cropRect.right),
               Math.min(0, imageRect.bottom - cropRect.bottom));
       
       Log.d("DATA", "crop: "+cropRect.left+", "+cropRect.top+", "+cropRect.right+", "+cropRect.bottom);
      
   }
   
   public void handleZoom(float scaleFactor)
   {
	   RectF prevRect = computeLayout(prevMatrix);
	   RectF nowRect = computeLayout(matrix);
	   
	   Log.d("ZOOM", "prev: "+prevRect.width()+","+prevRect.height()+" now: "+nowRect.width()+","+nowRect.height());
	   
	   float wScale = nowRect.width()/prevRect.width();
	   float hScale = nowRect.height()/prevRect.height();
	   
	   Log.d("ZOOM", "wscale: "+wScale+" hscale: "+hScale);
	   
	   float wMap = cropRect.width()/nowRect.width();
	   float hMap = cropRect.height()/nowRect.height();
	   
	   float curWidth = cropRect.width()/wScale;
	   float curHeight = cropRect.height()/hScale;
	   
	   Log.d("ZOOM", "curw: "+curWidth+", curh: "+curHeight);
	   
	   float xDelta = (nowRect.centerX() - prevRect.centerX()) * wMap;
	   float yDelta = (nowRect.centerY() - prevRect.centerY()) * wMap;
	   
	   cropRect.offset(-xDelta, -yDelta);
	   
	   float centerX = cropRect.centerX();
	   float centerY = cropRect.centerY();
	   
	   float transX = curWidth/2;
	   float transY = curHeight/2;
	   
	   float left = centerX - transX;
	   float right = centerX + transX;
	   float top = centerY - transY;
	   float bottom = centerY + transY;
	   
	   cropRect.set(left, top, right, bottom);
	   
	   Log.d("ZOOM", "crop: "+cropRect.left+", "+cropRect.top+", "+cropRect.right+", "+cropRect.bottom);
   }
    
    private float dpToPx(float dp) {
        return dp * imageView.getResources().getDisplayMetrics().density;
    }
    
    @SuppressLint("NewApi")
    private boolean isClipPathSupported(Canvas canvas) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        } else if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            || Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return true;
        } else {
            return !canvas.isHardwareAccelerated();
        }
    }
}
