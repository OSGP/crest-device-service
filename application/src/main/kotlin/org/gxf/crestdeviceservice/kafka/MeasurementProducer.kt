// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.crestdeviceservice.kafka

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import org.gxf.crestdeviceservice.kafka.configuration.KafkaProducerProperties
import org.gxf.sng.avro.DeviceMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MeasurementProducer(
        private val kafkaTemplate: KafkaTemplate<String, DeviceMessage>,
        private val kafkaProducerProperties: KafkaProducerProperties
) {
    private val logger = KotlinLogging.logger {}

    fun produceMessage(message: JsonNode) {
        logger.info { "Producing: ${message.get("ID")}" }
        kafkaTemplate.send(kafkaProducerProperties.topicName, DeviceMessage().apply {
            deviceId = message.get("ID").toString()
            timestamp = Instant.now().toEpochMilli()
            payload = message.toString()
        })
    }
}
