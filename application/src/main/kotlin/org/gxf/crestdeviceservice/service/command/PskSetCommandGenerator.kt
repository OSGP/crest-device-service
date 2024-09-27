// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.service.command

import org.gxf.crestdeviceservice.command.entity.Command
import org.gxf.crestdeviceservice.psk.exception.NoExistingPskException
import org.gxf.crestdeviceservice.psk.service.PskService
import org.springframework.stereotype.Service

@Service
class PskSetCommandGenerator(private val pskService: PskService) : CommandGenerator {
    override fun generateCommandString(command: Command): String {
        val readyKey =
            pskService.getCurrentReadyPsk(command.deviceId)
                ?: throw NoExistingPskException("There is no new key ready to be set")
        return "PSK:${readyKey.preSharedKey}:${readyKey.commandHash()}:SET"
    }

    override fun getSupportedCommand() = Command.CommandType.PSK_SET
}
