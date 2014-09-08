package com.mhk.android.croplib;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;


public class BaseActivity extends Activity implements HostActivityActionListener{

	public static final String EXTRA_OUTPUT_PATH="output_file";
	final int REQUEST_PICK=121;
	public final static int CROP_WIDTH = 600, CROP_HEIGHT = 600; //in dp
	final int OUTPUT_WIDTH = 165, OUTPUT_HEIGHT = 165;
	
	public static float MIN_SIZE = CROP_WIDTH;
	
	private boolean isSaving;
	
	private String OUTPUT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/cropTest";
	private final String OUTPUT_FILENAME = "cropped_image.jpg";
	
	private Uri sourceUri, outputUri;
	private int sampleSize, exifRotation;
	private RotateBitmap rotateBitmap;
	
	public CropImageView imageView;
	CropView cropView;
	Handler handler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		handler = new Handler();
		if(getIntent().getStringExtra(EXTRA_OUTPUT_PATH)!=null)
		{
			File file = new File(getIntent().getStringExtra(EXTRA_OUTPUT_PATH));
			outputUri = Uri.fromFile(file);
		}
		else
		{
			File dir=new File(OUTPUT_PATH);
			if(!dir.exists())
			{
				dir.mkdirs();
			}
			
			File file=new File(dir, OUTPUT_FILENAME);
			outputUri = Uri.fromFile(file);
		}
	}
	
	void setImageView(CropImageView view){
		imageView = view;
	}
	
	@Override
	public boolean isSaving() {
		// TODO Auto-generated method stub
		return isSaving;
	}

	public void pickImageFromGallery()
	{
		if(Build.VERSION.SDK_INT<19)
		{
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
	        try {
	            startActivityForResult(intent, REQUEST_PICK);
	        } catch (ActivityNotFoundException e) {
	            Toast.makeText(this, "No image sources found", Toast.LENGTH_SHORT).show();
	        }
		}
		else
		{
			Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		    startActivityForResult(i, REQUEST_PICK);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_PICK && resultCode == RESULT_OK && data!=null) 
		{
			sourceUri = data.getData();
			setupImage();
		}
	}
	
	void setupImage()
	{
		if (sourceUri != null) {
            exifRotation = CropUtil.getExifRotation(CropUtil.getFromMediaUri(getContentResolver(), sourceUri));

            InputStream is = null;
            try {
                sampleSize = CropUtil.calculateBitmapSampleSize(this, sourceUri);
                is = getContentResolver().openInputStream(sourceUri);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inSampleSize = sampleSize;
                rotateBitmap = new RotateBitmap(BitmapFactory.decodeStream(is, null, option), exifRotation);
            } catch (IOException e) {
               e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            } finally {
                CropUtil.closeSilently(is);
            }
            
            initCropView();
		}
	}
	
	void initCropView()
	{
		if(rotateBitmap!=null)
        {
        	 imageView.setImageRotateBitmapResetBase(rotateBitmap, true);
        	 
        	 CropView cv=new CropView(imageView);
        	 final int width = rotateBitmap.getWidth();
             final int height = rotateBitmap.getHeight();

             Rect imageRect = new Rect(0, 0, width, height);
             
             float cropWidth = CROP_WIDTH;
             float cropHeight = CROP_HEIGHT;
             
             if(width<cropWidth)
             {
            	 cropWidth  = cropHeight = width-20;
             }
             else if(height<cropHeight)
             {
            	 cropHeight = cropWidth = height - 20;
             }
             
             MIN_SIZE = Math.max(cropWidth, cropHeight);
             
             float x = (width - cropWidth) / 2;
             float y = (height - cropHeight) / 2;

             RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
             cv.setup(imageView.getUnrotatedMatrix(), imageRect, cropRect);
             imageView.add(cv);
             imageView.invalidate();
             
             if(imageView.mCropView!=null)
             {
            	 cropView=imageView.mCropView;
             }
        }
	}
	
	 private float dpToPx(float dp) {
	        return dp * imageView.getResources().getDisplayMetrics().density;
	    }
	
	 void clearImageView() 
	 {
        imageView.clear();
        if (rotateBitmap != null) {
            rotateBitmap.recycle();
        }
        System.gc();
	 }
	 
	 public void cropImage()
	 {
		 if(cropView==null || isSaving())
		 {
			 return;
		 }
		 isSaving = true;
		 //release memory
		 clearImageView();
		 
		 Bitmap croppedImage = null;
	     Rect r = cropView.getScaledCropRect(sampleSize);
         int width = r.width();
         int height = r.height();

	     int outWidth = width, outHeight = height;
         if (OUTPUT_WIDTH > 0 && OUTPUT_HEIGHT > 0 && (width > OUTPUT_WIDTH || height > OUTPUT_HEIGHT)) {
            float ratio = (float) width / (float) height;
            if ((float) OUTPUT_WIDTH / (float) OUTPUT_HEIGHT > ratio) {
                outHeight = OUTPUT_HEIGHT;
                outWidth = (int) ((float) OUTPUT_HEIGHT * ratio + .5f);
            } else {
                outWidth = OUTPUT_WIDTH;
                outHeight = (int) ((float) OUTPUT_WIDTH / ratio + .5f);
            }
         }

	     try {
             croppedImage = CropUtil.decodeRegionCrop(this, sourceUri, r);
         } catch (IllegalArgumentException e) {
        	 e.printStackTrace();
        	 isSaving = false;
             return;
         }
	        
        if (croppedImage != null) {
         imageView.setImageRotateBitmapResetBase(new RotateBitmap(croppedImage, exifRotation), true);
         imageView.center(true, true);
         imageView.mCropView=null;
         cropView=null;
         
         saveImage(croppedImage);
        }
        else
        {
        	isSaving=false;
        }
	   
	 }
	 
	 void saveImage(Bitmap croppedImage)
	 {
		if (outputUri != null) 
        {
            OutputStream outputStream = null;
            try {
                outputStream = getContentResolver().openOutputStream(outputUri);
                if (outputStream != null) {
                    croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                }
            } catch (IOException e) {
               e.printStackTrace();
            } finally {
                CropUtil.closeSilently(outputStream);
            }

            CropUtil.copyExifRotation(
                    CropUtil.getFromMediaUri(getContentResolver(), sourceUri),
                    CropUtil.getFromMediaUri(getContentResolver(), outputUri)
            );
        }

        /*final Bitmap b = croppedImage;
        handler.post(new Runnable() {
            public void run() {
                b.recycle();
            }
        });*/
        isSaving=false;
	 }
	
	@Override
    protected void onDestroy() {
        super.onDestroy();
        if (rotateBitmap != null) {
            rotateBitmap.recycle();
        }
    }
}
