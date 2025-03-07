package io.github.detekt.metrics.processors

import io.github.detekt.test.utils.compileContentForTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ComplexityVisitorSpec {
    @Nested
    inner class `something` {

        @Test
        fun `complexityOfDefaultCaseIsOne`() {
            val mcc = calcComplexity(default)

            assertThat(mcc).isEqualTo(0)
        }

        @Test
        fun `complexityOfComplexAndNestedClass`() {
            val mcc = calcComplexity(complexClass)

            assertThat(mcc).isEqualTo(44)
        }
    }
}

private fun calcComplexity(content: String) =
    with(compileContentForTest(content)) {
        accept(ComplexityVisitor())
        getUserData(complexityKey)
    }
