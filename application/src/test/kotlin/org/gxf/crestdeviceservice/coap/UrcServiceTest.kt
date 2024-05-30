// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.coap

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BaseJsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.gxf.crestdeviceservice.psk.PskService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.util.ResourceUtils
import java.util.stream.Stream

class UrcServiceTest {
    private val pskService = mock<PskService>()
    private val urcService = UrcService(pskService)
    private val mapper = spy<ObjectMapper>()

    companion object {
        private const val URC_FIELD = "URC"
        private const val DL_FIELD = "DL"
        private const val PSK_COMMAND =
            "!PSK:umU6KJ4g7Ye5ZU6o:4a3cfdd487298e2f048ebfd703a1da4800c18f2167b62192cf7dc9fd6cc4bcd3;PSK:umU6KJ4g7Ye5ZU6o:4a3cfdd487298e2f048ebfd703a1da4800c18f2167b62192cf7dc9fd6cc4bcd3:SET"
        private const val IDENTITY = "867787050253370"

        @JvmStatic
        private fun containingPskOrDownlinkErrorUrcs() =
            Stream.of(
                listOf("PSK:EQER"),
                listOf("DL:UNK"),
                listOf("[DL]:DLNA"),
                listOf("[DL]:DLER"),
                listOf("[DL]:#ERR"),
                listOf("[DL]:HSER"),
                listOf("[DL]:CSER"),
                listOf("TS:ERR", "[DL]:#ERR", "[DL]:DLER"),
                listOf("[DL]:#ERR", "[DL]:DLER")
            )

        @JvmStatic
        private fun notContainingPskOrDownlinkErrorUrcs() =
            Stream.of(
                listOf("INIT"),
                listOf("ENPD"),
                listOf("TEL:RBT"),
                listOf("JTR"),
                listOf("WDR"),
                listOf("BOR"),
                listOf("EXR"),
                listOf("POR"),
                listOf("TS:ERR"),
                listOf("INIT", "BOR", "POR")
            )
    }

    @Test
    fun shouldChangeActiveKeyWhenSuccessURCReceived() {
        val urcs = listOf("PSK:SET")
        interpretURCWhileNewKeyIsPending(urcs)
        verify(pskService).changeActiveKey(IDENTITY)
    }

    @ParameterizedTest(name = "should set pending key as invalid for {0}")
    @MethodSource("containingPskOrDownlinkErrorUrcs")
    fun shouldSetPendingKeyAsInvalidWhenFailureURCReceived(urcs: List<String>) {
        interpretURCWhileNewKeyIsPending(urcs)
        verify(pskService).setPendingKeyAsInvalid(IDENTITY)
    }

    @ParameterizedTest(name = "should not set pending key as invalid for {0}")
    @MethodSource("notContainingPskOrDownlinkErrorUrcs")
    fun shouldNotSetPendingKeyAsInvalidWhenOtherURCReceived(urcs: List<String>) {
        interpretURCWhileNewKeyIsPending(urcs)
        verify(pskService, times(0)).setPendingKeyAsInvalid(IDENTITY)
    }

    private fun interpretURCWhileNewKeyIsPending(urcs: List<String>) {
        whenever(pskService.needsKeyChange(IDENTITY)).thenReturn(false)
        whenever(pskService.isPendingKeyPresent(IDENTITY)).thenReturn(true)

        val message = updatePskCommandInMessage(urcs)

        urcService.interpretURCInMessage(IDENTITY, message)
    }

    private fun updatePskCommandInMessage(urcs: List<String>): JsonNode {
        val message = messageTemplate()
        val urcFieldValue = urcFieldValue(urcs)

        message.replace(URC_FIELD, urcFieldValue)
        return message
    }

    private fun messageTemplate(): ObjectNode {
        val fileToUse = ResourceUtils.getFile("classpath:message-template.json")
        return mapper.readTree(fileToUse) as ObjectNode
    }

    private fun urcFieldValue(urcs: List<String>): ArrayNode? {
        val urcNodes = urcs.map { urc -> TextNode(urc) }
        val downlinkNode =
            ObjectNode(JsonNodeFactory.instance, mapOf(DL_FIELD to TextNode(PSK_COMMAND)))
        val urcsPlusReceivedDownlink: MutableList<BaseJsonNode> = mutableListOf()
        urcsPlusReceivedDownlink.addAll(urcNodes)
        urcsPlusReceivedDownlink.add(downlinkNode)
        val urcFieldValue = mapper.valueToTree<ArrayNode>(urcsPlusReceivedDownlink)
        return urcFieldValue
    }
}
