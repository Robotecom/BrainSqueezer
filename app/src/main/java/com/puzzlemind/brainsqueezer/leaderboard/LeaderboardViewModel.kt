package com.puzzlemind.brainsqueezer.leaderboard


import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.AppRepository
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.data.LeaderboardItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class LeaderboardViewModel(appRepository: AppRepository) : ViewModel() {

    val TAG = "LeaderboardVM"
    private val _uiState = MutableStateFlow(LeaderboardScreenState())
    val uistate = _uiState.asStateFlow()
    val repository: AppRepository = appRepository

    init {

        viewModelScope.launch {

            emitState(_uiState.value.copy(refreshing = true))
            getLeaderboardListLive()
        }

    }


    private fun getFirstThree(leaderboardList: MutableList<LeaderboardItem>): MutableList<LeaderboardItem> {

        val firstTheeList = mutableListOf<LeaderboardItem>()

        for (item in leaderboardList) {
            if (firstTheeList.size < 3) {
                firstTheeList.add(item)
            } else {
                break
            }
        }
        return firstTheeList
    }

    private suspend fun emitState(leaderboardScreenState: LeaderboardScreenState) {
        _uiState.emit(leaderboardScreenState)
    }


    private suspend fun getLeaderboardListLive() {

        callForNewUpdate(Order.BY_LEVEL)

        repository.getLeaderboardListLive().collect { value: MutableList<LeaderboardItem> ->

            updateUi(value)

        }

    }


    private suspend fun updateUi(value: MutableList<LeaderboardItem>) {
        if (value.isEmpty()) {
            emitState(
                _uiState.value.copy(
                    empty = true,
                )
            )
            return
        }
        val firstThreeList = getFirstThree(value)

        if (firstThreeList.size > 2)
            Collections.swap(firstThreeList, 0, 1)

        Log.d(TAG, "updateUi: after swapping")
        if (firstThreeList.size > 1)
        value.removeAt(0)
        if (firstThreeList.size > 1)
        value.removeAt(0)
        if (firstThreeList.size > 1)
        value.removeAt(0)

        emitState(
            _uiState.value.copy(
                leaderboardList = value,
                firstThreeList = firstThreeList,
                empty = false,
                refreshing = false
            )
        )
    }

    fun onOrderSelected(order: Order){

        _uiState.value = _uiState.value.copy(orderBy = order.orderBy)
        callForNewUpdate(order)
    }

    private fun callForNewUpdate(order:Order) {

        viewModelScope.launch {

            emitState(_uiState.value.copy(refreshing = true))

        }
        Log.d(TAG, "callForNewUpdate: new leaderboard list")
        Firebase.firestore.collection(Order.getCollectionFromOrderStr(order = order))
            .orderBy(Order.getFieldFromOrder(order.orderBy), Query.Direction.DESCENDING)
            .limit(50)
            .get().addOnSuccessListener { snapshot ->

                Log.d(TAG, "callForNewUpdateSize: ${snapshot.size()}")
                if (!snapshot.isEmpty) {

                    val leaderboardItems = mutableListOf<LeaderboardItem>()
                    snapshot.documents.forEachIndexed {index,it ->
                        val leaderItem = it.toObject(LeaderboardItem::class.java)!!
                        leaderItem.rank = index + 1
                        leaderboardItems.add(leaderItem)
                    }

                    viewModelScope.launch {

                        updateUi(leaderboardItems)


                    }

                }else{
                    Log.d(TAG, "callForNewUpdate: snap is Empty")
                }
            }.addOnFailureListener{

                Log.d(TAG, "callForNewUpdate: failure:${it}")
            }


    }


}

class LeaderboardModelFactory(private val appRepository: AppRepository) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {

            return LeaderboardViewModel(appRepository = appRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}

@Keep
data class LeaderboardScreenState(
    val firstThreeList: MutableList<LeaderboardItem> = mutableListOf(),
    val leaderboardList: MutableList<LeaderboardItem> = mutableListOf(),
    val refreshing: Boolean = false,
    val error: ErrorState = ErrorState(false, ""),
    val empty: Boolean = true,
    val orderBy: OrderBy = OrderBy.BY_LEVEL

)

@Keep
data class ErrorState(
    var isError: Boolean,
    val errorMsg: String,
    var hasImage:Boolean = false
)

enum class OrderBy{
    BY_LEVEL, BY_POINT, BY_TROPHY
}

sealed class Order(val orderBy: OrderBy,@StringRes val stringRes: Int){
    object BY_LEVEL:Order(orderBy = OrderBy.BY_LEVEL, R.string.by_level)
    object BY_POINT:Order(orderBy = OrderBy.BY_POINT, R.string.by_point)
    object BY_TROPHY:Order(orderBy = OrderBy.BY_TROPHY, R.string.by_trophy)

    companion object{
        fun  getFieldFromOrder(orderBy: OrderBy):String{
            return when(orderBy){
                OrderBy.BY_POINT -> LeaderboardItem.POINTS
                OrderBy.BY_TROPHY -> LeaderboardItem.TROPHIES
                else -> LeaderboardItem.MAX_LEVEL_MCQ
            }
        }

        fun getCollectionFromOrderStr(order: Order): String {

            return when(order){
                BY_LEVEL -> "MaxLevel"
                BY_POINT -> "MaxPoint"
                BY_TROPHY -> "MaxTrophy"
                else -> "MaxLevel"
            }
        }

        fun  getOrderLabelFromOrder(orderBy: OrderBy):Int{
            return when(orderBy){
                OrderBy.BY_POINT -> R.string.by_point
                OrderBy.BY_TROPHY -> R.string.by_trophy
                else -> R.string.by_level
            }
        }
    }
}