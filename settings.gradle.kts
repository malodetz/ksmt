rootProject.name = "ksmt"
include("ksmt-core")
include("ksmt-z3")
include("ksmt-bitwuzla")
include("ksmt-runner")
include("ksmt-test")

sourceControl {
    gitRepository(uri("https://github.com/UnitTestBot/kosat.git")) {
        producesModule("org.kosat:kosat")
    }
}

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.name == "rdgen") {
                useModule("com.jetbrains.rd:rd-gen:${requested.version}")
            }
        }
    }
}
include("ksmt-kbva")
