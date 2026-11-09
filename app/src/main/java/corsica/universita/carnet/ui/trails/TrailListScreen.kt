package corsica.universita.carnet.ui.trails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailListScreen(onOpenTrail: (String) -> Unit, onCreate: () -> Unit, viewModel: TrailListViewModel = viewModel()) {
    val trails by viewModel.trails.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Parcours") }) },
        floatingActionButton = { FloatingActionButton(onClick = onCreate) { Icon(Icons.Default.Add, "Créer un parcours") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(query, viewModel::setQuery, modifier = Modifier.padding(16.dp), label = { Text("Rechercher un parcours") })
            viewModel.error?.let { Text(it, modifier = Modifier.padding(16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (trails.isEmpty()) item { Text("Aucun parcours pour le moment.") }
                items(trails, key = { it.id }) { trail ->
                    Card(onClick = { onOpenTrail(trail.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(trail.title)
                            Text(trail.description)
                            Text("${trail.soundsCount} étapes · Par ${trail.nickname}")
                        }
                    }
                }
            }
        }
    }
}
