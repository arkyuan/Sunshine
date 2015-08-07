package app.com.ark.android.sunshine;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by ark on 8/5/2015.
 */
public class SunshineApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(
                                Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(
                                Stetho.defaultInspectorModulesProvider(this))
                        .build());
        // Your normal application code here.  See SampleDebugApplication for Stetho initialization.
    }
}
