// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.consumer

import com.alliander.sng.DeviceCredentials
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gxf.crestdeviceservice.command.entity.Command
import org.gxf.crestdeviceservice.command.entity.Command.CommandStatus
import org.gxf.crestdeviceservice.command.service.CommandService
import org.gxf.crestdeviceservice.psk.service.PskDecryptionService
import org.gxf.crestdeviceservice.psk.service.PskService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class IncomingDeviceCredentialsConsumer(
    private val pskService: PskService,
    private val pskDecryptionService: PskDecryptionService,
    private val commandService: CommandService
) {

    private val logger = KotlinLogging.logger {}

    @KafkaListener(
        id = "pre-shared-key",
        idIsGroup = false,
        topics = ["\${kafka.consumers.pre-shared-key.topic}"])
    fun handleIncomingDeviceCredentials(deviceCredentials: DeviceCredentials) {
        logger.info { "Received key for ${deviceCredentials.imei}" }

        val deviceId = deviceCredentials.imei

        try {
            setInitialKey(deviceCredentials, deviceId)

            pskService.generateNewReadyKeyForDevice(deviceId)
            if(pskService.changeInitialPsk()) {
                preparePskSetCommand(deviceId)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set device credentials for $deviceId" }
        }
    }

    private fun setInitialKey(
        deviceCredentials: DeviceCredentials,
        deviceId: String
    ) {
        val decryptedPsk =
            pskDecryptionService.decryptSecret(deviceCredentials.psk, deviceCredentials.keyRef)
        val decryptedSecret =
            pskDecryptionService.decryptSecret(
                deviceCredentials.secret, deviceCredentials.keyRef
            )

        pskService.setInitialKeyForIdentity(deviceId, decryptedPsk, decryptedSecret)
    }

    private fun preparePskSetCommand(deviceId: String) {
        val pskSetCommand = Command(
            id = UUID.randomUUID(),
            deviceId = deviceId,
            correlationId = UUID.randomUUID(),
            timestampIssued = Instant.now(),
            type = Command.CommandType.PSK,
            status = CommandStatus.PENDING,
            commandValue = null,
        )
        commandService.saveCommandEntity(pskSetCommand)
    }
}
