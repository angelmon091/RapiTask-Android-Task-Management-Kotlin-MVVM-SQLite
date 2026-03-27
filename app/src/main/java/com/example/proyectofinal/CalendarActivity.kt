package com.example.proyectofinal

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectofinal.databinding.ActivityCalendarBinding
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter
    
    private var allNotes: List<Note> = emptyList()
    private var selectedDateString: String = ""
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Decoradores de estado para los puntitos
    private val overdueDecorator by lazy { EventDecorator(ContextCompat.getColor(this, R.color.status_overdue)) }
    private val soonDecorator by lazy { EventDecorator(ContextCompat.getColor(this, R.color.status_soon)) }
    private val onTimeDecorator by lazy { EventDecorator(ContextCompat.getColor(this, R.color.status_on_time)) }
    private val completedDecorator by lazy { EventDecorator(ContextCompat.getColor(this, R.color.status_completed)) }
    
    // Decoradores de UI
    private val todayDecorator by lazy { TodayDecorator() }
    private val selectionDecorator by lazy { SelectionDecorator() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupCalendar()
        observeNotes()
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = systemBars.bottom)
            binding.appBarLayout.updatePadding(top = systemBars.top)
            insets
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCalendar() {
        val today = CalendarDay.today()
        selectionDecorator.setSelectedDay(today)
        selectedDateString = dateFormatter.format(today.date)

        binding.calendarView.apply {
            setSelectedDate(today)
            // Añadimos todos los decoradores
            addDecorators(
                todayDecorator, 
                selectionDecorator, 
                overdueDecorator, 
                soonDecorator, 
                onTimeDecorator, 
                completedDecorator
            )
            
            setOnDateChangedListener { _, date, _ ->
                selectedDateString = dateFormatter.format(date.date)
                selectionDecorator.setSelectedDay(date)
                invalidateDecorators()
                filterNotesByDate()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClick = { note ->
                val intent = Intent(this, AddNoteActivity::class.java).apply {
                    putExtra("NOTE_ID", note.id)
                }
                startActivity(intent)
            },
            onItemLongClick = { _, _ -> }
        )
        binding.rvDayNotes.layoutManager = LinearLayoutManager(this)
        binding.rvDayNotes.adapter = adapter
    }

    private fun observeNotes() {
        viewModel.allNotes.observe(this) { notes ->
            allNotes = notes
            calculateDaysWithNotes()
            filterNotesByDate()
        }
    }

    private fun calculateDaysWithNotes() {
        val overdueDays = mutableSetOf<CalendarDay>()
        val soonDays = mutableSetOf<CalendarDay>()
        val onTimeDays = mutableSetOf<CalendarDay>()
        val completedDays = mutableSetOf<CalendarDay>()

        val todayDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // Agrupamos notas por fecha
        val notesByDate = allNotes.groupBy { it.endDate }

        notesByDate.forEach { (dateStr, notes) ->
            if (dateStr == null) return@forEach
            
            try {
                val date = dateFormatter.parse(dateStr) ?: return@forEach
                val day = CalendarDay.from(date)
                
                // Determinamos el estado prioritario para el día
                // Prioridad: 0: Vencida (Rojo), 1: Próxima (Naranja), 2: A tiempo (Verde), 3: Completada (Gris)
                var worstStatus = 3
                
                notes.forEach { note ->
                    if (!note.isCompleted) {
                        val diff = date.time - todayDate.time
                        val daysDiff = diff / (1000 * 60 * 60 * 24)
                        
                        val status = when {
                            date.before(todayDate) -> 0
                            daysDiff <= 2 -> 1
                            else -> 2
                        }
                        if (status < worstStatus) worstStatus = status
                    }
                }
                
                when (worstStatus) {
                    0 -> overdueDays.add(day)
                    1 -> soonDays.add(day)
                    2 -> onTimeDays.add(day)
                    3 -> completedDays.add(day)
                }
            } catch (_: Exception) {}
        }
        
        overdueDecorator.setDates(overdueDays)
        soonDecorator.setDates(soonDays)
        onTimeDecorator.setDates(onTimeDays)
        completedDecorator.setDates(completedDays)
        
        binding.calendarView.invalidateDecorators()
    }

    private fun filterNotesByDate() {
        val filtered = allNotes.filter { it.endDate == selectedDateString }
        adapter.submitList(filtered)
        
        val isEmpty = filtered.isEmpty()
        binding.rvDayNotes.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyStateCalendar.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    // --- DECORADORES ---

    inner class EventDecorator(private val color: Int) : DayViewDecorator {
        private var dates: Set<CalendarDay> = emptySet()
        fun setDates(newDates: Set<CalendarDay>) { this.dates = newDates }
        override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f, color))
        }
    }

    inner class SelectionDecorator : DayViewDecorator {
        private var selectedDay: CalendarDay? = null
        private val drawable: Drawable? by lazy {
            ContextCompat.getDrawable(this@CalendarActivity, R.drawable.calendar_selection_ring)
        }
        fun setSelectedDay(day: CalendarDay) { this.selectedDay = day }
        override fun shouldDecorate(day: CalendarDay): Boolean = day == selectedDay
        override fun decorate(view: DayViewFacade) {
            drawable?.let { view.setSelectionDrawable(it) }
        }
    }

    inner class TodayDecorator : DayViewDecorator {
        private val today = CalendarDay.today()
        private val drawable: Drawable? by lazy {
            ContextCompat.getDrawable(this@CalendarActivity, R.drawable.calendar_today_indicator)
        }
        override fun shouldDecorate(day: CalendarDay): Boolean = day == today
        override fun decorate(view: DayViewFacade) {
            drawable?.let { view.setBackgroundDrawable(it) }
        }
    }
}
