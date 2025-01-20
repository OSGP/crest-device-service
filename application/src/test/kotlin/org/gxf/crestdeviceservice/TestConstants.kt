// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice

import java.time.Instant
import java.util.UUID

object TestConstants {
    const val DEVICE_ID = "device-id"
    val CORRELATION_ID = UUID.randomUUID()
    val timestamp = Instant.now()

    const val DEVICE_MESSAGE_TOPIC = "device-message"
    const val COMMAND_FEEDBACK_TOPIC = "command-feedback"
    const val FIRMWARE_TOPIC = "firmware-topic"
    const val FIRMWARE_KEY = "firmware-key"

    const val MESSAGE_RECEIVED = "Command received"

    const val FIRMWARE_VERSION = "99.99"
    const val FIRMWARE_FROM_VERSION = "23.10"
    const val FIRMWARE_NAME = "RTU#DELTA#FROM#$FIRMWARE_FROM_VERSION#TO#$FIRMWARE_VERSION"
    const val NUMBER_OF_PACKETS = 13

    const val ANALOG_ALARM_THRESHOLDS_MBAR_PORT_3 = "3:0,1250,2500,3750,25"
    const val ANALOG_ALARM_THRESHOLDS_PAYLOAD_PORT_3 = "AL6:0,500,1000,1500,10"
    const val ANALOG_ALARM_THRESHOLDS_MBAR_PORT_4 = "4:0,1250,2500,3750,25"
    const val ANALOG_ALARM_THRESHOLDS_PAYLOAD_PORT_4 = "AL7:0,500,1000,1500,10"
}
