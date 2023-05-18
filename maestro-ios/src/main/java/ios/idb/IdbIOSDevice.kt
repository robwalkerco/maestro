/*
 *
 *  Copyright (c) 2022 mobile.dev inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package ios.idb

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.expect
import com.github.michaelbull.result.map
import com.github.michaelbull.result.runCatching
import com.google.protobuf.ByteString
import hierarchy.XCUIElement
import idb.CompanionServiceGrpc
import idb.HIDEventKt
import idb.Idb
import idb.Idb.HIDEvent.HIDButtonType
import idb.Idb.RecordResponse
import idb.PushRequestKt.inner
import idb.accessibilityInfoRequest
import idb.clearKeychainRequest
import idb.fileContainer
import idb.hIDEvent
import idb.installRequest
import idb.launchRequest
import idb.location
import idb.mkdirRequest
import idb.openUrlRequest
import idb.payload
import idb.point
import idb.pullRequest
import idb.pushRequest
import idb.recordRequest
import idb.rmRequest
import idb.setLocationRequest
import idb.targetDescriptionRequest
import idb.terminateRequest
import idb.uninstallRequest
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import ios.IOSDevice
import ios.IOSScreenRecording
import ios.device.DeviceInfo
import ios.grpc.BlockingStreamObserver
import okio.Buffer
import okio.Sink
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream

class IdbIOSDevice(
    override val deviceId: String?,
    private val idbRunner: IdbRunner,
) : IOSDevice, AutoCloseable {

    private var channel: ManagedChannel? = null
    private lateinit var blockingStub: CompanionServiceGrpc.CompanionServiceBlockingStub
    private lateinit var asyncStub: CompanionServiceGrpc.CompanionServiceStub

    init {
        restartCompanion()
    }

    private fun restartCompanion() {
        channel?.let { idbRunner.stop(it) }
        channel = idbRunner.start()
        blockingStub = CompanionServiceGrpc.newBlockingStub(channel)
        asyncStub = CompanionServiceGrpc.newStub(channel)
    }

    override fun open() {
        ensureGrpcChannel()
    }

    @SuppressWarnings("Used in cloud")
    private fun ensureGrpcChannel() {
        deviceInfo().expect {}
    }

    override fun deviceInfo(): Result<DeviceInfo, Throwable> {
        return runWithRestartRecovery {
            val response = blockingStub.describe(targetDescriptionRequest {})
            val screenDimensions = response.targetDescription.screenDimensions

            DeviceInfo(
                widthPixels = screenDimensions.width.toInt(),
                heightPixels = screenDimensions.height.toInt(),
                widthPoints = screenDimensions.widthPoints.toInt(),
                heightPoints = screenDimensions.heightPoints.toInt(),
            )
        }
    }

    override fun contentDescriptor(): Result<XCUIElement, Throwable> {
        return runWithRestartRecovery {
            val accessibilityResponse = blockingStub.accessibilityInfo(accessibilityInfoRequest {})
            val accessibilityNode: XCUIElement = mapper.readValue(accessibilityResponse.json)
            accessibilityNode
        }
    }

    override fun tap(x: Int, y: Int): Result<Unit, Throwable> {
        return press(x, y, holdDelay = 50)
    }

    override fun longPress(x: Int, y: Int, durationMs: Long): Result<Unit, Throwable> {
        return press(x, y, holdDelay = durationMs)
    }

    override fun pressKey(name: String) {
        TODO("Not yet implemented")
    }

    override fun pressButton(name: String) {
        TODO("Not yet implemented")
    }

    override fun scroll(xStart: Double, yStart: Double, xEnd: Double, yEnd: Double, duration: Double): Result<Unit, Throwable> {
        TODO("Not yet implemented")
    }

    private fun press(
        x: Int,
        y: Int,
        holdDelay: Long
    ): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            val responseObserver = BlockingStreamObserver<Idb.HIDResponse>()
            val stream = asyncStub.hid(responseObserver)

            val pressAction = HIDEventKt.hIDPressAction {
                touch = HIDEventKt.hIDTouch {
                    point = point {
                        this.x = x.toDouble()
                        this.y = y.toDouble()
                    }
                }
            }

            stream.onNext(
                hIDEvent {
                    press = HIDEventKt.hIDPress {
                        action = pressAction
                        direction = Idb.HIDEvent.HIDDirection.DOWN
                    }
                }
            )
            Thread.sleep(holdDelay)
            stream.onNext(
                hIDEvent {
                    press = HIDEventKt.hIDPress {
                        action = pressAction
                        direction = Idb.HIDEvent.HIDDirection.UP
                    }
                }
            )
            stream.onCompleted()

            responseObserver.awaitResult()
        }
    }

    override fun input(
        text: String,
    ): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            val responseObserver = BlockingStreamObserver<Idb.HIDResponse>()
            val stream = asyncStub.hid(responseObserver)

            TextInputUtil.textToListOfEvents(text)
                .forEach {
                    stream.onNext(it)
                    Thread.sleep(75)
                }
            stream.onCompleted()
        }
    }

    override fun install(stream: InputStream): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            val responseObserver = BlockingStreamObserver<Idb.InstallResponse>()
            val requestStream = asyncStub.install(responseObserver)

            requestStream.onNext(
                installRequest {
                    destination = Idb.InstallRequest.Destination.APP
                    payload = payload {
                        compression = Idb.Payload.Compression.GZIP
                    }
                }
            )

            stream.buffered()
                .iterator()
                .asSequence()
                .chunked(CHUNK_SIZE)
                .forEach {
                    requestStream.onNext(
                        installRequest {
                            payload = payload {
                                data = ByteString.copyFrom(it.toByteArray())
                            }
                        }
                    )
                }
            requestStream.onCompleted()

            responseObserver.awaitResult()
        }
    }

    override fun uninstall(id: String): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            try {
                blockingStub.uninstall(
                    uninstallRequest {
                        bundleId = id
                    }
                )
            } catch (e: StatusRuntimeException) {
                if (e.status.description != "$id is not installed") {
                    throw e
                }
            }
        }
    }

    override fun pullAppState(id: String, file: File): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            val observer = BlockingStreamObserver<Idb.PullResponse>()
            asyncStub.pull(pullRequest {
                container = fileContainer {
                    kind = Idb.FileContainer.Kind.APPLICATION
                    bundleId = id
                }
                srcPath = "/"
                dstPath = file.absolutePath
            }, observer)
            observer.awaitResult()
        }
    }

    override fun pushAppState(id: String, file: File): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            val observer = BlockingStreamObserver<Idb.PushResponse>()
            val stream = asyncStub.push(observer)

            stream.onNext(pushRequest {
                inner = inner {
                    container = fileContainer {
                        kind = Idb.FileContainer.Kind.APPLICATION
                        bundleId = id
                    }
                    dstPath = "/"
                }
            })

            if (file.isDirectory) {
                file.listFiles()?.map { it.absolutePath }?.forEach {
                    stream.onNext(pushRequest {
                        payload = payload {
                            filePath = it
                        }
                    })
                }
            } else {
                stream.onNext(pushRequest {
                    payload = payload {
                        filePath = file.absolutePath
                    }
                })
            }

            stream.onCompleted()
            observer.awaitResult()
        }
    }

    override fun clearAppState(id: String): Result<Unit, Throwable> {

        // Stop the app before clearing the file system
        // This prevents the app from saving its state after it has been cleared
        stop(id)

        // Wait for the app to be stopped, unfortunately idb's stop()
        // does not wait for the process to finish
        Thread.sleep(1500)

        // deletes app data, including container folder
        val result = runWithRestartRecovery {
            blockingStub.rm(rmRequest {
                container = fileContainer {
                    kind = Idb.FileContainer.Kind.APPLICATION
                    bundleId = id
                }
                paths.add("/")
            })
        }

        // forces app container folder to be re-created
        val paths = listOf(
            "Documents",
            "Library",
            "Library/Caches",
            "Library/Preferences",
            "SystemData",
            "tmp"
        )

        runWithRestartRecovery {
            paths.forEach { path ->
                blockingStub.mkdir(mkdirRequest {
                    container = fileContainer {
                        kind = Idb.FileContainer.Kind.APPLICATION
                        bundleId = id
                    }
                    this.path = path
                })
            }
        }

        return result.map {  }
    }

    override fun clearKeychain(): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            blockingStub.clearKeychain(clearKeychainRequest { })
        }
    }

    override fun launch(
        id: String,
        launchArguments: Map<String, Any>,
        maestroSessionId: UUID?,
    ): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            val responseObserver = BlockingStreamObserver<Idb.LaunchResponse>()
            val stream = asyncStub.launch(responseObserver)
            stream.onNext(
                launchRequest {
                    start = idb.LaunchRequestKt.start {
                        bundleId = id
                    }
                }
            )

            stream.onCompleted()
            responseObserver.awaitResult()
        }
    }

    override fun stop(id: String): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            val responseObserver = BlockingStreamObserver<Idb.TerminateResponse>()
            asyncStub.terminate(
                terminateRequest {
                    bundleId = id
                },
                responseObserver
            )

            responseObserver.awaitResult()
        }
    }

    override fun openLink(link: String): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            blockingStub.openUrl(openUrlRequest {
                url = link
            })
        }
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean): Result<Unit, Throwable> {
        error("Not supported")
    }

    // Warning: This method reads all bytes into memory. This can probably be optimized if necessary.
    override fun startScreenRecording(out: Sink): Result<IOSScreenRecording, Throwable> {
        val future = CompletableFuture<Void>()
        val bufferedOut = out.buffer()
        val compressedData = Buffer()
        return runWithRestartRecovery {
            val request = asyncStub.record(object : StreamObserver<RecordResponse> {
                override fun onNext(value: RecordResponse) {
                    if (value.payload.compression == Idb.Payload.Compression.GZIP) {
                        compressedData.write(value.payload.data.toByteArray())
                    } else {
                        bufferedOut.write(value.payload.data.toByteArray())
                    }
                }

                override fun onError(t: Throwable) {
                    try {
                        finish()
                    } finally {
                        future.completeExceptionally(t)
                    }
                }

                override fun onCompleted() {
                    try {
                        finish()
                    } finally {
                        future.complete(null)
                    }
                }

                private fun finish() {
                    if (compressedData.size > 0) {
                        val gzippedInputStream = GZIPInputStream(compressedData.inputStream())
                        gzippedInputStream.source().buffer().use { source ->
                            source.readAll(out)
                        }
                    } else {
                        bufferedOut.flush()
                    }
                }
            })
            request.onNext(recordRequest {
                start = Idb.RecordRequest.Start.newBuilder().build()
            })

            object : IOSScreenRecording {

                override fun close() {
                    request.onNext(recordRequest {
                        stop = Idb.RecordRequest.Stop.newBuilder().build()
                    })
                    future.get()
                }
            }
        }
    }

    override fun setLocation(latitude: Double, longitude: Double): Result<Unit, Throwable> {
        return runWithRestartRecovery {
            blockingStub.setLocation(setLocationRequest {
                location = location { this.latitude = latitude; this.longitude = longitude }
            })
        }
    }

    override fun isShutdown(): Boolean {
        return channel?.isShutdown ?: true
    }

    override fun close() {
        channel?.let { idbRunner.stop(it) }
    }

    override fun isScreenStatic(): Result<Boolean, Throwable> {
        TODO("Not yet implemented")
    }

    override fun setPermissions(id: String, permissions: Map<String, String>): Result<Unit, Throwable> {
        TODO("Not yet implemented")
    }

    override fun eraseText(charactersToErase: Int) {
        TODO("Not yet implemented")
    }

    override fun copyText(text: String): Result<Unit, Throwable> {
        TODO("Not yet implemented")
    }

    override fun doubleTap(x: Int, y: Int): Result<Unit, Throwable> {
        TODO("Not yet implemented")
    }

    private infix fun <T, V> T.runWithRestartRecovery(block: T.() -> V): Result<V, Throwable> {
        return runCatching {
            try {
                block()
            } catch (e: StatusRuntimeException) {
                restartCompanion()
                block()
            }
        }
    }

    companion object {
        // 4Mb, the default max read for gRPC
        private const val CHUNK_SIZE = 1024 * 1024 * 3
        private val mapper = jacksonObjectMapper()
    }

}
