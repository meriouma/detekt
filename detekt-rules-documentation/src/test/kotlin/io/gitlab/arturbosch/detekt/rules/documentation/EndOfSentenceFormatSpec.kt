package io.gitlab.arturbosch.detekt.rules.documentation

import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EndOfSentenceFormatSpec {
    val subject = EndOfSentenceFormat()

    @Nested
    inner class `KDocStyle rule` {

        @Test
        fun `reports invalid KDoc endings on classes`() {
            val code = """
            /** Some doc */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).hasSize(1)
        }

        @Test
        fun `reports invalid KDoc endings on function with expression body`() {
            val code = """
                /** Some doc */
                fun f(x: Int, y: Int, z: Int) = 
                    if (x == 0) y + z else x + y
            """
            assertThat(subject.compileAndLint(code)).hasSize(1)
        }

        @Test
        fun `reports invalid KDoc endings on properties`() {
            val code = """
            class Test {
                /** Some doc */
                val test = 3
            }
            """
            assertThat(subject.compileAndLint(code)).hasSize(1)
        }

        @Test
        fun `reports invalid KDoc endings on top-level functions`() {
            val code = """
            /** Some doc */
            fun test() = 3
            """
            assertThat(subject.compileAndLint(code)).hasSize(1)
        }

        @Test
        fun `reports invalid KDoc endings on functions`() {
            val code = """
            class Test {
                /** Some doc */
                fun test() = 3
            }
            """
            assertThat(subject.compileAndLint(code)).hasSize(1)
        }

        @Test
        fun `reports invalid KDoc endings`() {
            val code = """
            class Test {
                /** Some doc-- */
                fun test() = 3
            }
            """
            assertThat(subject.compileAndLint(code)).hasSize(1)
        }

        @Test
        fun `reports invalid KDoc endings in block`() {
            val code = """
            /**
             * Something off abc@@
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).hasSize(1)
        }

        @Test
        fun `does not validate first sentence KDoc endings in a multi sentence comment`() {
            val code = """
            /**
             * This sentence is correct.
             *
             * This sentence doesn't matter
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report KDoc which doesn't contain any real sentence`() {
            val code = """
            /**
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report KDoc which doesn't contain any real sentence but many tags`() {
            val code = """
            /**
             * @configuration this - just an example (default: `150`)
             *
             * @active since v1.0.0
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report KDoc which doesn't contain any real sentence but html tags`() {
            val code = """
            /**
             *
             * <noncompliant>
             * fun foo(): Unit { }
             * </noncompliant>
             *
             * <compliant>
             * fun foo() { }
             * </compliant>
             *
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report KDoc ending with periods`() {
            val code = """
            /**
             * Something correct.
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report KDoc ending with questionmarks`() {
            val code = """
            /**
             * Something correct?
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report KDoc ending with exclamation marks`() {
            val code = """
            /**
             * Something correct!
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report KDoc ending with colon`() {
            val code = """
            /**
             * Something correct:
             */
            class Test {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        @Test
        fun `does not report URLs in comments`() {
            val code = """
            /** http://www.google.com */
            class Test1 {
            }

            /** Look here
            http://google.com */
            class Test2 {
            }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }
    }
}
