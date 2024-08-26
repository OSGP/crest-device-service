// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.service

import com.alliander.sng.CommandStatus as ExternalCommandStatus
import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.gxf.crestdeviceservice.command.entity.Command
import org.gxf.crestdeviceservice.command.entity.Command.CommandStatus
import org.gxf.crestdeviceservice.command.service.CommandFeedbackService
import org.gxf.crestdeviceservice.command.service.CommandService
import org.gxf.crestdeviceservice.command.service.CommandService.Companion.INITIALISATION
import org.gxf.crestdeviceservice.model.ErrorUrc
import org.gxf.crestdeviceservice.model.ErrorUrc.Companion.isErrorUrc
import org.gxf.crestdeviceservice.model.ErrorUrc.Companion.isPskErrorUrc
import org.gxf.crestdeviceservice.psk.exception.NoExistingPskException
import org.gxf.crestdeviceservice.psk.service.PskService
import org.springframework.stereotype.Service

@Service
class UrcService(
    private val pskService: PskService,
    private val commandService: CommandService,
    private val commandFeedbackService: CommandFeedbackService
) {
    companion object {
        private const val URC_FIELD = "URC"
        private const val DL_FIELD = "DL"
        private const val URC_PSK_SUCCESS = "PSK:SET"
    }

    private val logger = KotlinLogging.logger {}

    fun interpretURCInMessage(deviceId: String, body: JsonNode) {
        val urcs = getUrcsFromMessage(body)
        if (urcs.isEmpty()) {
            logger.debug { "Received message without urcs" }
            return
        }
        logger.debug { "Received message with urcs ${urcs.joinToString(", ")}" }

        if (urcsContainPskError(urcs)) {
            handlePskErrors(deviceId, urcs)
            return
        } else if (urcsContainPskSuccess(urcs)) {
            handlePskSuccess(deviceId)
            return
        }

        val commandInProgress = commandService.getFirstCommandInProgressForDevice(deviceId)
        if (commandInProgress != null) {
            val downlink = getDownlinkFromMessage(body)
            handleUrcsForCommand(urcs, deviceId, commandInProgress, downlink)
        }
    }

    private fun getUrcsFromMessage(body: JsonNode) =
        body[URC_FIELD].filter { it.isTextual }.map { it.asText() }

    private fun getDownlinkFromMessage(body: JsonNode) =
        body[URC_FIELD].first { it.isObject }[DL_FIELD].asText()

    private fun urcsContainPskError(urcs: List<String>) = urcs.any { urc -> isPskErrorUrc(urc) }

    private fun handlePskErrors(deviceId: String, urcs: List<String>) {
        if (!pskService.isPendingKeyPresent(deviceId)) {
            throw NoExistingPskException(
                "Failure URC received, but no pending key present to set as invalid")
        }

        urcs
            .filter { urc -> isPskErrorUrc(urc) }
            .forEach { urc ->
                logger.warn {
                    "PSK set failed for device with id ${deviceId}: ${ErrorUrc.messageFromCode(urc)}"
                }
            }

        pskService.setPendingKeyAsInvalid(deviceId)
    }

    private fun urcsContainPskSuccess(urcs: List<String>) =
        urcs.any { urc -> urc.contains(URC_PSK_SUCCESS) }

    private fun handlePskSuccess(deviceId: String) {
        if (!pskService.isPendingKeyPresent(deviceId)) {
            throw NoExistingPskException(
                "Success URC received, but no pending key present to set as active")
        }
        logger.info { "PSK set successfully, changing active key" }
        pskService.changeActiveKey(deviceId)
    }

    private fun handleUrcsForCommand(
        urcs: List<String>,
        deviceId: String,
        commandInProgress: Command,
        downlink: String
    ) {
        // check if urc is about this command
        if (downlink.contains(commandInProgress.type.downlink)) {
            if (urcsContainErrors(urcs)) {
                handleCommandError(deviceId, commandInProgress, urcs)
            } else if (urcsContainSuccesses(urcs, commandInProgress)) {
                handleCommandUrcs(deviceId, commandInProgress, urcs)
            }
        }
    }

    private fun urcsContainErrors(urcs: List<String>) = urcs.any { urc -> isErrorUrc(urc) }

    private fun urcsContainSuccesses(urcs: List<String>, command: Command) =
        command.type.urcsSuccess.any { successUrc -> urcs.contains(successUrc) }

    private fun handleCommandError(deviceId: String, command: Command, urcs: List<String>) {
        val errorUrcs = urcs.filter { urc -> isErrorUrc(urc) }
        val message =
            "Command failed for device with id $deviceId with code(s): ${errorUrcs.joinToString { ", " }}"

        logger.error { message }

        val commandWithErrorStatus = commandService.saveCommandWithNewStatus(command, CommandStatus.ERROR)

        commandFeedbackService.sendFeedback(commandWithErrorStatus, ExternalCommandStatus.Error, message)
    }

    private fun handleCommandUrcs(deviceId: String, command: Command, urcs: List<String>) {
        when (command.type) {
            Command.CommandType.REBOOT -> handleRebootUrcs(deviceId, command, urcs)
        }
    }

    private fun handleRebootUrcs(deviceId: String, command: Command, urcs: List<String>) {
        if (urcs.contains(INITIALISATION)) {
            val message = "Reboot for device $deviceId went successfully"
            logger.info { message }
            val successfulCommand = commandService.saveCommandWithNewStatus(command, CommandStatus.SUCCESSFUL)

            commandFeedbackService.sendFeedback(successfulCommand, ExternalCommandStatus.Successful, message)
        } else {
            logger.warn {
                "Reboot command sent for device $deviceId, did not receive expected urc: $INITIALISATION. Urcs received: ${urcs.joinToString { ", " }}"
            }
        }
    }
}
