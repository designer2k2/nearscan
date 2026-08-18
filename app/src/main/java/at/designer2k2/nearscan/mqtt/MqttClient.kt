package at.designer2k2.nearscan.mqtt

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * Thin wrapper around the Eclipse Paho v3 [org.eclipse.paho.client.mqttv3.MqttClient] managing a
 * single persistent connection. All operations are exception-safe and logged.
 */
@Singleton
class MqttClient @Inject constructor() {
    private var client: org.eclipse.paho.client.mqttv3.MqttClient? = null
    private var currentBroker: String = ""

    /** Connects to [brokerUrl]. No-op if already connected to the same broker. */
    fun connect(brokerUrl: String) {
        if (isConnected() && currentBroker == brokerUrl) return

        // Tear down any existing connection before opening a new one.
        disconnect()

        try {
            val clientId = "nearscan-${System.currentTimeMillis()}"
            val c = org.eclipse.paho.client.mqttv3.MqttClient(
                brokerUrl,
                clientId,
                MemoryPersistence(),
            )
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
                // A session can run for hours/days; without this, the very first network blip
                // (Doze maintenance window, WiFi roam, broker restart) permanently kills
                // publishing until the user manually toggles MQTT off and on again.
                isAutomaticReconnect = true
            }
            c.connect(options)
            client = c
            currentBroker = brokerUrl
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to connect to broker $brokerUrl", e)
            client = null
            currentBroker = ""
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid broker URL $brokerUrl", e)
            client = null
            currentBroker = ""
        }
    }

    /** Disconnects the current connection if any, releasing the client. */
    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: MqttException) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            client = null
            currentBroker = ""
        }
    }

    /** Publishes [payload] to [topic] at QoS 0, non-retained. No-op when not connected. */
    fun publish(topic: String, payload: String) {
        val c = client ?: return
        if (!c.isConnected) return
        try {
            val message = MqttMessage(payload.toByteArray()).apply {
                qos = 0
                isRetained = false
            }
            c.publish(topic, message)
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to publish to $topic", e)
        }
    }

    fun isConnected(): Boolean = client?.isConnected == true

    private companion object {
        const val TAG = "MqttClient"
    }
}
