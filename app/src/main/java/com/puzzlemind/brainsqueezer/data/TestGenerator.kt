package com.puzzlemind.brainsqueezer.data

import android.util.Log
import com.puzzlemind.brainsqueezer.AppRepository
import kotlin.random.Random

class TestGenerator(repository: AppRepository) {

    private val repos = repository
    val TAG = "TestGenerator"


        fun getQuestionNotInList(count: Int):Int{


            val indexRequired = Random.nextInt(count)

            return indexRequired

        }

    //this function return a range of rows
    fun createTest(level: Int, range: Int): MutableList<Question> {

        Log.d(TAG, "createTest: from:${level * range} to:${(level + 1) * range}")

        return repos.fetchQuestionList(level * range, range)
    }




}