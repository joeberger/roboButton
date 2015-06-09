package com.ndipatri.roboButton.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.ImageView;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class TestUtils {


    /**
     * Creates a matcher of {@link ImageView}s that matches when an examined ImageView
     * has the same Bitmap as resource with given resourceId
     * <pre>
     * Example:
     * onView(withId(R.id.imageDown)).check(matches(isBitmapTheSame(mActivity, R.drawable.ic_down)));
     * </pre>
     *
     * @param context    - activity's context
     * @param resourceId - the id of resource with expected bitmap
     * @return Matcher<View> for given resourceId
     */
    public static Matcher<View> isBitmapTheSame(final Context context, final int resourceId) {
        return new BoundedMatcher<View, ImageView>(ImageView.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("has the same Bitmap as the resource with id=");
                description.appendValue(resourceId);
                description.appendText(" has.");
            }

            @Override
            public boolean matchesSafely(ImageView view) {
                Bitmap bitmapCompare = BitmapFactory.decodeResource(context.getResources(), resourceId);
                Drawable drawable = view.getDrawable();
                Bitmap bitmap = drawableToBitmap(drawable);
                boolean result = bitmapCompare.sameAs(bitmap);
                return result;
            }
        };
    }

    /**
     * Creates a matcher of {@link ImageView}s that matches when an examined ImageView
     * has the same Bitmap in Drawable as
     * the specified <code>operand</code>
     * <pre>
     * Example:
     * onView(withId(R.id.imageDown)).check(matches(isBitmapTheSame(theDrawableWithExpectedBitmap)));
     * </pre>
     *
     * @param the Drawable with expected Bitmap
     * @return Matcher<View> for given Drawable
     */
    public static Matcher<View> isBitmapTheSame(final Drawable drawable) {
        return new BoundedMatcher<View, ImageView>(ImageView.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("has the same Bitmap as this drawable: ");
                description.appendValue(drawable);
                description.appendText(" has.");
            }

            @Override
            public boolean matchesSafely(ImageView view) {
                Bitmap bitmapCompare = drawableToBitmap(drawable);
                Drawable drawable = view.getDrawable();
                Bitmap bitmap = drawableToBitmap(drawable);
                return bitmapCompare.sameAs(bitmap);
            }
        };
    }

    /**
     * This helper method extracts Bitmap from Drawable
     *
     * @param drawable - The drawable to convert to bitmap
     * @return Bitmap from the given drawable
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
