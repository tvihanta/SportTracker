package com.vihanta.sportkeeper;

import android.app.Application;

public class SKapp extends Application {

    private MainComponent mMainComponent;

    @Override
    public void onCreate(){
        super.onCreate();
        mMainComponent =  DaggerMainComponent.builder()
                // list of modules that are part of this component need to be created here too
                .mainModule(new MainModule())
                .build();
    }

    MainComponent getmMainComponent(){
        return mMainComponent;
    }


}
