#!/usr/bin/env bash
# Runs the project's automated tests, stage by stage, the way a CI server would (docs/testing.md).
#
#   scripts/ci.sh              everything: check, then the device stages
#   scripts/ci.sh check        what GitHub Actions runs: static analysis, then ./gradlew check. No emulator.
#   scripts/ci.sh device       only the emulator stages: a11y, then e2e
#   scripts/ci.sh a11y         the accessibility suite and the real-font checks on the emulator
#   scripts/ci.sh e2e          the end-to-end tests on the emulator
#   scripts/ci.sh engine       the Translator's engine with the real packs on the emulator (spec 012); fill the packs
#                              first with scripts/fetch-language-packs.sh
#   scripts/ci.sh engine --all every language of packs/languages.tsv, five packs at a time (spec 080; fetches them
#                              into build/language-packs, so a debug bundle keeps only pt and es)
#
# Options for the emulator stages:
#   --test <class[#method]>    only this E2E test, such as TranslatorJourney
#   --keep-emulator            leave the emulator running, and reuse it next time
#
# The emulator stages only ever talk to their own emulator (emulator-5580), never to a phone attached to adb, because
# they wipe the app's data. Results and failure details go to build/ci/.
set -uo pipefail

cd "$(dirname "$0")/.."
ROOT=$PWD
OUT=$ROOT/build/ci
SERIAL=emulator-5580
PORT=5580
AVD=${CI_AVD:-pocket36atd}  # the development machine's Android 36 ATD emulator
APP_PACKAGE=tech.onova.offlinetranslator
E2E_PACKAGE=$APP_PACKAGE.e2e
GRADLE_JVM_ARGS="-Xmx3g -Xshare:off -XX:+UseSerialGC -Dfile.encoding=UTF-8"
BOOT_TIMEOUT_SECONDS=420

[ -f "$HOME/Android/env.sh" ] && . "$HOME/Android/env.sh"

STAGE=all
E2E_TEST=""
KEEP_EMULATOR=false
ENGINE_ALL=false
ALL_PACKS=build/language-packs
while [ $# -gt 0 ]; do
    case "$1" in
    check | device | a11y | e2e | engine | all) STAGE=$1 ;;
    --test)
        E2E_TEST=$2
        shift
        ;;
    --keep-emulator) KEEP_EMULATOR=true ;;
    --all) ENGINE_ALL=true ;;
    -h | --help)
        sed -n '2,17p' "$0" | sed 's/^# \{0,1\}//'
        exit 0
        ;;
    *)
        echo "Unknown option: $1 (see scripts/ci.sh --help)" >&2
        exit 2
        ;;
    esac
    shift
done

mkdir -p "$OUT"
SUMMARY=()
FAILED=false
STARTED_EMULATOR=false

note() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

record() { # stage, result, detail, start time
    local seconds=$(($(date +%s) - $4))
    SUMMARY+=("$(printf '%-8s %-7s %3dm%02ds  %s' "$1" "$2" $((seconds / 60)) $((seconds % 60)) "$3")")
    [ "$2" = FAILED ] && FAILED=true
    return 0
}

print_summary() {
    note "Summary"
    printf '%s\n' "${SUMMARY[@]}"
    if [ "$FAILED" = true ]; then
        echo "Result: FAILED. Details in build/ci/ and in each module's build/reports/."
    else
        echo "Result: OK"
    fi
}

# Gradle, tolerating the development machine's known JVM crashes: they are retried once, never a test failure.
gradle() {
    local log=$OUT/gradle.log attempt
    for attempt in 1 2; do
        ./gradlew --console=plain -Dorg.gradle.vfs.watch=false "-Dorg.gradle.jvmargs=$GRADLE_JVM_ARGS" "$@" 2>&1 |
            tee "$log" | grep -E '^(BUILD |FAILURE|> Task .*FAILED|.* FAILED$|e: |[0-9]+ tests completed)' 
        local status=${PIPESTATUS[0]}
        [ "$status" -eq 0 ] && return 0
        if [ "$attempt" -eq 1 ] && grep -qE 'exit value 134|daemon disappeared|Unexpected LIR opcode' "$log"; then
            note "The JVM crashed (known on this machine). Trying once more."
            continue
        fi
        return "$status"
    done
}

stage_check() {
    local start result=OK
    start=$(date +%s)
    # The fast checks first: on GitHub, a formatting mistake then costs minutes rather than a whole run.
    note "check: static analysis and architecture rules"
    gradle ktlintCheck detekt :tooling:architecture-tests:test || result=FAILED
    if [ "$result" = OK ]; then
        note "check: ./gradlew check, and the device tests still compile"
        gradle check :androidApp:compileDebugAndroidTestKotlin :tooling:e2e:compileDebugKotlin || result=FAILED
    fi
    record check "$result" "unit, UI, screenshots, fit, audit, rules, lint, coverage" "$start"
}

adb_emulator() { adb -s "$SERIAL" "$@"; }

build_device_apks() {
    note "device: building the app and the test packages"
    # The bundle too: the app goes on through bundletool's local testing, with its language packs (spec 012 T042).
    gradle :androidApp:assembleDebug :androidApp:bundleDebug :androidApp:bundletoolClasspath \
        :androidApp:assembleDebugAndroidTest :tooling:e2e:assembleDebug || return 1
    # Gradle and the emulator together exhaust this machine's memory.
    ./gradlew --stop >/dev/null 2>&1
    pkill -f '[K]otlinCompileDaemon' 2>/dev/null
    return 0
}

start_emulator() {
    if adb_emulator get-state >/dev/null 2>&1; then
        note "device: reusing the emulator already on $SERIAL"
    else
        note "device: starting the $AVD emulator"
        local command="$ANDROID_HOME/emulator/emulator -avd $AVD -port $PORT -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -memory 2048 -cores 2"
        if [ -w /dev/kvm ]; then
            nohup $command >"$OUT/emulator.log" 2>&1 &
        else
            nohup sg kvm -c "$command" >"$OUT/emulator.log" 2>&1 &
        fi
        STARTED_EMULATOR=true
    fi
    local waited=0
    while [ "$(adb_emulator shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != 1 ]; do
        sleep 5
        waited=$((waited + 5))
        if [ "$waited" -ge "$BOOT_TIMEOUT_SECONDS" ]; then
            echo "The emulator didn't boot in ${BOOT_TIMEOUT_SECONDS}s; see build/ci/emulator.log" >&2
            return 1
        fi
    done
    # Refuses a physical device, whatever answers on this serial.
    if [ "$(adb_emulator shell getprop ro.kernel.qemu | tr -d '\r')" != 1 ]; then
        echo "$SERIAL isn't an emulator. Stopping before touching it." >&2
        return 1
    fi
    adb_emulator shell settings put global window_animation_scale 0
    adb_emulator shell settings put global transition_animation_scale 0
    adb_emulator shell settings put global animator_duration_scale 0
    adb_emulator shell svc power stayon true
    adb_emulator shell wm dismiss-keyguard >/dev/null 2>&1
    # The image starts with its radios off; the ad journeys need them.
    adb_emulator shell svc wifi enable 2>/dev/null
    adb_emulator shell svc data enable 2>/dev/null
    local tries=0
    until adb_emulator shell ping -c1 -W2 8.8.8.8 >/dev/null 2>&1; do
        tries=$((tries + 1))
        if [ "$tries" -ge 10 ]; then
            echo "No internet on the emulator: the ad journeys will be skipped."
            break
        fi
        sleep 3
    done
    return 0
}

stop_emulator() {
    if [ "$STARTED_EMULATOR" = true ] && [ "$KEEP_EMULATOR" = false ]; then
        adb_emulator emu kill >/dev/null 2>&1
    fi
}

install_apks() {
    note "device: installing"
    install_app_with_packs &&
        adb_emulator install -r -t androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk >/dev/null &&
        adb_emulator install -r -t tooling/e2e/build/outputs/apk/debug/e2e-debug.apk >/dev/null
}

# Spec 012 T042: the app from the bundle, with bundletool's local testing, so its on-demand language packs are fetched
# from the emulator's own storage instead of Google Play (Google's "Test asset delivery"). bundletool runs from the
# classpath the build wrote (AGP's version, :androidApp:bundletoolClasspath). Local testing keeps the packs in the app's
# external files, which a journey's wipe deletes: a copy goes to /data/local/tmp for TranslatorJourney to put back.
bundletool() {
    java -cp "$(cat androidApp/build/bundletool.classpath)" com.android.tools.build.bundletool.BundleToolMain "$@"
}

# The library jar has no aapt2 of its own, unlike bundletool's standalone one: the SDK's newest build-tools has it.
sdk_aapt2() {
    local tools
    tools=$(ls -d "$ANDROID_HOME"/build-tools/*/ 2>/dev/null | sort -V | tail -1)
    echo "${tools}aapt2"
}

install_app_with_packs() {
    local bundle=androidApp/build/outputs/bundle/debug/androidApp-debug.aab apks=$OUT/app-local-testing.apks
    if [ ! -f androidApp/build/bundletool.classpath ] || [ ! -f "$bundle" ]; then
        note "device: no bundle or bundletool; installing the plain APK (the Translator's pack journey will skip)"
        adb_emulator install -r -t androidApp/build/outputs/apk/debug/androidApp-debug.apk >/dev/null
        return
    fi
    bundletool build-apks --bundle="$bundle" --output="$apks" --overwrite --local-testing --aapt2="$(sdk_aapt2)" \
        --ks="$HOME/.android/debug.keystore" --ks-pass=pass:android --ks-key-alias=androiddebugkey \
        --key-pass=pass:android >"$OUT/bundletool.txt" 2>&1 || return 1
    bundletool install-apks --apks="$apks" --device-id="$SERIAL" --adb="$(command -v adb)" \
        >>"$OUT/bundletool.txt" 2>&1 || return 1
    adb_emulator shell rm -rf /data/local/tmp/offlinetranslator-local-testing
    adb_emulator shell cp -r "/sdcard/Android/data/$APP_PACKAGE/files/local_testing" /data/local/tmp/offlinetranslator-local-testing
}

# Runs one instrumentation and turns its raw output into a result; "OK (n tests)" is the only success.
run_instrumentation() { # stage, runner component, extra arguments
    local stage=$1 component=$2 output=$OUT/$1.txt start
    shift 2
    start=$(date +%s)
    note "$stage: running on $SERIAL"
    # Room for a whole run, so the driver's warnings at its start are still there at its end.
    adb_emulator logcat -G 16M >/dev/null 2>&1
    adb_emulator logcat -c
    adb_emulator shell am instrument -w -r "$@" "$component" 2>&1 | tr -d '\r' | tee "$output" |
        grep --line-buffered -E '^INSTRUMENTATION_STATUS: test=|^INSTRUMENTATION_STATUS_CODE: -|^OK |^FAILURES|^Tests run|^INSTRUMENTATION_RESULT: shortMsg|Process crashed' |
        awk -F'test=' '/test=/ {name=$2; next} /STATUS_CODE: -1|STATUS_CODE: -2/ {print "  FAILED  " name; next} /STATUS_CODE: -3|STATUS_CODE: -4/ {print "  skipped " name; next} /^(OK |FAILURES|Tests run|INSTRUMENTATION_RESULT|.*Process crashed)/ {print}'
    local passed skipped failed
    passed=$(grep -cE '^INSTRUMENTATION_STATUS_CODE: 0$' "$output")
    skipped=$(grep -cE '^INSTRUMENTATION_STATUS_CODE: -(3|4)$' "$output")
    failed=$(grep -cE '^INSTRUMENTATION_STATUS_CODE: -(1|2)$' "$output")
    local detail="$passed passed, $failed failed, $skipped skipped"
    # A key tapped again passes the test, so its warning is kept even then (the lost tap, status-2026-11-08).
    adb_emulator logcat -d -s AppDriver:W >"$OUT/$stage-driver.txt" 2>/dev/null
    local retried
    retried=$(grep -c "had no effect" "$OUT/$stage-driver.txt" 2>/dev/null)
    [ "${retried:-0}" -gt 0 ] && detail="$detail, $retried key(s) tapped again (build/ci/$stage-driver.txt)"
    if grep -qE '^OK \(' "$output" && [ "$failed" -eq 0 ]; then
        record "$stage" OK "$detail" "$start"
    else
        adb_emulator logcat -d >"$OUT/$stage-logcat.txt" 2>/dev/null
        record "$stage" FAILED "$detail; see build/ci/$stage.txt" "$start"
        return 1
    fi
}

stage_a11y() {
    run_instrumentation a11y "$APP_PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" \
        -e class "$APP_PACKAGE.AccessibilityChecksTest,$APP_PACKAGE.RealFontsTest"
}

stage_e2e() {
    local args=()
    if [ -n "$E2E_TEST" ]; then
        args=(-e class "$E2E_PACKAGE.$E2E_TEST")
    else
        args=(-e package "$E2E_PACKAGE")
    fi
    run_instrumentation e2e "$E2E_PACKAGE/androidx.test.runner.AndroidJUnitRunner" "${args[@]}"
    local status=$?
    if [ "$status" -ne 0 ]; then
        adb_emulator pull "/sdcard/Android/data/$E2E_PACKAGE/files/failures" "$OUT/e2e-failures" >/dev/null 2>&1
    fi
    return "$status"
}

# The real engine with the real packs (spec 012 T031). The packs are too big for a test APK: they go to
# /data/local/tmp, and the test copies them into the app's files with run-as.
# Spec 080 SC-002: every language of the catalog, five packs at a time, so the emulator never holds them all.
stage_engine_all() {
    local codes batch=() n=0 pack
    # Outside packs/, so the next debug bundle still takes only Portuguese and Spanish.
    scripts/fetch-language-packs.sh --all --into "$ALL_PACKS" >"$OUT/fetch-all.txt" 2>&1 || {
        note "engine: fetching every pack failed (build/ci/fetch-all.txt)"
        return 1
    }
    : >"$OUT/engine-languages.txt"
    codes=$(grep -v -e '^#' -e '^code' packs/languages.tsv | cut -f1 | uniq)
    for code in $codes; do
        batch+=("translate_$code")
        if [ ${#batch[@]} -eq 5 ]; then
            n=$((n + 1))
            engine_batch "$n" "${batch[@]}"
            batch=()
        fi
    done
    if [ ${#batch[@]} -gt 0 ]; then
        n=$((n + 1))
        engine_batch "$n" "${batch[@]}"
    fi
}

engine_batch() { # number, packs...
    local number=$1 pack list
    shift
    adb_emulator shell rm -rf /data/local/tmp/engine-batch
    adb_emulator shell mkdir -p /data/local/tmp/engine-batch
    for pack in "$@"; do
        adb_emulator push "$ALL_PACKS/$pack" /data/local/tmp/engine-batch/ >/dev/null || return 1
    done
    list=$(IFS=,; echo "$*")
    run_instrumentation "engine-$number" "$APP_PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" \
        -e class "$APP_PACKAGE.BergamotEngineTest#everyGivenLanguageTranslatesBothWaysWithEnglish" -e packs "$list"
    # Each language's sentence there and back, or why it failed.
    adb_emulator logcat -d -s BergamotEngineTest:I >>"$OUT/engine-languages.txt" 2>/dev/null
    adb_emulator shell rm -rf /data/local/tmp/engine-batch
}

stage_engine() {
    local pack source
    if [ "$ENGINE_ALL" = true ]; then
        stage_engine_all
        return
    fi
    for pack in translate_pt translate_es; do
        source="packs/$pack/src/main/assets/$pack"
        if [ -z "$(find "$source" -name '*.bin' -print -quit 2>/dev/null)" ]; then
            note "engine: the packs are empty; run scripts/fetch-language-packs.sh first"
            return 1
        fi
        adb_emulator shell rm -rf "/data/local/tmp/$pack"
        adb_emulator push "$source" /data/local/tmp/ >/dev/null || return 1
    done
    run_instrumentation engine "$APP_PACKAGE.test/androidx.test.runner.AndroidJUnitRunner" \
        -e class "$APP_PACKAGE.BergamotEngineTest"
}

stage_device() { # a11y, e2e or both
    local start
    start=$(date +%s)
    if ! command -v adb >/dev/null || [ ! -x "${ANDROID_HOME:-}/emulator/emulator" ]; then
        record device FAILED "adb or the emulator isn't installed" "$start"
        return
    fi
    if ! build_device_apks; then
        record device FAILED "the packages didn't build" "$start"
        return
    fi
    trap stop_emulator EXIT
    if ! start_emulator; then
        record device FAILED "the emulator didn't get ready" "$start"
        stop_emulator
        return
    fi
    if ! install_apks; then
        record device FAILED "the app didn't install (build/ci/bundletool.txt)" "$start"
        stop_emulator
        return
    fi
    case "$1" in
    a11y) stage_a11y ;;
    e2e) stage_e2e ;;
    engine) stage_engine ;;
    *)
        stage_a11y
        stage_e2e
        ;;
    esac
    stop_emulator
}

case "$STAGE" in
check) stage_check ;;
a11y | e2e | engine) stage_device "$STAGE" ;;
device) stage_device both ;;
all)
    stage_check
    if [ "$FAILED" = true ]; then
        note "Skipping the emulator stages: check failed."
    else
        stage_device both
    fi
    ;;
esac

print_summary
[ "$FAILED" = false ]
