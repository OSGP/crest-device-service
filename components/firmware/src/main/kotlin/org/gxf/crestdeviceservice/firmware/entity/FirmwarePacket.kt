// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.firmware.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne

@Entity
@IdClass(FirmwarePacketCompositeKey::class)
class FirmwarePacket(@ManyToOne @Id val firmware: Firmware, @Id val packetNumber: Int, val packet: String)
