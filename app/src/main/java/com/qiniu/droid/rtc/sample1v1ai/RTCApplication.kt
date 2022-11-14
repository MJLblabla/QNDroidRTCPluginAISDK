package com.qiniu.droid.rtc.sample1v1ai

import android.app.Application
import com.qiniu.droid.rtc.QNLogLevel
import com.qiniu.droid.rtc.QNRTCEnv
import com.qiniu.droid.rtc.ai.QNRtcAISdkManager
import com.uuzuche.lib_zxing.activity.ZXingLibrary

class RTCApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 设置 QNRTC Log 输出等级
        QNRTCEnv.setLogLevel(QNLogLevel.INFO)
        QNRTCEnv.init(this)
        // 初始化使用的第三方二维码扫描库，与 QNRTC 无关，请忽略
        ZXingLibrary.initDisplayOpinion(applicationContext)
        QNRtcAISdkManager.init(this)
    }

}