package com.baosystems.icrc.psm.viewmodels.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baosystems.icrc.psm.data.models.Transaction
import com.baosystems.icrc.psm.service.StockManager
import com.baosystems.icrc.psm.service.scheduler.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class ManageStockViewModelFactory(
    private val disposable: CompositeDisposable,
    private val schedulerProvider: BaseSchedulerProvider,
    private val stockManager: StockManager,
    private val transaction: Transaction
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(ManageStockViewModel::class.java))
            return ManageStockViewModel(
                disposable,
                schedulerProvider,
                stockManager,
                transaction
            ) as T

        throw IllegalAccessException("Unknown ViewModel class")
    }
}