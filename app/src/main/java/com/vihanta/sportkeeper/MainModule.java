package com.vihanta.sportkeeper;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = ContextModule.class)
public class MainModule {


    // Dagger will only look for methods annotated with @Provides
    @Provides
    @Singleton
        // SKapp reference must come from AppModule.class
    TrailModel providesModel() {
        TrailModel model = new TrailModel();
        model.init();
        return model;
    }


    // Dagger will only look for methods annotated with @Provides
    @Provides
    @Singleton
    // SKapp reference must come from AppModule.class
    Utils providesUtils() {
        Utils utils = new Utils();
        return utils;
    }



}
