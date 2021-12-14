package com.baosystems.icrc.psm.ui.base

import org.hisp.dhis.rules.models.RuleEffect

interface ItemWatcher<A, B, C> {
    fun quantityChanged(item: A, value: B?, callback: OnQuantityValidated?)
    fun updateStockOnHand(item: A, value: C, pos: Int)
    fun getQuantity(item: A): B?
    fun getStockOnHand(item: A): C?
    fun removeItem(item:A)

    interface OnQuantityValidated {
        fun validationCompleted(ruleEffects: List<RuleEffect>)
    }
}