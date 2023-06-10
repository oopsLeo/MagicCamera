package com.frost.magiccamera

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar
import com.chad.library.adapter.base.listener.OnItemChildClickListener
import com.frost.magiccamera.adapter.StyleAdapter
import com.frost.magiccamera.bean.StyleBean
import com.frost.magiccamera.http.HttpHelper
import com.frost.magiccamera.utils.saveToAlbum
import com.squareup.picasso.Picasso
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.util.*


class AIActivity : AppCompatActivity() {

    private var styleList: MutableList<StyleBean> = ArrayList()
    private var progressBar: RoundCornerProgressBar?= null
    private var mBlurView: BlurView? = null
    private lateinit var iv: ImageView

    private var counter:Int = 0
    private lateinit var timer:Timer
    private var isSuccessful:Boolean = false

    private lateinit var sourceUri:Uri
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai)
        iv= findViewById(R.id.ivImage)
        progressBar = findViewById(R.id.progress_bar)
        mBlurView = findViewById(R.id.blurView)
        sourceUri = intent.data!!

        setImage(sourceUri)
        getStylesAndPics()

        //获取SharedPreferences对象
        val sharedPreferences = getSharedPreferences("login_state", MODE_PRIVATE)
        // 获取用户名，默认为空字符串
        val username = sharedPreferences.getString("username", "")

        val back = findViewById<ImageView>(R.id.style_back)
        back.setOnClickListener {
            finish()
        }

        val save = findViewById<ImageView>(R.id.style_save)
        save.setOnClickListener{
            // 在后台线程中执行保存图片任务
            val bitmap = (iv.getDrawable() as BitmapDrawable).bitmap
            val filename = System.currentTimeMillis().toString() + ".jpeg"
            val resultUri = bitmap.saveToAlbum(this, filename, "MagicCamera", 75)

            HttpHelper.delete(sourceUri.toString())
            HttpHelper.upload(username, this@AIActivity, resultUri)
            sendBroadcastToMainActivity()
            Toast.makeText(
                this, "Saved Successfully", Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun sendBroadcastToMainActivity() {
        val intent = Intent("UPDATE_IMAGES")
        sendBroadcast(intent)
    }


    /**
     * 图片传递放置
     */
    private fun setImage(Uri: Uri) {

        Picasso.get()
            .load(Uri)
            .into(iv)
    }





    /**
     * 获取风格data
     */
    private fun getStylesAndPics() {
        HttpHelper.getStyles(HttpHelper.OnStyleResultListener {styles ->
            if (styles!=null){
                styleList = styles
                initStyles()
            }
            else {
                styleList.add(
                    StyleBean(
                        "style1",
                        ("android.resource://com.example.app/drawable/style1")
                    )
                )
                styleList.add(
                    StyleBean(
                        "style2",
                        ("android.resource://com.example.app/drawable/style2")
                    )
                )
                styleList.add(
                    StyleBean(
                        "style3",
                        ("android.resource://com.example.app/drawable/style3")
                    )
                )
                styleList.add(
                    StyleBean(
                        "style4",
                        ("android.resource://com.example.app/drawable/style4")
                    )
                )
                styleList.add(
                    StyleBean(
                        "style5",
                        ("android.resource://com.example.app/drawable/style5")
                    )
                )
            }
        })
    }


    /**
     * 初始化风格
     */
    private fun initStyles() {

        val styleAdapter = StyleAdapter(styleList)
        val rvStyle: RecyclerView = findViewById(R.id.rvStyle)
        rvStyle.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rvStyle.adapter = styleAdapter

        // 设置点击事件
        styleAdapter.addChildClickViewIds(R.id.elastic_card)
        styleAdapter.setOnItemChildClickListener(OnItemChildClickListener { adapter, view, position ->
            generateImg(styleList[position].style)

        })
    }

    private fun generateImg(style:String){
        startProgress()
        HttpHelper.generate(style, sourceUri.toString()){image_uri ->
            setImage(image_uri)
            progressBar?.setProgress(100)
            startFadeAnim(mBlurView!!, false, 500)
            startFadeAnim(progressBar!!, false, 500)
            timer.cancel()
        }
    }


    /**
     * 进度条
     */
    private fun startProgress(){

        //模糊
        val windowBackground = window.decorView.background
        mBlurView!!.setupWith(findViewById<ViewGroup>(R.id.root), RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(5f)
        //模糊效果
        startFadeAnim(mBlurView!!, true, 500)
        startFadeAnim(progressBar!!, true, 1000)

        timer = Timer()
        counter=0
        timer.scheduleAtFixedRate(object : TimerTask() {

            override fun run() {
                if (counter <= 90) {
                    // 在UI线程上更新计数器的值
                    runOnUiThread {
                        counter++
                        progressBar?.setProgress(counter)
                    }
                } else {
                    // 定时器任务完成后取消定时器
                    timer.cancel()
                }
            }
        }, 0, 20) // 初始延时为0ms，每隔100ms执行一次

    }

    /**
     * 渐入渐出
     */
    private fun startFadeAnim(view: View, isFadeIn: Boolean, duration: Int) {

        var animator: ObjectAnimator? = null
        animator = if (isFadeIn) {
            view.isClickable = true
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        } else {
            view.isClickable = false
            ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        }
        animator.duration = duration.toLong() // 设置动画持续时间，单位为毫秒
        animator.start() // 启动动画
    }
}