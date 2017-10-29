package zlc.season.rxdownload3.core


import io.reactivex.Flowable
import io.reactivex.Maybe
import zlc.season.rxdownload3.helper.ANY
import zlc.season.rxdownload3.http.HttpCore
import java.io.File

class NormalDownload(mission: RealMission) : DownloadType(mission) {
    override fun setUpMission(totalSize: Long, statusCode: Int, lastModified: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLastModified(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val targetFile = NormalTargetFile(mission)

    override fun initStatus() {
        val status = targetFile.getStatus()
        mission.status = when {
            targetFile.isFinish() -> Succeed(status)
            else -> Normal(status)
        }
    }

    override fun getFile(): File? {
        if (targetFile.isFinish()) {
            return targetFile.realFile()
        }
        return null
    }

    override fun delete() {
        targetFile.delete()
    }

    override fun download(): Flowable<out Status> {
        if (targetFile.isFinish()) {
            return Flowable.empty()
        }

        targetFile.checkFile()

        return Maybe.just(ANY)
                .flatMap { HttpCore.download(mission) }
                .flatMapPublisher {
                    targetFile.save(it)
                }
    }
}