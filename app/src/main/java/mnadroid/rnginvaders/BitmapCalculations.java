package mnadroid.rnginvaders;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

class BitmapCalculations {
    //Bitmaps rescalen
    static int getScaledBitmapSize(int targetScreenSize, int defaultScreenSize, int bitmapSize) {
        return (bitmapSize * targetScreenSize) / defaultScreenSize;
        //(((targetScreenSize)/(defaultScreenSize))*bitmapSize))
    }

    //X-Y Koordinaten rescalen
    static int getScaledCoordinates(int targetScreenSize, int defaultScreenSize, int defaultCoordinate) {
        return (defaultCoordinate * targetScreenSize) / defaultScreenSize;
    }

    //Einlesen der minimalen Bitmaps (vong Google perösnlich der Algorithmus)
    static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    //Annäherung an eine minimale Größe der Bitmaps vor dem Einlesen
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
