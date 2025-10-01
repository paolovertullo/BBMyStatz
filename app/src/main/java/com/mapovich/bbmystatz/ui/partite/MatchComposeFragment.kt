package com.mapovich.bbmystatz.ui.partite

import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mapovich.bbmystatz.ui.partite.AppTheme

class MatchComposeFragment : Fragment() {

    companion object {
        private const val ARG_PARTITA_ID = "partitaId"
        fun newInstance(partitaId: Int) = MatchComposeFragment().apply {
            arguments = Bundle().apply { putInt(ARG_PARTITA_ID, partitaId) }
        }
    }

    private val vm: MatchComposeViewModel by viewModels {
        viewModelFactory {
            initializer {
                val id = requireArguments().getInt(ARG_PARTITA_ID, -1)
                android.util.Log.d("MatchComposeFragment", "arg partitaId=$id")
                MatchComposeViewModel(requireActivity().application, id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Salva quando l'utente preme indietro (gestione back press)
        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.saveNow()   // minuti + plus/minus + risultato
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            val partitaId = requireArguments().getInt("partitaId")
            setContent {
                AppTheme {
                    val state = vm.uiState

                    // Salva anche quando il Composable viene smontato (es. cambio nav)
                    DisposableEffect(Unit) {
                        onDispose { vm.saveNow() }
                    }
                    MatchRoute(partitaId = partitaId) // <-- IMPORTANTE: usa Route (non MatchScreen)

                    /*            MatchScreen(
                                    state = state,
                                    callbacks = MatchCallbacks(
                                        onToggleOnCourt = { vm.toggleOnCourt() },
                                        onTeamPoints = { vm.addTeamPoints(it) },
                                        onTeamPointsWithAssist = { vm.addTeamPointsWithAssist(it) },
                                        onOppPoints = { vm.addOppPoints(it) },
                                        onPlayerMade = { vm.playerMade(it) },
                                        onPlayerMissed = { vm.playerMissed(it) },
                                        onRebOff = { vm.incReb(true) },
                                        onRebDef = { vm.incReb(false) },
                                        onAst = { vm.incAst() },
                                        onStl = { vm.incStl() },
                                        onBlk = { vm.incBlk() },
                                        onUndo = { vm.undo() },
                                        onPeriodoChange = {}
                                    )
                                )*/
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        vm.saveNow()  // rete di sicurezza: salva quando la schermata va in background
    }

    override fun onStop() {
        super.onStop()
        vm.saveNow()  // ulteriore rete (es. app chiusa dal recents)
    }
}
