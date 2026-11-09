package corsica.universita.carnet.ui.trails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailCreateScreen(onBack: () -> Unit, onCreated: (String) -> Unit, viewModel: TrailCreateViewModel = viewModel()) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val selected by viewModel.selectedIds.collectAsStateWithLifecycle()
    val sounds by viewModel.sounds.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.createdId) { viewModel.createdId?.let(onCreated) }
    Scaffold(topBar = { TopAppBar(title = { Text("Créer un parcours") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedTextField(title, viewModel::setTitle, label = { Text("Titre") }) }
            item { OutlinedTextField(description, viewModel::setDescription, label = { Text("Description") }) }
            item { Text("Choisissez les sons dans l’ordre de la balade") }
            items(sounds.filter { it.serverId != null }, key = { it.id }) { sound ->
                val id = requireNotNull(sound.serverId)
                Row { Checkbox(checked = id in selected, onCheckedChange = { viewModel.toggle(id) }); Text(sound.title, Modifier.padding(top = 12.dp)) }
                if (id in selected) {
                    Text("Étape ${selected.indexOf(id) + 1}")
                    OutlinedTextField(notes[id].orEmpty(), onValueChange = { viewModel.setNote(id, it) }, label = { Text("Note facultative") })
                }
            }
            item {
                viewModel.error?.let { Text(it) }
                Button(onClick = viewModel::create, enabled = title.isNotBlank() && selected.isNotEmpty() && !viewModel.creating) { Text("Publier le parcours") }
            }
        }
    }
}
