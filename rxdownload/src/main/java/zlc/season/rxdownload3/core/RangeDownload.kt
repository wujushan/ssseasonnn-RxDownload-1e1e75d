package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers
import zlc.season.rxdownload3.core.DownloadConfig.maxRange
import zlc.season.rxdownload3.core.RangeTmpFile.Segment
import zlc.season.rxdownload3.helper.logd
import zlc.season.rxdownload3.http.HttpCore
import java.io.File


class RangeDownload(mission: RealMission) : DownloadType(mission) {


    private val targetFile = RangeTargetFile(mission)

    private val tmpFile = RangeTmpFile(mission)

    override fun initStatus() {
        val status = tmpFile.currentStatus()

        mission.status = when {
            isFinish() -> Succeed(status)
            else -> Normal(status)
        }
    }

    override fun getFile(): File? {
        if (isFinish()) {
            return targetFile.realFile()
        }
        return null
    }

    override fun delete() {
        targetFile.delete()
        tmpFile.delete()
    }

    private fun isFinish(): Boolean {
        return tmpFile.isFinish() && targetFile.isFinish()
    }

    private fun isTargetFileExists(): Boolean = targetFile.isFinish()

    private fun isTmpFileExits(): Boolean = tmpFile.isFileExits()

    private fun checkExists(): Boolean = isTargetFileExists() && isTmpFileExits()

    private fun insertLastModified(lastModified: Long) {
        tmpFile.setLastModified(lastModified)
    }

    override fun setUpMission(totalSize: Long, statusCode: Int, lastModified: Long) {
        mission.totalSize = totalSize
        mission.statusCode = statusCode
        mission.actual.lastModified = lastModified
        tmpFile.mission.totalSize = totalSize
        tmpFile.mission.statusCode = statusCode
        tmpFile.mission.actual.lastModified = lastModified
    }

    override fun getLastModified(): Long {
        return tmpFile.getLastModified()
    }
    override fun download(): Flowable<out Status> {
        //check whether the target file exists and tmpFile is finished
        if (isFinish()) {
            return Flowable.empty()
        }
        val arrays = mutableListOf<Flowable<Any>>()
/*        HttpCore.checkLastModified(tmpFile.getLastModified(), mission)
                .map { it ->
                    mission.actual.lastModified = lastModified(it)
                    when (it.code()) {
                        304 -> {
                            targetFile.delete()
                            targetFile.createShadowFile()
                            tmpFile.reset()
                        }
                        200 -> {
                            if (targetFile.isShadowExists()) {
                                tmpFile.checkFile()
                            } else {
                                targetFile.createShadowFile()
                                //reset the tmp file (delete tnp file -- create new tmp file -- write structure)
                                tmpFile.reset()
                            }
                        }
                    }
                    logd("code ${it.code()}")
                }
                .subscribe(object : Consumer<Any> {
                    override fun accept(p0: Any) {

                    }

                })*/


/*
        if (targetFile.isShadowExists()) {
            tmpFile.checkFile()
        } else {
            targetFile.createShadowFile()
            //reset the tmp file (delete tnp file -- create new tmp file -- write structure)
            tmpFile.reset()
        }
*/
        when (mission.statusCode) {
            304 -> {
                logd("304")
                targetFile.delete()
                targetFile.createShadowFile()
                tmpFile.reset()
                tmpFile.setLastModified(mission.actual.lastModified)
            }
            200 -> {
                logd("200")
                if (targetFile.isShadowExists()) {
                    tmpFile.checkFile()
                } else {
                    targetFile.createShadowFile()
                    //reset the tmp file (delete tnp file -- create new tmp file -- write structure)
                    tmpFile.reset()
                    tmpFile.setLastModified(mission.actual.lastModified)
                }
            }
        }

        tmpFile.getSegments()
                .filter { !it.isComplete() }
                .forEach { arrays.add(rangeDownload(it)) }

        return Flowable.mergeDelayError(arrays, maxRange)
                .map { Downloading(tmpFile.currentStatus()) }
                .doOnComplete { targetFile.rename() }
    }


    private fun rangeDownload(segment: Segment): Flowable<Any> {
        return Maybe.just(segment)
                .subscribeOn(Schedulers.io())
                .map { "bytes=${it.current}-${it.end}" }
                .doOnSuccess { logd("Range: $it") }
                .flatMap { HttpCore.download(mission, it) }
                .flatMapPublisher { targetFile.save(it, segment, tmpFile) }
    }
}


