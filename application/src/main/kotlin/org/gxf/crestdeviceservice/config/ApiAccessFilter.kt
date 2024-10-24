// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class ApiAccessFilter(private val serverProperties: ServerProperties) : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val requestUri = (request as HttpServletRequest).requestURI
        val isProxyService = requestUri.startsWith("/sng") || requestUri.startsWith("/psk")

        if (isProxyService && !correctPortForProxyService(request)) {
            (response as HttpServletResponse).sendError(404)
        } else {
            chain.doFilter(request, response)
        }
    }

    private fun correctPortForProxyService(request: ServletRequest) = request.serverPort == serverProperties.port
}
