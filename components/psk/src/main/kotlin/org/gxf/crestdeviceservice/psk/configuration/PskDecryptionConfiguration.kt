// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.psk.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

import java.security.interfaces.RSAPrivateKey

/**
 * @property privateKey
 * @property method
 */
@ConfigurationProperties("psk.decryption")
class PskDecryptionConfiguration(val privateKey: Map<String, RSAPrivateKey>, val method: String)
