package zlc.season.rxdownload3.core

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.internal.operators.maybe.MaybeToPublisher.INSTANCE
import io.reactivex.schedulers.Schedulers.newThread
import zlc.season.rxdownload3.extension.Extension
import zlc.season.rxdownload3.helper.ANY
import zlc.season.rxdownload3.helper.logd
import java.io.File
import java.util.concurrent.Semaphore

class LocalMissionBox : MissionBox {
    private val maxMission = DownloadConfig.maxMission
    private val semaphore = Semaphore(maxMission, true)

    private val SET = mutableSetOf<RealMission>()

    override fun create(mission: Mission): Flowable<Status> {
        val realMission = SET.find { it.actual == mission }
        return if (realMission != null) {
            logd("get existing flowable")
//            realMission.checkTotalSize()
            realMission.getFlowable()
        } else {
            logd("create new mission")
            val new = RealMission(mission, semaphore) //new a RealMission (new )
            SET.add(new)
            new.getFlowable()
        }
    }

    override fun start(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))

        return realMission.start()
    }

    override fun stop(mission: Mission): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))

        return realMission.stop()
    }

    override fun delete(mission: Mission, deleteFile: Boolean): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))
        return realMission.delete(deleteFile)
    }

    override fun createAll(missions: List<Mission>): Maybe<Any> {
        return Maybe.create<Any> {
            missions.forEach { mission ->
                val realMission = SET.find { it.actual == mission }
                if (realMission == null) {
                    val new = RealMission(mission, semaphore)
                    SET.add(new)
                }
            }
            it.onSuccess(ANY)
        }.subscribeOn(newThread())
    }

    override fun startAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.start()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE, true)
                .lastElement()
    }

    override fun stopAll(): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.stop()) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE)
                .lastElement()
    }

    override fun deleteAll(deleteFile: Boolean): Maybe<Any> {
        val arrays = mutableListOf<Maybe<Any>>()
        SET.forEach { arrays.add(it.delete(deleteFile)) }
        return Flowable.fromIterable(arrays)
                .flatMap(INSTANCE)
                .lastElement()
    }

    override fun file(mission: Mission): Maybe<File> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))
        return realMission.file()
    }

    override fun extension(mission: Mission, type: Class<out Extension>): Maybe<Any> {
        val realMission = SET.find { it.actual == mission } ?:
                return Maybe.error(RuntimeException("Mission not create"))

        return realMission.findExtension(type).action()
    }
}
