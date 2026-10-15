package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.RadioProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and permission strings match specification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HHP Radio", appName)

        val permTitle = context.getString(R.string.permission_mic_title)
        assertEquals("อนุญาตให้เข้าถึงไมโครโฟน", permTitle)

        val permDesc = context.getString(R.string.permission_mic_desc)
        assertTrue(permDesc.contains("HHP Radio จำเป็นต้องใช้ไมโครโฟนเพื่อส่งเสียงพูดไปยังสมาชิกในช่องวิทยุที่เชื่อมต่ออยู่"))

        val allowBtn = context.getString(R.string.permission_allow_button)
        assertEquals("อนุญาตไมโครโฟน", allowBtn)

        val denyBtn = context.getString(R.string.permission_deny_button)
        assertEquals("ยังไม่อนุญาต", denyBtn)

        val noServer = context.getString(R.string.status_no_server)
        assertEquals("ยังไม่ได้ตั้งค่าเซิร์ฟเวอร์", noServer)
    }

    @Test
    fun `verify radio packet serialization and header decoding`() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        val payload = "TEST_AUDIO_PAYLOAD".toByteArray(Charsets.UTF_8)

        RadioProtocol.writePacket(dos, RadioProtocol.TYPE_PTT_GRANT, payload)

        val bytes = baos.toByteArray()
        val bais = ByteArrayInputStream(bytes)
        val dis = DataInputStream(bais)

        val header = RadioProtocol.readHeader(dis)
        assertTrue(header != null)
        assertEquals(RadioProtocol.MAGIC, header!!.magic)
        assertEquals(RadioProtocol.TYPE_PTT_GRANT, header.type)
        assertEquals(payload.size, header.length)
    }
}
