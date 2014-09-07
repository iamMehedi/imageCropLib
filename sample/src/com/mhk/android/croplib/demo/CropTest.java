package com.mhk.android.croplib.demo;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.mhk.android.croplib.BaseActivity;
import com.mhk.android.croplib.CropImageView;
import com.mhk.android.croplib.Recycler;
import com.mhk.android.croplib.demo.R;

@SuppressLint("NewApi")
public class CropTest extends BaseActivity implements OnClickListener{

	Button pick, crop;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crop_view);
		
		imageView = (CropImageView) findViewById(R.id.crop_image);
		
		pick=(Button) findViewById(R.id.pick);
		crop=(Button) findViewById(R.id.crop);
		
		pick.setOnClickListener(this);
		crop.setOnClickListener(this);
		
		imageView.setRecycler(new Recycler() {

			@Override
			public void recycle(Bitmap b) {
				// TODO Auto-generated method stub
				 b.recycle();
	             System.gc();
			}
		});
	}

	final int REQUEST_PICK=121;
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
		{
			case R.id.pick:
				pickImageFromGallery();
			break;
			
			case R.id.crop:
				cropImage();
			break;
		}
	}
}
