package com.datonicgroup.narrate.app.ui.entries;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.ui.base.BaseActivity;
import com.datonicgroup.narrate.app.util.LogUtil;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by timothymiko on 11/14/14.
 */
public class ViewPhotoActivity extends BaseActivity {

    private ImageView mImageView;

    private Bitmap mImage;
    private PhotoViewAttacher mAttacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_photo);

        Bundle extras = getIntent().getExtras();

        if ( extras == null ) {
            finish();
            return;
        }

        Photo photo = extras.getParcelable("photo");
        LogUtil.log(ViewPhotoActivity.class.getSimpleName(), "Photo Path: " + photo.path);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        mImage = BitmapFactory.decodeFile(photo.path, options);

        if ( mImage.getWidth() > 4096 || mImage.getHeight() > 4096 ) {
            float ratio = (float) mImage.getHeight() / (float) mImage.getWidth();
            int width = mImage.getWidth();
            int height = 0;

            if ( width > 4096 ) {
                width = 4096;
                height = Math.round(width * ratio);
            } else {
                height = 4096;
                width = Math.round(height / ratio);
            }

            mImage = Bitmap.createScaledBitmap(mImage, width, height, false);
        }


        mImageView.setImageBitmap(mImage);
        mAttacher = new PhotoViewAttacher(mImageView);
    }

    @Override
    protected void assignViews() {
        super.assignViews();
        mImageView = (ImageView) findViewById(R.id.image);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImage.recycle();
    }
}
