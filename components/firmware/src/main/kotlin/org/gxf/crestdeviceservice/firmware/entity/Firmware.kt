// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.firmware.entity

import jakarta.annotation.Generated
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import java.util.UUID
import org.hibernate.annotations.Cascade
import org.hibernate.annotations.CascadeType

@Entity
class Firmware(
    @Id @Generated val id: UUID,
    val name: String,
    val hash: String,
    val previousFirmwareId: UUID? = null,
    @OneToMany @Cascade(CascadeType.ALL) val packets: List<FirmwarePacket> = emptyList(),
)
