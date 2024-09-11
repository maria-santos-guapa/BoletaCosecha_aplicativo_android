package com.example.boletascosecha

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.wear.compose.material.ContentAlpha
import androidx.wear.compose.material.LocalContentAlpha
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import viewmodels.DatosGuardadosViewModel
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val isLoggedIn = rememberSaveable { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val viewModel: DatosGuardadosViewModel = viewModel(
                factory = DatosGuardadosViewModelFactory(applicationContext)
            )
            NavHost(navController, startDestination = "startScreen/{username}") {
                composable(
                    "formulario/{muestreo}/{bloque}/{username}",
                    arguments = listOf(
                        navArgument("muestreo") { type = NavType.IntType },
                        navArgument("bloque") { type = NavType.StringType },
                        navArgument("username") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val muestreo = backStackEntry.arguments?.getInt("muestreo")
                    val bloque = backStackEntry.arguments?.getString("bloque")
                    val usuario = backStackEntry.arguments?.getString("username")
                    muestreo?.let { muestreoValue ->
                        bloque?.let { bloqueValue ->
                            usuario?.let { usuarioValue ->
                                formulario(
                                    navController = navController,
                                    bloque = bloqueValue,
                                    viewModel = viewModel,
                                    username = usuarioValue
                                )
                            }
                        }
                    }
                }
                composable("panelControl/{username}",
                    arguments = listOf(
                        navArgument("username") { type = NavType.StringType }
                    )) {
                        backStackEntry ->
                    val username = backStackEntry.arguments?.getString("username")
                    username?.let { muestreoValue ->
                        panelControl(navController = navController,viewModel = viewModel,context = applicationContext, username = muestreoValue)
                    }
                }
                composable("startScreen/{username}",
                    arguments = listOf(
                        navArgument("username") { type = NavType.StringType }
                    )) { backStackEntry ->
                    val username = backStackEntry.arguments?.getString("username") ?: "organolepticas"
                    startScreen(navController = navController, viewModel = viewModel,context = applicationContext,username = username)
                }
            }
        }
    }
}
class DatosGuardadosViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatosGuardadosViewModel::class.java)) {
            return DatosGuardadosViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
private fun guardar(
    numBoleta: String,
    bloque: String,
    grupo_forza: String,
    bin: String,
    frutas: String,
    tipo_floracion: String,
    peso: String,
    cosecha: String,
    descarte: String,
    cuadrillero: String,
    fecha: String,
    fecha_muestreo: String,
    viewModel: DatosGuardadosViewModel,
    nuevaenfermedad: String,
    usuario: String,
): Boolean {
    return try {
        val nuevoDato = mapOf(
            "numero_boleta" to numBoleta,
            "bloque" to bloque,
            "grupo_forza" to grupo_forza,
            "bin" to bin,
            "frutas" to frutas,
            "tipo_floracion" to tipo_floracion,
            "peso" to peso,
            "tipo_cosecha" to cosecha,
            "cuadrillero" to cuadrillero,
            "descarte" to descarte,
            "fecha" to fecha,
            "observaciones" to nuevaenfermedad,
            "fecha_muestreo" to fecha_muestreo,
            "usuario" to usuario
        )
        viewModel.agregarDato(nuevoDato)
        println("Exitoso")
        println("Valores guardados: $nuevoDato")
        true // Retorna true si se guardó exitosamente
    } catch (e: Exception) {
        println("Fallo: $e")
        false // Retorna false si ocurrió un fallo al guardar
    }
}
private fun fecha_ahora(): String {
    val currentDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return currentDateTime.format(formatter)
}
@Composable
fun DropdownMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            )
            .then(modifier)
    ) {
        CompositionLocalProvider(LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled) {
            Row(
                modifier = Modifier.padding(contentPadding)
            ) {
                ProvideTextStyle(TextStyle.Default) {
                    content()
                }
            }
        }
    }
}
@Composable
fun formulario(
    bloque: String,
    viewModel: DatosGuardadosViewModel,
    navController: NavController,
    username: String
): Boolean {
    val cal = Calendar.getInstance()
    val añoActual = cal.get(Calendar.YEAR).toString()
    val mesActual = (cal.get(Calendar.MONTH) + 1).toString() // Se suma 1 porque los meses van de 0 a 11
    val diaActual = cal.get(Calendar.DAY_OF_MONTH).toString()
    var nuevoAnio by remember { mutableStateOf(añoActual) }
    var nuevoDia by remember { mutableStateOf(diaActual) }
    var nuevoMes by remember { mutableStateOf(mesActual) }
    val muestreosActuales = viewModel.obtenerDatosGuardados().filter { it["Origen"] != "Web" }
    val muestreoActual = muestreosActuales.count { it["bloque"] == bloque }.toInt() + 1
    var guardadoExitoso = false
    var conteoLeveCochinilla by remember { mutableStateOf(0) }
    var conteoModeradoCochinilla by remember { mutableStateOf(0) }
    var conteoSeveroCochinilla by remember { mutableStateOf(0) }

    var bin by remember { mutableStateOf("") }
    var frutas by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var nuevaenfermedadSelected by remember { mutableStateOf("") }
    var cosechaSelected by remember { mutableStateOf("") }
    var dropdownCosechaExpanded = remember { mutableStateOf(false) }
    val cosechaOptions = listOf("Mécanica","Manual","")
    var descarteSelected by remember { mutableStateOf("") }
    var dropdownDescarteExpanded = remember { mutableStateOf(false) }
    val descarteOptions = listOf("Si","No","")
    var floracionSelected by remember { mutableStateOf("") }
    var dropdownFloracionExpanded = remember { mutableStateOf(false) }
    val floracionOptions = listOf("Inducción","Natural","")
    var selectedBlock1 by remember { mutableStateOf("") }
    val filteredData = viewModel.obtenerDatosGuardados().filter { it["Aplicacion"] == "consulta_bloques" }
    val consulta = filteredData.filter {
        it["bloque"] == selectedBlock1
    }
    println(consulta)
    val poblacion = consulta.maxByOrNull { it["poblacion"] as? Int ?: 0 }?.get("poblacion") as? String ?: "0.0"
    val area = consulta.maxByOrNull { it["area_bruta"] as? String ?: "" }?.get("area_bruta") as? String ?: "0.0"

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .background(Color.White)
    ) {
        // Sección de "Registrar Hallazgos"
        Text(
            text = "Registro Boleta",
            fontSize = 30.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bloque : $selectedBlock1",
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Área Neta : $area [Ha]",
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Población : $poblacion ",
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        val fechaActual = LocalDate.now()

        // Obtener el año, mes y día
        val añoCompleto = fechaActual.year
        val mes = fechaActual.monthValue
        val dia = fechaActual.dayOfMonth

        // Formatear el año (solo últimos dos dígitos)
        val año = añoCompleto.toString().takeLast(2)

        // Formatear mes y día a dos dígitos
        val mesFormatted = String.format("%02d", mes)
        val diaFormatted = String.format("%02d", dia)

        val numBoleta = "$año$mesFormatted$diaFormatted$selectedBlock1"
        Text(
            text = "Número Boleta: $numBoleta",
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        var grupo_forza by remember { mutableStateOf("") }
        val grupos = viewModel.obtenerDatosGuardados()
            .map { it["grupo_forza"] as? String }
            .filterNotNull() // Elimina valores nulos
            .distinct() // Elimina valores duplicados


        TextField(
            value = grupo_forza,
            onValueChange = { grupo_forza = it },
            label = { Text("Ingrese el grupo forza") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 7.dp).border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp)),
            singleLine = true
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp).horizontalScroll(rememberScrollState())
        ) {
            Text("Grupos forza disponibles:", fontWeight = FontWeight.Bold)
            if (grupo_forza.isNotBlank()) {
                Row {
                    grupos.filter { it.contains(grupo_forza, ignoreCase = true) }.take(4).forEach { block ->
                        ClickableText(
                            text = AnnotatedString(block),
                            onClick = { grupo_forza = block },
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
        val blocks1 = viewModel.obtenerDatosGuardados()
            .filter{it["grupo_forza"] == grupo_forza}
            .mapNotNull { it["bloque"] as? String } // Mapea y filtra nulos
            .distinct() // Elimina duplicados


        TextField(
            value = selectedBlock1,
            onValueChange = { selectedBlock1 = it },
            label = { Text("Ingrese el bloque") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 7.dp).border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp)),
            singleLine = true
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp).horizontalScroll(rememberScrollState())
        ) {
            Text("Bloques disponibles:", fontWeight = FontWeight.Bold)
            if (selectedBlock1.isNotBlank()) {
                Row {
                    blocks1.filter { it.contains(selectedBlock1, ignoreCase = true) }.take(4).forEach { block ->
                        ClickableText(
                            text = AnnotatedString(block),
                            onClick = { selectedBlock1 = block },
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = bin,
            onValueChange = {
                // Check if the input is a valid number
                val number = it.toIntOrNull()
                if (number != null && number in 0..200) {
                    bin = it
                }
            },
            label = { Text("Número de Bin : ") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus() // Clear focus to hide the keyboard
                }
            )
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = frutas,
            onValueChange = {
                // Check if the input is a valid number
                val number = it.toIntOrNull()
                if (number != null && number in 0..30000) {
                    frutas = it
                }
            },
            label = { Text("Número de Frutas : ") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus() // Clear focus to hide the keyboard
                }
            )
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(color = Color.White, shape = RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
                .height(52.dp)
                .clickable {
                    dropdownFloracionExpanded.value = true
                    focusManager.clearFocus()
                }
        ) {
            Text(
                text = "Tipo de Floración: $floracionSelected",
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .fillMaxWidth()
            )
            DropdownMenu(
                expanded = dropdownFloracionExpanded.value,
                onDismissRequest = { dropdownFloracionExpanded.value = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                floracionOptions.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            floracionSelected = option
                            dropdownFloracionExpanded.value = false
                        }
                    ) {
                        Text(text = option, fontSize = 25.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(color = Color.White, shape = RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
                .height(52.dp)
                .clickable {
                    dropdownCosechaExpanded.value = true
                    focusManager.clearFocus()
                }
        ) {
            Text(
                text = "Tipo de Cosecha: $cosechaSelected",
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .fillMaxWidth().clickable(onClick = {
                        dropdownCosechaExpanded.value = true
                    })
            )
            DropdownMenu(
                expanded = dropdownCosechaExpanded.value,
                onDismissRequest = { dropdownCosechaExpanded.value = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                cosechaOptions.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            cosechaSelected = option
                            dropdownCosechaExpanded.value = false
                        },
                        modifier = Modifier.fillMaxWidth().clickable {
                            cosechaSelected = option
                            dropdownCosechaExpanded.value = false
                        }
                    ) {
                        Text(
                            text = option,
                            fontSize = 25.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(color = Color.White, shape = RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
                .height(52.dp)
                .clickable {
                    dropdownDescarteExpanded.value = true
                    focusManager.clearFocus()
                }
        ) {
            Text(
                text = "Descarte: $descarteSelected",
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .fillMaxWidth().clickable(onClick = {
                        dropdownDescarteExpanded.value = true
                    })
            )
            DropdownMenu(
                expanded = dropdownDescarteExpanded.value,
                onDismissRequest = { dropdownDescarteExpanded.value = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                descarteOptions.forEach { option ->
                    DropdownMenuItem(
                        onClick = {
                            descarteSelected = option
                            dropdownDescarteExpanded.value = false
                        },
                        modifier = Modifier.fillMaxWidth().clickable {
                            descarteSelected = option
                            dropdownDescarteExpanded.value = false
                        }
                    ) {
                        Text(
                            text = option,
                            fontSize = 25.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        var selectedCuadrillero by remember { mutableStateOf("") }
        val Cuadrillero = viewModel.obtenerDatosGuardados()
            .filter { it["Aplicacion"] == "cuadrilleros" }
            .mapNotNull { it["cuadrillero"] as? String } // Mapea y filtra nulos
            .distinct() // Elimina duplicados


        TextField(
            value = selectedCuadrillero,
            onValueChange = { selectedCuadrillero = it },
            label = { Text("Ingrese el cuadrillero") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 7.dp).border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp)),
            singleLine = true
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp).horizontalScroll(rememberScrollState())
        ) {
            Text("Cuadrilleros Disponibles:", fontWeight = FontWeight.Bold)
            if (selectedCuadrillero.isNotBlank()) {
                Row {
                    Cuadrillero.filter { it.contains(selectedCuadrillero, ignoreCase = true) }.take(4).forEach { block ->
                        ClickableText(
                            text = AnnotatedString(block),
                            onClick = { selectedCuadrillero = block },
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Fecha Muestra Actual:",
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Campo para el año (4 dígitos)
            OutlinedTextField(
                value = nuevoAnio,
                onValueChange = { nuevoAnio = it.take(4) }, // Limitar a 4 dígitos
                label = { Text("Año") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            // Campo para el mes (2 dígitos)
            OutlinedTextField(
                value = nuevoMes,
                onValueChange = {
                    val nuevoValor = it.take(2).toIntOrNull() // Convertir a Int
                    nuevoMes = when {
                        nuevoValor == null -> "" // Si la conversión falla, establecer el valor como vacío
                        nuevoValor < 1 -> "01" // Si es menor que 1, establecer como "01"
                        nuevoValor > 12 -> "12" // Si es mayor que 12, establecer como "12"
                        else -> "%02d".format(nuevoValor) // Formatear como dos dígitos con ceros a la izquierda
                    }
                },
                label = { Text("Mes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )


            // Campo para el día (2 dígitos)
            OutlinedTextField(
                value = nuevoDia,
                onValueChange = {
                    val nuevoValor = it.take(2).toIntOrNull() // Convertir a Int
                    nuevoDia = when {
                        nuevoValor == null -> "" // Si la conversión falla, establecer el valor como vacío
                        nuevoValor < 1 -> "01" // Si es menor que 1, establecer como "01"
                        nuevoValor > 31 -> "31" // Si es mayor que 31, establecer como "31"
                        else -> "%02d".format(nuevoValor) // Formatear como dos dígitos con ceros a la izquierda
                    }
                },
                label = { Text("Día") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

        }


        var nuevaFecha = "$nuevoAnio-$nuevoMes-$nuevoDia"

        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = nuevaenfermedadSelected,
            onValueChange = { nuevaenfermedadSelected = it }, // Actualización de la variable
            label = { Text("Otras novedades y observaciones adicionales") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Fecha Sistema",
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${fecha_ahora()}",
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Botones de guardar y volver
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            val frutasInt = frutas.toIntOrNull() ?: 0
            Button(
                onClick = {
                    guardadoExitoso = guardar(
                        numBoleta = numBoleta,
                        bloque = selectedBlock1,
                        grupo_forza = grupo_forza,
                        bin = bin,
                        frutas = frutas,
                        cuadrillero = selectedCuadrillero,
                        tipo_floracion = floracionSelected,
                        peso = (frutasInt*1800).toString(),
                        cosecha = cosechaSelected,
                        descarte = descarteSelected,
                        fecha = fecha_ahora(),
                        fecha_muestreo = nuevaFecha,
                        viewModel = viewModel,
                        nuevaenfermedad = nuevaenfermedadSelected,
                        usuario = username,
                    )
                    if (guardadoExitoso) {
                        selectedBlock1 = ""
                        bin = ""
                        frutas = ""
                        selectedCuadrillero = ""
                        floracionSelected = ""
                        peso = ""
                        cosechaSelected = ""
                        descarteSelected = ""
                        nuevaenfermedadSelected = ""

                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(text = "Guardar")
            }
            Button(
                onClick = {
                    navController.navigate("startScreen/$username")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Volver a la pantalla principal")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.logi),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = "Powered by Guapa \n Versión 1.0",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
        }
    }
    return true
}
@Composable
fun startScreen(navController: NavController, viewModel: DatosGuardadosViewModel, context: Context,username: String) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val datos = viewModel.datosGuardados.filter { it["Origen"] != "Web" }
    var showDialog by remember { mutableStateOf(false) }
    var showDialogNew by remember { mutableStateOf(false) }
    var showDialogMuestras by remember { mutableStateOf(false) }
    if (showDialogMuestras) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog if the user clicks outside of it or presses the back button
                showDialogMuestras = false
            },
            title = {
                Text(text = "¿Está seguro de eliminar todas las muestras?")
                //TODO
                // "¿Está seguro de eliminar el bloque $bloque con fecha muestra $fecha_muestra y $cantidad de registros?"
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialogMuestras = false
                        viewModel.borrarTodosLosDatosGuardadosNoWeb()
                    }
                ) {
                    Text(text = "Aceptar")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDialogMuestras = false
                    }
                ) {
                    Text(text = "Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Agricola Guapa SAS \n Registro de Boletas",
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                navController.navigate("formulario/1/pc/user")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Registrar Boleta")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
            navController.navigate("panelControl/user")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Boletas Registradas")
        }
        Spacer(modifier = Modifier.height(16.dp))
        var showDialogData by remember { mutableStateOf(false) }
        if (showDialogData) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = "Se están actualizando los datos. Por favor, espere...") },
                confirmButton = {}
            )
        }
        Button(
            onClick = {
                showDialogData = true
                updateBlocksConsulta(viewModel) { success ->
                    showDialogData = !success
                    println(success)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Actualizar Datos")
        }
        Spacer(modifier = Modifier.height(16.dp))
        var showDialogData1 by remember { mutableStateOf(false) }
        if (showDialogData1) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = "Se están actualizando los datos. Por favor, espere...") },
                confirmButton = {}
            )
        }
        Button(
            onClick = {
                showDialogData1 = true
                updateCuadrilleros(viewModel) { success ->
                    showDialogData1 = !success
                    println(success)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Actualizar Cuadrilleros")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                navController.navigate("consultaBloques/$username")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Cargar Boletas")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                navController.navigate("consultaBloques/$username")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Generar Excel")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(id = R.drawable.logi),
            contentDescription = null,
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = "Powered by Guapa\nVersión 2.0",
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
fun updateBlocksConsulta(viewModel: DatosGuardadosViewModel, callback: (Boolean) -> Unit) {
    viewModel.borrarTodosLosDatosObservacion()
    val client = OkHttpClient.Builder()
        .callTimeout(200, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("http://controlgestionguapa.ddns.net:8000/consultor/api_get_consulta_bloques_boletas")
        .build()

    var success = false // Booleano para indicar el éxito de la operación

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()

            // Ocultar la barra de progreso en caso de fallo
            viewModel.setProgressVisible(false)
            callback(false) // Llamamos a la función de devolución de llamada con false
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseString = response.body?.string()
                val cleanedResponseString = responseString?.replace("NaN", "\" \"") // Reemplazar NaN por cadena vacía
                val jsonArray = cleanedResponseString?.let { JSONArray(it) } ?: JSONArray()
                var i = 0
                var saveSuccessful = true
                while (i < jsonArray.length() && saveSuccessful) {
                    val elemento = jsonArray.getJSONObject(i)
                    saveSuccessful = saveDataWebConsulta(elemento = elemento, viewModel = viewModel)
                    i++
                    println("Holi $i")
                }
                success = saveSuccessful // Establecer success basado en si todos los datos se guardaron correctamente
                callback(success) // Llamamos a la función de devolución de llamada con el valor final de success
            }
        }
    })
}
fun saveDataWebConsulta(elemento: JSONObject, viewModel: DatosGuardadosViewModel): Boolean {
    return try {
        val nuevoDato = mapOf(
            "Fecha_Cargue" to fecha_ahora(),
            "Aplicacion" to "consulta_bloques",
            "bloque" to elemento.optString("bloque", ""),
            "area_bruta" to elemento.optString("area_bruta", ""),
            "area_neta" to elemento.optString("area_neta", ""),
            "fecha_forza" to elemento.optString("fecha_forza", ""),
            "fecha_siembra" to elemento.optString("fecha_siembra", ""),
            "frutas" to elemento.optString("frutas", ""),
            "grupo_forza" to elemento.optString("grupo_forza", ""),
            "grupo_siembra" to elemento.optString("grupo_siembra", ""),
            "max" to elemento.optString("max", ""),
            "min" to elemento.optString("min", ""),
            "peso" to elemento.optString("peso", ""),
            "poblacion" to elemento.optInt("poblacion", 0).toString(), // Convertir a String
            "rango_semilla" to elemento.optString("rango_semilla", ""),
            "razon" to elemento.optString("razon", "")

        )
        viewModel.agregarDato(nuevoDato)
        true // Retorna true si se guardó exitosamente
    } catch (e: Exception) {
        println("Fallo: $e")
        false // Retorna false si ocurrió un fallo al guardar
    }
}
fun updateCuadrilleros(viewModel: DatosGuardadosViewModel, callback: (Boolean) -> Unit) {
    viewModel.borrarTodosLosDatos()
    val client = OkHttpClient.Builder()
        .callTimeout(200, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("http://controlgestionguapa.ddns.net:8000/consultor/api_cuadrilleros")
        .build()

    var success = false // Booleano para indicar el éxito de la operación

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()

            // Ocultar la barra de progreso en caso de fallo
            viewModel.setProgressVisible(false)
            callback(false) // Llamamos a la función de devolución de llamada con false
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseString = response.body?.string()
                val cleanedResponseString = responseString?.replace("NaN", "\" \"") // Reemplazar NaN por cadena vacía
                val jsonArray = cleanedResponseString?.let { JSONArray(it) } ?: JSONArray()
                var i = 0
                var saveSuccessful = true
                while (i < jsonArray.length() && saveSuccessful) {
                    val elemento = jsonArray.getJSONObject(i)
                    saveSuccessful = saveDataCuadrilleros(elemento = elemento, viewModel = viewModel)
                    i++
                    println("Holi $i")
                }
                success = saveSuccessful // Establecer success basado en si todos los datos se guardaron correctamente
                callback(success) // Llamamos a la función de devolución de llamada con el valor final de success
            }
        }
    })
}
fun saveDataCuadrilleros(elemento: JSONObject, viewModel: DatosGuardadosViewModel): Boolean {
    return try {
        val nuevoDato = mapOf(
            "Fecha_Cargue" to fecha_ahora(),
            "Aplicacion" to "cuadrilleros",
            "cuadrilla" to elemento.optString("cuadrilla", ""),
            "cuadrillero" to elemento.optString("cuadrillero", ""),
            "documentos" to elemento.optString("documentos", ""),
            "id" to elemento.optString("id", "")
        )
        viewModel.agregarDato(nuevoDato)
        true // Retorna true si se guardó exitosamente
    } catch (e: Exception) {
        println("Fallo: $e")
        false // Retorna false si ocurrió un fallo al guardar
    }
}
@Composable
fun panelControl(
    viewModel: DatosGuardadosViewModel,
    navController: NavController,
    context: Context,
    username: String
) {
    val datosGuardados = viewModel.datosGuardados.filter { it["Aplicacion"] != "consulta_bloques" }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
    ) {
        Text(
            text = "Resumen\nBoletas Registradas",
            fontSize = 30.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "día de consulta : ${obtenerFecha(fecha_ahora())}",
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        val filters = remember { mutableStateOf("") }

        val headers = arrayOf("Número de Boleta","Bloque","#Bin", "Tipo de Floración", "Tipo de Cosecha","Frutas","Peso [g]","Descarte","Cuadrillero","Eliminar")
        val data = datosGuardados.map { rowData ->
            val numBoleta = rowData["numero_boleta"] ?: ""
            val bloque = rowData["bloque"] ?: ""
            val numMuestras = rowData["bin"] ?: ""
            val pesoActual = rowData["tipo_floracion"] ?: ""
            val fecha = rowData["tipo_cosecha"] ?: ""
            val meristemo = rowData["frutas"] ?: ""
            val fusarium = rowData["peso"] ?: ""
            val sistema = rowData["descarte"] ?: ""
            val hallazgos = rowData["cuadrillero"] ?:""
            val acciones = "Eliminar Muestra"
            arrayOf(numBoleta,bloque,numMuestras, pesoActual,fecha,meristemo,fusarium,sistema,hallazgos,acciones)
        }.toTypedArray()

        val datos: Array<Array<String>> = data.map { it.map { it.toString() }.toTypedArray() }.toTypedArray()
        Button(
            onClick = {
                navController.navigate("startScreen/$username")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Volver a la pantalla principal")
        }

        Table(
            filter = filters,
            headers = headers,
            data = datos,
            viewModel = viewModel,
            numBottoms = 2,
            username = username,
            navController = navController// Alinear celdas a la izquierda
        )

    }
}
fun obtenerFecha(fechaHora: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val localDate = LocalDate.parse(fechaHora, formatter)
    return localDate.toString()
}
@Composable
fun Table(filter: MutableState<String>, headers: Array<String>, data: Array<Array<String>>, viewModel: DatosGuardadosViewModel, username: String, numBottoms: Int, navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = filter.value,
            onValueChange = { filter.value = it },
            label = { Text("Filtro por palabra clave") },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            LazyColumn {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        headers.forEachIndexed { index, header ->
                            val isButton = false
                            val bloque = if (isButton) "" else "Bloque"
                            TableCell(text = header, isButton = isButton, viewModel = viewModel, bloque = bloque,fecha = "",   usuario = username ,navController = navController)
                        }
                    }
                }
                items(data.filter { it.any { item -> item.contains(filter.value, ignoreCase = true) } }) { rowData ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowData.forEachIndexed { index, cellData ->
                            val isButton = index >= rowData.size - numBottoms
                            val bloque = rowData[1]
                            TableCell(text = cellData, isButton = isButton, viewModel = viewModel, bloque = bloque,usuario = username,fecha = rowData[0], navController = navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    isButton: Boolean = false,
    viewModel: DatosGuardadosViewModel,
    bloque: String,
    fecha: String,
    usuario: String,
    navController: NavController
) {
    val context = LocalContext.current

    val showDialog = remember { mutableStateOf(false) }
    var eliminandoBloque by remember { mutableStateOf(false) }
    var eliminandoMuestra by remember { mutableStateOf(false) }

    val cellModifier = if (isButton) {
        Modifier
            .padding(8.dp)
            .width(150.dp)
    } else {
        Modifier
            .padding(8.dp)
            .width(100.dp)
    }

    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isButton) {
                when (text) {
                    "Eliminar" -> {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = {
                                    showDialog.value = true
                                    eliminandoBloque = true
                                })
                                .background(Color.Transparent)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.images),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                            )
                        }
                    }
                    "Editar" -> {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = { navController.navigate("formularioEditar/$fecha/$bloque/$usuario") })
                                .background(Color.Transparent)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.images1),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                            )
                        }
                    }
                    "Eliminar Muestra" -> {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = {
                                    showDialog.value = true
                                    eliminandoMuestra = true
                                })
                                .background(Color.Transparent)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.images),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = text,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = {
                // Título dependiendo del contexto
                if (eliminandoBloque) {
                    Text("¿Desea eliminar todas las muestras del bloque?")
                } else {
                    Text("¿Desea eliminar la muestra del bloque $bloque del $fecha?")
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Lógica de eliminación dependiendo del contexto
                    if (eliminandoBloque) {
                        viewModel.borrarDatoPorBloque(bloque = bloque)
                    } else {
                        viewModel.borrarDatoPorFechaOrigenYBloque(fecha = fecha, bloque = bloque)
                    }
                    showDialog.value = false
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog.value = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

}