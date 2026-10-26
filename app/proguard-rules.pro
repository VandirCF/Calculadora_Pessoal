# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---- Gson models persisted as JSON in SharedPreferences ----------------------------------------
# Field names are also fixed with @SerializedName; these rules keep the classes and generic types
# intact so saved history and financial state keep loading after R8 shrinking.
-keepattributes Signature, *Annotation*
-keep class com.darkempire78.opencalculator.history.History { <init>(...); <fields>; }
-keep class com.darkempire78.opencalculator.finance.rpn.RpnSnapshot { <init>(...); <fields>; }
-keep class com.darkempire78.opencalculator.finance.TvmRegisters { <init>(...); <fields>; }
-keep class com.darkempire78.opencalculator.finance.CashFlow { <init>(...); <fields>; }
-keep class com.darkempire78.opencalculator.financial.FinancialStateStore$Envelope { <init>(...); <fields>; }
