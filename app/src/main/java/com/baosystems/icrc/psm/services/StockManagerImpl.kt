package com.baosystems.icrc.psm.services

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.baosystems.icrc.psm.commons.Constants
import com.baosystems.icrc.psm.data.AppConfig
import com.baosystems.icrc.psm.data.models.*
import com.baosystems.icrc.psm.utils.AttributeHelper
import com.baosystems.icrc.psm.utils.ConfigUtils.getTransactionDataElement
import com.baosystems.icrc.psm.utils.toDate
import io.reactivex.Single
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.event.EventCreateProjection
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitMode
import org.hisp.dhis.android.core.program.ProgramStage
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import java.util.*
import javax.inject.Inject

class StockManagerImpl @Inject constructor(
    val d2: D2, val config: AppConfig
): StockManager {

    override fun search(
        query: SearchParametersModel,
        ou: String?
    ): LiveData<PagedList<StockItem>> {
        var teiRepository = d2.trackedEntityModule().trackedEntityInstanceQuery()

        if (!ou.isNullOrEmpty())
            teiRepository.byOrgUnits()
                .eq(ou)
                .byOrgUnitMode()
                .eq(OrganisationUnitMode.SELECTED)
                .also { teiRepository = it }

        teiRepository.byProgram()
            .eq(config.program)
            .also { teiRepository = it }

        if (!query.name.isNullOrEmpty()) {
            teiRepository
                .byQuery()
                .like(query.name).also { teiRepository = it }
        }

        if (!query.code.isNullOrEmpty()) {
            teiRepository
                .byQuery()
                .eq(query.code)
                .also { teiRepository = it }
        }

        teiRepository.orderByAttribute(config.itemName)
            .eq(RepositoryScope.OrderByDirection.ASC)
            .also { teiRepository = it }

        val dataSource: DataSource<TrackedEntityInstance, StockItem> = teiRepository.dataSource
            .mapByPage(this::filterDeleted)
            .mapByPage { transform(it, config) }

        return LivePagedListBuilder(object : DataSource.Factory<TrackedEntityInstance, StockItem>() {
            override fun create(): DataSource<TrackedEntityInstance, StockItem> {
                return dataSource
            }
        }, Constants.ITEM_PAGE_SIZE).build()
    }

    private fun transform(teis: List<TrackedEntityInstance>, config: AppConfig): List<StockItem> {
        return teis.map { tei ->
            StockItem(
                tei.uid(),
                AttributeHelper.teiAttributeValueByAttributeUid(tei, config.itemName) ?: "",
                getStockOnHand(tei, config.stockOnHand) ?: ""
            )
        }
    }

    private fun getStockOnHand(tei: TrackedEntityInstance, stockOnHandUid: String): String? {
        val events = d2.eventModule()
            .events()
            .byTrackedEntityInstanceUids(Collections.singletonList(tei.uid()))
            .byDataValue(stockOnHandUid).like("")
            .byDeleted().isFalse
            .withTrackedEntityDataValues()
            .blockingGet()

        events.forEach { event ->
            event.trackedEntityDataValues()?.forEach { dataValue ->
                dataValue.dataElement().let { dv ->
                    if (dv.equals(stockOnHandUid)) {
                        return dataValue.value()
                    }
                }
            }
        }

        return null
    }

    private fun filterDeleted(list: MutableList<TrackedEntityInstance>):
            List<TrackedEntityInstance> {

        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val tei = iterator.next()
            if (tei.deleted() != null && tei.deleted()!!) iterator.remove()
        }

        return list
    }

    private fun addEventProjection(facility: IdentifiableModel,
                                   programStage: ProgramStage,
                                   enrollment: Enrollment): String {
        return d2.eventModule().events().blockingAdd(
            EventCreateProjection.builder()
                .enrollment(enrollment.uid())
                .program(config.program)
                .programStage(programStage.uid())
                .organisationUnit(facility.uid)
                .build()
        )
    }

    override fun saveTransaction(items: List<StockEntry>, transaction: Transaction):
            Single<Unit> {

        return Single.defer{
            d2.programModule()
                .programStages()
                .byProgramUid()
                .eq(config.program)
                .one()
                .get()
                .map { programStage ->
                    items.forEach { entry ->
                        val enrollment = getEnrollment(entry.item.id)
                        addEvent(entry, programStage, enrollment, transaction)
                    }
                }
        }
    }

    private fun addEvent(
        item: StockEntry,
        programStage: ProgramStage,
        enrollment: Enrollment,
        transaction: Transaction
    ) {
        val eventUid = addEventProjection(transaction.facility, programStage, enrollment)

        // Set the event date
        transaction.transactionDate.toDate()?.let {
            d2.eventModule().events().uid(eventUid).setEventDate(it)
        }

        d2.trackedEntityModule().trackedEntityDataValues().value(
            eventUid,
            getTransactionDataElement(transaction.transactionType, config)
        ).blockingSet(item.qty.toString())

        transaction.distributedTo?.let {
            val destination = d2.optionModule()
                .options()
                .uid(it.uid)
                .blockingGet()

            d2.trackedEntityModule().trackedEntityDataValues().value(
                eventUid,
                config.distributedTo
            ).blockingSet(destination.code())
        }
    }

    private fun getEnrollment(teiUid: String): Enrollment {
        return d2.enrollmentModule()
            .enrollments()
            .byTrackedEntityInstance()
            .eq(teiUid)
            .one()
            .blockingGet()
    }
}