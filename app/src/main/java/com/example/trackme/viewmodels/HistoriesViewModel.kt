package com.example.trackme.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.example.trackme.TrackMeApplication
import com.example.trackme.data.AppDatabase
import com.example.trackme.data.History
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

class HistoriesViewModel : ViewModel() {
    val mHistories = MutableLiveData<List<History>>()
    val mLiveMessage = MutableLiveData<String>()  //live message for errors, or messages

    fun loadAllHistory() {
        loadDatabase().map {
            it.historyDao().getAll()
        }.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mHistories.value = it
            }, {
                mLiveMessage.value = it.localizedMessage
            })
    }

    fun loadDatabase(): Observable<AppDatabase> {
        return Observable.fromCallable {
            Room.databaseBuilder(
                TrackMeApplication.appContext!!,
                AppDatabase::class.java, AppDatabase.NAME
            ).build()
        }
    }
}