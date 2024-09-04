package org.gxf.crestdeviceservice

import org.gxf.crestdeviceservice.TestHelper.CORRELATION_ID
import org.gxf.crestdeviceservice.TestHelper.DEVICE_ID
import org.gxf.crestdeviceservice.TestHelper.timestamp
import org.gxf.crestdeviceservice.command.entity.Command
import java.util.UUID

object CommandFactory {
    fun pendingPskCommand() =
        Command(
            id = UUID.randomUUID(),
            deviceId = DEVICE_ID,
            correlationId = CORRELATION_ID,
            timestampIssued = timestamp,
            type = Command.CommandType.PSK,
            commandValue = null,
            status = Command.CommandStatus.PENDING)

    fun pendingPskSetCommand() =
        Command(
            id = UUID.randomUUID(),
            deviceId = DEVICE_ID,
            correlationId = CORRELATION_ID,
            timestampIssued = timestamp,
            type = Command.CommandType.PSK_SET,
            commandValue = null,
            status = Command.CommandStatus.PENDING)

    fun pendingRebootCommand() =
        Command(
            id = UUID.randomUUID(),
            deviceId = DEVICE_ID,
            correlationId = CORRELATION_ID,
            timestampIssued = timestamp,
            type = Command.CommandType.REBOOT,
            commandValue = null,
            status = Command.CommandStatus.PENDING)

    fun pendingPskCommands() = listOf(pendingPskCommand(), pendingPskSetCommand())

    fun rebootCommandInProgress() =
        pendingRebootCommand().copy(status = Command.CommandStatus.IN_PROGRESS)

    fun pskCommandInProgress() =
        pendingPskCommand().copy(status = Command.CommandStatus.IN_PROGRESS)

    fun pskSetCommandInProgress() =
        pendingPskSetCommand().copy(status = Command.CommandStatus.IN_PROGRESS)

    fun pskCommandsInProgress() = listOf(pskCommandInProgress(), pskSetCommandInProgress())
}