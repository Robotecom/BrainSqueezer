package com.puzzlemind.brainsqueezer.scambled

import android.util.Log
import androidx.annotation.Keep
import java.util.*


class PuzzleEngine(private var shuffle: Boolean){

    var numberHintShown: Boolean = false
    private val tileList = mutableListOf<PuzzleTile>()
    val TAG = "PuzzleEngine"
    private var puzzleSize: Int = 0
    private lateinit var defaultConfig:List<Int>


    fun makePuzzle(tileListRes: MutableList<Int>,puzzleDimen: Int,defaultConfi: List<Int>) {
        puzzleSize = puzzleDimen
        defaultConfig = defaultConfi

        tileListRes.forEachIndexed{index: Int, resourceId: Int ->

            tileList.add(PuzzleTile(order = index,
                currentPos = index,
                resourceId = resourceId,
                uri = ""
                ))

        }


        tileList[tileList.lastIndex] = tileList[tileList.lastIndex].copy(isBlank = true)

        prepareTiles()


    }

    fun makePuzzleFromFiles(tileUris: MutableList<String>,puzzleDimen: Int,defaultConfi: List<Int>) {

        puzzleSize = puzzleDimen
        defaultConfig = defaultConfi

        tileUris.forEachIndexed{index: Int, uri: String ->

            tileList.add(PuzzleTile(order = index,
                currentPos = index,
                resourceId = 0,
                uri = uri
            ))

        }


        tileList[tileList.lastIndex] = tileList[tileList.lastIndex].copy(isBlank = true)

        prepareTiles()


    }

    private fun prepareTiles() {

        if (shuffle)
            shuffleTiles()
        else{
            if (defaultConfig.isEmpty()){
                shuffleTiles()
            }else
            defaultConfig(defaultConfig = defaultConfig)
        }

        makeItSolvable(matrixSize = puzzleSize,inversion = countInversions())
        updateDirections(puzzleSize)
    }

    //shuffle here is done with changing currentPos of each tile to reflect
    //the new change in pos
    private fun shuffleTiles() {

        tileList.shuffle()
        tileList.forEachIndexed{index, puzzleTile ->

            puzzleTile.currentPos = index
        }


    }

    private fun updateDirections(puzzleDimen: Int) {

        Log.d(TAG, "updateDirections: ")
        val spaceTile = tileList.find { it.isBlank }!!

        tileList.forEach {it.direction = TileDirection.CENTER }

        val adjacentTilesIndices =
            getAdjacentTiles(puzzleDimen, spaceTile.currentPos / puzzleDimen, spaceTile.currentPos % puzzleDimen)

        for (item in adjacentTilesIndices){
            tileList[item.index].direction = item.direction
        }


    }

    private fun getAdjacentTiles(matrixSize: Int, n: Int, m: Int): MutableList<AdjacentTile> {
        println("size:$matrixSize ,space n:$n , m:$m")
        val list = mutableListOf<AdjacentTile>()
        //top adjacent tile
        if (n - 1 >= 0) {
            list.add(AdjacentTile(matrixSize * (n - 1) + m, TileDirection.DOWN))
        }
        //bottom adjacent tile
        if (n + 1 < matrixSize) {
            list.add(AdjacentTile(matrixSize * (n + 1) + m, TileDirection.UP))
        }
        //left adjacent tile
        if (m - 1 >= 0) {
            list.add(AdjacentTile(matrixSize * n + m - 1, TileDirection.RIGHT))
        }
        //right adjacent tile
        if (m + 1 < matrixSize) {
            list.add(AdjacentTile(matrixSize * n + m + 1, TileDirection.LEFT))
        }

        for (item in list) {
            println("getAdjacentIndices: $item")
        }
        return list
    }


    fun moveTile(from:Int){

        Log.d(TAG, "moveTile: from:$from")
        val spaceIndex = tileList.find { it.isBlank }!!.currentPos

        Collections.swap(tileList,from,spaceIndex)

        tileList[spaceIndex].currentPos = spaceIndex
        tileList[from].currentPos = from

        Log.d(TAG, "moveTile: space:${tileList[from].currentPos}: tileMoved:${tileList[spaceIndex].currentPos}")
        updateDirections(puzzleDimen = puzzleSize)
        Log.d(TAG, "moveTile: listHashafter:${tileList.hashCode()}")



    }

    fun getTileList():List<PuzzleTile>{

        Log.d(TAG, "getTileList: ${tileList}")
        return tileList.toList()
    }

    private fun moveFromTo(from: Int, to:Int){

        Log.d(TAG, "moveFromTo: from:$from to:$to")
        val fromTile = tileList[from].copy(currentPos = to)
        val toTile = tileList[to].copy(currentPos = from)
        tileList[from] = toTile
        tileList[to] = fromTile


        Log.d(TAG, "moveFromTo: fromTileAfter:${tileList[from]} toTileAfter:${tileList[to]}")
        Log.d(TAG, "moveFromTo: countInversions:${countInversions()}")

    }



    private fun makeItSolvable(matrixSize: Int, inversion: Int) {
        val e = (tileList.find { it.isBlank }!!.currentPos / matrixSize) + 1
        println("Inversion make it solvable:$inversion e:$e")

        //if puzzle size is of even number
        if (matrixSize % 2 == 0) {
            if ((inversion + e) % 2 != 0) {
                if (e == 1) {//swap element far away from space
                    moveFromTo( 14, 15)
                } else {
                    moveFromTo( 0, 1)
                }
                println("Inversion + e is odd changing it to:even")
            }
            println("Inversions for even:$inversion")

        } else {
            //else if size is of odd number
            println("Inversions for odd:$inversion")
            if (inversion % 2 != 0) {
                if (e != 1) {
                    moveFromTo( 0, 1)
                } else {
                    moveFromTo( 7, 8)
                }
            }
        }
    }


    private fun countInversions(): Int {
        var inversion = 0
        for (index in 0 until tileList.size - 1) {

            val firstTileSelected = tileList[index]
            val i = firstTileSelected.order

            if (firstTileSelected.isBlank || i == 0) {
                continue
            }

            for (item in index + 1 until tileList.size) {

                val secondTileSelected = tileList[item]
                val j = secondTileSelected.order

                //Omit calculating inversions with empty tile which is of value 100
                if (secondTileSelected.isBlank) {
                    println("Omitting j:$j")
                    continue
                }

                if (i > j) {
                    inversion++
                }
            }
        }

        return inversion
    }

    fun isSolved( matrixSize: Int = puzzleSize): Boolean {
        var done = true
        for (item in 0 until tileList.size) {
            val value = tileList[item].order
            if (item == tileList.size - 1) {
                //omitting last item space
                continue
            }
            val second = item + 1
            println("checkIfDone:$value")
            val j: Int = if (second < matrixSize * matrixSize) {
                tileList[second].order
            } else {
                item
            }
            if (value > j) {
                println("not complete yet")
                done = false
                break
            }

        }

        return done
    }

    fun restart() {
        showBlankTile(false)
        shuffle = true
        Log.d(TAG, "restart: defaultConfig:${defaultConfig}")
        prepareTiles()
    }



    fun showNumberHint(b: Boolean) {

        numberHintShown = b
        tileList.forEach {
            it.hintShown = b
        }
    }

    fun showBlankTile(state:Boolean): List<PuzzleTile> {
         tileList.find { it.order == puzzleSize * puzzleSize - 1}?.isBlank = !state
        return tileList.toMutableList()
    }

    private fun defaultConfig(defaultConfig: List<Int>) {

        defaultConfig.forEachIndexed {index,value ->
            Log.d(TAG, "defaultConfig: $value")

            moveFromTo(index,tileList.find { it.order == value }?.currentPos!!)

        }
        Log.d(TAG, "defaultConfig: $tileList")

        Log.d(TAG, "defaultConfig: enddddddddddddddddd")


    }

}

@Keep
data class PuzzleTile(val order:Int,        //Right order of tile in solved puzzle in zero-based numbering
                      var currentPos:Int = 0,        //current Position in scrambled puzzle in zero-based numbering
                      val resourceId:Int,
                      var direction:TileDirection = TileDirection.CENTER,   //allowed direction of movement
                      var hintShown:Boolean = false,
                      var isBlank:Boolean = false,
                      var uri:String = ""

){
    override fun equals(other: Any?): Boolean {

        return other is PuzzleTile
                && other.resourceId == this.resourceId
                && other.currentPos == this.currentPos

    }

    override fun hashCode(): Int {
        return arrayOf(
            this.currentPos,
            this.resourceId,
            this.order,
            this.direction,
            this.isBlank
        ).contentHashCode()
    }
}

@Keep
data class AdjacentTile(val index:Int,val direction:TileDirection)
