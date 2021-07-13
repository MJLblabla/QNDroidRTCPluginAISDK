
# 七牛rtc 接入 AI 能力

## 快速接入
### 依赖配置

  ```java
   implementation files('libs/qndroid-rtc-ai-version.jar')//ai sdk

   implementation files('libs/qndroid-rtc-3.1.0.jar') //依赖 rtc 3.1.0版本以上

   implementation 'com.qiniu:happy-dns:xxx'
   implementation 'com.squareup.okhttp3:okhttp:xxx'
   implementation "org.java-websocket:Java-WebSocket:xxx"
   implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:x.x.x"

  ```
### 模块初始化

```java
class QNRtcAISdkManager {

    static void init(
            Context appContext,         // 安卓application context
            String aiToken ,            // ai能力的token
            SignCallback signCallback   // 请求url签名回调
     )

    //如果过期可以单独重置token
    static void resetToken( String aiToken)

    //请求url签名回调
    interface  SignCallback{

       /** 将url签名成token 此方法在子线程中
        * @param url 请求URL中已经包含了时间戳
        * @return 签名后的token
        */
        public String signUrlToToken(String url);
   }
}
```

### aiToken 和url签名

#### aiToken
```java
// app_id 加上过期时间
src = "<app_id>:<expiration>"
encodedSrc = urlsafe_base64_encode(src)
// 计算HMAC-SHA1签名，并对签名结果做URL安全的Base64编码
sign = hmac_sha1(encodedSrc, "Your_Secret_Key")
encodedSign = urlsafe_base64_encode(sign)
// 拼接上述结果得到 token
token = "QD " + Your_Access_Key + ":" + encodedSign + ":" + encodedSrc``

```
涉及到用户_Secret_Key 建议签名逻辑运行在接入方服务器端

### url签名

 public String signUrlToToken(String url)中参数为待签名的url（url中已经包含了时间戳）
 [将url签名成token 参考](https://developer.qiniu.com/kodo/1202/download-token)
 涉及到用户_Secret_Key 建议此逻辑房间服务器端

涉及到用户_Secret_Key 建议签名逻辑运行在接入方服务器端

###  绑定rtc轨道的ai能力

```java
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.qiniu.droid.rtc.ai.*;
import com.qiniu.whiteboard.*;
import java.util.ArrayList;
import java.util.List;


public class RoomActivity extends AppCompatActivity {

    private QNSurfaceView mLocalVideoSurfaceView;
    private QNSurfaceView mRemoteVideoSurfaceView;
    private QNRTCClient mClient;

    //本地轨道 用于ai识别本地轨道 其他轨道同理
    QNTrack videoTrack；
    QNTrack audioTrack；

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 在布局文件上配置本地预览窗口和远端预览窗口两个 QNSurfaceView
        mLocalVideoSurfaceView = findViewById(R.id.local_video_surface_view);
        mRemoteVideoSurfaceView = findViewById(R.id.remote_video_surface_view);

        // 初始化 QNRTC
        QNRTC.init(
            getApplicationContext(),
            // 配置默认摄像头 ID，此处配置为前置摄像头；配置摄像头采集的分辨率和 FPS
            new QNRTCSetting().setCameraID(QNCameraFacing.FRONT).setCameraPreviewFormat(new QNVideoFormat(640, 480, 30)),
            new QNRTCEventListener() {
                @Override
                public void onPlaybackDeviceChanged(QNAudioDevice device) {}
                @Override
                public void onCameraError(int errorCode, String errorMessage) {}
            });

            // 创建 QNRTCClient
            mClient = QNRTC.createClient(new QNClientEventListener() {
            @Override
            public void onConnectionStateChanged(QNConnectionState state) {
            }

            @Override
            public void onLeft() {
                // 自己离开房间成功的通知，调用 mClient.leave() 成功之后回调此方法
                RoomActivity.this.finish();
            }

            @Override
            public void onSubscribed(String remoteUserID, List<QNTrack> trackList) {
                for (QNTrack track : trackList) {
                    if (track.getTag().equals("camera")) {
                        // 设置渲染窗口
                        track.play(mRemoteVideoSurfaceView);
                        mRemoteVideoSurfaceView.setVisible(VISIBLE);
                    }
                }
            }

            @Override
            public void onUserJoined(String remoteUserID, String userData) {
                // 远端用户加入房间时会回调此方法，此时可在 UI 上提示，有用户进入房间
            }

            @Override
            public void onUserLeft(String remoteUserID) {
                // 远端用户离开房间时会回调此方法，此时可在 UI 上提示，有用户离开房间
            }

            @Override
            public void onUserPublished(String remoteUserID, List<QNTrack> trackList) {
                // 远端用户发布 tracks 时会回调此方法，SDK 会自动为您订阅对方发布的 tracks
            }

            @Override
            public void onUserUnpublished(String remoteUserID, List<QNTrack> trackList) {
                // 远端用户取消发布时会回调此方法，SDK 会把之前订阅的 tracks 自动为您取消订阅
                for (QNTrack track : trackList) {
                    if (track.getTag().equals("camera")) {
                        mRemoteVideoSurfaceView.setVisible(GONE);
                    }
                }
            }

            @Override
            public void onUserReconnecting(String remoteUserID) {
                // 远端用户断线重连时会回调此方法，此时可在 UI 上提示，对方正在重连
            }

            @Override
            public void onUserReconnected(String remoteUserID) {
                // 远端用户重连成功时会回调此方法，此时可在 UI 上提示，对方已经重连成功
            }
        });
    }

    //绑定自己的控制器事件
    void initView(){

        //点击了开始语音识别按钮翻译自己的轨道
        btnStartAudioToText.setOnClickListener((View.OnClickListener) v -> {
            onClickQNAudioToText(audioTrack);
        });

        //点击停止语音识别
        btnStopAudioToText.setOnClickListener((View.OnClickListener) v -> {
            onClickStopQNAudioToText();
        });

        //点击了自己轨道的身份证识别
        btnIdCardDetector.setOnClickListener((View.OnClickListener) v -> {
            onClickQNIDCardDetector(videoTrack);
        });
    }

    //点击了活体检测 指名分析那个轨道
    void onClickQNFaceActionLive(QNTrack track){
        QNFaceActLiveParam param = new QNFaceActLiveParam();
        List<QNFaceActAction> actions = new ArrayList<QNFaceActAction>();
        actions.add(QNFaceActAction.NOD);//添加点点头识别
        actions.add(QNFaceActAction.SHAKE);//摇摇头识别
        param.actionTypes = actions;
        //开始活体检测
        QNFaceActionLiveDetector  faceActLiveDetector = QNFaceActionLiveDetector.start(
                track, param
        );
        //取消活体检测
        faceActLiveDetector.cancel();
        dealy(2000);
        //假如两秒后用户动作完成
        //提交检测结果
        faceActLiveDetector.commit( new QNFaceActLiveDetector.QNFaceActLiveCallback() {
            @Override
            public void onResult(QNFaceActLive faceActLive) {
            }
        });
    }

    private QNAudioToTextAnalyzer audioToTextAnalyzer;
    //点击了语音转文字 指名分析那个轨道
    void onClickQNAudioToText(QNTrack track){
         audioToTextAnalyzer = QNAudioToTextAnalyzer.start(
                track,//要转化的轨道
                new AudioToTextParam(),//参数设置 可以使用默认
                new QNAudioToTextAnalyzer.QNAudioToTextCallback() {
                    @Override
                    public void onAudioToText(QNAudioToText audioToText) {
                    }
                }
        );
    }

    //停止语音转文字
    void onClickStopQNAudioToText(){
        audioToTextAnalyzer.stop();
    }

    //点击人脸对比 指名分析那个轨道
    void onClickQNFaceComparer(QNTrack track){
        //选择目标要对比的图片
        Bitmap targetImg = selectImg();
        QNFaceComparer.run(track,targetImg , null, new QNIDCardDetector.QNIDCardDetectCallback(){
            @Override
            public void onResult(QNIDCardDetect idCardDetect) {

            }
        });
    }

    //点击人脸检测 指名分析那个轨道
    void onClickQNFaceDetector(QNTrack track){
        QNFaceDetector.run(track, new QNFaceDetectParam(), new QNFaceDetector.QNFaceDetectCallback() {
            @Override
            public void onResult(QNFaceDetect faceDetect) {
            }
        });
    }

    //点击身份证识别 指名分析那个轨道
    void onClickQNIDCardDetector(QNTrack track){
        QNIDCardDetector.run(videoTrack, null, new QNIDCardDetector.QNIDCardDetectCallback() {
            @Override
            public void onResult(QNIDCardDetect idCardDetect) {

            }
        });
    }

     //光线活体检测
     void onClickFaceFlashLive(QNTrack track){
        QNFaceFlashLiveDetector faceFlashLiveDetector = QNFaceFlashLiveDetector.start(track);
        //todo 提示信息
        faceFlashLiveDetector.commit(new QNFaceFlashLiveDetector.QNFaceFlashLiveCallback(){
            @Override
            public void onResult(QNFaceFlashLive flashLive) {

            }
        });
    }

    //语音转文字
    void onClickTTS() {
        QNTTSParam param = new QNTTSParam();
        param.text = etText;
        QNTextToSpeakAnalyzer.run(param, new QNTextToSpeakAnalyzer.QNTextToSpeakCallback() {
            @Override
            public void onResult(QNTextToSpeak textToSpeak) {
                paly(textToSpeak.audioBytes);
            }
        });
    }


    void onClickJoinBtn() {
        // 加入房间
        mClient.join(mRoomToken, new QNJoinResultCallback() {
            public void onJoined() {
                // 创建一路以摄像头采集为数据源的视频 track
                videoTrack = QNRTC.createCameraVideoTrack(
                    // 设置摄像头 track 的编码参数，包括分辨率，FPS，码率
                    new QNCameraVideoTrackConfig("camera").setVideoEncodeFormat(new QNVideoFormat(640, 480, 30)).setBitrate(1500 * 1000)
                );
                // 设置摄像头采集的预览窗口
                videoTrack.play(mLocalVideoSurfaceView)

                // 创建一路以麦克风采集为数据源的音频 track
                audioTrack = QNRTC.createMicrophoneAudioTrack(
                    new QNMicrophoneAudioTrackConfig("microphone").setBitrate(100 * 1000)
                );

                // 加入房间成功，即可在房间内发布自己的音频和视频轨道
                mClient.publish(videoTrack, audioTrack);
            }

            public void onError(int errorCode, String errorMessage) {
                // 加入房间失败，可在 UI 提示
            }
        });
    }

    void onClickLeaveBtn() {
        // 离开房间
        mClient.leave();
    }

    @Override
    protected void onDestroy()  {
        // 取消初始化 QNRTC
        QNRTC.deinit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 程序切到前台，此时需开启摄像头采集
        QNRTC.startCameraCapture();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 程序切到后台，此时需停止摄像头采集
        QNRTC.stopCameraCapture();
    }
}
```


## api说明

### 活体检测

```java
class QNFaceActionLiveDetector {
    static QNFaceActionLiveDetector start(QNTrack videoTrack, QNFaceActionLiveParams params) // 开始活体动作检测
    void commit(QNFaceActionLiveCallback callback) // 动作结束提交获取识别结果
    void cancel() // 取消
}

// 活体检测参数
class QNFaceActionLiveParams {
    java.util.List<QNFaceAction> actionTypes // 动作列表
}

enum QNFaceAction {
    BLINK // 眨眨眼
    MOUTH // 张张嘴
    NOD   // 点点头
    SHAKE // 摇摇头
}

interface QNFaceActionLiveCallback {
    void onResult(QNFaceActionLive faceActLive) // 活体检测结果回调
}

class QNFaceActionLive {
    // 最优帧列表，列表中每个元素格式是 json，包括 base64 编码的二进制图片数据和图像质量分数
    java.util.List<QNFaceActionLive.BestFramesDTO>	bestFrames
    int	errorCode // 请求返回码
    String errorMsg // 错误提示
    int	liveStatus // 返回动作活体状态码，1 表示通过，0 表示不通过

    class BestFramesDTO {
        String	imageB64 // base64 编码的二进制图像数据
        double	quality  // 图像质量分数, 取值范围是[0,100]
    }
}

// example:
    QNFaceActLiveParam param = new QNFaceActLiveParam();
    List<QNFaceActAction> actions = new ArrayList<QNFaceActAction>();
    actions.add(QNFaceActAction.NOD);//添加点点头识别
    actions.add(QNFaceActAction.SHAKE);//摇摇头识别
    param.actionTypes = actions;
    //开始活体检测
    QNFaceActLiveDetector faceActLiveDetector = QNFaceActLiveDetector.start(
            videoTrack, param
    );
    //取消活体检测
    faceActLiveDetector.cancel();
    //提交检测结果
    faceActLiveDetector.commit( new QNFaceActLiveDetector.QNFaceActLiveCallback() {
                @Override
                public void onResult(QNFaceActLive faceActLive) {
                }
            });

```

### 语音转文字

```java
class QNAudioToTextAnalyzer {
    static QNAudioToTextAnalyzer start(
        QNTrack audioTrack, QNAudioToTextParams params, QNAudioToTextCallback callback) // 开始语音实时识别
    void stop() // 停止语音实时识别
}

// 语音识别参数
class QNAudioToTextParam {
    int	forceFinal  // 是否在text为空的时候返回final信息, 1->强制返回;0->不强制返回。
    int	maxSil      // 最长静音间隔，单位秒，默认10s
    int	modelType   // 0->cn; 默认0
    int	needPartial // 是否返回partial文本，1->返回，0-> 不返回;默认1
    int	needVad     // 是否需要vad;0->关闭;1->开启; 默认1
    int	needWords   // 是否返回词语的对齐信息，1->返回， 0->不返回;默认0。
    double vadSilThres // vad断句的累积时间，大于等于0， 如果设置为0，或者没设置，系统默认
}

// 实时语音转文字回调
interface QNAudioToTextCallback {
    void onAudioToText(QNAudioToText audioToText) // 实时转化文字数据
    void onError(int code, String errMsg) // 错误
    void onStart() // 开始成功
    void onStop() // 实时转化结束
}

// 语音识别结果
// 当前片段的结果文字数据，开始到结束过程中会实时返回数据，包含当前一句中的总结果和当前语音片段的结果
class QNAudioToText {
    double startTime // 该片段的起始时间，毫秒
    double endTime   // 该片段的终止时间，毫秒
    int	ended      // 是否是websocket最后一条数据,0:非最后一条数据,1: 最后一条数据。
    int	endSeq     // 为该文本所在的切片的终点(包含)，否则为-1
    int	finalX     // 分片结束,当前消息的transcript为该片段最终结果，否则为partial结果
    int	longSil    // 是否长时间静音，0:否;1:是
    int	segBegin   // 是否分段开始: 1:是; 0:不是。
    int	segIndex   // 是否是vad分段开始说话的开始1:是分段开始说话; 0:不是。
    int	spkBegin   // 是否是vad分段开始说话的开始1:是分段开始说话; 0:不是。
    int	startSeq   // 该文本所在的切片的起点(包含), 否则为-1
    String uuid    // 服务端生成的uuid
    String transcript // 语音的文本, 如果final=0, 则为partinal结果 (后面可能会更改),final=1为该片段最终结果
    String partialTranscript  // partial结果文本, 开启needpartial后返回
    java.util.List<QNAudioToText.WordsDTO> words // 返回词语的对齐信息, 参数need_words=1时返回详细内存见下表。

    class WordsDTO {
        double segEnd   // 该词语相对整个数据流的起始时间, 毫秒
        double segStart // 该词语相对当前分段的起始时间, 毫秒
        double voiceEnd   // 该词语相对整个数据流的终止时间, 毫秒
        double voiceStart // 该词语相对当前分段的终止时间, 毫秒
        String word // 词语本身，包括标点符号
    }
}

// example:
QNAudioToTextAnalyzer audioToTextAnalyzer = QNAudioToTextAnalyzer.start(
            audioTrack,//要转化的轨道
            new AudioToTextParam(),//参数设置 可以使用默认
            new QNAudioToTextAnalyzer.QNAudioToTextCallback() {
                @Override
                public void onAudioToText(QNAudioToText audioToText) {

                }
            }
    );
//停止语音转文字
audioToTextAnalyzer.stop()
```

### 人脸对比

```java
class QNFaceComparer {
    // videoTrack - 视频轨道；targetImg - 目标要对比的图片；params - 参数；callback - 回调
    static void	run(
        QNTrack videoTrack, android.graphics.Bitmap targetImg,
        QNFaceCompareParam param, QNFaceCompareCallback callback) // 开始一次人脸对比
}

// 人脸对比参数
class QNFaceCompareParam {
    boolean	maxFaceA    // 图像 A 中检测到多张人脸时是否取最大区域的人脸作为输出，默认值为 True
    boolean	maxFaceB    // 图像 B 中检测到多张人脸时是否取最大区域的人脸作为输出，默认值为 True
    boolean	rotateA     // 人脸检测失败时，是否对图像 A 做旋转再检测，旋转角包括 90、180、270 三个角度，默认值为 False
    boolean	rotateB     // 人脸检测失败时，是否对图像 B 做旋转再检测，旋转角包括 90、180、270 三个角度，默认值为 False
}

// 人脸对比结果
class QNFaceCompare {
     int	errorCode // 请求返回码
     string errorMsg // 错误提示
     double similarity //相似度
}

// 人脸对比回调
interface QNFaceCompareCallback {
    void onResult(QNFaceCompare faceCompare) // 比较结果回调
}

// example:
QNFaceComparer.run(videoTrack,targetImg , null, new QNIDCardDetector.QNIDCardDetectCallback(){
        @Override
        public void onResult(IDCardDetect idCardDetect) {

        }
    });
```

### 人脸检测

```java
class QNFaceDetector {
    // 开始一次人脸检测
    static void	run(
        QNTrack videoTrack, QNFaceDetectParams params, QNFaceDetectCallback callback)
}

// 人脸检测参数
class QNFaceDetectParam {
    boolean rotate // 人脸检测失败时，是否对图像 A 做旋转再检测，旋转角包 括 90、180、270 三个角度，默认值为 False
}

// 人脸检测回调
interface QNFaceDetectCallback {
    void onResult(QNFaceDetect faceDetect) // 人脸检测结果
}
// 人脸检测结果
class QNFaceDetect {
    int	errorCode   // 请求返回码
    String errorMsg // 错误提示
    int	numFace     // 图像中人脸数量
    int	rotateAngle // 图像旋转角度
    java.util.List<FaceDTO> faces // [face1,face2,…]，其中 face1,face2,…等为 json 格式，具体格式见下表

    class FaceDTO {
          int	age      // 年龄，区间 1-107 岁
          double	area // 人脸区域的大小
          double	blur // 人脸模糊度，取值范围[0,1]，越大越清晰
          double	completeness // 取值0到100；0表示人脸不完整，溢出了图像边界，100 表示人脸是完整的，在图像边界内
          double	eye  // 闭眼概率,取值范围[0,100]
          String	faceAlignedB64 // 使用 base64 编码的对齐后人脸图片数据
          double	faceZize // 人脸尺寸分数，取值分数[0,100]， 越大人脸尺寸越大
          String	gender   // 性别，’M’代表男，’F’代表女
          int	height       // 人脸框的高度
          double	illumination // 人脸光照范围，取值范围[0,100]，越大光照质量越好
          double	mouth // 闭嘴概率,取值范围[0,100]
          double	pitch // 三维旋转之俯仰角，[-180,180]
          double	quality // 人脸综合质量分数，取值范围[0,100], 越大质量越好
          double	roll  // 三维旋转之左右旋转角, [-180,180]
          double	score // 人脸分数 取值范围 [0,100]
          int	width     // 人脸框的宽度
          int	x         // 人脸框的左上角 x 坐标
          int	y         // 人脸框的左上角 y 坐标
          double	yaw   // 三维旋转之旋转角，[-180,180]
          FaceShapeDTO	faceShape // 人脸尺寸分数，取值分数[0,100]， 越大人脸尺寸越大


          class FaceShapeDTO {
              java.util.List<FaceProfileDTO>	faceProfile
              java.util.List<FaceProfileDTO>	leftEye
              java.util.List<FaceProfileDTO>	leftEyebrow
              java.util.List<FaceProfileDTO>	mouth
              java.util.List<FaceProfileDTO>	nose
              java.util.List<FaceProfileDTO>	pupil
              java.util.List<FaceProfileDTO>	rightEye
              java.util.List<FaceProfileDTO>	rightEyebrow

              // 人脸部位坐标
              class FaceProfileDTO {
                  int	x
                  int	y
              }
          }
      }
}

// example:
QNFaceDetector.run(videoTrack, new QNFaceDetectParam(), new QNFaceDetector.QNFaceDetectCallback() {
        @Override
        public void onResult(QNFaceDetect faceDetect) {
       }
    });
```


### 身份证识别

```java
class QNIDCardDetector {
    // 开始一次身份证识别
    static void	run(QNTrack videoTrack, QNIDCardDetectParam param, QNIDCardDetectCallback callback)
}

// 身份证识别请求参数
class QNIDCardDetectParam {
    boolean	enableBorderCheck // 身份证遮挡检测开关，如果输入图片中的身份证卡片边框不完整则返回告警
    boolean	enableDetectCopy  // 复印件、翻拍件检测开关，如果输入图片中的身份证卡片是复印件，则返回告警
    String	refSide     // 当图片中同时存在身份证正反面时，通过该参数指定识别的版面:取值'Any' - 识别人像面或国徽面，'F' - 仅 识别人像面，'B' - 仅识别国徽面
    boolean	retImage    // 是否返回识别后的切图(切图是指精确剪裁对齐后的身份证正反面图片)，返回格式为 JPEG 格式二进制图片使用 base64 编码后的字符串
    boolean	retPortrait // 是否返回身份证(人像面)的人脸图 片，返回格式为 JPEG 格式二进制图片使用 base64 编码后的字符串
}

// 身份证识别结果回调
interface QNIDCardDetectCallback {
    void onResult(QNIDCardDetect idCardDetect)
}

// 身份证识别结果
class QNIDCardDetect {
    int	errorCode   // 请求返回码
    String errorMsg // 错误提示
    QNIDCardDetect.ImageResultDTO imageResult // 图片检测结果
    QNIDCardDetect.OCRResultDTO	ocrResult // 文字识别结果

    class ImageResultDTO {
        // 框坐标，格式为 [[x0, y0], [x1, y1], [x2, y2], [x3, y3]]
        java.util.List<java.util.List<Integer>> idCardBox // ??? 为什么用数组的数组
    }

    class OCRResultDTO {
        String	address   // 地址(人像面)
        String	birthDate // 生日(人像面) eg. "19900111"
        String	gender    // 性别(人像面)
        String	idno      // 身份号码(人像面)
        String	issuedBy  // 签发机关(国徽面)
        String	name      // 姓名(人像面)
        String	nation    // 民族(人像面)
        String	side      // F-身份证人像面，B-身份 证国徽面
        String	validthru // 有效期(国徽面) eg. "20001010-20101009"
    }
}

// example:
QNIDCardDetector.run(videoTrack, null, new QNIDCardDetector.QNIDCardDetectCallback() {
        @Override
        public void onResult(QNIDCardDetect idCardDetect) {

        }
    });
```

```java
class QNRtcAISdkManager {

    static void init(
            Context appContext,         // 安卓application context
            String aiToken ,              //ai能力的token
            SignCallback signCallback   //  请求url签名回调
     )

      //如果过期可以单独重置token
    static void resetToken( String aiToken)

    interface  SignCallback{

       /** 将url签名成token
        * @param url 请求URL
        * @return 签名后的token
        */
        public String signUrlToToken(String url);
   }
}
```

### 活体检测

```java
class QNFaceActionLiveDetector {
    static QNFaceActionLiveDetector start(QNTrack videoTrack, QNFaceActionLiveParams params) // 开始活体动作检测
    void commit(QNFaceActionLiveCallback callback) // 动作结束提交获取识别结果
    void cancel() // 取消
}

// 活体检测参数
class QNFaceActionLiveParams {
    java.util.List<QNFaceAction> actionTypes // 动作列表
}

enum QNFaceAction {
    BLINK // 眨眨眼
    MOUTH // 张张嘴
    NOD   // 点点头
    SHAKE // 摇摇头
}

interface QNFaceActionLiveCallback {
    void onResult(QNFaceActionLive faceActLive) // 活体检测结果回调
}

class QNFaceActionLive {
    // 最优帧列表，列表中每个元素格式是 json，包括 base64 编码的二进制图片数据和图像质量分数
    java.util.List<QNFaceActionLive.BestFramesDTO>	bestFrames
    int	errorCode // 请求返回码
    String errorMsg // 错误提示
    int	liveStatus // 返回动作活体状态码，1 表示通过，0 表示不通过

    class BestFramesDTO {
        String	imageB64 // base64 编码的二进制图像数据
        double	quality  // 图像质量分数, 取值范围是[0,100]
    }
}

// example:
    QNFaceActLiveParam param = new QNFaceActLiveParam();
    List<QNFaceActAction> actions = new ArrayList<QNFaceActAction>();
    actions.add(QNFaceActAction.NOD);//添加点点头识别
    actions.add(QNFaceActAction.SHAKE);//摇摇头识别
    param.actionTypes = actions;
    //开始活体检测
    QNFaceActLiveDetector faceActLiveDetector = QNFaceActLiveDetector.start(
            videoTrack, param
    );
    //取消活体检测
    faceActLiveDetector.cancel();
    //提交检测结果
    faceActLiveDetector.commit( new QNFaceActLiveDetector.QNFaceActLiveCallback() {
                @Override
                public void onResult(QNFaceActLive faceActLive) {
                }
            });

```

### 语音转文字

```java
class QNAudioToTextAnalyzer {
    static QNAudioToTextAnalyzer start(
        QNTrack audioTrack, QNAudioToTextParams params, QNAudioToTextCallback callback) // 开始语音实时识别
    void stop() // 停止语音实时识别
}

// 语音识别参数
class QNAudioToTextParam {
    int	forceFinal  // 是否在text为空的时候返回final信息, 1->强制返回;0->不强制返回。
    int	maxSil      // 最长静音间隔，单位秒，默认10s
    int	modelType   // 0->cn; 默认0
    int	needPartial // 是否返回partial文本，1->返回，0-> 不返回;默认1
    int	needVad     // 是否需要vad;0->关闭;1->开启; 默认1
    int	needWords   // 是否返回词语的对齐信息，1->返回， 0->不返回;默认0。
    double vadSilThres // vad断句的累积时间，大于等于0， 如果设置为0，或者没设置，系统默认
}

// 实时语音转文字回调
interface QNAudioToTextCallback {
    void onAudioToText(QNAudioToText audioToText) // 实时转化文字数据
    void onError(int code, String errMsg) // 错误
    void onStart() // 开始成功
    void onStop() // 实时转化结束
}

// 语音识别结果
// 当前片段的结果文字数据，开始到结束过程中会实时返回数据，包含当前一句中的总结果和当前语音片段的结果
class QNAudioToText {
    double startTime // 该片段的起始时间，毫秒
    double endTime   // 该片段的终止时间，毫秒
    int	ended      // 是否是websocket最后一条数据,0:非最后一条数据,1: 最后一条数据。
    int	endSeq     // 为该文本所在的切片的终点(包含)，否则为-1
    int	finalX     // 分片结束,当前消息的transcript为该片段最终结果，否则为partial结果
    int	longSil    // 是否长时间静音，0:否;1:是
    int	segBegin   // 是否分段开始: 1:是; 0:不是。
    int	segIndex   // 是否是vad分段开始说话的开始1:是分段开始说话; 0:不是。
    int	spkBegin   // 是否是vad分段开始说话的开始1:是分段开始说话; 0:不是。
    int	startSeq   // 该文本所在的切片的起点(包含), 否则为-1
    String uuid    // 服务端生成的uuid
    String transcript // 语音的文本, 如果final=0, 则为partinal结果 (后面可能会更改),final=1为该片段最终结果
    String partialTranscript  // partial结果文本, 开启needpartial后返回
    java.util.List<QNAudioToText.WordsDTO> words // 返回词语的对齐信息, 参数need_words=1时返回详细内存见下表。

    class WordsDTO {
        double segEnd   // 该词语相对整个数据流的起始时间, 毫秒
        double segStart // 该词语相对当前分段的起始时间, 毫秒
        double voiceEnd   // 该词语相对整个数据流的终止时间, 毫秒
        double voiceStart // 该词语相对当前分段的终止时间, 毫秒
        String word // 词语本身，包括标点符号
    }
}

// example:
QNAudioToTextAnalyzer audioToTextAnalyzer = QNAudioToTextAnalyzer.start(
            audioTrack,//要转化的轨道
            new AudioToTextParam(),//参数设置 可以使用默认
            new QNAudioToTextAnalyzer.QNAudioToTextCallback() {
                @Override
                public void onAudioToText(QNAudioToText audioToText) {

                }
            }
    );
//停止语音转文字
audioToTextAnalyzer.stop()
```

### 人脸对比

```java
class QNFaceComparer {
    // videoTrack - 视频轨道；targetImg - 目标要对比的图片；params - 参数；callback - 回调
    static void	run(
        QNTrack videoTrack, android.graphics.Bitmap targetImg,
        QNFaceCompareParam param, QNFaceCompareCallback callback) // 开始一次人脸对比
}

// 人脸对比参数
class QNFaceCompareParam {
    boolean	maxFaceA    // 图像 A 中检测到多张人脸时是否取最大区域的人脸作为输出，默认值为 True
    boolean	maxFaceB    // 图像 B 中检测到多张人脸时是否取最大区域的人脸作为输出，默认值为 True
    boolean	rotateA     // 人脸检测失败时，是否对图像 A 做旋转再检测，旋转角包括 90、180、270 三个角度，默认值为 False
    boolean	rotateB     // 人脸检测失败时，是否对图像 B 做旋转再检测，旋转角包括 90、180、270 三个角度，默认值为 False
}

// 人脸对比结果
class QNFaceCompare {
     int	errorCode // 请求返回码
     string errorMsg // 错误提示
     double similarity //相似度
}

// 人脸对比回调
interface QNFaceCompareCallback {
    void onResult(QNFaceCompare faceCompare) // 比较结果回调
}

// example:
QNFaceComparer.run(videoTrack,targetImg , null, new QNIDCardDetector.QNIDCardDetectCallback(){
        @Override
        public void onResult(IDCardDetect idCardDetect) {

        }
    });
```

### 人脸检测

```java
class QNFaceDetector {
    // 开始一次人脸检测
    static void	run(
        QNTrack videoTrack, QNFaceDetectParams params, QNFaceDetectCallback callback)
}

// 人脸检测参数
class QNFaceDetectParam {
    boolean rotate // 人脸检测失败时，是否对图像 A 做旋转再检测，旋转角包 括 90、180、270 三个角度，默认值为 False
}

// 人脸检测回调
interface QNFaceDetectCallback {
    void onResult(QNFaceDetect faceDetect) // 人脸检测结果
}

// 人脸检测结果
class QNFaceDetect {
    int	errorCode   // 请求返回码
    String errorMsg // 错误提示
    int	numFace     // 图像中人脸数量
    int	rotateAngle // 图像旋转角度
    java.util.List<FaceDTO> faces // [face1,face2,…]，其中 face1,face2,…等为 json 格式，具体格式见下表

    class FaceDTO {
          int	age      // 年龄，区间 1-107 岁
          double	area // 人脸区域的大小
          double	blur // 人脸模糊度，取值范围[0,1]，越大越清晰
          double	completeness // 取值0到100；0表示人脸不完整，溢出了图像边界，100 表示人脸是完整的，在图像边界内
          double	eye  // 闭眼概率,取值范围[0,100]
          String	faceAlignedB64 // 使用 base64 编码的对齐后人脸图片数据
          double	faceZize // 人脸尺寸分数，取值分数[0,100]， 越大人脸尺寸越大
          String	gender   // 性别，’M’代表男，’F’代表女
          int	height       // 人脸框的高度
          double	illumination // 人脸光照范围，取值范围[0,100]，越大光照质量越好
          double	mouth // 闭嘴概率,取值范围[0,100]
          double	pitch // 三维旋转之俯仰角，[-180,180]
          double	quality // 人脸综合质量分数，取值范围[0,100], 越大质量越好
          double	roll  // 三维旋转之左右旋转角, [-180,180]
          double	score // 人脸分数 取值范围 [0,100]
          int	width     // 人脸框的宽度
          int	x         // 人脸框的左上角 x 坐标
          int	y         // 人脸框的左上角 y 坐标
          double	yaw   // 三维旋转之旋转角，[-180,180]
          FaceShapeDTO	faceShape // 人脸尺寸分数，取值分数[0,100]， 越大人脸尺寸越大


          class FaceShapeDTO {
              java.util.List<FaceProfileDTO>	faceProfile
              java.util.List<FaceProfileDTO>	leftEye
              java.util.List<FaceProfileDTO>	leftEyebrow
              java.util.List<FaceProfileDTO>	mouth
              java.util.List<FaceProfileDTO>	nose
              java.util.List<FaceProfileDTO>	pupil
              java.util.List<FaceProfileDTO>	rightEye
              java.util.List<FaceProfileDTO>	rightEyebrow

              // 人脸部位坐标
              class FaceProfileDTO {
                  int	x
                  int	y
              }
          }
      }
}


// example:
QNFaceDetector.run(videoTrack, new QNFaceDetectParam(), new QNFaceDetector.QNFaceDetectCallback() {
        @Override
        public void onResult(QNFaceDetect faceDetect) {
       }
    });
```


### 身份证识别

```java
class QNIDCardDetector {
    // 开始一次身份证识别
    static void	run(QNTrack videoTrack, QNIDCardDetectParam param, QNIDCardDetectCallback callback)
}

// 身份证识别请求参数
class QNIDCardDetectParam {
    boolean	enableBorderCheck // 身份证遮挡检测开关，如果输入图片中的身份证卡片边框不完整则返回告警
    boolean	enableDetectCopy  // 复印件、翻拍件检测开关，如果输入图片中的身份证卡片是复印件，则返回告警
    String	refSide     // 当图片中同时存在身份证正反面时，通过该参数指定识别的版面:取值'Any' - 识别人像面或国徽面，'F' - 仅 识别人像面，'B' - 仅识别国徽面
    boolean	retImage    // 是否返回识别后的切图(切图是指精确剪裁对齐后的身份证正反面图片)，返回格式为 JPEG 格式二进制图片使用 base64 编码后的字符串
    boolean	retPortrait // 是否返回身份证(人像面)的人脸图 片，返回格式为 JPEG 格式二进制图片使用 base64 编码后的字符串
}

// 身份证识别结果回调
interface QNIDCardDetectCallback {
    void onResult(QNIDCardDetect idCardDetect)
}

// 身份证识别结果
class QNIDCardDetect {
    int	errorCode   // 请求返回码
    String errorMsg // 错误提示
    QNIDCardDetect.ImageResultDTO imageResult // 图片检测结果
    QNIDCardDetect.OCRResultDTO	ocrResult // 文字识别结果

    class ImageResultDTO {
        // 框坐标，格式为 [[x0, y0], [x1, y1], [x2, y2], [x3, y3]]
        java.util.List<java.util.List<Integer>> idCardBox // ??? 为什么用数组的数组
    }

    class OCRResultDTO {
        String	address   // 地址(人像面)
        String	birthDate // 生日(人像面) eg. "19900111"
        String	gender    // 性别(人像面)
        String	idno      // 身份号码(人像面)
        String	issuedBy  // 签发机关(国徽面)
        String	name      // 姓名(人像面)
        String	nation    // 民族(人像面)
        String	side      // F-身份证人像面，B-身份 证国徽面
        String	validthru // 有效期(国徽面) eg. "20001010-20101009"
    }
}

// example:
QNIDCardDetector.run(videoTrack, null, new QNIDCardDetector.QNIDCardDetectCallback() {
        @Override
        public void onResult(QNIDCardDetect idCardDetect) {

        }
    });
```

### 光线活体检测

```java
class QNFaceFlashLiveDetector {
    /**
     * 开始活体光线检测
     * @param videoTrack 要检测的视频轨道
     */
    static QNFaceFlashLiveDetector start(QNTrack videoTrack)

    //获取结果识别结果
    void commit(QNFaceFlashLiveCallback callBack)

    //取消
    void cancel()
}

// 回调函数
interface QNFaceFlashLiveCallback {
    //结果回调
    void onResult(QNFaceFlashLive flashLive)
}

//光线活体结果
class QNFaceFlashLive {
    int score      //检测分数
    int passNum    //视频中通过的人脸帧数
    int faceNum    //视频中检测到的人脸帧数
    int errorCode
    String errorMsg
}

// example:
QNFaceFlashLiveDetector faceFlashLiveDetector = QNFaceFlashLiveDetector.start(localVideoTrack);
//todo 提示活体检测开始
faceFlashLiveDetector.commit {
       faceFlashLive ->
     //todo 拿到结果 判断分数是否满足
}


```

### 文字转语音

```java
class QNTextToSpeakAnalyzer {

    /**
     * 开始文字转语音
     * @param ttsParam 参数
     * @param callback 转化结果回调
     */
    static void run(QNTTSParam ttsParam,QNTextToSpeakCallback callback)
}

//文转音回调函数
interface QNTextToSpeakCallback{
     // 结果回调
    void onResult(QNTextToSpeak textToSpeak)
}

//文字转语音参数
class QNTTSParam {
    String text                    // 需要进⾏语⾳合成的⽂本内容，最短1个字，最⻓200字
    QNSpeaker speaker              //发⾳⼈id，⽤于选择不同⻛格的⼈声，⽬前默认为kefu1， 可选的包括female3，female5，female6，male1，male2， male4，kefu1，girl1
    QNAudioEncoding audioEncoding  //合成⾳频格式，⽬前默认为wav，可选的包括wav，pcm，mp3
    int sampleRate = 16000         //合成⾳频的采样率，默认为16000，可选的包括8000，16000， 24000，48000
    int volume = 50                //⾳量⼤⼩，取值范围为0~100，默认为50
    int speed = 0                  //语速，取值范围为-100~100，默认为0
}

//文字转语音声效枚举
enum QNSpeaker {
    MALE1("male1")        //男声1
    MALE2("male2")        //男声2
    FEMALE3("female3")    //女声3
    MALE4("male4")        //男声4
    FEMALE5("female5")    //女声5
    FEMALE6("female6")    //女声6
    KEFU1("kefu1")        //客服1
    GIRL1("girl1")        //女孩1
}

//tts 音频编码格式枚举
enum QNAudioEncoding {
    WAV("wav")
    PCM("pcm")
    MP3("mp3")
}

//文转音结果
class QNTextToSpeak {
    int errorCode            //错误码
    String errorMsg          //提示
    String audioText = ""    //合成的⾳频文本，采⽤base64编码
    byte[] audioBytes        //可播放的音频二进制数据
}

// example:
QNTTSParam param = new QNTTSParam();
param.text = "test";
QNTextToSpeakAnalyzer.run(param) {
                textToSpeak ->
               //todo 播放音频二进制数据
                play(textToSpeak)
            }
```


### 错误码

```c
业务错误码	信息

//通用:
0	成功
1000	未知异常
1001	音频/视频轨道没有数据返回
1002	音频/视频数据异常

//语音转文字:
2000	网络异常连接中断

//身份证识别:
53090001	请求解析失败
53090002	图片解码错误
53090003	OCR 内部错误
53090004	无法识别的身份证(非中国身份证等)
53090005	参数错误
55060030	鉴权失败
53091001	黑白复印件
53091003	无法检测到人脸
53091004	证件信息缺失或错误
53091005	证件过期
53091006	身份证不完整

//人脸检测:
55060001	请求字段有非法传输
55060002	图片解码失败
55060006	人脸特征提取失败
55060018	人脸配准失败
55060019	人脸检测图片 Base64 解码失败
55060033	人脸图片无效

//人脸对比:
55060001	请求字段有非法传输
55060002	图片解码失败
55060028	人脸比对图片 A Base64 解码失败
55060029	人脸比对图片 B Base64 解码失败
55060040	图片A人脸检测失败
55060041	图片B人脸检测失败

// 动作活体检测:
55060001	请求字段有非法传输
55060002	图片解码失败
55060012	点头动作检测失败
55060013	摇头动作检测失败
55060014	眨眼动作检测失败
55060015	张嘴动作检测失败
55060016	不是活体
55060024	视频帧率过低
55060016	动作类型无效

//光线活体
55060001    请求字段有非法传输
55060002	图片解码失败
55060009	视频无效
55060011	视频中人脸检测失败
55060016	不是活体

//文转音
100        请求参数缺失
101        请求参数不合法，⽐如合成⽂本过⻓
102        服务不可⽤
103        语⾳合成错误


```