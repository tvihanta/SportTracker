package com.vihanta.sportkeeper;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { MainModule.class })
interface MainComponent {

    void inject(LocationService loc);
    void inject(MapsActivity mapsActivity);
    void inject(TrailModel trailModel);
    void inject(ResultDialogFragment resultDialogFragment);


}
