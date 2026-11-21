plugins {
    alias(libs.plugins.android.asset.pack)
}

// An on-demand pack, data only (constitution Principle I), downloaded from Google Play only after the person confirms
// its size: a Translator language (specs 012, 080: Mozilla's models, both directions with English) or speech (spec 082:
// Whisper, espeak-ng's data or one voice). Every pack module uses this script; settings.gradle.kts lists them from
// packs/languages.tsv and packs/speech.tsv. pack.json is committed; the fetch scripts add the files (git-ignored).
assetPack {
    packName.set(project.name)
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
