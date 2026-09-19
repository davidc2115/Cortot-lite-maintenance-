package fr.cortotelite.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.cortotelite.app.data.Client
import fr.cortotelite.app.data.ClientType
import fr.cortotelite.app.data.Document
import fr.cortotelite.app.data.DocumentKind
import fr.cortotelite.app.data.DocumentLine
import fr.cortotelite.app.data.DocumentStatus
import fr.cortotelite.app.data.Installation
import fr.cortotelite.app.data.Repo
import fr.cortotelite.app.data.Travel
import fr.cortotelite.app.data.TravelMode
import fr.cortotelite.app.util.euro
import fr.cortotelite.app.util.Distance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(repo: Repo, nav: NavController) {
    val clients by repo.clients().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Cortot Élite — Clients") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("client/0") }) {
                Icon(Icons.Outlined.Add, "Nouveau")
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            items(clients, key = { it.id }) { c ->
                Card(Modifier.padding(12.dp).fillMaxWidth().clickable { nav.navigate("client/${c.id}") }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.displayName(), style = MaterialTheme.typography.titleMedium)
                        Text(if (c.type == ClientType.PROFESSIONNEL) "Professionnel" else "Particulier")
                        if (c.phoneMobile.isNotBlank()) Text("Port. ${c.phoneMobile}")
                        if (c.siteAddress.isNotBlank()) Text("Chantier : ${c.siteAddress}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(repo: Repo, id: Long, nav: NavController) {
    val loaded by repo.clientWithInstall(id).collectAsState(null)
    var last by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ClientType.PARTICULIER) }
    var billing by remember { mutableStateOf("") }
    var site by remember { mutableStateOf("") }
    var land by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("") }
    var invBrand by remember { mutableStateOf("") }
    var invModel by remember { mutableStateOf("") }
    var invCount by remember { mutableStateOf("1") }
    var panelBrand by remember { mutableStateOf("") }
    var panelCount by remember { mutableStateOf("") }
    var ladder by remember { mutableStateOf(false) }
    var roof by remember { mutableStateOf("") }
    var access by remember { mutableStateOf("") }
    var distanceKm by remember { mutableStateOf("") }
    var distanceStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(loaded) {
        val c = loaded?.client ?: return@LaunchedEffect
        last = c.lastName; first = c.firstName; company = c.companyName
        type = c.type; billing = c.billingAddress; site = c.siteAddress
        land = c.phoneLandline; mobile = c.phoneMobile; email = c.email; notes = c.notes
        distanceKm = if (c.distanceKm > 0) c.distanceKm.toString() else ""
        loaded?.installations?.firstOrNull()?.let {
            power = if (it.powerKwc == 0.0) "" else it.powerKwc.toString()
            invBrand = it.inverterBrand; invModel = it.inverterModel
            invCount = it.inverterCount.toString(); panelBrand = it.panelBrand
            panelCount = if (it.panelCount == 0) "" else it.panelCount.toString()
            ladder = it.needsLadder; roof = it.roofType; access = it.accessNotes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Nouvelle fiche" else "Fiche client") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == ClientType.PARTICULIER, onClick = { type = ClientType.PARTICULIER }, label = { Text("Particulier") })
                FilterChip(selected = type == ClientType.PROFESSIONNEL, onClick = { type = ClientType.PROFESSIONNEL }, label = { Text("Professionnel") })
            }
            if (type == ClientType.PROFESSIONNEL) Field("Société / enseigne", company) { company = it }
            Field("Nom", last) { last = it }
            Field("Prénom", first) { first = it }
            Field("Adresse facturation", billing) { billing = it }
            Field("Adresse chantier", site) { site = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Field("Distance aller (km)", distanceKm, KeyboardType.Decimal, Modifier.weight(1f)) { distanceKm = it }
                OutlinedButton(onClick = {
                    scope.launch {
                        distanceStatus = "Calcul…"
                        // Sauvegarde temporaire des adresses pour le calcul
                        val tmpId = if (id == 0L) {
                            repo.saveClient(
                                Client(
                                    type = type, lastName = last, firstName = first, companyName = company,
                                    billingAddress = billing, siteAddress = site, phoneLandline = land,
                                    phoneMobile = mobile, email = email, notes = notes
                                ),
                                Installation(clientId = 0, accessNotes = access)
                            )
                        } else id
                        val km = repo.refreshClientDistance(context, tmpId)
                        if (km != null) {
                            distanceKm = km.toString()
                            distanceStatus = "OK · ${km} km (aller) · A/R ${(km * 2)}"
                        } else {
                            distanceStatus = "Échec géocodage — vérifie l'adresse société (Paramètres) et le chantier"
                        }
                    }
                }) { Text("Auto") }
            }
            if (distanceStatus != null) Text(distanceStatus!!, style = MaterialTheme.typography.bodySmall)
            Field("Téléphone fixe", land, KeyboardType.Phone) { land = it }
            Field("Téléphone portable", mobile, KeyboardType.Phone) { mobile = it }
            Field("E-mail", email, KeyboardType.Email) { email = it }
            Text("Installation photovoltaïque", style = MaterialTheme.typography.titleMedium)
            Field("Puissance (kWc)", power, KeyboardType.Decimal) { power = it }
            Field("Onduleur — marque", invBrand) { invBrand = it }
            Field("Onduleur — modèle", invModel) { invModel = it }
            Field("Nombre d'onduleurs", invCount, KeyboardType.Number) { invCount = it }
            Field("Panneaux — marque", panelBrand) { panelBrand = it }
            Field("Nombre de panneaux", panelCount, KeyboardType.Number) { panelCount = it }
            Field("Type de toiture", roof) { roof = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(ladder, { ladder = it })
                Text("Échelle nécessaire")
            }
            Field("Accès / contraintes chantier", access) { access = it }
            Field("Notes", notes) { notes = it }
            Button(onClick = {
                scope.launch {
                    val client = Client(
                        id = if (id == 0L) 0 else id, type = type, lastName = last, firstName = first,
                        companyName = company, billingAddress = billing, siteAddress = site,
                        phoneLandline = land, phoneMobile = mobile, email = email, notes = notes,
                        distanceKm = distanceKm.replace(",", ".").toDoubleOrNull() ?: 0.0
                    )
                    val inst = Installation(
                        clientId = client.id, powerKwc = power.toDoubleOrNull() ?: 0.0,
                        inverterBrand = invBrand, inverterModel = invModel,
                        inverterCount = invCount.toIntOrNull() ?: 1,
                        panelBrand = panelBrand, panelCount = panelCount.toIntOrNull() ?: 0,
                        needsLadder = ladder, roofType = roof, accessNotes = access
                    )
                    repo.saveClient(client, inst)
                    nav.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
            if (id != 0L) {
                OutlinedButton(onClick = { nav.navigate("docs") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Voir devis / factures")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(repo: Repo, nav: NavController) {
    val docs by repo.documents().collectAsState(emptyList())
    val clients by repo.clients().collectAsState(emptyList())
    val company by repo.company().collectAsState(null)
    val scope = rememberCoroutineScope()
    var pick by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(DocumentKind.DEVIS) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Devis & factures") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { pick = true }) { Icon(Icons.Outlined.Add, null) }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(kind == DocumentKind.DEVIS, { kind = DocumentKind.DEVIS }, label = { Text("Devis") })
                FilterChip(kind == DocumentKind.FACTURE, { kind = DocumentKind.FACTURE }, label = { Text("Factures") })
            }
            if (pick) {
                Card(Modifier.padding(12.dp).fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Nouveau ${if (kind == DocumentKind.DEVIS) "devis" else "facture"} — choisir un client")
                        clients.forEach { c ->
                            TextButton(onClick = {
                                scope.launch {
                                    val vat = company?.defaultVatRate ?: 20.0
                                    val newId = repo.createDocument(c.id, kind, vat, "Maintenance PV")
                                    pick = false
                                    nav.navigate("doc/$newId")
                                }
                            }) { Text(c.displayName()) }
                        }
                        TextButton(onClick = { pick = false }) { Text("Annuler") }
                    }
                }
            }
            LazyColumn {
                items(docs.filter { it.kind == kind }, key = { it.id }) { d ->
                    val client = clients.find { it.id == d.clientId }
                    Card(Modifier.padding(12.dp).fillMaxWidth().clickable { nav.navigate("doc/${d.id}") }) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${d.number} · ${d.status}", style = MaterialTheme.typography.titleMedium)
                            Text(client?.displayName().orEmpty())
                            Text(d.title)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(repo: Repo, id: Long, nav: NavController) {
    val packed by repo.documentWithLines(id).collectAsState(null)
    val clients by repo.clients().collectAsState(emptyList())
    val company by repo.company().collectAsState(null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var travelMsg by remember { mutableStateOf<String?>(null) }
    var label by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var priceIsTtc by remember { mutableStateOf(false) }
    val doc = packed?.document
    val lines = packed?.lines.orEmpty()
    val client = clients.find { it.id == doc?.clientId }
    val preferTtc = client?.type == ClientType.PARTICULIER
    val totals = repo.documentTotals(lines, doc?.vatRate ?: 20.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.number ?: "Document") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                },
                actions = {
                    if (doc != null) {
                        IconButton(onClick = { scope.launch { repo.deleteDocument(doc); nav.popBackStack() } }) {
                            Icon(Icons.Outlined.Delete, null)
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (doc == null) { Text("Chargement…"); return@Column }
            Text(client?.displayName().orEmpty(), style = MaterialTheme.typography.titleMedium)
            Text("${if (doc.kind == DocumentKind.DEVIS) "Devis" else "Facture"} · ${doc.status}")
            Text("TVA ${doc.vatRate.toInt()} % — saisie ${if (preferTtc) "orientée TTC (particulier)" else "orientée HT (pro)"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.0, 10.0, 20.0).forEach { rate ->
                    FilterChip(doc.vatRate == rate, {
                        scope.launch { repo.updateDocument(doc.copy(vatRate = rate)) }
                    }, label = { Text("${rate.toInt()} %") })
                }
            }
            lines.forEach { line ->
                val lineHt = line.quantity * line.unitPriceHt
                val b = fr.cortotelite.app.util.breakdown(lineHt, doc.vatRate)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.label)
                        Text("${line.quantity} × ${line.unitPriceHt.euro()} HT  →  ${b.ht.euro()} HT / ${b.ttc.euro()} TTC")
                    }
                    IconButton(onClick = { scope.launch { repo.deleteLine(line) } }) {
                        Icon(Icons.Outlined.Delete, null)
                    }
                }
            }
            Field("Prestation", label) { label = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Qté", qty, KeyboardType.Decimal, Modifier.weight(1f)) { qty = it }
                Field(if (priceIsTtc) "Prix TTC" else "Prix HT", price, KeyboardType.Decimal, Modifier.weight(1f)) { price = it }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(priceIsTtc, { priceIsTtc = it })
                Text("Le prix saisi est TTC")
            }
            Button(onClick = {
                val raw = price.replace(",", ".").toDoubleOrNull() ?: return@Button
                val ht = if (priceIsTtc) fr.cortotelite.app.util.htFromTtc(raw, doc.vatRate) else raw
                scope.launch {
                    repo.addLine(
                        DocumentLine(
                            documentId = doc.id, label = label.ifBlank { "Prestation" },
                            quantity = qty.replace(",", ".").toDoubleOrNull() ?: 1.0,
                            unitPriceHt = ht
                        )
                    )
                    label = ""; price = ""; qty = "1"
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Ajouter la ligne") }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total HT  ${totals.ht.euro()}")
                    Text("TVA ${totals.rate.toInt()} %  ${totals.vat.euro()}")
                    Text("Total TTC  ${totals.ttc.euro()}", style = MaterialTheme.typography.titleMedium)
                }
            }
            // Déplacement automatique basé sur la distance client
            val rate = company?.travelRatePerKmHt ?: 0.80
            val oneWay = client?.distanceKm ?: 0.0
            val rtDefault = company?.travelRoundTripDefault != false
            if (client != null) {
                Text(
                    if (oneWay > 0)
                        "Distance en cache : ${oneWay} km aller · tarif ${rate} € HT/km"
                    else
                        "Pas de distance en cache — calcul au clic (adresse société + chantier)",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            travelMsg = "Calcul…"
                            val line = repo.addAutoTravelLine(context, doc.id, client.id, roundTrip = true)
                            travelMsg = if (line != null) "Ligne A/R ajoutée" else "Impossible : renseigne adresse société (onglet Société) et adresse chantier"
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Dépl. A/R auto") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            travelMsg = "Calcul…"
                            val line = repo.addAutoTravelLine(context, doc.id, client.id, roundTrip = false)
                            travelMsg = if (line != null) "Ligne aller simple ajoutée" else "Impossible : vérifie les adresses"
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Aller simple") }
                }
                if (travelMsg != null) Text(travelMsg!!, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { scope.launch { repo.updateDocument(doc.copy(status = DocumentStatus.ENVOYE)) } }, label = { Text("Envoyé") })
                AssistChip(onClick = { scope.launch { repo.updateDocument(doc.copy(status = DocumentStatus.PAYE)) } }, label = { Text("Payé") })
            }
            if (doc.kind == DocumentKind.DEVIS) {
                Button(onClick = {
                    scope.launch {
                        val invoiceId = repo.convertQuoteToInvoice(doc.id)
                        if (invoiceId != null) nav.navigate("doc/$invoiceId")
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Transformer en facture") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelsScreen(repo: Repo, nav: NavController) {
    val travels by repo.travels().collectAsState(emptyList())
    val clients by repo.clients().collectAsState(emptyList())
    val docs by repo.documents().collectAsState(emptyList())
    val company by repo.company().collectAsState(null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var clientId by remember { mutableStateOf(0L) }
    var mode by remember { mutableStateOf(TravelMode.KM) }
    var km by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(false) }
    var docId by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var autoInfo by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Déplacements") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nouveau déplacement")
            TextButton(onClick = { menu = true }) {
                Text(clients.find { it.id == clientId }?.displayName() ?: "Choisir un client")
            }
            DropdownMenu(menu, { menu = false }) {
                clients.forEach {
                    DropdownMenuItem(text = { Text(it.displayName()) }, onClick = {
                        clientId = it.id
                        menu = false
                        scope.launch {
                            val c = it
                            var oneWay = c.distanceKm
                            if (oneWay <= 0) {
                                autoInfo = "Calcul distance…"
                                oneWay = repo.refreshClientDistance(context, c.id) ?: 0.0
                            }
                            val rt = company?.travelRoundTripDefault != false
                            val useKm = if (rt) oneWay * 2 else oneWay
                            if (useKm > 0) {
                                mode = TravelMode.KM
                                km = useKm.toString()
                                autoInfo = if (rt) "A/R auto : ${useKm} km (${oneWay} × 2)" else "Aller auto : ${useKm} km"
                            } else {
                                autoInfo = "Pas de distance — saisie manuelle ou renseigne les adresses"
                            }
                        }
                    })
                }
            }
            if (autoInfo != null) Text(autoInfo!!, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == TravelMode.KM, { mode = TravelMode.KM }, label = { Text("Au km") })
                FilterChip(mode == TravelMode.FORFAIT, { mode = TravelMode.FORFAIT }, label = { Text("Forfait") })
                FilterChip(mode == TravelMode.TEMPS, { mode = TravelMode.TEMPS }, label = { Text("À l'heure") })
            }
            if (mode == TravelMode.KM) Field("Kilomètres", km, KeyboardType.Decimal) { km = it }
            if (mode == TravelMode.TEMPS) Field("Heures", hours, KeyboardType.Decimal) { hours = it }
            Field("Commentaire", comment) { comment = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(attach, { attach = it })
                Text("Ajouter sur un devis / facture")
            }
            if (attach) {
                docs.filter { it.clientId == clientId }.forEach { d ->
                    FilterChip(docId == d.id, { docId = d.id }, label = { Text(d.number) })
                }
            }
            Button(
                enabled = clientId != 0L,
                onClick = {
                    scope.launch {
                        repo.saveTravel(
                            Travel(
                                clientId = clientId, documentId = if (attach) docId else null,
                                mode = mode, kilometers = km.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                hours = hours.replace(",", ".").toDoubleOrNull() ?: 0.0, comment = comment
                            ),
                            attachAsLine = attach && docId != null
                        )
                        km = ""; hours = ""; comment = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Enregistrer le déplacement") }
            Spacer(Modifier.height(8.dp))
            travels.forEach { t ->
                val c = clients.find { it.id == t.clientId }
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c?.displayName().orEmpty())
                            Text("${t.mode} · ${t.amountHt.euro()} HT · ${fr.cortotelite.app.util.breakdown(t.amountHt, t.vatRate).ttc.euro()} TTC")
                            if (t.comment.isNotBlank()) Text(t.comment)
                        }
                        IconButton(onClick = { scope.launch { repo.deleteTravel(t) } }) {
                            Icon(Icons.Outlined.Delete, null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repo: Repo) {
    val company by repo.company().collectAsState(null)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("Cortot Élite") }
    var legal by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var siret by remember { mutableStateOf("") }
    var tva by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var vat by remember { mutableStateOf("20") }
    var vatRed by remember { mutableStateOf("10") }
    var km by remember { mutableStateOf("0.80") }
    var forfait by remember { mutableStateOf("45") }
    var hourly by remember { mutableStateOf("55") }
    var roundTrip by remember { mutableStateOf(true) }
    LaunchedEffect(company) {
        val c = company ?: return@LaunchedEffect
        name = c.name; legal = c.legalName; address = c.address; siret = c.siret
        tva = c.tvaNumber; phone = c.phone; email = c.email
        vat = c.defaultVatRate.toString(); vatRed = c.reducedVatRate.toString()
        km = c.travelRatePerKmHt.toString(); forfait = c.travelForfaitHt.toString()
        hourly = c.travelHourlyHt.toString()
        roundTrip = c.travelRoundTripDefault
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Société") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field("Nom commercial", name) { name = it }
            Field("Raison sociale", legal) { legal = it }
            Field("Adresse", address) { address = it }
            Field("SIRET", siret) { siret = it }
            Field("N° TVA", tva) { tva = it }
            Field("Téléphone", phone) { phone = it }
            Field("E-mail", email) { email = it }
            Field("TVA normale %", vat, KeyboardType.Decimal) { vat = it }
            Field("TVA réduite %", vatRed, KeyboardType.Decimal) { vatRed = it }
            Field("Déplacement € HT / km", km, KeyboardType.Decimal) { km = it }
            Field("Forfait déplacement € HT", forfait, KeyboardType.Decimal) { forfait = it }
            Field("Heure déplacement € HT", hourly, KeyboardType.Decimal) { hourly = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(roundTrip, { roundTrip = it })
                Text("Déplacement auto en aller-retour (km × 2)")
            }
            Text(
                "Le calcul auto utilise l'adresse société ci-dessus et l'adresse chantier du client (géocodage Android).",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = {
                scope.launch {
                    repo.saveCompany(
                        (company ?: fr.cortotelite.app.data.CompanySettings()).copy(
                            name = name, legalName = legal, address = address, siret = siret,
                            tvaNumber = tva, phone = phone, email = email,
                            defaultVatRate = vat.replace(",", ".").toDoubleOrNull() ?: 20.0,
                            reducedVatRate = vatRed.replace(",", ".").toDoubleOrNull() ?: 10.0,
                            travelRatePerKmHt = km.replace(",", ".").toDoubleOrNull() ?: 0.8,
                            travelForfaitHt = forfait.replace(",", ".").toDoubleOrNull() ?: 45.0,
                            travelHourlyHt = hourly.replace(",", ".").toDoubleOrNull() ?: 55.0,
                            travelRoundTripDefault = roundTrip
                        )
                    )
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    type: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type)
    )
}

private fun Client.displayName(): String =
    if (type == ClientType.PROFESSIONNEL && companyName.isNotBlank())
        "$companyName — $firstName $lastName".trim(' ', '—')
    else "$firstName $lastName".trim().ifBlank { "Sans nom" }
