#!/usr/bin/env bash
#
# G5 — verificacao de fumaca do artefato de release ofuscado.
#
# Monta o APK com R8 ligado, instala num dispositivo e confirma que ele sobe: se uma
# keep rule estiver faltando, a serializacao, o Ktor, o grafo do Koin ou o @Parcelize
# quebram por reflexao e o processo morre aqui.
#
# Fica FORA do comando G8 de proposito: G8 e uma suite JVM sem emulador (G7).
# Rode manualmente ou em CI com um emulador disponivel:
#
#     ./scripts/release-smoke-check.sh
#
set -euo pipefail

PACKAGE="com.desafiomercadobitcoin"
ACTIVITY=".MainActivity"
APK="app/build/outputs/apk/release/app-release.apk"
SETTLE_SECONDS="${SETTLE_SECONDS:-6}"

ADB="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}/platform-tools/adb"
[ -x "$ADB" ] || ADB="$(command -v adb)"

fail() {
    echo "FALHOU: $1" >&2
    exit 1
}

echo "==> Montando o release com R8"
./gradlew :app:assembleRelease
[ -f "$APK" ] || fail "APK de release nao foi gerado em $APK"

echo "==> Conferindo que o R8 realmente rodou"
if [ -d app/build/outputs/mapping/release ]; then
    echo "    saida do R8 presente em app/build/outputs/mapping/release"
else
    fail "sem saida do R8 -- a otimizacao do build type release esta desligada?"
fi

echo "==> Conferindo que ha um dispositivo"
"$ADB" wait-for-device
[ "$("$ADB" devices | grep -c '\sdevice$')" -gt 0 ] || fail "nenhum dispositivo/emulador conectado"

echo "==> Instalando"
"$ADB" uninstall "$PACKAGE" >/dev/null 2>&1 || true
"$ADB" install -r "$APK" >/dev/null || fail "instalacao recusada"

echo "==> Iniciando a frio"
"$ADB" logcat -c
"$ADB" shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null
sleep "$SETTLE_SECONDS"

echo "==> Verificando"
PID="$("$ADB" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
[ -n "$PID" ] || fail "o processo morreu apos o start -- provavel keep rule ausente"

CRASHES="$("$ADB" logcat -d -s AndroidRuntime:E | grep -E "FATAL|ClassNotFound|NoClassDef|NoSuchMethod" || true)"
[ -z "$CRASHES" ] && echo "    sem crash no logcat" || {
    echo "$CRASHES" >&2
    fail "excecao de reflexao no artefato ofuscado -- keep rule ausente"
}

FOCUSED="$("$ADB" shell dumpsys activity activities | grep -c "topResumedActivity.*$PACKAGE" || true)"
[ "$FOCUSED" -gt 0 ] || fail "a activity nao chegou ao estado resumed"
echo "    $PACKAGE/$ACTIVITY em foreground (pid $PID)"

"$ADB" shell am force-stop "$PACKAGE"
echo
echo "OK: o artefato de release ofuscado sobe sem quebrar reflexao."
