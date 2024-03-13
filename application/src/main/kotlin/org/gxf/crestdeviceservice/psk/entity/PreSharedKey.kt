// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.crestdeviceservice.psk.entity

import jakarta.persistence.*
import org.gxf.crestdeviceservice.psk.entity.convertors.DatabaseFieldEncryptor
import java.time.Instant

@Entity
@IdClass(PreSharedKeyCompositeKey::class)
class PreSharedKey(
    @Id val identity: String,
    @Id val revision: Int,
    val revisionTime: Instant,
    @Convert(converter = DatabaseFieldEncryptor::class) val preSharedKey: String,
    @Convert(converter = DatabaseFieldEncryptor::class) val secret: String,
    @Enumerated(EnumType.STRING) var status: PreSharedKeyStatus
)

enum class PreSharedKeyStatus {
    ACTIVE,
    INACTIVE,
    PENDING,
    INVALID
}
