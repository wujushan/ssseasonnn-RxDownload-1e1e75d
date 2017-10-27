package zlc.season.rxdownload.kotlin_demo

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import zlc.season.rxdownload.kotlin_demo.databinding.ActivityBasicDownloadBinding
import zlc.season.rxdownload.kotlin_demo.databinding.ContentBasicDownloadBinding
import zlc.season.rxdownload3.RxDownload
import zlc.season.rxdownload3.core.*
import zlc.season.rxdownload3.extension.ApkInstallExtension
import zlc.season.rxdownload3.helper.dispose
import zlc.season.rxdownload3.helper.logd

class BasicDownloadActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityBasicDownloadBinding
    private lateinit var contentBinding: ContentBasicDownloadBinding

    private var disposable: Disposable? = null
    private var currentStatus = Status()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_basic_download)
        contentBinding = mainBinding.contentBasicDownload!!

        setSupportActionBar(mainBinding.toolbar)

        loadImg()
        setAction()
        create()
    }

    override fun onDestroy() {
        super.onDestroy()
        dispose(disposable)
    }

    private fun loadImg() {
        Picasso.with(this).load(iconUrl).into(contentBinding.img)
    }

    private fun setAction() {
        contentBinding.action.setOnClickListener {
            when (currentStatus) {
                is Normal -> start()
                is Suspend -> start()
                is Failed -> start()
                is Downloading -> stop()
                is Succeed -> start()
                is ApkInstallExtension.Installed -> open()
            }
        }

        contentBinding.finish.setOnClickListener { finish() }
    }

    private fun create() {
        disposable = RxDownload.create(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { status ->
                    currentStatus = status
                    setProgress(status)
                    setActionText(status)
                }
    }

    private fun setProgress(status: Status) {
        contentBinding.progress.max = status.totalSize.toInt()
        contentBinding.progress.progress = status.downloadSize.toInt()

        contentBinding.percent.text = status.percent()
        contentBinding.size.text = status.formatString()
    }

    private fun setActionText(status: Status) {
        val text = when (status) {
            is Normal -> "开始"
            is Suspend -> "已暂停"
            is Waiting -> {
                logd("等待中")
                "等待中"}
            is Downloading -> "暂停"
            is Failed -> "失败"
            is Succeed -> "下载完成"
            is ApkInstallExtension.Installing -> "安装中"
            is ApkInstallExtension.Installed -> "打开"
            else -> ""
        }
       /* if(status is Succeed){
            RxDownload.delete(url,true).subscribe()
        }*/
        contentBinding.action.text = text
    }

    private fun start() {
        RxDownload.start(url).subscribe()
    }

    private fun stop() {
        RxDownload.stop(url).subscribe()
    }

    private fun install() {
        RxDownload.extension(url, ApkInstallExtension::class.java).subscribe()
    }

    private fun open() {
        //TODO: open app
    }


    companion object {
        private val TAG = "BasicDownloadActivity"

        private val iconUrl = "http://p5.qhimg.com/dr/72__/t01a362a049573708ae.png"
//        private val url = "http://shouji.360tpcdn.com/170922/9ffde35adefc28d3740d4e16612f078a/com.tencent.tmgp.sgame_22011304.apk"
        private val url = "http://192.168.43.1:8080/download/apks.pz"
//        private val url = "http://h.hiphotos.baidu.com/image/pic/item/4a36acaf2edda3ccfbe265e108e93901203f92e9.jpg"
    }

}
