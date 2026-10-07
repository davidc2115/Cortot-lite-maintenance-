package fr.cortotelite.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.cortotelite.app.data.*
import fr.cortotelite.app.util.euro
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(repo: Repo, nav: NavController) {
    val employees by repo.employees().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Employés") },
                actions = {
                    IconButton(onClick = { nav.navigate("employee/0") }) {
                        Icon(Icons.Outlined.Add, "Ajouter")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(employees, key = { it.id }) { emp ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("employee/${emp.id}") }
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${emp.firstName} ${emp.lastName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${emp.role.name.lowercase().replaceFirstChar { it.uppercase() }} · ${emp.baseSalaryBrut.euro()} brut",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!emp.active) {
                                Text("Inactif", color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        IconButton(onClick = { nav.navigate("payslips/${emp.id}") }) {
                            Icon(Icons.Outlined.Payments, "Paie")
                        }
                    }
                }
            }
            if (employees.isEmpty()) {
                item {
                    Text(
                        "Aucun employé. Appuyez sur + pour en créer un.",
                        modifier = Modifier.padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeEditScreen(repo: Repo, id: Long, nav: NavController) {
    val scope = rememberCoroutineScope()
    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(EmployeeRole.TECHNICIEN) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var ssn by remember { mutableStateOf("") }
    var baseSalary by remember { mutableStateOf("") }
    var commissionPct by remember { mutableStateOf("") }
    var dividendPct by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id > 0) {
            val e = repo.employee(id)
            if (e != null) {
                lastName = e.lastName
                firstName = e.firstName
                role = e.role
                email = e.email
                phone = e.phone
                address = e.address
                iban = e.iban
                ssn = e.socialSecurityNumber
                baseSalary = if (e.baseSalaryBrut > 0) e.baseSalaryBrut.toString() else ""
                commissionPct = if (e.commissionPercent > 0) e.commissionPercent.toString() else ""
                dividendPct = if (e.dividendPercent > 0) e.dividendPercent.toString() else ""
                active = e.active
                notes = e.notes
            }
        }
        loaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Nouvel employé" else "Employé") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (id > 0) {
                        IconButton(onClick = {
                            scope.launch {
                                repo.employee(id)?.let { repo.deleteEmployee(it) }
                                nav.popBackStack()
                            }
                        }) { Icon(Icons.Outlined.Delete, "Supprimer") }
                    }
                    TextButton(onClick = {
                        scope.launch {
                            val emp = Employee(
                                id = id,
                                lastName = lastName.trim(),
                                firstName = firstName.trim(),
                                role = role,
                                email = email.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                iban = iban.trim(),
                                socialSecurityNumber = ssn.trim(),
                                baseSalaryBrut = baseSalary.toDoubleOrNull() ?: 0.0,
                                commissionPercent = commissionPct.toDoubleOrNull() ?: 0.0,
                                dividendPercent = dividendPct.toDoubleOrNull() ?: 0.0,
                                active = active,
                                notes = notes.trim()
                            )
                            repo.saveEmployee(emp)
                            nav.popBackStack()
                        }
                    }) { Text("Enregistrer") }
                }
            )
        }
    ) { pad ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(firstName, { firstName = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lastName, { lastName = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
            Text("Rôle", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EmployeeRole.entries.forEach { r ->
                    FilterChip(
                        selected = role == r,
                        onClick = { role = r },
                        label = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text("Adresse") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(iban, { iban = it }, label = { Text("IBAN") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ssn, { ssn = it }, label = { Text("N° Sécurité sociale") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(baseSalary, { baseSalary = it }, label = { Text("Salaire de base brut (€)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(commissionPct, { commissionPct = it }, label = { Text("Commission % (0 = société)") }, modifier = Modifier.fillMaxWidth())
            if (role == EmployeeRole.DIRIGEANT) {
                OutlinedTextField(dividendPct, { dividendPct = it }, label = { Text("Dividendes %") }, modifier = Modifier.fillMaxWidth())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(active, { active = it })
                Text("Actif")
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            if (id > 0) {
                Button(
                    onClick = { nav.navigate("payslips/$id") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Voir / générer fiches de paie") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipsScreen(repo: Repo, employeeId: Long, nav: NavController) {
    val payslips by repo.payslipsFor(employeeId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var empName by remember { mutableStateOf("") }
    val cal = Calendar.getInstance()
    var year by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(cal.get(Calendar.MONTH) + 1) }
    var generating by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(employeeId) {
        repo.employee(employeeId)?.let {
            empName = "${it.firstName} ${it.lastName}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paie — $empName") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    year.toString(),
                    { year = it.toIntOrNull() ?: year },
                    label = { Text("Année") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    month.toString(),
                    { month = (it.toIntOrNull() ?: month).coerceIn(1, 12) },
                    label = { Text("Mois") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = !generating,
                    onClick = {
                        generating = true
                        scope.launch {
                            val slip = repo.generatePayslip(employeeId, year, month)
                            msg = if (slip != null)
                                "Fiche ${slip.month}/${slip.year} — Net ${slip.netAPayer.euro()}"
                            else "Erreur génération"
                            generating = false
                        }
                    }
                ) {
                    if (generating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Générer")
                }
            }
            msg?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payslips, key = { it.id }) { slip ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${slip.month.toString().padStart(2, '0')}/${slip.year}",
                                    fontWeight = FontWeight.Bold
                                )
                                AssistChip(
                                    onClick = {
                                        scope.launch {
                                            val next = when (slip.status) {
                                                PayslipStatus.BROUILLON -> PayslipStatus.VALIDE
                                                PayslipStatus.VALIDE -> PayslipStatus.PAYE
                                                PayslipStatus.PAYE -> PayslipStatus.BROUILLON
                                            }
                                            repo.updatePayslipStatus(slip.id, next)
                                        }
                                    },
                                    label = { Text(slip.status.name) }
                                )
                            }
                            Text("Base : ${slip.baseBrut.euro()}  ·  Commission : ${slip.commissionBrut.euro()}")
                            Text("CA payé : ${slip.caPayeHt.euro()}  ·  Attente : ${slip.caEnAttenteHt.euro()}  ·  Retard : ${slip.caRetardHt.euro()}",
                                style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Net à payer : ${slip.netAPayer.euro()}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (payslips.isEmpty()) {
                    item {
                        Text(
                            "Aucune fiche. Choisissez mois/année et appuyez sur Générer.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}
