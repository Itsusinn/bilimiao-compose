package com.a10miaomiao.bilimiao.compose.ui.tool

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.math.max

class MutableLazyListSaver<T : Any?>(
    private val listState: LazyListState
) : Saver<SnapshotStateList<T>, ArrayList<T>> {
    override fun restore(value: ArrayList<T>): SnapshotStateList<T> {
        return SnapshotStateList<T>().apply { addAll(value) }
    }

    override fun SaverScope.save(value: SnapshotStateList<T>): ArrayList<T> {
        val visible = listState.layoutInfo.visibleItemsInfo
        return if (visible.isEmpty()) {
            arrayListOf()
        } else {
            val first = max(visible.first().index - visible.count(),0)
            val last =  minOf(visible.last().index + visible.count(), value.size)
            ArrayList(value.subList(first, last))
        }
    }
}