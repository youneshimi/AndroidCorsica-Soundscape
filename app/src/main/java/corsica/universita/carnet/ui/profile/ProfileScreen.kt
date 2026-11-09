package corsica.universita.carnet.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    Column(
        Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Choisissez un pseudo pour partager vos sons.")
        OutlinedTextField(value = nickname, onValueChange = viewModel::setNickname, label = { Text("Pseudo") }, singleLine = true)
        viewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = viewModel::save, enabled = nickname.isNotBlank() && !viewModel.saving, modifier = Modifier.padding(top = 16.dp)) {
            Text("Commencer")
        }
    }
}
