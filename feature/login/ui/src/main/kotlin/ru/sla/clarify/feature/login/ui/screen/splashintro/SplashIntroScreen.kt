package ru.sla.clarify.feature.login.ui.screen.splashintro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun SplashIntroScreen(viewModel: SplashIntroViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { _, intents ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.Bottom,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      VSpacer(30.dp)
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        onClick = intents.signIn
      ) {
        Text("Sign in")
      }
    }
  }
}
