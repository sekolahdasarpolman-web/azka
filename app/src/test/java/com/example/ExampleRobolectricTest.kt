package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EnglishStyle
import com.example.data.remote.LocalLinguisticEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TuturInggris", appName)
    }

    @Test
    fun `local linguistic engine corrects broken English and generates cara membaca`() {
        val broken = "Me and my friend was go to the library yesterday for study English grammar."
        val result = LocalLinguisticEngine.processParagraph(broken, EnglishStyle.STANDARD)

        assertTrue(result.standardEnglishText.contains("My friend and I"))
        assertTrue(result.standardEnglishText.contains("went"))
        assertTrue(result.standardEnglishText.contains("to study"))
        assertTrue(result.caraMembacaFull.isNotBlank())
        assertTrue(result.ipaFull.isNotBlank())
        assertTrue(result.grammarNotes.isNotEmpty())
    }
}
