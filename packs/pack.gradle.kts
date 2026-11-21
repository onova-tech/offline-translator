plugins {
    alias(libs.plugins.android.asset.pack)
}

// A Translator language pack (spec 012, spec 080): Mozilla's models for one language, both directions with English,
// data only (constitution Principle I), downloaded from Google Play only after the person confirms its size. Every
// pack module uses this script; settings.gradle.kts lists them from packs/languages.tsv. pack.json is committed;
// scripts/fetch-language-packs.sh adds the model files (git-ignored).
assetPack {
    packName.set(project.name)
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
