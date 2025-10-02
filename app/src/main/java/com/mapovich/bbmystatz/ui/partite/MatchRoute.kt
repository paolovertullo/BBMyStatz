package com.mapovich.bbmystatz.ui.partite

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MatchRoute(partitaId: Int) {
    val app = LocalContext.current.applicationContext as Application
    // Factory memoizzata per evitare ricreazioni
    val factory = remember(partitaId) { MatchComposeViewModelFactory(app, partitaId) }
    val vm: MatchComposeViewModel = viewModel(factory = factory)

    val state = vm.uiState

    LaunchedEffect(state.periodo) {
        Log.d("MatchRoute", "UI mostra periodo -> ${state.periodo}")
    }

    MatchScreen(
        state = state,
        callbacks = MatchCallbacks(
            onToggleOnCourt = { vm.toggleOnCourt() },
            onTeamPoints = { p -> vm.addTeamPoints(p) },
            onTeamPointsWithAssist = { p -> vm.addTeamPointsWithAssist(p) },
            onOppPoints = { p -> vm.addOppPoints(p) },
            onPlayerMade = { p -> vm.playerMade(p) },
            onPlayerMissed = { p -> vm.playerMissed(p) },
            onRebOff = { vm.incReb(offensive = true) },
            onRebDef = { vm.incReb(offensive = false) },
            onAst = { vm.incAst() },
            onStl = { vm.incStl() },
            onBlk = { vm.incBlk() },
            onUndo = { vm.undo() },
            onPeriodoChange = { p -> vm.setPeriodo(p) },
            onUpdateTitle = { t -> vm.updateMatchTitle(t) },
            onUpdateTorneoStagioneData = { torneo, stagione, data -> vm.updateTorneoStagioneData(torneo, stagione, data) }
        )
    )

}

/** Factory per MatchComposeViewModel */
class MatchComposeViewModelFactory(
    private val app: Application,
    private val partitaId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MatchComposeViewModel::class.java)) {
            "Unknown ViewModel class ${modelClass.name}"
        }
        return MatchComposeViewModel(app, partitaId) as T
    }
}
