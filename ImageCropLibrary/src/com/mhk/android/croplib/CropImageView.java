package com.mhk.android.croplib;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;

@SuppressLint("NewApi")
public class CropImageView extends ImageViewTouchBase implements OnTouchListener{

	public CropView mCropView;
	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	PointF last = new PointF();
	PointF start = new PointF();
	
	ScaleGestureDetector mScaleDetector;
		
	public CropImageView(Context context) {
		super(context);
		initialize();
	}
	
	public CropImageView(Context context, AttributeSet attr)
	{
		super(context, attr);
		initialize();
	}
	
	public CropImageView(Context context, AttributeSet attrs, int defstyle)
	{
		super(context, attrs, defstyle);
		initialize();
	}
	
	void initialize()
	{
		super.setClickable(true);
		mScaleDetector=new ScaleGestureDetector(getContext(), new ScaleListener());
		
		setOnTouchListener(this);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		// TODO Auto-generated method stub
		super.onLayout(changed, left, top, right, bottom);
		
		if(bitmapDisplayed.getBitmap()!=null)
		{
			if(mCropView!=null)
			{
				mCropView.setMatrix(getUnrotatedMatrix());
			}
		}
	}
	
	@Override
	protected void zoomTo(float scaleFactor, float centerX, float centerY) {
		// TODO Auto-generated method stub
		super.zoomTo(scaleFactor, centerX, centerY);
		
		if(mCropView!=null)
		{
			mCropView.setMatrix(getUnrotatedMatrix());
			mCropView.handleZoom(scaleFactor);
		}
	}
	
	@Override
	protected void postTranslate(float dx, float dy) {
		// TODO Auto-generated method stub
		super.postTranslate(dx, dy);
		
		if(mCropView!=null)
		{
			mCropView.setMatrix(getUnrotatedMatrix());
			mCropView.handlePan(dx, dy);
		}
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mode = ZOOM;
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float mScaleFactor = detector.getScaleFactor();
			
			zoomTo(mScaleFactor, detector.getFocusX(), detector.getFocusY());
		
			return true;
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		mScaleDetector.onTouchEvent(event);
		PointF curr = new PointF(event.getX(), event.getY());

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			last.set(curr);
			start.set(last);
			mode = DRAG;
			break;

		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				
				float deltaX = curr.x - last.x;
				float deltaY = curr.y - last.y;
				dragBy(deltaX, deltaY);
				last.set(curr.x, curr.y);
				invalidate();
			}
			break;

		case MotionEvent.ACTION_UP:
			mode = NONE;
			int xDiff = (int) Math.abs(curr.x - start.x);
			int yDiff = (int) Math.abs(curr.y - start.y);
			if (xDiff < 3 && yDiff < 3)
				performClick();
			break;

		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			break;
		}
		
		return true; 
	}
	
	@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mCropView!=null)
        {
        	mCropView.draw(canvas);
        }
    }

    public void add(CropView hv) {
    	mCropView=hv;
        invalidate();
    }
}
