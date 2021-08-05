package com.puzzlemind.brainsqueezer.utils

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.*
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.Constants.PACKAGE_NAME
import com.puzzlemind.brainsqueezer.Constants.PACKAGE_NAME_DEBUG
import java.net.URLEncoder

class LinkSchema{

    companion object{

        const val DOMAIN = "https://brainsqueezer.page.link/"
        const val SCRAMBLED_DOMAIN = "https://scrambled.page.link/"
        private const val SCHEME = "https"
        private const val HOST = "www.brainsqueezer.com"
        private const val MCQ_PATH = "mcq"
        private const val SHARE_APP_PATH = "share"
        private const val LEVEL = "level"
        private const val BASE_URI = "${SCHEME}://${HOST}"
        private const val SLIDING_PUZZLE_PATH = "sliding_puzzle"
        const val SCRAMBLED_DIFFICULTY = "difficulty"

        const val USER_ID = "userid"

        fun getLinkOfMCQ(level:Int,userId:String):String{

            return "${BASE_URI}/${MCQ_PATH}?${LEVEL}=${level}&${USER_ID}=${userId}"
        }

        fun getShareAppLink(userId:String): String {
            return "${BASE_URI}/${SHARE_APP_PATH}?${USER_ID}=${userId}"
        }

        fun getLinkOfSlidingPuzzle(level: Int, scrambledDifficulty: Int, userId: String): String {

            val parameters = "${SCRAMBLED_DIFFICULTY}=${scrambledDifficulty}&${LEVEL}=${level}&${USER_ID}=${userId}"
            return "${BASE_URI}/${SLIDING_PUZZLE_PATH}?${URLEncoder.encode(parameters,"UTF-8")}"
        }
    }
}

class DynamicLinksCreator {

    companion object{

        fun generateLongDynamicLink(packageName:String,
            socialTagTitle: String,
            socialTagDesc: String,
            socialImageLink: String,
            userId: String,
            level: Int
        ): Task<ShortDynamicLink> {

             val longDynamicLink = Firebase.dynamicLinks.dynamicLink { // or Firebase.dynamicLinks.shortLinkAsync
                link = Uri.parse(LinkSchema.getLinkOfMCQ(level = level, userId = userId))
                domainUriPrefix = "https://brainsqueezer.page.link"
                androidParameters(packageName) {
                    minimumVersion = 100
                    fallbackUrl = Uri.parse("engineerguide.org")
                }

                googleAnalyticsParameters {
                    source = "orkut"
                    medium = "social"
                    campaign = "example-promo"
                }

                socialMetaTagParameters {
                    title = socialTagTitle
                    description = socialTagDesc
                    imageUrl = Uri.parse(socialImageLink)

                }
            }

            return Firebase.dynamicLinks.shortLinkAsync {
                longLink = Uri.parse("https://brainsqueezer.page.link/?link=" +
                        "https://www.engineerguide.org/&apn=com.puzzlemind.brainsqueezer&ibn=com.example.ios")


            }
        }

        fun getShortDynamicLinkForSharingGame(socialTagTitle:String,
                                    socialTagDesc:String,
                                    socialImageLink:String,
                                    userId: String): Task<ShortDynamicLink> {
            return Firebase.dynamicLinks.shortLinkAsync{
                longLink = Uri.parse(createLongLink(
                    URLEncoder.encode(socialTagTitle,"UTF-8"), URLEncoder.encode(socialTagDesc,"UTF-8"),
                    URLEncoder.encode(socialImageLink,"UTF-8"), 1, userId))
            }
        }


        fun getDynamicLongShortLink(socialTagTitle:String,
                                    socialTagDesc:String,
                                    socialImageLink:String,
                                    level: Int,
                                    userId: String): Task<ShortDynamicLink> {
            return Firebase.dynamicLinks.shortLinkAsync{
                longLink = Uri.parse(createLongLink(URLEncoder.encode(socialTagTitle,"UTF-8"),
                    URLEncoder.encode(socialTagDesc,"UTF-8"),
                    URLEncoder.encode(socialImageLink,"UTF-8"), level, userId))
            }
        }


        private fun createLongLink(
            socialTagTitle: String,
            socialTagDesc: String,
            socialImageLink: String,
            level: Int,
            userId: String
        ): String {
            val packageName = if (BuildConfig.DEBUG){
                PACKAGE_NAME_DEBUG
            }else{
                PACKAGE_NAME
            }
            return LinkSchema.DOMAIN + "?link=" +
                    LinkSchema.getLinkOfMCQ(level = level,userId = userId) +
                    "&apn=${packageName}" +
                    "&st=${socialTagTitle}&sd=${socialTagDesc}&si=${socialImageLink}"
        }


        fun createLinkForSlidingPuzzle(socialTagTitle:String,
                                    socialTagDesc:String,
                                    socialImageLink:String,
                                           scrambledDifficulty: Int,
                                    level: Int,
                                    userId: String): Task<ShortDynamicLink> {
            return Firebase.dynamicLinks.shortLinkAsync{
                longLink = Uri.parse(createLongLinkForSlidingPuzzle(
                    socialTagTitle = URLEncoder.encode(socialTagTitle,"UTF-8"),
                    socialTagDesc = URLEncoder.encode(socialTagDesc,"UTF-8"),
                    socialImageLink = URLEncoder.encode(socialImageLink,"UTF-8"),
                    scrambledDifficulty = scrambledDifficulty,
                    level = level,
                    userId = userId))
            }
        }

        private fun createLongLinkForSlidingPuzzle(
            socialTagTitle: String,
            socialTagDesc: String,
            socialImageLink: String,
            scrambledDifficulty:Int,
            level: Int,
            userId: String
        ): String {
            val packageName = if (BuildConfig.DEBUG){
                PACKAGE_NAME_DEBUG
            }else{
                PACKAGE_NAME
            }
            return LinkSchema.SCRAMBLED_DOMAIN + "?link=" +
                    LinkSchema.getLinkOfSlidingPuzzle(level = level,scrambledDifficulty = scrambledDifficulty,userId = userId) +
                    "&apn=${packageName}" +
                    "&st=${socialTagTitle}&sd=${socialTagDesc}&si=${socialImageLink}"
        }
    }
}