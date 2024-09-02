// SPDX-FileCopyrightText: Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.crestdeviceservice.psk.service

import org.assertj.core.api.Assertions.assertThat
import org.gxf.crestdeviceservice.psk.PSKTestHelper
import org.gxf.crestdeviceservice.psk.configuration.PskConfiguration
import org.gxf.crestdeviceservice.psk.entity.PreSharedKey
import org.gxf.crestdeviceservice.psk.entity.PreSharedKeyStatus
import org.gxf.crestdeviceservice.psk.repository.PskRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class PskServiceTest {

    @Mock private lateinit var pskRepository: PskRepository

    @Mock private lateinit var pskConfiguration: PskConfiguration

    @InjectMocks private lateinit var pskService: PskService

    @Captor private lateinit var pskCaptor: ArgumentCaptor<PreSharedKey>

    @Captor private lateinit var pskListCaptor: ArgumentCaptor<List<PreSharedKey>>

    @Test
    fun getCurrentActiveKey() {
        val deviceId = "identity"
        val psk = PSKTestHelper.preSharedKeyActive()
        whenever(
                pskRepository.findFirstByIdentityAndStatusOrderByRevisionDesc(
                    deviceId, PreSharedKeyStatus.ACTIVE))
            .thenReturn(psk)

        val currentActiveKey = pskService.getCurrentActiveKey(deviceId)

        assertThat(currentActiveKey).isEqualTo(psk.preSharedKey)
    }

    @Test
    fun pendingKeyPresentTrue() {
        val deviceId = "identity"
        val psk = PSKTestHelper.preSharedKeyPending()
        whenever(
                pskRepository.findFirstByIdentityAndStatusOrderByRevisionDesc(
                    deviceId, PreSharedKeyStatus.PENDING))
            .thenReturn(psk)

        val pendingKeyPresent = pskService.isPendingPskPresent(deviceId)

        assertThat(pendingKeyPresent).isEqualTo(true)
    }

    @Test
    fun pendingKeyPresentFalse() {
        val deviceId = "identity"
        whenever(
                pskRepository.findFirstByIdentityAndStatusOrderByRevisionDesc(
                    deviceId, PreSharedKeyStatus.PENDING))
            .thenReturn(null)

        val pendingKeyPresent = pskService.isPendingPskPresent(deviceId)

        assertThat(pendingKeyPresent).isEqualTo(false)
    }

    @Test
    fun setPendingKeyAsInvalid() {
        val psk = PSKTestHelper.preSharedKeyPending()
        val deviceId = psk.identity
        whenever(
                pskRepository.findFirstByIdentityAndStatusOrderByRevisionDesc(
                    deviceId, PreSharedKeyStatus.PENDING))
            .thenReturn(psk)

        pskService.setPendingKeyAsInvalid(deviceId)

        verify(pskRepository).save(pskCaptor.capture())
        assertThat(pskCaptor.value.status).isEqualTo(PreSharedKeyStatus.INVALID)
    }

    @Test
    fun saveReadyKeyForIdentityAsPending() {
        val psk = PSKTestHelper.preSharedKeyReady()
        val deviceId = psk.identity
        val status = psk.status

        whenever(pskRepository.findFirstByIdentityAndStatusOrderByRevisionDesc(deviceId, status))
            .thenReturn(psk)
        whenever(pskRepository.save(psk)).thenReturn(psk)

        val result = pskService.setPskToPendingForDevice(deviceId)

        verify(pskRepository).save(pskCaptor.capture())
        assertThat(pskCaptor.value.status).isEqualTo(PreSharedKeyStatus.PENDING)
        assertThat(result.status).isEqualTo(PreSharedKeyStatus.PENDING)
    }

    @Test
    fun changeActiveKey() {
        val deviceId = "identity"
        val currentPsk = PSKTestHelper.preSharedKeyActive()
        val newPsk = PSKTestHelper.preSharedKeyPending()

        whenever(
                pskRepository.findFirstByIdentityAndStatusOrderByRevisionDesc(
                    deviceId, PreSharedKeyStatus.ACTIVE))
            .thenReturn(currentPsk)
        whenever(
                pskRepository.findFirstByIdentityAndStatusOrderByRevisionDesc(
                    deviceId, PreSharedKeyStatus.PENDING))
            .thenReturn(newPsk)

        pskService.changeActiveKey(deviceId)

        verify(pskRepository).saveAll(pskListCaptor.capture())
        val actualSavedCurrentPsk = pskListCaptor.value[0]
        val actualSavedNewPsk = pskListCaptor.value[1]
        assertThat(actualSavedCurrentPsk.status).isEqualTo(PreSharedKeyStatus.INACTIVE)
        assertThat(actualSavedNewPsk.status).isEqualTo(PreSharedKeyStatus.ACTIVE)
    }
}
