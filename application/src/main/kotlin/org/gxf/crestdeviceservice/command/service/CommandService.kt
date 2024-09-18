// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.command.service

import org.gxf.crestdeviceservice.command.entity.Command
import org.gxf.crestdeviceservice.command.exception.CommandValidationException
import org.gxf.crestdeviceservice.command.repository.CommandRepository

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

import java.time.Instant
import java.util.UUID

import kotlin.jvm.Throws

/**
 * @param commandRepository
 * @param commandFeedbackService
 */
@Service
class CommandService(
    private val commandRepository: CommandRepository,
    private val commandFeedbackService: CommandFeedbackService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Validate the Command.
     *
     * @param command
     * @throws CommandValidationException if validation fails
     */
    @Throws(CommandValidationException::class)
    fun validate(command: Command) {
        if (hasDeviceNewerSameCommand(command.deviceId, command.type, command.timestampIssued)) {
            throw CommandValidationException("There is a newer command of the same type")
        }

        if (hasDeviceSameCommandAlreadyInProgress(command.deviceId, command.type)) {
            throw CommandValidationException("A command of the same type is already in progress.")
        }
    }

    /**
     * Check if the device already has a newer command pending of the same type that was issued at a
     * later date. This check prevents issues if commands arrive out of order or if we reset the
     * kafka consumer group.
     */
    private fun hasDeviceNewerSameCommand(
        deviceId: String,
        commandType: Command.CommandType,
        timestampNewCommand: Instant
    ): Boolean {
        val latestCommandInDatabase =
            getLatestCommandInDatabase(deviceId, commandType) ?: return false

        // If the device already has a newer command in the database
        return latestCommandInDatabase.timestampIssued.isAfter(timestampNewCommand)
    }

    private fun hasDeviceSameCommandAlreadyInProgress(
        deviceId: String,
        commandType: Command.CommandType
    ) =
        commandRepository
            .findAllByDeviceIdAndTypeAndStatusOrderByTimestampIssuedAsc(
                deviceId, commandType, Command.CommandStatus.IN_PROGRESS)
            .isNotEmpty()

    private fun getLatestCommandInDatabase(deviceId: String, commandType: Command.CommandType) =
        commandRepository.findFirstByDeviceIdAndTypeOrderByTimestampIssuedDesc(
            deviceId, commandType)

    fun cancelOlderCommandIfNecessary(pendingCommand: Command) {
        getSameCommandForDeviceAlreadyPending(pendingCommand)?.let { it ->
            logger.warn {
                "Device ${it.deviceId} already has a pending command of type ${it.type}. The first command will be" +
                        " cancelled."
            }
            cancelCommand(it, pendingCommand.correlationId)
        }
    }

    private fun getSameCommandForDeviceAlreadyPending(command: Command): Command? {
        val latestCommandInDatabase =
            getLatestCommandInDatabase(command.deviceId, command.type) ?: return null

        if (latestCommandInDatabase.status == Command.CommandStatus.PENDING) {
            return latestCommandInDatabase
        }

        return null
    }

    private fun cancelCommand(commandToBeCancelled: Command, newCorrelationId: UUID) {
        val message =
            "Command cancelled by newer same command with correlation id: $newCorrelationId"
        commandFeedbackService.sendCancellationFeedback(commandToBeCancelled, message)
        saveCommandWithNewStatus(commandToBeCancelled, Command.CommandStatus.CANCELLED)
    }

    fun isPskCommand(command: Command) =
        command.type == Command.CommandType.PSK || command.type == Command.CommandType.PSK_SET

    fun getFirstCommandInProgressForDevice(deviceId: String) =
        commandRepository.findFirstByDeviceIdAndStatusOrderByTimestampIssuedAsc(
            deviceId, Command.CommandStatus.IN_PROGRESS)

    fun getAllPendingCommandsForDevice(deviceId: String) =
        commandRepository.findAllByDeviceIdAndStatusOrderByTimestampIssuedAsc(
            deviceId, Command.CommandStatus.PENDING)

    fun getAllCommandsInProgressForDevice(deviceId: String) =
        commandRepository.findAllByDeviceIdAndStatusOrderByTimestampIssuedAsc(
            deviceId, Command.CommandStatus.IN_PROGRESS)

    fun save(command: Command) {
        commandRepository.save(command)
    }

    fun saveCommandEntities(commands: List<Command>): MutableIterable<Command> =
        commandRepository.saveAll(commands)

    fun saveCommandWithNewStatus(command: Command, status: Command.CommandStatus): Command {
        command.status = status
        return commandRepository.save(command)
    }
}
