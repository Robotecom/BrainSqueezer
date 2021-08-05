package com.puzzlemind.brainsqueezer.scambled.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.firebase.firestore.FieldValue
import com.puzzlemind.brainsqueezer.data.Game
import com.puzzlemind.brainsqueezer.data.GameConverter
import com.puzzlemind.brainsqueezer.profile.GameType
import com.puzzlemind.brainsqueezer.scambled.ScrambledDifficulty
import kotlinx.parcelize.Parcelize

data class Puzzle(
    var fileName:String = "",
    var downLoadUrl:String = "",
    var difficulty:String = "",
    var puzzleSize:Int = 0,
    var defaultConfig:List<Int> = listOf(),
    var tag:MutableList<String> = mutableListOf(),
    var name:String = "",           //shown name of puzzle to users
    var timeToSolve:Int = 0,
    var ownerName:String = "",
    var ownerId:String = "",
    var group:String = "",
    var level:Int = 0,
    var numberHintTime:Int = 0,
    var previewHintTime:Int = 0
)

@Keep
@Parcelize
@Entity(tableName = "scrambled_level_table")
data class ScrambledLevel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var level: Int = 1,
    var preview:String = "",
    var hintPreview:String = "",
    var difficulty:Int = ScrambledDifficulty.Easy.index,
    var puzzleSize:Int = 1,
    var fileName:String = "",
    var downloadUrl:String = "",
    var previewUrl:String = "",
    var defaultConfig:List<Int> = listOf(),
    var tags:List<String> = listOf(),
    var isResource:Boolean = false,
    var highScore:Int = 0,
    var trophy:Boolean = false,
    var timeToSolve:Int = 0,        //a minimum time to solve puzzle if user exceeds it he lose a star
    var hintNumber:Int = 0,         //a number specifying number of hints available for user
    var hintImage:Int = 0,          //number indicating number of hint images available for user
    var minimumMovesCount:Int = 0,  //a number indicating the minimum number of moves if user exceeds this number he lose a star
    var stars:Int = 0,
    var isOpen:Boolean = false,
    var isPassed:Boolean = false,
    var scheduled:Boolean = false,
    var name:String = "",           //Name that appears to users
    var fid:String = "",            //firebase id

) : Parcelable{

    fun getMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String,Any>()

        map["level"] = level
        map["game"] = GameConverter.getIntForGame(Game.SCRAMBLED)
        map["score"] = highScore
        map["stars"] = stars
        map["trophy"] = trophy
        map["timestamp"] = FieldValue.serverTimestamp()

        return map
    }
}


class ListConverter {
    @TypeConverter
    fun fromListToString(list:List<Int>):String{
        return list.toString()
    }

    @TypeConverter
    fun  fromStringToList(listStr:String):List<Int>{

        val list = listStr.removePrefix("[")
            .removeSuffix("]")
        if (list.contains(","))
            return list
            .split(",").map { it.trim().toInt() }
        else return listOf()

    }

}

@Keep
@Entity(tableName = "scrambled_dashboard_table")
data class ScrambledDashboard(
    @PrimaryKey(autoGenerate = true) val id:Int = 0,
    var skillfulness:Float = 0f,
    var points:Int = 0,
    var matchWon:Int = 0,
    var matchLost:Int = 0,
    var totalMatch:Int = 0,
    var easySkillful:Int = 0,
    var mediumSkillful:Int = 0,
    var hardSkillful:Int = 0,
    var trophies:Int = 0,
    var stars:Int = 0
)