package de.easynfp.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.easynfp.app.text.ExplanationFormatter
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplanationFormatterTest {
    private val formatter = ExplanationFormatter(
        ApplicationProvider.getApplicationContext<android.content.Context>().resources,
    )

    @Test
    fun `F-41 summary sentence of the typical cycle`() {
        val completed = TestData.snapshot().evaluations[1]
        assertEquals(
            "Temperatur ausgewertet am Tag 17 nach der regulären Regel; Schleimhöhepunkt Tag 15; " +
                "unfruchtbar ab Abend Tag 18",
            formatter.summary(completed),
        )
    }

    @Test
    fun `every explanation step has a rule id and a text`() {
        TestData.snapshot(currentDay = 16).evaluations.forEach { evaluation ->
            val lines = formatter.steps(evaluation)
            assertTrue(lines.isNotEmpty())
            lines.forEach { line ->
                assertTrue(line.ruleCode.matches(Regex("""[RF]-\d{2}""")), line.ruleCode)
                assertTrue(line.text.isNotBlank())
                assertTrue(!line.text.contains("%"), line.text)
            }
        }
    }
}
