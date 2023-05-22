rootProject.name = "ksmt"
include("ksmt-core")
include("ksmt-z3")
include("ksmt-bitwuzla")
include("ksmt-runner")
include("ksmt-test")
include("ksmt-kbva")
include("ksmt-kissat")
include("ksmt-kbva-aig")

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.name == "rdgen") {
                useModule("com.jetbrains.rd:rd-gen:${requested.version}")
            }
        }
    }
}

