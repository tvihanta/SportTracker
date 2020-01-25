package com.vihanta.sportkeeper;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.location.Location;

import java.util.List;

import javax.inject.Inject;

public class TrailViewModel extends ViewModel {

    @Inject
    TrailModel mModel;

    //MutableLiveData<List<Location>> trail = null;

    public MutableLiveData<List<Location>> getTrail(){
        return mModel.getTrail();
    }

}
