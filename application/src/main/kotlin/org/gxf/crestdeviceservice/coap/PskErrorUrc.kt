// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.coap

enum class PskErrorUrc(val code: String, val message: String) {
    DL_UNK("DL:UNK", "Downlink unknown"),
    PSK_EQER("PSK:EQER", "Set PSK does not equal earlier PSK"),
    PSK_DLNA("PSK:DLNA", "Downlink not allowed"),
    PSK_DLER("PSK:DLER", "Downlink (syntax) error"),
    PSK_ERR("PSK:#ERR", "Error processing (downlink) value"),
    PSK_HSER("PSK:HSER", "SHA256 hash error"),
    PSK_CSER("PSK:CSER", "Checksum error");

    companion object {
        fun messageFromCode(code: String): String {
            val error = entries.firstOrNull { it.code == code }
            return error?.message ?: "Unknown URC"
        }

        fun isPskErrorURC(code: String) = entries.any { it.code == code }
    }
}
