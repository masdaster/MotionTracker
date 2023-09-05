package io.github.masdaster.motion_tracker;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

/**
 * Created by Z-Byte on .
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
