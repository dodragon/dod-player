// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects {
    ext.set("gprUser", localProperties.getProperty("gpr.user"))
    ext.set("gprKey", localProperties.getProperty("gpr.key"))
}