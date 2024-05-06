/* Copyright 2022 Lin Yi. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

/** 本应用主要对 Tensorflow Lite Pose Estimation 示例项目的 MainActivity.kt
 *  文件进行了重写，示例项目中其余文件除了包名调整外基本无改动，原版权归
 *  The Tensorflow Authors 所有 */

package lyi.AIworkout.posemon

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PointF
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.MediaStore.Images.ImageColumns
import android.provider.MediaStore.Video
import android.renderscript.Sampler.Value
import android.text.TextWatcher
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Spinner
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lyi.AIworkout.posemon.camera.CameraSource
import lyi.AIworkout.posemon.data.Camera
import lyi.AIworkout.posemon.data.Device
import lyi.AIworkout.posemon.data.Person
import lyi.AIworkout.posemon.data.SurveyResult
import lyi.AIworkout.posemon.ml.ModelType
import lyi.AIworkout.posemon.ml.MoveNet
import lyi.AIworkout.posemon.ml.PoseClassifier
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt


fun calculateAngle(point1: Array<Double>, point2: Array<Double>, point3: Array<Double>): Double {
    val point1Array = point1.toDoubleArray()
    val point2Array = point2.toDoubleArray()
    val point3Array = point3.toDoubleArray()

    val radians = (atan2(point3Array[1] - point2Array[1], point3Array[0] - point2Array[0])
            - atan2(point1Array[1] - point2Array[1], point1Array[0] - point2Array[0]))

    var angle = abs(radians * 180 / PI)
    if (angle < 180) {
        return angle
    } else {
        angle = 360 - angle
        return angle
    }
}

fun pointFToArray(point: PointF?): Array<Double> {
    return arrayOf(point?.x!!.toDouble(), point?.y!!.toDouble())
}
var doingdirection = "左"
var handsfrontcantick = false
var PlayingVideo = false
var doneagroup = false
var wrongaction = false
var wrongaction1 = false
var doingaction = 0
var rightneck = 0
var leftneck = 0
var acttime = 0
var countdoingaction = 0
val time1 = Calendar.getInstance().time
@SuppressLint("SimpleDateFormat")
val formatter = SimpleDateFormat("yyyy-MM-dd")
val currenttime = formatter.format(time1)
class MainActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** 为视频画面创建一个 SurfaceView */
    private lateinit var surfaceView: SurfaceView

    /** 修改默认计算设备：CPU、GPU、NNAPI（AI加速器） */
    private var device = Device.CPU
    /** 修改默认摄像头：FRONT、BACK */
    private var selectedCamera = Camera.BACK

    /** 定义几个计数器 */
    private var forwardheadCounter = 0
    private var crosslegCounter = 0
    private var standardCounter = 0
    private var missingCounter = 0
    private var lastTimeStartUnixTime = 0;

    /** 定义一个历史姿态寄存器 */
    private var poseRegister = "standard"

    /** 设置一个用来显示 Debug 信息的 TextView */
    //private lateinit var tvDebug: TextView

    /** 设置一个用来显示当前坐姿状态的 ImageView */
    //private lateinit var ivStatus: ImageView
    private lateinit var VideoFrame:FrameLayout
    private lateinit var showcaseimage: ImageView
    private lateinit var handsbacktick: ImageView
    private lateinit var handsfronttick: ImageView
    private lateinit var handsuptick: ImageView
    private lateinit var necktick: ImageView
    private lateinit var foottick: ImageView
    private lateinit var foot1tick: ImageView
    private lateinit var hiptick: ImageView
    private lateinit var smallimageshows: ImageButton
    private lateinit var hip: ImageButton
    private lateinit var foot1: ImageButton
    private lateinit var foot: ImageButton
    private lateinit var neck: ImageButton
    private lateinit var hands_: TextView
    private lateinit var hip_: TextView
    private lateinit var neck_:TextView
    private lateinit var foot_:TextView
    private lateinit var nowact: TextView
    private lateinit var actTimes: TextView
    private lateinit var tvFPS: TextView
    private lateinit var tvScore: TextView
    //private lateinit var spnDevice: Spinner
    private lateinit var spnCamera: Spinner
    private lateinit var closeimage: Button
    private lateinit var hand1: ImageButton
    private lateinit var hand2: ImageButton
    private lateinit var hand3: ImageButton
    private lateinit var back: Button
    private lateinit var handup: Button
    private lateinit var showcaseVideo: VideoView
    private var cameraSource: CameraSource? = null
    private var isClassifyPose = true

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                /** 得到用户相机授权后，程序开始运行 */
                openCamera()
            } else {
                /** 提示用户“未获得相机权限，应用无法运行” */
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            /** 如果用户未选择运算设备，使用默认设备进行计算 */
        }
    }

    private var changeCameraListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, view: View?, direction: Int, id: Long) {
            changeCamera(direction)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            /** 如果用户未选择摄像头，使用默认摄像头进行拍摄 */
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /** 程序运行时保持屏幕常亮 */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //tvScore = findViewById(R.id.tvScore)
        /**button*/
        VideoFrame = findViewById(R.id.VideoFrame)
        hand1 = findViewById(R.id.handsback)
        hand1.setOnClickListener {
            if(!PlayingVideo) {
                hand1Fun = 1;
                selectedimage = 1;
                acttime = 0
                doingaction = 0
                smallimageshows.setImageResource(R.drawable.handback)
                smallimageshows.visibility = View.VISIBLE
            }
        }


        hand2 = findViewById(R.id.handsup)
        hand2.setOnClickListener {
            if(!PlayingVideo) {
                hand1Fun = 2;
                selectedimage = 2;
                acttime = 0
                doingaction = 0
                smallimageshows.setImageResource(R.drawable.handup)
                smallimageshows.visibility = View.VISIBLE
            }
        }
        hand3 = findViewById(R.id.handsfront)
        hand3.setOnClickListener {
            if(!PlayingVideo) {
                hand1Fun = 3;
                selectedimage = 3;
                acttime = 0
                doingaction = 0
                smallimageshows.setImageResource(R.drawable.handsfront)
                smallimageshows.visibility = View.VISIBLE
            }
        }
        hip = findViewById(R.id.hip)
        hip.setOnClickListener{
            if(!PlayingVideo) {
                hand1Fun = 4;
                selectedimage = 4;
                acttime = 0
                doingaction = 0
                doingdirection = "左"
                smallimageshows.setImageResource(R.drawable.hip)
                smallimageshows.visibility = View.VISIBLE
            }
        }
        neck = findViewById(R.id.neck)
        neck.setOnClickListener{
            if(!PlayingVideo) {
                hand1Fun = 5;
                selectedimage = 5;
                acttime = 0
                doingaction = 0
                doingdirection = "左"
                smallimageshows.setImageResource(R.drawable.neck)
                smallimageshows.visibility = View.VISIBLE
            }
        }
        foot = findViewById(R.id.foot)
        foot.setOnClickListener{
            if(!PlayingVideo) {
                hand1Fun = 6;
                selectedimage = 6;
                acttime = 0
                doingaction = 0
                doingdirection = "左"
                smallimageshows.setImageResource(R.drawable.foot)
                smallimageshows.visibility = View.VISIBLE
            }
        }
        foot1 = findViewById(R.id.foot1)
        foot1.setOnClickListener{
            if(!PlayingVideo) {
                hand1Fun = 7;
                selectedimage = 7;
                acttime = 0
                doingaction = 0
                doingdirection = "左"
                smallimageshows.setImageResource(R.drawable.foot1)
                smallimageshows.visibility = View.VISIBLE
            }
        }
        back = findViewById(R.id.button7)
        back.setOnClickListener{
            val Intent = Intent(this, MainActivity3::class.java)
            startActivity(Intent)
            PlayingVideo = false
        }
        /**handup = findViewById(R.id.handup)
        handup.setOnClickListener{
            var te = 1
            acttime = 0
            if (hand1Fun != 5 && te == 1){
                hand1Fun = 5
                te = 0
            }
            if (hand1Fun != 4 && te == 1){
                hand1Fun = 4
                te = 0
            }
        }*/
        closeimage = findViewById(R.id.closeimage)
        closeimage.setOnClickListener{
            PlayingVideo = false
            showcaseimage.visibility = View.GONE
            //showcaseVideo.visibility = View.INVISIBLE
            VideoFrame.visibility = View.INVISIBLE
            closeimage.visibility = View.GONE
            smallimageshows.visibility = View.VISIBLE
        }
        showcaseVideo = findViewById(R.id.showcaseVideo)
        smallimageshows = findViewById(R.id.smallimageshows)
        smallimageshows.setOnClickListener{
            when (selectedimage) {
                1 -> {
                    PlayingVideo = true
                    var packageName = "android.resource://"+ packageName +"/"+ R.raw.handsback
                    var uri = Uri.parse(packageName)
                    showcaseVideo.setVideoURI(uri)
                    showcaseVideo.start()
                }
                2 -> {
                    PlayingVideo = true
                    var packageName = "android.resource://"+ packageName +"/"+ R.raw.handsup
                    var uri = Uri.parse(packageName)
                    showcaseVideo.setVideoURI(uri)
                    showcaseVideo.start()
                }
                3 -> {
                    PlayingVideo = true
                    var packageName = "android.resource://"+ packageName +"/"+ R.raw.handsfront
                    var uri = Uri.parse(packageName)
                    showcaseVideo.setVideoURI(uri)
                    showcaseVideo.start()
                }
                4 -> {PlayingVideo = true
                    var packageName = "android.resource://"+ packageName +"/"+ R.raw.hip
                    var uri = Uri.parse(packageName)
                    showcaseVideo.setVideoURI(uri)
                    showcaseVideo.start()}
                5 -> {PlayingVideo = true
                    var packageName = "android.resource://"+ packageName +"/"+ R.raw.neck
                    var uri = Uri.parse(packageName)
                    showcaseVideo.setVideoURI(uri)
                    showcaseVideo.start()}
                6 -> {PlayingVideo = true
                    var packageName = "android.resource://"+ packageName +"/"+ R.raw.foot
                    var uri = Uri.parse(packageName)
                    showcaseVideo.setVideoURI(uri)
                    showcaseVideo.start()}
                7 -> {PlayingVideo = true
                    var packageName = "android.resource://"+ packageName +"/"+ R.raw.foot1
                    var uri = Uri.parse(packageName)
                    showcaseVideo.setVideoURI(uri)
                    showcaseVideo.start()}
            }
            //showcaseimage.visibility = View.VISIBLE
            val mediacontroller = MediaController(this)
            showcaseVideo.setMediaController(mediacontroller)
            closeimage.visibility = View.VISIBLE
            VideoFrame.visibility = View.VISIBLE
            //showcaseVideo.visibility = View.VISIBLE
            smallimageshows.visibility = View.GONE
        }
        /** 用来显示 Debug 信息 */
        //tvDebug = findViewById(R.id.tvDebug)

        /** 用来显示当前坐姿状态 */
        nowact = findViewById(R.id.now_act)
        actTimes = findViewById(R.id.act_time)
        back = findViewById(R.id.button7)
        hands_ = findViewById(R.id.hands_)
        hip_ = findViewById(R.id.hip_)
        neck_ = findViewById(R.id.neck_)
        foot_ = findViewById(R.id.foot_)
        hiptick = findViewById(R.id.hiptick)
        handsbacktick = findViewById(R.id.handsbacktick)
        handsfronttick = findViewById(R.id.handsfronttick)
        handsuptick = findViewById(R.id.handsuptick)
        necktick = findViewById(R.id.necktick)
        foottick = findViewById(R.id.foottick)
        foot1tick = findViewById(R.id.foot1tick)
        //tvFPS = findViewById(R.id.tvFps)
        //spnDevice = findViewById(R.id.spnDevice)
        spnCamera = findViewById(R.id.spnCamera)
        surfaceView = findViewById(R.id.surfaceView)
        showcaseimage = findViewById(R.id.showcaseimage)
        initSpinner()
        if (!isCameraPermissionGranted()) {
            requestPermission()
        }
    }


    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }
    /** 检查相机权限是否有授权 */
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }
    var selectedimage = 0
    var hand1Fun = 0;

    private fun openCamera() {
        /** 音频播放 */
        val crosslegPlayer = MediaPlayer.create(this, R.raw.crossleg)
        //val forwardheadPlayer = MediaPlayer.create(this, R.raw.forwardhead)
        //val standardPlayer = MediaPlayer.create(this, R.raw.standard)
        val incorrectsound = MediaPlayer.create(this, R.raw.incorrect)
        val standardPlayer = MediaPlayer.create(this, R.raw.correct)
        val doneAGroup = MediaPlayer.create(this, R.raw.doneagroup)
        val doneAll = MediaPlayer.create(this, R.raw.doneall)
        val hand1fun1wrong = MediaPlayer.create(this, R.raw.hand1fun1wrong)
        val hand1fun1wrong1 = MediaPlayer.create(this, R.raw.hand1fun1wrong1)
        val hand1fun2wrong = MediaPlayer.create(this, R.raw.hand1fun2wrong)
        val hand1fun2wrong1 = MediaPlayer.create(this, R.raw.hand1fun2wrong1)
        var crosslegPlayerFlag = true
        var forwardheadPlayerFlag = true
        var standardPlayerFlag = true
        var startCount = false
        print("BYE")
        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, selectedCamera, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {

                            /** 解释一下，tfe_pe_tv 的意思：tensorflow example、pose estimation、text view */
                            //tvDebug.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        var startAction = 0
                        var countAction:Int = 0
                        var countAction1:Int = 0
                        var startCountFps = 0;
                        var wrongfps = 0
                        var wrongfps1 = 0
                        var endCountFps = 0;
                        var startTime = 0;
                        var armdis:Float = 0F;
                        var armparline:Float = 0F;
                        var armheight = 0
                        var handdis:Float = 0F
                        var handparline:Float = 0F
                        var handheight = 0
                        var handheight1 = 0
                        var armheight1 = 0
                        var wrongmessage:String = ""
                        var act1 = 0
                        var act2 = 0
                        var act3 = 0
                        var act4 = 0
                        //var acttime = 0
                        var requireact = 0
                        var requireact2 = 0
                        var requireact3 = 0
                        var requireact4 = 0
                        var pairact = 0
                        var counter = 0
                        @SuppressLint("SetTextI18n")
                        override fun onDetectedInfo2(allData: MutableList<Person>?){
                            //println(allData?.get(0)?.keyPoints?.get(0)?.coordinate?.x.toString() + "," + allData?.get(0)?.keyPoints?.get(0)?.coordinate?.y.toString())
                            //println(allData?.get(0)?.keyPoints?.get(0))
//                            var a = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(13)?.coordinate))
//
//                            println("angle: " + a);
//                            println("LEFT_SHOULDER: " + allData?.get(0)?.keyPoints?.get(5)?.coordinate?.x)
//                            println("LEFT_HIP: " + allData?.get(0)?.keyPoints?.get(11)?.coordinate)
//                            println("LEFT_KNEE: " + allData?.get(0)?.keyPoints?.get(13)?.coordinate)



                            //var leftarm = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(7)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(9)?.coordinate))




                            var diff = SurveyResult.Diff
                            if(diff == "低"){
                                requireact = 2
                                requireact2 = 2
                                requireact3 = 1
                                requireact4 = 1
                                pairact = 2
                            }else if(diff == "中"){
                                requireact = 5
                                requireact2 = 5
                                requireact3 = 2
                                requireact4 = 2
                                pairact = 4
                            }else if(diff == "高"){
                                requireact = 9
                                requireact2 = 9
                                requireact3 = 5
                                requireact4 = 6
                                pairact = 6
                            }
                            //var hand1clicked = false
                            //var legclicked = false

                            println("distance: "+handdis)
                            println("hand distance: "+handdis+" hand height: "+ handheight+" "+handheight1)

                            //handsback
                            if(hand1Fun == 1) {
                                var handtoback = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(7)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate))
                                var handtoback2 = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(8)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(12)?.coordinate))
                                var rightbody = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(12)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(14)?.coordinate))
                                var leftbody = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(13)?.coordinate))
                                var lefthand = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(9)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(7)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate))
                                var righthand = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(8)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(10)?.coordinate))
                                var leftcrossangle = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(7)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(12)?.coordinate))
                                var rightcrossangle = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(8)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate))
                                //println(handtoback2.toDouble())
                                if (lefthand.toDouble() < 150 || righthand.toDouble() < 150){
                                    wrongfps1++
                                    if(wrongfps1 >= 40 && !wrongaction1){
                                        wrongfps1 = 0
                                        wrongmessage = "arms' angle"
                                        hand1fun1wrong1.start()
                                        wrongaction1 = true
                                    }
                                }else if(handtoback.toDouble() < 50 || handtoback2.toDouble() < 50 || leftcrossangle.toDouble() < 50 || rightcrossangle.toDouble() < 50){
                                    wrongfps++
                                    if(wrongfps >= 40 && !wrongaction){
                                        wrongfps = 0
                                        wrongmessage = "hands' angle"
                                        hand1fun1wrong.start()
                                        wrongaction = true
                                    }
                                }
                                if ((handtoback.toDouble() > 25 || handtoback2.toDouble() > 25) && leftbody.toDouble() > 140 && rightbody.toDouble() > 140 && (lefthand.toDouble() > 140 || righthand.toDouble() > 140)) {
                                    wrongmessage = ""

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0 || countdoingaction!=0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt() - countdoingaction
                                        countdoingaction = 0
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && (handtoback.toDouble() > 25 || handtoback2.toDouble() > 25) && leftbody.toDouble() > 140 && rightbody.toDouble() > 140 && (lefthand.toDouble() > 140 || righthand.toDouble() > 140)) {
                                    doingaction++
                                    wrongaction = false
                                    wrongaction1 = false
                                    wrongfps = 0
                                    wrongfps1 = 0
                                    countAction =
                                        ((currentTimeMillis() / 1000L).toInt() - startTime)

                                    if (countAction >= 10) {
                                        doneagroup = true
                                        doingaction = 0
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                    }
                                    if(doneagroup){
                                        doneAGroup.start()
                                        doneagroup = false
                                    }
                                } else {
                                    countdoingaction = countAction;
                                    startTime = 0;
//                                    startTime = (currentTimeMillis() / 1000L).toInt() - startTime //                                    countAction = 0
                                    if(doingaction >= 20) {
                                        //incorrectsound.start()
                                        doingaction = 0
                                    }
                                }

                                if(acttime == requireact){
                                    hand1.alpha = 0.5F
                                    handsbacktick.alpha = 1F
                                    hand1Fun = 2
                                    acttime = 0
                                    countAction = 0
                                    //smallimageshows.setImageResource(R.drawable.handup)
                                }
                                //var textcount = "%.2f".format(handtoback.toDouble()) + " Count: " + countAction + "ActTime: "+ acttime
                                //tvFPS.text = textcount
                                nowact.text = "正在做：手後直"
                                var counter = ""+acttime+"組/"+requireact+"組 "+countAction+"秒/10秒"+wrongmessage
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                            }

                            //handsup
                            if(hand1Fun == 2) {
                                var handup = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(7)?.coordinate))
                                var leftbody = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(13)?.coordinate))
                                var rightbody = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(12)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(14)?.coordinate))
                                var lefthandup = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(7)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(9)?.coordinate))
                                var righthandup = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(8)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(10)?.coordinate))
                                //var leftarmxy = ((allData?.get(0)?.keyPoints?.get(7)?.coordinate))
                                //var rightarmxy = ((allData?.get(0)?.keyPoints?.get(8)?.coordinate))
                                var leftwristxy = (allData?.get(0)?.keyPoints?.get(9)?.coordinate)
                                var rightwristxy = (allData?.get(0)?.keyPoints?.get(10)?.coordinate)
                                println(handup)

                                if (leftwristxy != null && rightwristxy != null) {
                                    var righthandx = rightwristxy.x
                                    var righthandy = rightwristxy.y
                                    var lefthandx = leftwristxy.x
                                    var lefthandy = leftwristxy.y
                                    handdis = sqrt(((lefthandx-righthandx)*(lefthandx-righthandx))+((lefthandy-righthandy)*(lefthandy-righthandy)))
                                    //handparline = sqrt((lefthandx-righthandx)*(lefthandx-righthandx))
                                    //handheight = sqrt((handdis*handdis)-(handparline*handparline)).toInt()
                                    handheight1 = sqrt((lefthandy-righthandy)*(lefthandy-righthandy)).toInt()
                                }

                                if(lefthandup.toDouble() < 150 || righthandup.toDouble() < 150){
                                    wrongfps++
                                    if(wrongfps >= 40 && !wrongaction1){
                                        wrongfps = 0
                                        wrongmessage = "arms' angle"
                                        hand1fun2wrong.start()
                                        wrongaction1 = true
                                    }
                                }

                                else if(handup.toDouble() < 150){
                                    wrongfps1++
                                    if(wrongfps1 >= 40 && !wrongaction1){
                                        wrongfps1 = 0
                                        wrongmessage = "handdis' angle"
                                        hand1fun1wrong.start()
                                        wrongaction1 = true
                                    }
                                }
                                if (handup.toDouble() > 135 && lefthandup.toDouble() > 135 && righthandup.toDouble() > 140 && leftbody.toDouble() > 140 && rightbody.toDouble() > 140) {
                                        wrongmessage = ""
                                        startCountFps++;

                                        if (startCountFps >= 10 && startTime == 0 || countdoingaction!=0) {
                                            startTime = (currentTimeMillis() / 1000L).toInt() - countdoingaction
                                            countdoingaction = 0;
                                            startAction = 1;
                                            startCountFps = 0;
                                        }
                                    }
                                    if (startTime != 0 && handup.toDouble() > 130 && lefthandup.toDouble() > 140 && righthandup.toDouble() > 140 && leftbody.toDouble() > 140 && rightbody.toDouble() > 140) {
                                        doingaction++
                                        wrongaction = false
                                        wrongaction1 = false
                                        wrongfps = 0
                                        wrongfps1 = 0
                                        countAction =
                                            (currentTimeMillis() / 1000L).toInt() - startTime
                                        if (countAction >= 10) {
                                            doneagroup = true
                                            doingaction = 0
                                            acttime++
                                            countAction = 0
                                            startTime = 0
                                        }
                                    } else {
//                                        countAction = 0
                                        startTime = 0
                                        countdoingaction = countAction;
                                    }
                                if(doneagroup){
                                    doneAGroup.start()
                                    doneagroup = false
                                    doingaction = 0
                                }
                                if(countAction == 0 && doingaction >= 20){
                                    //incorrectsound.start()
                                    doingaction = 0
                                }
                                if(acttime == requireact2){
                                    hand2.alpha = 0.5F
                                    handsuptick.alpha = 1F
                                    hand1Fun = 3
                                    acttime = 0
                                    countAction = 0
                                    //smallimageshows.setImageResource(R.drawable.handsfront)
                                }
                                //var textcount = "%.2f".format(lefthandup .toDouble()) + " | " +"%.2f".format(righthandup .toDouble())+" | "+"%.2f".format(handup .toDouble()) + " handdis: " + handdis
                                //tvFPS.text = textcount
                                nowact.text = "正在做：手上"
                                var counter = ""+acttime+"組/"+requireact2+"組 "+countAction+"秒/10秒"+wrongmessage
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                                }

                            //handsfront
                            if(hand1Fun == 3) {
                                var leftarmxy = ((allData?.get(0)?.keyPoints?.get(7)?.coordinate))
                                var rightarmxy = ((allData?.get(0)?.keyPoints?.get(8)?.coordinate))
                                var leftwristxy = (allData?.get(0)?.keyPoints?.get(9)?.coordinate)
                                var rightwristxy = (allData?.get(0)?.keyPoints?.get(10)?.coordinate)
                                var lefthandup = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(7)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(9)?.coordinate))
                                var righthandup = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(8)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(10)?.coordinate))
                                if (leftwristxy != null && rightwristxy != null) {
                                    var righthandx = rightwristxy.x
                                    var righthandy = rightwristxy.y
                                    var lefthandx = leftwristxy.x
                                    var lefthandy = leftwristxy.y
                                    handdis = sqrt(((lefthandx-righthandx)*(lefthandx-righthandx))+((lefthandy-righthandy)*(lefthandy-righthandy)))
                                    //handparline = sqrt((lefthandx-righthandx)*(lefthandx-righthandx))
                                    //handheight = sqrt((handdis*handdis)-(handparline*handparline)).toInt()
                                    handheight1 = sqrt((lefthandy-righthandy)*(lefthandy-righthandy)).toInt()
                                }
                                if (leftarmxy != null && rightarmxy != null) {
                                    var rightarmx = rightarmxy.x
                                    var rightarmy = rightarmxy.y
                                    var leftarmx = leftarmxy.x
                                    var leftarmy = leftarmxy.y

                                    //armdis = sqrt(((leftarmx -rightarmx)*(leftarmx -rightarmx))+((leftarmy-rightarmy)*(leftarmy-rightarmy))).toFloat()
                                    //armparline = sqrt(((leftarmx-rightarmx)*(leftarmx-rightarmx)))
                                    //armheight = sqrt((armdis*armdis)-(armparline*armparline)).toInt()
                                    armheight1 = sqrt((leftarmy-rightarmy)*(leftarmy-rightarmy)).toInt()
                                }
                                if(lefthandup.toDouble() < 150 || righthandup.toDouble() < 150){
                                    wrongfps++
                                    if(wrongfps > 20){
                                        wrongfps = 0
                                    }
                                }

                                if (armheight1 < 40 && handheight1 < 70 && handdis < 70 && (lefthandup.toDouble() < 150 && righthandup.toDouble() < 150)){

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && (armheight1 < 40 && handheight1 < 70 && handdis < 70 && (lefthandup.toDouble() < 150 && righthandup.toDouble() < 150))) {
                                    doingaction++
                                    countAction =
                                        (currentTimeMillis() / 1000L).toInt() - startTime
                                    if (countAction >= 10) {
                                        doneagroup = true
                                        doingaction = 0
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                    }
                                } else {
                                    startTime = 0
                                    countAction = 0
                                }
                                if(doneagroup){
                                    doneAGroup.start()
                                    doneagroup = false
                                }
                                if(countAction == 0 && doingaction >= 20){
                                    incorrectsound.start()
                                    doingaction = 0
                                }
                                if(acttime == requireact3){
                                    hand3.alpha = 0.5F
                                    handsfronttick.alpha = 1F
                                    hand1Fun = 4
                                    acttime = 0
                                    countAction = 0
                                    //smallimageshows.setImageResource(R.drawable.hip)
                                }
                                //var textcount = "$handheight1 $armheight1 $startCountFps Time: $countAction1"
                                //tvFPS.text = textcount
                                nowact.text = "正在做：手前"
                                var counter = ""+acttime+"組/"+requireact3+"組 "+countAction+"秒/10秒"
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                            }

                            //hip
                            if(hand1Fun == 4) {
                                var handtobackleft = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(9)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(5)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate))
                                var handtobackright = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(10)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(6)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(12)?.coordinate))
                                if (handtobackleft.toDouble() > 140 && handtobackright.toDouble() < 30 && doingdirection == "左"){

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }else if(handtobackright.toDouble() > 140 && handtobackleft.toDouble() < 30 && doingdirection == "右"){
                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && ((handtobackleft.toDouble() > 140 && handtobackright.toDouble() < 30) || (handtobackright.toDouble() > 140 && handtobackleft.toDouble() < 30))) {
                                    doingaction++
                                    countAction =
                                        (currentTimeMillis() / 1000L).toInt() - startTime
                                    if(acttime == pairact/2) {
                                        doingdirection = "右"
                                    }
                                    if (countAction >= 10) {
                                        doneagroup = true
                                        doingaction = 0
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                    }
                                } else {
                                    startTime = 0
                                    countAction = 0
                                }
                                if(doneagroup){
                                    doneAGroup.start()
                                    doneagroup = false
                                }
                                if(countAction == 0 && doingaction >= 20){
                                    incorrectsound.start()
                                    doingaction = 0
                                }
                                /////////////////////////
                                if(acttime == pairact/2){
                                    doingdirection = "左"
                                    foot.alpha = 0.5F
                                    foottick.alpha = 1F
                                    hand1Fun = 5
                                    acttime = 0
                                    countAction = 0
                                    //smallimageshows.setImageResource(R.drawable.neck)
                                }
                                /////////////////////////
                                //var textcount = "$handheight1 $armheight1 $startCountFps Time: $countAction1"
                                //tvFPS.text = textcount
                                nowact.text = "正在做：彎腰 (" + doingdirection + "手向上)"
                                var counter = ""+acttime+"組/"+pairact+"組 "+countAction+"秒/10秒"
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                            }

                            //neck
                            if(hand1Fun == 5) {
                                var leftearxy = ((allData?.get(0)?.keyPoints?.get(3)?.coordinate))
                                var rightearxy = ((allData?.get(0)?.keyPoints?.get(4)?.coordinate))
                                var leftshoulderxy = (allData?.get(0)?.keyPoints?.get(5)?.coordinate)
                                var rightshoulderxy = (allData?.get(0)?.keyPoints?.get(6)?.coordinate)
                                if (rightearxy != null && rightshoulderxy != null) {
                                    var rightshoulderx = rightshoulderxy.x
                                    var rightshouldery = rightshoulderxy.y
                                    var rightearx = rightearxy.x
                                    var righteary = rightearxy.y
                                    rightneck =
                                        sqrt(((rightearx - rightshoulderx) * (rightearx - rightshoulderx)) + ((righteary - rightshouldery) * (righteary - rightshouldery))).toInt()
                                }
                                if (leftearxy != null && leftshoulderxy != null) {
                                    var leftearx = leftearxy.x
                                    var lefteary = leftearxy.y
                                    var leftshoulderx = leftshoulderxy.x
                                    var leftshouldery = leftshoulderxy.y
                                    leftneck =
                                        sqrt(((leftearx - leftshoulderx) * (leftearx - leftshoulderx)) + ((lefteary - leftshouldery) * (lefteary - leftshouldery))).toInt()
                                }
                                if (leftneck < rightneck && doingdirection == "左"){

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }else if(leftneck > rightneck && doingdirection == "右"){

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && ((leftneck < 30 && rightneck > 30) || (rightneck < 30 && leftneck > 30))) {
                                    doingaction++
                                    countAction =
                                        (currentTimeMillis() / 1000L).toInt() - startTime
                                    if(acttime == pairact/2) {
                                        doingdirection = "右"
                                    }
                                    if (countAction >= 10) {
                                        doneagroup = true
                                        doingaction = 0
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                    }
                                } else {
                                    startTime = 0
                                    countAction = 0
                                }
                                if(doneagroup){
                                    doneAGroup.start()
                                    doneagroup = false
                                }
                                if(countAction == 0 && doingaction >= 20){
                                    incorrectsound.start()
                                    doingaction = 0
                                }
                                /////////////////////////
                                if(acttime == pairact/2){
                                    doingdirection = "左"
                                    foot.alpha = 0.5F
                                    foottick.alpha = 1F
                                    hand1Fun = 6
                                    acttime = 0
                                    countAction = 0
                                    //smallimageshows.setImageResource(R.drawable.foot)
                                }
                                /////////////////////////
                                //var textcount = "$handheight1 $armheight1 $startCountFps Time: $countAction1"
                                //tvFPS.text = textcount
                                nowact.text = "正在做：頸 (向" + doingdirection + "歪)"
                                var counter = ""+acttime+"組/"+pairact+"組 "+countAction+"秒/10秒"
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                            }

                            //foot
                            if(hand1Fun == 6) {
                                var leftleg = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(13)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(15)?.coordinate))
                                var rightleg = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(12)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(14)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(16)?.coordinate))
                                if (leftleg.toDouble() < 120 && rightleg.toDouble() > 100 && doingdirection == "左"){

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }else if(leftleg.toDouble() > 100 && rightleg.toDouble() < 120 && doingdirection == "右"){

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && ((leftleg.toDouble() < 120 && rightleg.toDouble() > 100) || (leftleg.toDouble() > 100 && rightleg.toDouble() < 120))) {
                                    doingaction++
                                    countAction =
                                        (currentTimeMillis() / 1000L).toInt() - startTime
                                    if(acttime == pairact/2) {
                                        doingdirection = "右"
                                    }
                                    if (countAction >= 10) {
                                        doneagroup = true
                                        doingaction = 0
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                    }
                                } else {
                                    startTime = 0
                                    countAction = 0
                                }
                                if(doneagroup){
                                    doneAGroup.start()
                                    doneagroup = false
                                }
                                if(countAction == 0 && doingaction >= 20){
                                    incorrectsound.start()
                                    doingaction = 0
                                }
                                /////////////////////////
                                if(acttime == pairact){
                                    doingdirection = "左"
                                    foot.alpha = 0.5F
                                    foottick.alpha = 1F
                                    hand1Fun = 7
                                    acttime = 0
                                    countAction = 0
                                    //smallimageshows.setImageResource(R.drawable.foot1)
                                }
                                /////////////////////////
                            //var textcount = "$handheight1 $armheight1 $startCountFps Time: $countAction1"
                            //tvFPS.text = textcount
                            nowact.text = "正在做：前曲後直 (" + doingdirection + "腳伸前)"
                            var counter = ""+acttime+"組/"+pairact+"組 "+countAction+"秒/10秒"
                            actTimes.text = counter
                            nowact.alpha = 1F
                            actTimes.alpha = 1F
                            }

                            //foot1
                            if(hand1Fun == 7) {
                                var leftleg = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(11)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(13)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(15)?.coordinate))
                                var rightleg = calculateAngle(pointFToArray(allData?.get(0)?.keyPoints?.get(12)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(14)?.coordinate),pointFToArray(allData?.get(0)?.keyPoints?.get(16)?.coordinate))
                                if (leftleg.toDouble() > 130 && rightleg.toDouble() < 160 && doingdirection == "左"){

                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }else if(leftleg.toDouble() < 160 && rightleg.toDouble() > 130 && doingdirection == "右"){
                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && ((leftleg.toDouble() > 130 && rightleg.toDouble() < 160) || (leftleg.toDouble() < 160 && rightleg.toDouble() > 130))) {
                                    doingaction++
                                    countAction =
                                        (currentTimeMillis() / 1000L).toInt() - startTime
                                    if(acttime == pairact/2) {
                                        doingdirection = "右"
                                    }
                                    if (countAction >= 10) {
                                        doneagroup = true
                                        doingaction = 0
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                    }
                                } else {
                                    startTime = 0
                                    countAction = 0
                                }
                                if(doneagroup){
                                    doneAGroup.start()
                                    doneagroup = false
                                }
                                if(countAction == 0 && doingaction >= 20){
                                    incorrectsound.start()
                                    doingaction = 0
                                }
                                //////////////////////////
                                if(acttime == pairact){
                                    foot1.alpha = 0.5F
                                    foot1tick.alpha = 1F
                                    hand1Fun = 8
                                    acttime = 0
                                    countAction = 0
                                    doingdirection = "左"
                                }
                                //////////////////////////
                                //var textcount = "$handheight1 $armheight1 $startCountFps Time: $countAction1"
                                //tvFPS.text = textcount
                                nowact.text = "正在做：曲腳 (" + doingdirection + "腳伸前)"
                                var counter = ""+acttime+"組/"+pairact+"組 "+countAction+"秒/10秒"
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                            }


                            if(hand1Fun == 8 && foottick.alpha == 1F && foot1tick.alpha == 1F && handsfronttick.alpha == 1F && handsbacktick.alpha == 1F && handsuptick.alpha == 1F && hiptick.alpha == 1F && necktick.alpha == 1F){
                                doneAll.start()
                                nowact.text = "恭喜完成了所有動作"
                                actTimes.alpha = 0F
                                hand1Fun = 0
                            }
                            /**if (hand1Fun == 4){
                                if((lefthandup.toDouble() > 30 && lefthandup.toDouble() < 140)&&(righthandup.toDouble() < 150)){
                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && (lefthandup.toDouble() > 30 && lefthandup.toDouble() < 140)&&(righthandup.toDouble() < 120)) {
                                    startTime = 0
                                    countAction =
                                        (currentTimeMillis() / 1000L).toInt() - startTime
                                    if(countAction != 0){
                                        countAction = 0
                                        counter++
                                    }

                                    if (counter >= 10) {
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                        counter = 0
                                    }
                                }else{
                                    startTime = 0
                                }
                                if(acttime == requireact4){
                                    hand1Fun = 6
                                    acttime = 0
                                    countAction = 0
                                    counter = 0
                                }
                                nowact.text = "正在做：手後曲拉右手"
                                var counter = ""+acttime+"組 "+counter+"秒/10秒"
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                                //var textcount = "%.2f".format(lefthandup .toDouble()) + " | " +"%.2f".format(righthandup .toDouble())
                                //tvFPS.text = textcount
                            }
                            if (hand1Fun == 5){
                                if((righthandup.toDouble() > 30 && righthandup.toDouble() < 140)&&(lefthandup.toDouble() < 150)){
                                    startCountFps++;

                                    if (startCountFps >= 10 && startTime == 0) {
                                        startTime = (currentTimeMillis() / 1000L).toInt()
                                        startAction = 1;
                                        startCountFps = 0;
                                    }
                                }
                                if (startTime != 0 && (righthandup.toDouble() > 30 && righthandup.toDouble() < 140)&&(lefthandup.toDouble() < 120)) {

                                    countAction =
                                        (currentTimeMillis() / 1000L).toInt() - startTime
                                    if(countAction == 1){
                                        countAction = 0
                                        startTime = 0
                                        counter++
                                    }

                                    if (counter >= 10) {
                                        acttime++
                                        countAction = 0
                                        startTime = 0
                                        counter = 0
                                    }
                                }else{
                                    startTime = 0
                                }
                                if(acttime == requireact4){
                                    hand1Fun = 4
                                    acttime = 0
                                    countAction = 0
                                    counter = 0
                                }
                                nowact.text = "正在做：手後曲拉左手"
                                var counter = ""+acttime+"組 "+counter+"秒/10秒"
                                actTimes.text = counter
                                nowact.alpha = 1F
                                actTimes.alpha = 1F
                                //var textcount = "%.2f".format(lefthandup .toDouble()) + " | " +"%.2f".format(righthandup .toDouble()) + "    "+(currentTimeMillis() / 1000L).toInt()+ "     "+ startCountFps
                                //tvFPS.text = textcount
                            }*/





                            /**if(handtoback.toDouble() <60) {

                                startCountFps++;

                                if(startCountFps >= 10 && startTime == 0) {
                                    startTime = (currentTimeMillis() / 1000L).toInt()
                                    startAction = 1;
                                    startCountFps = 0;
                                }
                            }
                            if(startTime != 0 && handtoback.toDouble() > 30) {
                                countAction = (currentTimeMillis() / 1000L).toInt() - startTime
                                if(countAction >= 15) {
                                    countAction = 0
                                    startTime = 0
                                }
                            } else {
                                countAction=0
                                startTime = 0

                            }*/
                            /*if(startAction ==1 && handtoback.toDouble() > 30) {
                                countAction++;
                                //startAction = 0;
                                if(countAction >= 15) {
                                    startAction = 0;
                                    endCountFps = 0;
                                    countAction = 0;
                                }
                            }else if(handtoback.toDouble() <= 30){
                                countAction=0
                                startAction=0
                            }*/








                        }

                        /** 对检测结果进行处理 */
                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?
                        ) {
                            return;
                            //tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)
                            tvScore.text = poseLabels?.get(0)?.first;
                            /** 分析目标姿态，给出提示 */
                            if (poseLabels != null && personScore != null && personScore > 0.3) {
                                missingCounter = 0
                                val sortedLabels = poseLabels.sortedByDescending { it.second }
                                when (sortedLabels[0].first) {
                                    "incorrect" -> {
                                        crosslegCounter = 0
                                        standardCounter = 0
                                        if (poseRegister == "incorrect") {
                                            forwardheadCounter++
                                        }
                                        poseRegister = "incorrect"

                                        /** 显示当前坐姿状态：脖子前伸 */
                                        if (forwardheadCounter > 60) {

                                            /** 播放提示音 */
                                            if (forwardheadPlayerFlag) {
                                                //forwardheadPlayer.start()
                                            }
                                            standardPlayerFlag = true
                                            crosslegPlayerFlag = true
                                            forwardheadPlayerFlag = false

                                            //ivStatus.setImageResource(R.drawable.incorrect)
                                        } else if (forwardheadCounter > 30) {

                                            //ivStatus.setImageResource(R.drawable.incorrect)
                                        }

                                        /** 显示 Debug 信息 */
                                        //tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $forwardheadCounter")
                                    }
                                    "crossleg" -> {
                                        forwardheadCounter = 0
                                        standardCounter = 0
                                        if (poseRegister == "crossleg") {
                                            crosslegCounter++
                                        }
                                        poseRegister = "crossleg"

                                        /** 显示当前坐姿状态：翘二郎腿 */
                                        if (crosslegCounter > 60) {

                                            /** 播放提示音 */
                                            if (crosslegPlayerFlag) {
                                                crosslegPlayer.start()
                                            }
                                            standardPlayerFlag = true
                                            crosslegPlayerFlag = false
                                            forwardheadPlayerFlag = true
                                            //ivStatus.setImageResource(R.drawable.crossleg_confirm)
                                        } else if (crosslegCounter > 30) {
                                            //ivStatus.setImageResource(R.drawable.crossleg_suspect)
                                        }

                                        /** 显示 Debug 信息 */
                                        //tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $crosslegCounter")
                                    }
                                    else -> {
                                        forwardheadCounter = 0
                                        crosslegCounter = 0
                                        if (poseRegister == "correct") {
                                            standardCounter++
                                        }
                                        poseRegister = "correct"

                                        /** 显示当前坐姿状态：标准 */
                                        if (standardCounter > 30) {

                                            /** 播放提示音：坐姿标准 */
                                            if (standardPlayerFlag) {
                                                standardPlayer.start()
                                            }
                                            standardPlayerFlag = false
                                            crosslegPlayerFlag = true
                                            forwardheadPlayerFlag = true

                                            //ivStatus.setImageResource(R.drawable.correct)
                                        }

                                        /** 显示 Debug 信息 */
                                        //tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $standardCounter")
                                    }
                                }


                            }
                            else {
                                missingCounter++
                                if (missingCounter > 30) {
                                    //ivStatus.setImageResource(R.drawable.incorrect)
                                    tvScore.text = "Missing"
                                }

                                /** 显示 Debug 信息 */
                                //tvDebug.text = getString(R.string.tfe_pe_tv_debug, "missing $missingCounter")
                            }
                        }
                    }).apply {
                        prepareCamera()
                    }
                isPoseClassifier()
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }

    private fun isPoseClassifier() {
        cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(this) else null)
    }

    /** 初始化运算设备选项菜单（CPU、GPU、NNAPI） */
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            //spnDevice.adapter = adapter
            //spnDevice.onItemSelectedListener = changeDeviceListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_camera_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnCamera.adapter = adapter
            spnCamera.onItemSelectedListener = changeCameraListener
        }
    }

    /** 在程序运行过程中切换运算设备 */
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    /** 在程序运行过程中切换摄像头 */
    private fun changeCamera(direaction: Int) {
        val targetCamera = when (direaction) {
            0 -> Camera.BACK
            else -> Camera.FRONT
        }
        if (selectedCamera == targetCamera) return
        selectedCamera = targetCamera

        cameraSource?.close()
        cameraSource = null
        openCamera()
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(this, device, ModelType.Thunder)
        poseDetector.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    /** 显示报错信息 */
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // pass
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
