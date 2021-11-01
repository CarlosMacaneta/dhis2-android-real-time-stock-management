package com.baosystems.icrc.psm.viewmodels.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baosystems.icrc.psm.data.models.ReviewStockData
import com.baosystems.icrc.psm.service.StockManager
import com.baosystems.icrc.psm.service.scheduler.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable

class ReviewStockViewModelFactory(
    private val disposable: CompositeDisposable,
    private val schedulerProvider: BaseSchedulerProvider,
    private val stockManager: StockManager,
    private val data: ReviewStockData
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewStockViewModel::class.java)) {
            return ReviewStockViewModel(
                disposable,
                schedulerProvider,
                stockManager,
                data
            ) as T
        }

        throw IllegalAccessException("Unknown ViewModel class")
    }
}