package com.baosystems.icrc.psm.service

import io.reactivex.Observable
import org.hisp.dhis.android.core.arch.call.D2Progress

interface SyncManager {
    fun metadataSync(): Observable<D2Progress>
    fun dataSync(program: String): Observable<D2Progress>
}