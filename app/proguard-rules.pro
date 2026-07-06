# Add project specific ProGuard rules here.

# Eclipse Paho MQTT client (org.eclipse.paho:org.eclipse.paho.client.mqttv3) predates AGP/R8
# consumer proguard rules and uses reflection internally for its persistence layer — keep it
# whole rather than risk R8 stripping something it reaches only via reflection.
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**

# Room entities are read/written via reflection-free generated DAOs, but keep field names
# anyway since they mirror the exported CSV/GeoJSON/MQTT column names throughout this app.
-keep class at.designer2k2.nearscan.db.** { *; }

# Kotlin coroutines' debug/internal probes reference classes that may not exist on all
# platforms; safe to silence rather than keep.
-dontwarn kotlinx.coroutines.debug.internal.**
