# Regras exclusivas do APK de teste instrumentado (nao vao para o app).
# androidx.test arrasta anotacoes de compile-time do errorprone/Guava que, por definicao,
# nao existem em runtime. Declarar isso ao R8 nao afrouxa nenhum guardrail do app.
-dontwarn javax.lang.model.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**

# Sem os atributos de anotacao em runtime o runner nao enxerga @Test e reporta 0 tests.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# O runner encontra os testes por reflexao a partir das anotacoes JUnit; sem isto o R8
# renomeia as classes e o instrumentation reporta "0 tests".
-keep @org.junit.runner.RunWith class * { *; }
-keepclasseswithmembers class * {
    @org.junit.Test <methods>;
}
-keep class androidx.test.** { *; }
-keep class androidx.tracing.** { *; }
-dontwarn androidx.tracing.**
-keep class org.junit.** { *; }
