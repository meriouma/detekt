package io.gitlab.arturbosch.detekt.rules.style

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.setupKotlinEnvironment
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class CanBeNonNullableSpec : Spek({
    setupKotlinEnvironment()

    val env: KotlinCoreEnvironment by memoized()
    val subject by memoized { CanBeNonNullable(Config.empty) }

    describe("CanBeNonNullable Rule") {
        it("does not report when there is no context") {
            val code = """
                class A {
                    private var a: Int? = 5
                    fun foo() {
                        a = 6
                    }
                }
            """
            assertThat(subject.compileAndLint(code)).isEmpty()
        }

        context("evaluating private vars") {
            it("reports when class-level vars are never assigned nullable values") {
                val code = """
                class A(bVal: Int) {
                    private var a: Int? = 5
                    private var b: Int?
                    
                    init {
                        b = bVal
                    }
                    
                    fun foo(): Int {
                        val b = a!!
                        a = b + 1
                        val a = null
                        return b
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(2)
            }

            it("reports when vars utilize non-nullable delegate values") {
                val code = """
                    import kotlin.reflect.KProperty
                    
                    class A {
                        private var a: Int? by PropDelegate()
                    
                        fun foo(): Int {
                            val b = a!!
                            a = b + 1
                            val a = null
                            return b
                        }
                    }
                    
                    class PropDelegate(private var propVal: Int = 0) {
                        operator fun getValue(thisRef: A, property: KProperty<*>): Int {
                            return propVal
                        }
                    
                        operator fun setValue(thisRef: A, property: KProperty<*>, value: Any?) {
                            if (value is Int) {
                                propVal = value
                            }
                        }
                    }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(2)
            }

            it("reports when file-level vars are never assigned nullable values") {
                val code = """
                private var fileA: Int? = 5
                private var fileB: Int? = 5

                fun fileFoo() {
                    fileB = 6
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(2)
            }

            it("does not report when class-level vars are assigned nullable values") {
                val code = """
                import kotlin.random.Random

                class A(fVal: Int?) {
                    private var a: Int? = 0
                    private var b: Int? = 0
                    private var c: Int? = 0
                    private var d: Int? = 0
                    private var e: Int? = null
                    private var f: Int?
                    
                    init {
                        f = fVal
                    }

                    fun foo(fizz: Int): Int {
                        a = null
                        b = if (fizz % 2 == 0) fizz else null
                        c = buzz(fizz)
                        d = a
                        return fizz
                    }

                    private fun buzz(bizz: Int): Int? {
                        return if (bizz % 2 == 0) null else bizz
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report vars that utilize nullable delegate values") {
                val code = """
                    class A(private var aDelegate: Int?) {
                        private var a: Int? by this::aDelegate
                    }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when file-level vars are assigned nullable values") {
                val code = """
                import kotlin.random.Random

                private var fileA: Int? = 5
                private var fileB: Int? = null
                private var fileC: Int? = 5

                fun fileFoo() {
                    fileC = null
                }

                class A {
                    fun foo() {
                        fileA = null
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("reports when vars with private setters are never assigned nullable values") {
                val code = """
                class A {
                    var a: Int? = 5
                        private set
                    fun foo() {
                        a = 6
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
            }

            it("does not report when vars with private setters are assigned nullable values") {
                val code = """
                class A {
                    var a: Int? = 5
                        private set
                    fun foo() {
                        a = null
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when vars use public setters") {
                val code = """
                class A {
                    var a: Int? = 5
                    fun foo() {
                        a = 6
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when vars use non-private setters") {
                val code = """
                    class A {
                        var a: Int? = 5
                            internal set
                        fun foo() {
                            a = 6
                        }
                    }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when private vars are declared in the constructor") {
                val code = """
                class A(private var a: Int?) {
                    fun foo() {
                        a = 6
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }
        }

        context("evaluating private vars") {
            it("reports when class-level vals are set to non-nullable values") {
                val code = """
                class A(cVal: Int) {
                    val a: Int? = 5
                    val b: Int?
                    val c: Int?
                    
                    init {
                        b = 5
                        c = cVal
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(3)
            }

            it("reports when vals utilize non-nullable delegate values") {
                val code = """
                class A {
                    val a: Int? by lazy {
                        5
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
            }

            it("reports when file-level vals are set to non-nullable values") {
                val code = """
                val fileA: Int? = 5
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
            }

            it("does not report when class-level vals are assigned a nullable value") {
                val code = """
                import kotlin.random.Random

                class A(cVal: Int?) {
                    val a: Int? = null
                    val b: Int?
                    val c: Int?
                    
                    init {
                        b = null
                        c = cVal
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when vals utilize nullable delegate values") {
                val code = """
                import kotlin.random.Random

                class A {
                    val d: Int? by lazy {
                        val randVal = Random.nextInt()
                        if (randVal % 2 == 0) randVal else null
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when file-level vals are assigned a nullable value") {
                val code = """
                val fileA: Int? = null
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when vals are declared non-nullable") {
                val code = """
                class A {
                    val a: Int = 5
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("does not report when vals are declared in the constructor") {
                val code = """
                class A(private val a: Int?)
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }

            it("reports when vals with getters never return nullable values") {
                val code = """
                class A {
                    val a: Int?
                        get() = 5
                    val b: Int?
                        get() {
                            return 5
                        }
                    val c: Int?
                        get() = foo()
                    
                    private fun foo(): Int {
                        return 5
                    }
                }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).hasSize(3)
            }

            it("does not report when vals with getters return potentially-nullable values") {
                val code = """
                    import kotlin.random.Random
                    
                    class A {
                        val a: Int?
                            get() = Random.nextInt()?.let { if (it % 2 == 0) it else null }
                        val b: Int?
                            get() {
                                return Random.nextInt()?.let { if (it % 2 == 0) it else null }
                            }
                        val c: Int?
                            get() = foo()
                        
                        private fun foo(): Int? {
                            val randInt = Random.nextInt()
                            return if (randInt % 2 == 0) randInt else null
                        }
                    }
                """
                assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
            }
        }

        it("does not report open properties") {
            val code = """
                abstract class A {
                    open val a: Int? = 5
                    open var b: Int? = 5
                }
            """
            assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
        }

        it("does not report properties whose initial assignment derives from unsafe non-Java code") {
            val code = """
                class A(msg: String?) {
                    private val e = Exception(msg)
                    // e.localizedMessage is marked as String! by Kotlin, meaning Kotlin
                    // cannot guarantee that it will be non-null, even though it is treated
                    // as non-null in Kotlin code.
                    private var a: String? = e.localizedMessage
                }
            """
            assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
        }

        it("does not report interface properties") {
            val code = """
                interface A {
                    val a: Int?
                    var b: Int?
                }
            """
            assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
        }

        context("nullable function parameters") {
            context("using a de-nullifier") {
                it("does report when a param is de-nullified with a postfix expression") {
                    val code = """
                        fun foo(a: Int?) {
                            val b = a!! + 2
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                }

                it("does report when a param is de-nullified with a dot-qualified expression") {
                    val code = """
                        fun foo(a: Int?) {
                            val b = a!!.plus(2)
                        }

                        fun fizz(b: Int?) = b!!.plus(2)
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).hasSize(2)
                }

                it("does report when a de-nullifier precondition is called on the param") {
                    val code = """
                        fun foo(a: Int?, b: Int?) {
                            val aNonNull = requireNotNull(a)
                            val c = aNonNull + checkNotNull(b)
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).hasSize(2)
                }

                it("does not report a double-bang call the field of a non-null param") {
                    val code = """
                        class A(val a: Int?)

                        fun foo(a: A) {
                            val b = a.a!! + 2
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                }

                it("does not report on overridden function parameter") {
                    val code = """
                        interface A {
                            fun foo(a: Int?)
                        }
                        
                        class B : A {
                            override fun foo(a: Int?) {
                                val b = a!! + 2 
                            }
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                }
            }

            context("using a null-safe expression") {
                context("in initializer") {
                    it("does not report when the safe-qualified expression is the only expression of the function") {
                        val code = """
                            class A {
                                val foo = "BAR"
                            }
                            
                            fun foo(a: A?) = a?.foo
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }
                }

                context("in a non-return statement") {
                    it("does report when the safe-qualified expression is the only expression of the function") {
                        val code = """   
                            class A(val foo: String)

                            fun foo(a: A?) {
                                a?.let { println(it.foo) }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                    }

                    it("does not report when the safe-qualified expression is within a lambda") {
                        val code = """
                            class A {
                                fun doFoo(callback: () -> Unit) {
                                    callback.invoke()
                                }
                            }
    
                            fun foo(a: String?, aObj: A) {
                                aObj.doFoo {
                                    a?.let { println("a not null") }
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does not report when the safe-qualified expression is not the only expression of the function") {
                        val code = """
                            class A {
                                fun doFoo() { println("FOO") }
                            }
                            
                            fun foo(a: A?) {
                                a?.doFoo()
                                val b = 5 + 2
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }
                }
                context("in a return statement") {
                    it("does not report when the safe-qualified expression is the only expression of the function") {
                        val code = """
                            class A {
                                val foo = "BAR"
                            }
                            
                            fun fizz(aObj: A?): String? {
                                return aObj?.foo
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }
                }
            }

            context("when statements") {
                context("without a subject") {
                    it("does not report when the parameter is checked on nullity") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a == null -> println("a is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does not report when the parameter is checked on nullity in a reversed manner") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    null == a -> println("a is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does not report when the parameter is checked on nullity with multiple clauses") {
                        val code = """
                            fun foo(a: Int?, other: Int) {
                                when {
                                    a == null && other % 2 == 0 -> println("a is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does report when the parameter is only checked on non-nullity") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a != null -> println(2 + a)
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                    }

                    it("does report when the parameter is only checked on non-nullity with multiple clauses") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a != null && a % 2 == 0 -> println(2 + a)
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                    }

                    it("does not report when the parameter is checked on non-nullity with an else statement") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a != null -> println(2 + a)
                                    else -> println("a is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does not report on nullable type matching") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a !is Int -> println("a is null")
                                }
                            }

                            fun fizz(b: Int?) {
                                when {
                                    b is Int? -> println("b is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does report on non-null type matching") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a is Int -> println(2 + a)
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                    }

                    it("does report on non-null type matching with multiple clauses") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a is Int && a % 2 == 0 -> println(2 + a)
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                    }

                    it("does not report on non-null type matching with an else statement") {
                        val code = """
                            fun foo(a: Int?) {
                                when {
                                    a is Int -> println(2 + a)
                                    else -> println("a is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }
                }

                context("with a subject") {
                    it("does not report when the parameter is checked on nullity") {
                        val code = """
                            fun foo(a: Int?) {
                                when (a) {
                                    null -> println("a is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does not report on nullable type matching") {
                        val code = """
                            fun foo(a: Int?) {
                                when (a) {
                                    !is Int -> println("a is null")
                                }
                            }

                            fun fizz(b: Int?) {
                                when (b) {
                                    is Int? -> println("b is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }

                    it("does report on non-null type matching") {
                        val code = """
                            fun foo(a: Int?) {
                                when(a) {
                                    is Int -> println(2 + a)
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                    }

                    it("does not report on non-null type matching with an else statement") {
                        val code = """
                            fun foo(a: Int?) {
                                when(a) {
                                    is Int -> println(2 + a)
                                    else -> println("a is null")
                                }
                            }
                        """.trimIndent()
                        assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                    }
                }
            }

            context("if-statements") {
                it("does not report when the parameter is checked on nullity") {
                    val code = """
                        fun foo(a: Int?) {
                            if (a == null) {
                                println("'a' is null")
                            }
                        }

                        fun fizz(a: Int?) {
                            if (null == a) {
                                println("'a' is null")
                            }
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                }

                it("does not report when the if-check is in the else statement") {
                    val code = """
                        fun foo(num: Int, a: Int?) {
                            if (num % 2 == 0) {
                                println("'num' is even")
                            } else if (a == null) {
                                println("'a' is null")
                            }
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                }

                it("does report when the parameter is only checked on non-nullity in a function") {
                    val code = """
                        fun foo(a: Int?) {
                            if (a != null) {
                                println(a + 5)
                            }
                        }

                        fun fizz(a: Int?) {
                            if (null != a) {
                                println(a + 5)
                            }
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).hasSize(2)
                }

                it("does report when the parameter is only checked on non-nullity with multiple clauses") {
                    val code = """
                        fun foo(a: Int?, other: Int) {
                            if (a != null && other % 2 == 0) {
                                println(a + 5)
                            }
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).hasSize(1)
                }

                it("does not report when the parameter is checked on non-nullity with an else statement") {
                    val code = """
                        fun foo(a: Int?) {
                            if (a != null) {
                                println(a + 5)
                            } else {
                                println(5)
                            }
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                }

                it("does not report when there are other expressions after the non-null check") {
                    val code = """
                        fun foo(a: Int?) {
                            if (a != null) {
                                println(a + 5)
                            }
                            val b = 5 + 2
                        }
                    """.trimIndent()
                    assertThat(subject.compileAndLintWithContext(env, code)).isEmpty()
                }
            }
        }
    }
})
