package com.carto.advancedmap.test;


import android.app.Activity;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitor;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.carto.advancedmap.main.ActivityData;
import com.carto.advancedmap.main.GalleryRow;
import com.carto.advancedmap.main.MainActivity;
import com.carto.advancedmap.baseclasses.activities.MapBaseActivity;
import com.carto.advancedmap.main.Sample;
import com.carto.advancedmap.main.Samples;
import com.carto.advancedmap.test.Utils.PermissionGranter;
import com.carto.advancedmap.test.Utils.Screenshot;
import com.carto.graphics.Bitmap;
import com.carto.renderers.RendererCaptureListener;
import com.carto.ui.MapView;
import com.carto.utils.BitmapUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class Test1 {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    private static MainActivity mainActivity;

    /**
     * This test opens the application, clicks on a mainActivity item to open a MapView sample,
     * waits 4 seconds in order to render the map and take a screenshot
     * (if it's not rendered in that time, the screenshot is not taken),
     * then goes back and opens the next MapView sample
     */
    @Test
    public void test1() {

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mainActivity = (MainActivity) getCurrentActivity();
                mainActivity.unlockScreen();
            }
        });

        /**
         * Android 6.0 requires runtime permission for write access as well, god damnit.
         * and folder creation is a context-based operation, so it'll need to be in the activity.
         * ... and since we're saving screenshots, just do it at the initial step
         */
        String path = Screenshot.getDirectory() + Screenshot.DEVICE_FARM_ESPRESSO_SCREEN_DIRECTORY;
        mainActivity.mkFolder(path);
        new PermissionGranter().allowPermissionsIfNeeded();

        Sample[] maps = Samples.LIST;

        /**
         * I have no idea what's going on here, Android's Espresso API is super weird,
         * but apparently this snippet returns an "interaction" where we can perform clicks.
         */
        DataInteraction interaction = onData(allOf(is(instanceOf(GalleryRow.class))));

        for (Integer j = 0; j < maps.length; j++) {

            Sample map = maps[j];
            String text = "Testing sample: " + map.title + " - " + map.description;
            System.out.println(text);

            try {
                interaction.atPosition(j).perform(click());
                new PermissionGranter().allowPermissionsIfNeeded();
                final android.graphics.Bitmap[] screenshot = new android.graphics.Bitmap[1];

                getInstrumentation().runOnMainSync(new Runnable() {
                    @Override
                    public void run() {
                        Activity current = getCurrentActivity();

                        /**
                         * Check if it inherits from MapBaseActivity. Some activities do not, but that's fine,
                         * we can see enough of the map even if we ignore those activities
                         */
                        Boolean isAssignable = MapBaseActivity.class.isAssignableFrom(current.getClass());

                        if (isAssignable) {
                            final MapView map = ((MapBaseActivity)current).contentView.mapView;
                            /**
                             * Using our SDK's built-in function to capture screenshots,
                             * as Android's screenshot takes black screenshots when taking pictures of GLSurfaceView.
                             * A more detailed explanation:
                             * https://stackoverflow.com/questions/5514149/capture-screen-of-glsurfaceview-to-bitmap
                             */
                            map.getMapRenderer().captureRendering(new RendererCaptureListener() {

                                @Override
                                public void onMapRendered(Bitmap bitmap) {
                                    super.onMapRendered(bitmap);
                                    screenshot[0] = BitmapUtils.createAndroidBitmapFromBitmap(bitmap);
                                    map.getMapRenderer().setMapRendererListener(null);
                                }
                            }, true);
                        }

                        /**
                         * MapView rendering takes some time.
                         * This 2 * 2 second delay is a hacky fail-safe to ensure the screenshot has actually been taken.
                         *
                         * There's probably a more elegant solution, but it's a damn auto-test
                         * with weird threading issues: pressBack() does not work when run on the main thread,
                         * so we can't place this entire logic in the runnable
                         */
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (screenshot[0] == null) {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (screenshot[0] != null) {
                            /**
                             * Slightly modified screenshot taker for Espresso tests within AWS Device Farm
                             * (link to source in file) in order to save our MapView's screenshots,
                             * not plain 'ol Android screenshots.
                             */
                            Screenshot.take(current, screenshot[0]);
                        }
                    }

                });

                pressBack();

            } catch (Exception e) {
                /**
                 * I wrote this some time ago, edited/improved it in September 2017 for AWS Device farm
                 * and I have no idea why there is a try-catch block here, but let's leave it here just in case.
                 * Java is mostly try-catch anyway, so it looks quite natural.
                 */
                Log.d("TEST: ", e.getLocalizedMessage());
                Log.d("COUNTER: ", j.toString());
            }
        }

    }

    static Activity getCurrentActivity() {

        ActivityLifecycleMonitor registry = ActivityLifecycleMonitorRegistry.getInstance();
        Collection<Activity> activities = registry.getActivitiesInStage(Stage.RESUMED);

        if (activities.size() > 0) {
            return (Activity) activities.toArray()[0];
        }
        return null;
    }
}
