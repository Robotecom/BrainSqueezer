package com.puzzlemind.brainsqueezer.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.mcq.RewardedVideoAdCallBacks
import java.io.File

@Suppress("DEPRECATION")
fun isOnline(context: Context?): Boolean {
    var connected = false
    @Suppress("LiftReturnOrAssignment")
    context?.let {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = cm.activeNetwork ?: return false
            val actNw = cm.getNetworkCapabilities(networkCapabilities) ?: return false
            connected = actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val netInfo = cm.activeNetworkInfo
            connected = netInfo?.isConnectedOrConnecting == true
        }
    }
    return connected
}


class AdManager(val context: Activity,val adUnitId:String, val rewardedAdCallback: RewardedVideoAdCallBacks){

    var rewardedAd:RewardedAd? = null
    private var finished: Boolean = false
    var loaded: Boolean = false
    var loading:Boolean = false
    var showing:Boolean = false
    var pendingShow:Boolean = false
    var hasError:Boolean = false

     fun loadRewardedAd(){
        println("VidRewardAd: Loading reward ad..........")
        hasError = false
        if (loading){
            println("VidRewardAd: currently loading return")
            return
        }
        else loading = true

        val request = AdRequest.Builder().build()
        RewardedAd.load(context,adUnitId,request,object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                loading = false
                println( adError?.message)
                rewardedAd = null
                hasError = true
                loaded = false
                rewardedAdCallback.onError(adError.message)
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                println( "VidRewardAd: loadRewardedAd: Ad was loaded.")
                if (finished)return
                loading = false
                loaded = true
                this@AdManager.rewardedAd = rewardedAd
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        println("VidRewardAd: onAdDismissedFullScreenContent: ")
                        showing = false
                        pendingShow = false
                        rewardedAdCallback.onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        println("VidRewardAd: onAdFailedToShowFullScreenContent: ${adError?.message}")
                        showing = false
                        hasError = true
                        pendingShow = false
                        loaded = false
                        rewardedAdCallback.onError(adError?.message!!)
                    }

                    override fun onAdShowedFullScreenContent() {
                        println("VidRewardAd: onAdShowedFullScreenContent: ")
                        // Called when ad is dismissed.
                        // Don't set the ad reference to null to avoid showing the ad a second time.

                        rewardedAdCallback.onAdShowing()
                        showing = true
                        this@AdManager.rewardedAd = null
                        pendingShow = false
                        loaded = false
                    }
                }

                if (pendingShow ){
                    showAd(context = context)
                }

            }
        })
    }

    fun showAd(context: Activity,){
        pendingShow = true

        println("VidRewardAd: showAd")
        rewardedAd?.show(context){
            println("VidRewardAd: reward earned")

            rewardedAdCallback.onRewardEarned()
        }



    }

    fun clear() {

        rewardedAd = null
        finished = true
    }
}


fun File.deleteDirectory(): Boolean {
    return if (exists()) {
        listFiles()?.forEach {
            if (it.isDirectory) {
                it.deleteDirectory()
            } else {
                it.delete()
            }
        }

        delete()
    } else false
}

sealed class Result{

    object Idle : Result()
    object Loading:Result()
    data class Success(val resId: Int,val isEmpty: Boolean = false):Result()
    data class Failure(val resId:Int ) :Result()

}


