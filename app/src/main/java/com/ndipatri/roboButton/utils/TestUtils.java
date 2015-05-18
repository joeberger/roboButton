package com.ndipatri.roboButton.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class TestUtils {

    public static Matcher<View> isImageTheSame(final Drawable drawable) {
        return new BoundedMatcher<View, ImageView>(ImageView.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("is image the same as: ");
                description.appendValue(drawable);
            }

            @Override
            public boolean matchesSafely(ImageView view) {
                Bitmap bitmapCompare = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Drawable drawable = view.getDrawable();
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                return bitmapCompare.sameAs(bitmap);
            }
        };
    }
}
