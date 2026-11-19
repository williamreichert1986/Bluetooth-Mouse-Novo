
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    private var hidDevice: BluetoothHidDevice? = null

    private var mouseDevice: android.bluetooth.BluetoothDevice? = null

    private val dispositivosPareados =
        mutableStateListOf<android.bluetooth.BluetoothDevice>()

    private var botoesPressionados = 0

    private val sensibilidade = mutableIntStateOf(50)

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
        // MOUSE - RELATÓRIO 1
        0x05, 0x01, 0x09, 0x02, 0xA1.toByte(), 0x01,
        0x85.toByte(), 0x01,
        0x09, 0x01, 0xA1.toByte(), 0x00,
        0x05, 0x09, 0x19, 0x01, 0x29, 0x03,
        0x15, 0x00, 0x25, 0x01, 0x95.toByte(), 0x03,
        0x75, 0x01, 0x81.toByte(), 0x02,
        0x95.toByte(), 0x01, 0x75, 0x05,
        0x81.toByte(), 0x01,
        0x05, 0x01, 0x09, 0x30, 0x09, 0x31,
        0x09, 0x38, 0x15, 0x81.toByte(), 0x25, 0x7F,
        0x75, 0x08, 0x95.toByte(), 0x03,
        0x81.toByte(), 0x06, 0xC0.toByte(), 0xC0.toByte(),

        // TECLADO - RELATÓRIO 2
        0x05, 0x01, 0x09, 0x06, 0xA1.toByte(), 0x01,
        0x85.toByte(), 0x02,
        0x05, 0x07, 0x19, 0xE0.toByte(),
        0x29, 0xE7.toByte(), 0x15, 0x00, 0x25, 0x01,
        0x75, 0x01, 0x95.toByte(), 0x08,
        0x81.toByte(), 0x02,
        0x95.toByte(), 0x01, 0x75, 0x08,
        0x81.toByte(), 0x01,
        0x95.toByte(), 0x06, 0x75, 0x08,
        0x15, 0x00, 0x25, 0x65,
        0x05, 0x07, 0x19, 0x00, 0x29, 0x65,
        0x81.toByte(), 0x00,
        0xC0.toByte()
    )

    val settings = BluetoothHidDeviceAppSdpSettings(
        "Bluetooth Mouse",
        "Mouse e Teclado Bluetooth",
        "Bluetooth Mouse",
        BluetoothHidDevice.SUBCLASS1_COMBO,
        descriptor
    )

    val executor = Executor { command ->
        runOnUiThread {
            command.run()
        }
    }

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
        "Não foi possível registrar mouse e teclado"
    }
}

@SuppressLint("MissingPermission")
private fun enviarMovimento(x: Int, y: Int) {
    val device = mouseDevice ?: return
    val hid = hidDevice ?: return

val fator = sensibilidade.intValue / 50f

    val movimentoX = (x * fator).toInt()
    val movimentoY = (y * fator).toInt()

    hid.sendReport(
        device,
        1,
        byteArrayOf(
            botoesPressionados.toByte(),
            movimentoX.coerceIn(-127, 127).toByte(),
            movimentoY.coerceIn(-127, 127).toByte(),
            0
        )
    )
    
}
    
    @SuppressLint("MissingPermission")
private fun enviarClique(
    duplo: Boolean = false,
    botao: Int = 1
) {
    val device = mouseDevice ?: return
    val hid = hidDevice ?: return

    val handler = android.os.Handler(
        android.os.Looper.getMainLooper()
    )

    fun report(button: Int) {
        if (mouseDevice == device && hidDevice === hid) {
            hid.sendReport(
                device,
                1,
                byteArrayOf(button.toByte(), 0, 0, 0)
            )
        }
    }

    report(botao)

    handler.postDelayed({
        report(0)

        if (duplo) {
            handler.postDelayed({
                report(botao)

                handler.postDelayed({
                    report(0)
                }, 60)
            }, 120)
        }
    }, 60)
}

@SuppressLint("MissingPermission")
private fun pressionarEsquerdo() {
    val device = mouseDevice ?: return
    val hid = hidDevice ?: return

    botoesPressionados = botoesPressionados or 1

    hid.sendReport(
        device,
        1,
        byteArrayOf(botoesPressionados.toByte(), 0, 0, 0)
    )
}

@SuppressLint("MissingPermission")
private fun soltarEsquerdo() {
    val device = mouseDevice ?: return
    val hid = hidDevice ?: return

    botoesPressionados = botoesPressionados and 0xFE

    hid.sendReport(
        device,
        1,
        byteArrayOf(botoesPressionados.toByte(), 0, 0, 0)
    )
}
@SuppressLint("MissingPermission")
private fun soltarDireito() {
    val device = mouseDevice ?: return
    val hid = hidDevice ?: return

    botoesPressionados = botoesPressionados and 0xFD

    hid.sendReport(
        device,
        1,
        byteArrayOf(botoesPressionados.toByte(), 0, 0, 0)
    )
}

@SuppressLint("MissingPermission")
private fun pressionarDireito() {
    val device = mouseDevice ?: return
    val hid = hidDevice ?: return

    botoesPressionados = botoesPressionados or 0x02

    hid.sendReport(
        device,
        1,
        byteArrayOf(botoesPressionados.toByte(), 0, 0, 0)
    )
}

@SuppressLint("MissingPermission")
private fun carregarDispositivosPareados() {

    val adapter = BluetoothAdapter.getDefaultAdapter()
        ?: return

    if (Build.VERSION.SDK_INT >= 31 &&
        checkSelfPermission(
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        bluetoothStatus.value = "Permissão Bluetooth necessária"
        return
    }

    if (!adapter.isEnabled) {
        bluetoothStatus.value = "Ative o Bluetooth"
        return
    }

    dispositivosPareados.clear()

    dispositivosPareados.addAll(
        adapter.bondedDevices.sortedBy {
            it.name ?: it.address
        }
    )
}
@SuppressLint("MissingPermission")
private fun conectarDispositivo(
    dispositivo: android.bluetooth.BluetoothDevice
) {
    val hid = hidDevice

    if (hid == null) {
        bluetoothStatus.value = "Serviço HID indisponível"
        return
    }

    bluetoothStatus.value = "Solicitando conexão..."

    val solicitado = hid.connect(dispositivo)

    if (!solicitado) {
        bluetoothStatus.value = "Falha ao solicitar conexão"
    }
}

@SuppressLint("MissingPermission")
private fun enviarTexto(texto: String) {
    val device = mouseDevice ?: return
    val hid = hidDevice ?: return

    val handler = android.os.Handler(
        android.os.Looper.getMainLooper()
    )

    var atraso = 0L

    texto.forEach { caractere ->

        val tecla = when (caractere) {
            in 'a'..'z' -> (0x04 + caractere.code - 'a'.code) to false
            in 'A'..'Z' -> (0x04 + caractere.code - 'A'.code) to true

            in '1'..'9' -> (0x1E + caractere.code - '1'.code) to false
            '0' -> 0x27 to false

            ' ' -> 0x2C to false
            '\n', '\r' -> 0x28 to false

            '!' -> 0x1E to true
            '@' -> 0x1F to true
            '#' -> 0x20 to true
            '$' -> 0x21 to true
            '%' -> 0x22 to true
            '^' -> 0x23 to true
            '&' -> 0x24 to true
            '*' -> 0x25 to true
            '(' -> 0x26 to true
            ')' -> 0x27 to true

            '-' -> 0x2D to false
            '_' -> 0x2D to true
            '=' -> 0x2E to false
            '+' -> 0x2E to true
            '[' -> 0x2F to false
            '{' -> 0x2F to true
            ']' -> 0x30 to false
            '}' -> 0x30 to true
            '\\' -> 0x31 to false
            '|' -> 0x31 to true
            ';' -> 0x33 to false
            ':' -> 0x33 to true
            '\'' -> 0x34 to false
            '"' -> 0x34 to true
            '`' -> 0x35 to false
            '~' -> 0x35 to true
            ',' -> 0x36 to false
            '<' -> 0x36 to true
            '.' -> 0x37 to false
            '>' -> 0x37 to true
            '/' -> 0x38 to false
            '?' -> 0x38 to true

            else -> null
        }

        if (tecla != null) {
            val modificador = if (tecla.second) 0x02 else 0x00

            handler.postDelayed({
                if (mouseDevice == device && hidDevice === hid) {
                    hid.sendReport(
                        device,
                        2,
                        byteArrayOf(
                            modificador.toByte(),
                            0,
                            tecla.first.toByte(),
                            0, 0, 0, 0, 0
                        )
                    )

                    handler.postDelayed({
                        hid.sendReport(
                            device,
                            2,
                            byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                        )
                    }, 20)
                }
            }, atraso)

            atraso += 40
        }
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
            MouseScreen(
                status = bluetoothStatus.value,
                sensibilidade = sensibilidade.intValue,
                onSensibilidadeChange = { sensibilidade.intValue = it },
                onMove = { x, y -> enviarMovimento(x, y) },
                onPressLeft = { pressionarEsquerdo() },
                onReleaseLeft = { soltarEsquerdo() },
                onClick = { enviarClique() },
                onDoubleClick = { enviarClique(duplo = true) },
                onRightClick = { enviarClique(botao = 2) },
                onRightDoubleClick = {
                    enviarClique(duplo = true, botao = 2)
                },
                onBottomLeftClick = {
                    enviarClique()
                },
                onBottomRightClick = {
                    enviarClique(botao = 2)
                },
                onBottomLeftPress = {
                    pressionarEsquerdo()
                },
                onBottomLeftRelease = {
                    soltarEsquerdo()
                },
                onBottomRightPress = {
                    pressionarDireito()
                },
                               onBottomRightRelease = {
                    soltarDireito()
                },
               onLoadPairedDevices = {
    carregarDispositivosPareados()
},
onConnectDevice = { dispositivo ->
    conectarDispositivo(dispositivo)
},
onSendText = { texto ->
    enviarTexto(texto)
},
dispositivosPareados = dispositivosPareados
            )
        }

        iniciarBluetooth()
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

    if (requestCode == 100) {

        val todasConcedidas =
            grantResults.isNotEmpty() &&
            grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }

        if (todasConcedidas) {
            iniciarBluetooth()
        } else {
            bluetoothStatus.value =
                "Permissões Bluetooth não concedidas"
        }
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
    sensibilidade: Int,
    onSensibilidadeChange: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onPressLeft: () -> Unit,
    onReleaseLeft: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onRightClick: () -> Unit,
    onRightDoubleClick: () -> Unit,
    onBottomLeftClick: () -> Unit,
    onBottomRightClick: () -> Unit,
    onBottomLeftPress: () -> Unit,
    onBottomLeftRelease: () -> Unit,
    onBottomRightPress: () -> Unit,
        onBottomRightRelease: () -> Unit,
    onLoadPairedDevices: () -> Unit,
onConnectDevice: (android.bluetooth.BluetoothDevice) -> Unit,
onSendText: (String) -> Unit,
dispositivosPareados: List<android.bluetooth.BluetoothDevice>
) {

    var botoesInvertidos by remember { mutableStateOf(false) }
    var mostrarDispositivos by remember { mutableStateOf(false) }
    var mostrarTeclado by remember { mutableStateOf(false) }
    var textoDigitado by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
val tecladoCelular = LocalSoftwareKeyboardController.current

val view = LocalView.current

LaunchedEffect(mostrarTeclado) {
    if (mostrarTeclado) {
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
        kotlinx.coroutines.delay(200)
        tecladoCelular?.show()
    }
}

SideEffect {
    view.systemUiVisibility =
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
}

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
        text = status,
        color = Color.White
    )

        Slider(
        value = sensibilidade.toFloat(),
        onValueChange = {
            onSensibilidadeChange(it.toInt())
        },
        valueRange = 1f..100f,
        modifier = Modifier.height(28.dp)
    )

    // BOTÃO BLUETOOTH
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
               Surface(
            modifier = Modifier
                .size(42.dp)
                .clickable {
                    onLoadPairedDevices()
                    mostrarDispositivos = true
                },
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF80CBC4)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "ᛒ",
                    color = Color.Black,
                    fontSize = 26.sp
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

Box(
    modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(
            Color(0xFF263746),
            RoundedCornerShape(18.dp)
        )
        .pointerInput(Unit) {
            awaitEachGesture {
                val primeiroDedo = awaitFirstDown(
                    requireUnconsumed = false
                )

                val posicaoInicial = primeiroDedo.position.x
                var arrastando = false
                var houveMovimento = false

                try {
                    while (true) {
                        val evento = awaitPointerEvent()

                        val primeiro = evento.changes.firstOrNull {
                            it.id == primeiroDedo.id
                        }

                        val segundo = evento.changes.firstOrNull {
                            it.id != primeiroDedo.id && it.pressed
                        }

                        if (primeiro?.pressed != true) break

                        if (segundo != null) {
                            if (!arrastando) {
                                onPressLeft()
                                arrastando = true
                            }

                            val movimento = segundo.positionChange()

                            if (movimento != Offset.Zero) {
                                onMove(
                                    movimento.x.toInt(),
                                    movimento.y.toInt()
                                )
                                segundo.consume()
                            }
                        } else {
                            val movimento = primeiro.positionChange()

                            if (movimento != Offset.Zero) {
                                houveMovimento = true

                                onMove(
                                    movimento.x.toInt(),
                                    movimento.y.toInt()
                                )

                                primeiro.consume()
                            }
                        }
                    }
                } finally {
                    if (arrastando) {
                        onReleaseLeft()
                    }
                }

                if (!houveMovimento && !arrastando) {
                    if (posicaoInicial >= size.width * 0.75f) {
                        onRightClick()
                    } else {
                        val segundoToque = withTimeoutOrNull(500L) {
                            awaitFirstDown(requireUnconsumed = false)
                        }

                        if (segundoToque == null) {
                            onClick()
                        } else {
                            val segundaPosicao = segundoToque.position.x
                            var segundoMoveu = false

                            while (true) {
                                val evento = awaitPointerEvent()
                                val dedo = evento.changes.firstOrNull {
                                    it.id == segundoToque.id
                                }

                                if (dedo?.pressed != true) break

                                if (dedo.positionChange() != Offset.Zero) {
                                    segundoMoveu = true
                                }
                            }

                            if (!segundoMoveu &&
                                segundaPosicao < size.width * 0.75f
                            ) {
                                onDoubleClick()
                            } else {
                                onClick()

                                if (segundaPosicao >= size.width * 0.75f) {
                                    onRightClick()
                                }
                            }
                        }
                    }
                }
            }
        }
)



// BOTÕES INFERIORES

Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    // BOTÃO ESQUERDO - PRESSIONAR E SEGURAR
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(67.dp)
            .pointerInput(botoesInvertidos) {
                detectTapGestures(
                    onPress = {
                        if (botoesInvertidos) {
                            onBottomRightPress()
                        } else {
                            onBottomLeftPress()
                        }

                        try {
                            tryAwaitRelease()
                        } finally {
                            if (botoesInvertidos) {
                                onBottomRightRelease()
                            } else {
                                onBottomLeftRelease()
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF405565)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("ESQUERDO", color = Color.White)
        }
    }

    // TECLADO E INVERSÃO
    Column(
        modifier = Modifier.weight(0.5f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
       Surface(
    modifier = Modifier
        .fillMaxWidth()
        .height(34.dp)
        .clickable {
            mostrarTeclado = true
        },
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFF80CBC4)
) {
    Box(contentAlignment = Alignment.Center) {
        Text("⌨", color = Color.Black)
    }
}

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clickable {
                    botoesInvertidos = !botoesInvertidos
                },
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF80CBC4)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("⇄", color = Color.Black)
            }
        }
    }

    
    // BOTÃO DIREITO - CLIQUE NORMAL
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(67.dp)
            .clickable {
                if (botoesInvertidos) {
                    onBottomLeftClick()
                } else {
                    onBottomRightClick()
                }
            },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF405565)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("DIREITO", color = Color.White)
        }
    }
}

// JANELA DE DISPOSITIVOS BLUETOOTH
if (mostrarDispositivos) {
    AlertDialog(
        onDismissRequest = {
            mostrarDispositivos = false
        },
        title = {
            Text("Dispositivos pareados")
        },
        text = {
            if (dispositivosPareados.isEmpty()) {
                Text("Nenhum dispositivo pareado encontrado.")
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    dispositivosPareados.forEach { dispositivo ->
                        Text(
                            text = "${dispositivo.name ?: "Sem nome"}\n${dispositivo.address}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
    onConnectDevice(dispositivo)
    mostrarDispositivos = false
}
                                .padding(vertical = 10.dp),
                            color = Color.White
                        )

                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onLoadPairedDevices()
                }
            ) {
                Text("Atualizar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    mostrarDispositivos = false
                }
            ) {
                Text("Fechar")
            }
        }
    )
}
// JANELA DE DIGITAÇÃO
if (mostrarTeclado) {
    AlertDialog(
        onDismissRequest = {
            mostrarTeclado = false
        },
        title = {
            Text("Digitar texto")
        },
        text = {
           OutlinedTextField(
    value = textoDigitado,
    onValueChange = {
        textoDigitado = it
    },
    label = {
        Text("Digite seu texto")
    },
    modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester),
    minLines = 3
)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSendText(textoDigitado)
                    textoDigitado = ""
                    mostrarTeclado = false
                }
            ) {
                Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    mostrarTeclado = false
                }
            ) {
                Text("Cancelar")
            }
        }
    )
}
    }
}
}
