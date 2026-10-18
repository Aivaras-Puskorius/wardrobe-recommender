# HACK: firebase-components (used by ML Kit) keeps its component registrars with
# "-keep class * implements ComponentRegistrar", which in R8 full mode no longer keeps the no-argument
# constructor. ML Kit creates the registrars by reflection, so without it the release build crashes on
# start with a null MultiFlavorDetectorCreator. Remove when the library's rule keeps <init>.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
