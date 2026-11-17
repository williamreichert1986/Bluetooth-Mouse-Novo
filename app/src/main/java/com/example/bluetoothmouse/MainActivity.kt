
package com.example.bluetoothmouse

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.Executor

class MainActivity : ComponentActivity() {

    private var hidDevice: BluetoothHidDevice? = null

    private var mouseDevice: android.bluetooth.BluetoothDevice? = null
    
    private var bluetoothStatus = mutableStateOf("Preparando Bluetooth...")

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(
            pluggedDevice: android.bluetooth.BluetoothDevice?,
            registered: Boolean
        ) {
            runOnUiThread {
                bluetoothStatus.value = if (registered) {
                    "Bluetooth HID registrado"
                } else {
                    "Bluetooth HID não registrado"
                }
            }
        }

        override fun onConnectionStateChanged(
    device: android.bluetooth.BluetoothDevice?,
    state: Int
) {
    runOnUiThread {
        mouseDevice = if (state == BluetoothProfile.STATE_CONNECTED) {
            device
        } else {
            null
        }

        bluetoothStatus.value = when (state) {
            BluetoothProfile.STATE_CONNECTED -> "Mouse conectado"
            BluetoothProfile.STATE_CONNECTING -> "Conectando..."
            BluetoothProfile.STATE_DISCONNECTED -> "Bluetooth desconectado"
            else -> "Estado Bluetooth: $state"
        }
    }
}
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(
            profile: Int,
            proxy: BluetoothProfile
        ) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                registrarMouse()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
            bluetoothStatus.value = "Serviço Bluetooth desconectado"
        }
    }

    @SuppressLint("MissingPermission")
    private fun registrarMouse() {
        val descriptor = byteArrayOf(
            0x05, 0x01, 0x09, 0x02, 0xA1.toByte(), 0x01,
            0x09, 0x01, 0xA1.toByte(), 0x00,
            0x05, 0x09, 0x19, 0x01, 0x29, 0x03,
            0x15, 0x00, 0x25, 0x01, 0x95.toByte(), 0x03,
            0x75, 0x01, 0x81.toByte(), 0x02,
            0x95.toByte(), 0x01, 0x75, 0x05,
            0x81.toByte(), 0x01,
            0x05, 0x01, 0x09, 0x30, 0x09, 0x31,
            0x09, 0x38, 0x15, 0x81.toByte(), 0x25, 0x7F,
            0x75, 0x08, 0x95.toByte(), 0x03,
            0x81.toByte(), 0x06, 0xC0.toByte(), 0xC0.toByte()
        )

        val settings = BluetoothHidDeviceAppSdpSettings(
            "Bluetooth Mouse",
            "Mouse Bluetooth",
            "Bluetooth Mouse",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            descriptor
        )

        val executor = Executor { command -> command.run() }

        val registered = hidDevice?.registerApp(
            settings,
            null,
            null,
            executor,
            hidCallback
        ) ?: false

        bluetoothStatus.value = if (registered) {
            "Solicitação de registro enviada"
        } else {
            "Não foi possível registrar o mouse"
        }
    }


    @SuppressLint("MissingPermission")
    private fun iniciarBluetooth() {
        if (Build.VERSION.SDK_INT >= 31) {
            val connectPermission =
                Manifest.permission.BLUETOOTH_CONNECT
            val advertisePermission =
                Manifest.permission.BLUETOOTH_ADVERTISE

            if (checkSelfPermission(connectPermission) !=
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(advertisePermission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        connectPermission,
                        advertisePermission
                    ),
                    100
                )
                return
            }
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()

        if (adapter == null) {
            bluetoothStatus.value = "Bluetooth não disponível"
            return
        }

        if (!adapter.isEnabled) {
            bluetoothStatus.value = "Ative o Bluetooth do celular"
            return
        }

        val discoverableIntent = android.content.Intent(
            BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE
        ).apply {
            putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                300
            )
        }

        startActivity(discoverableIntent)

        val sucesso = adapter.getProfileProxy(
            this,
            profileListener,
            BluetoothProfile.HID_DEVICE
        )

        bluetoothStatus.value = if (sucesso) {
            "Ativando Bluetooth HID e visibilidade..."
        } else {
            "Não foi possível iniciar o serviço HID"
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MouseScreen(bluetoothStatus.value) {
                iniciarBluetooth()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarBluetooth()
        } else {
            bluetoothStatus.value = "Permissão Bluetooth não concedida"
        }
    }


    override fun onDestroy() {
        hidDevice?.unregisterApp()
        super.onDestroy()
    }
}

@Composable
fun MouseScreen(
    status: String,
    onConnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101820)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                "BLUETOOTH MOUSE",
                color = Color.White,
                fontSize = 21.sp
            )

            Text(
                status,
                color = Color(0xFF80CBC4),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Color(0xFF263746),
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ÁREA DO TOUCHPAD",
                    color = Color.White,
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text("CLIQUE DIREITO")
                }

                Button(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text("CLIQUE ESQUERDO")
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("INICIAR BLUETOOTH HID")
            }
        }
    }
}
