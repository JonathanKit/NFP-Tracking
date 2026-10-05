package de.easynfp.data

import de.easynfp.data.model.CycleRecord
import de.easynfp.data.model.DayEntry
import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.SensiplanEngine
import de.easynfp.rules.model.CycleBuilder
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Everything the screens need: stored data plus the current rule evaluation. */
data class EvaluationSnapshot(
    val today: LocalDate,
    val cycles: List<CycleRecord>,
    val entries: Map<LocalDate, DayEntry>,
    val evaluations: List<CycleEvaluation>,
) {
    /** The cycle that contains today, null before the first period start was recorded. */
    val current: CycleEvaluation? get() = evaluationFor(today)

    fun evaluationFor(date: LocalDate): CycleEvaluation? = evaluations.lastOrNull { date in it.cycle }

    fun recordFor(evaluation: CycleEvaluation): CycleRecord? =
        cycles.firstOrNull { it.startDate == evaluation.cycle.startDate }

    val hasCycles: Boolean get() = evaluations.isNotEmpty()

    companion object {
        fun compute(today: LocalDate, cycles: List<CycleRecord>, entries: List<DayEntry>): EvaluationSnapshot {
            val cycleData = CycleBuilder.build(
                starts = cycles.map { it.toCycleStart() },
                observations = entries.map { it.toObservation() },
            )
            return EvaluationSnapshot(
                today = today,
                cycles = cycles,
                entries = entries.associateBy { it.date },
                evaluations = SensiplanEngine.evaluate(cycleData, today),
            )
        }
    }
}

/** F-40: re-evaluates automatically whenever data changes or a new day begins. */
class EvaluationRepository(
    repository: NfpRepository,
    private val clock: Clock,
) {
    /** Emits the current date and checks once a minute for a date change. */
    val today: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(DATE_CHECK_INTERVAL_MILLIS)
        }
    }.distinctUntilChanged()

    val snapshot: Flow<EvaluationSnapshot> =
        combine(repository.cycles, repository.dayEntries, today) { cycles, entries, today ->
            EvaluationSnapshot.compute(today, cycles, entries)
        }.flowOn(Dispatchers.Default)

    suspend fun current(): EvaluationSnapshot = snapshot.first()

    private companion object {
        const val DATE_CHECK_INTERVAL_MILLIS = 60_000L
    }
}
