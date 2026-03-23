package com.example.proyectofinal

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.proyectofinal.databinding.ActivityAddNoteBinding
import com.example.proyectofinal.databinding.LayoutFormatSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNoteBinding
    private val viewModel: NoteViewModel by viewModels()
    private var existingNote: Note? = null
    private var category: String = "Todas"
    private var endDate: String? = null
    
    private var imageUri: String? = null
    private var audioPath: String? = null
    private var fileUri: String? = null
    
    // Formatting states
    private var isBoldActive = false
    private var isUnderlineActive = false
    private var activeFontSize: Int = 16 // in sp

    private lateinit var subtaskAdapter: SubtaskAdapter
    private val subtasks = mutableListOf<Subtask>()

    // Audio recording variables
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            imageUri = it.toString()
            showImage(it)
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val size = getFileSize(it)
            if (size > 10 * 1024 * 1024) { // 10MB limit
                showToast("El archivo es demasiado grande (máx. 10MB)")
                return@let
            }
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            fileUri = it.toString()
            showFile(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupSubtasks()
        setupFormattingWatcher()
        
        category = intent.getStringExtra("CATEGORY_EXTRA") ?: "Todas"
        
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        if (noteId != -1) {
            loadExistingNote(noteId)
        } else {
            setCurrentDate()
            setupCategoryDropdown()
        }
    }

    private fun setupFormattingWatcher() {
        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > before) { // Text was added
                    val end = start + count
                    val editable = binding.etContent.text
                    
                    if (isBoldActive) {
                        editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    if (isUnderlineActive) {
                        editable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    val sizePx = (activeFontSize * resources.displayMetrics.scaledDensity).toInt()
                    editable.setSpan(AbsoluteSizeSpan(sizePx), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSubtasks() {
        subtaskAdapter = SubtaskAdapter(
            onTitleChanged = { position, newTitle ->
                if (position < subtasks.size) {
                    subtasks[position] = subtasks[position].copy(title = newTitle)
                }
            },
            onCheckedStateChanged = { position, isChecked ->
                if (position < subtasks.size) {
                    subtasks[position] = subtasks[position].copy(isCompleted = isChecked)
                }
            },
            onDeleteClicked = { position ->
                if (position < subtasks.size) {
                    subtasks.removeAt(position)
                    subtaskAdapter.submitList(subtasks.toList())
                }
            }
        )
        binding.rvSubtasks.layoutManager = LinearLayoutManager(this)
        binding.rvSubtasks.adapter = subtaskAdapter
    }

    private fun loadExistingNote(id: Int) {
        lifecycleScope.launch {
            existingNote = viewModel.getNoteById(id)
            existingNote?.let { note ->
                binding.etTitle.setText(note.title)
                
                val spannedContent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(note.content, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(note.content)
                }
                binding.etContent.setText(spannedContent)
                
                binding.tvDate.text = "Creada el ${note.date}"
                binding.swCompleted.isChecked = note.isCompleted
                category = note.category
                endDate = note.endDate
                imageUri = note.imagePath
                audioPath = note.audioPath
                fileUri = note.filePath

                if (endDate != null) {
                    binding.chipEndDate.text = "Vence: $endDate"
                }
                
                imageUri?.let { showImage(Uri.parse(it)) }
                audioPath?.let { showAudio(it) }
                fileUri?.let { showFile(Uri.parse(it)) }

                setupCategoryDropdown()
                
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                val loadedSubtasks = db.subtaskDao().getSubtasksByNoteId(note.id).first()
                subtasks.clear()
                subtasks.addAll(loadedSubtasks)
                subtaskAdapter.submitList(subtasks.toList())
            }
        }
    }

    private fun setupCategoryDropdown() {
        val sharedPref = getSharedPreferences("TaskEzzPrefs", MODE_PRIVATE)
        val saved = sharedPref.getStringSet("DYNAMIC_CATEGORIES", setOf("Escuela", "Trabajo"))
        val categories = mutableListOf("Todas")
        categories.addAll(saved ?: emptySet())
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
        binding.actvCategory.setText(category, false)
        
        binding.actvCategory.setOnItemClickListener { _, _, position, _ ->
            category = adapter.getItem(position) ?: "Todas"
        }
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = systemBars.left, right = systemBars.right, bottom = 0)
            binding.toolbar.updatePadding(top = systemBars.top)
            
            // Ajustar dinámicamente la barra inferior para que flote sobre los botones de navegación
            binding.bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val baseMargin = (24 * resources.displayMetrics.density).toInt()
                bottomMargin = systemBars.bottom + baseMargin
            }
            
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveNote() }

        binding.chipEndDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnAddSubtask.setOnClickListener {
            val newSubtask = Subtask(noteId = existingNote?.id ?: 0, title = "")
            subtasks.add(newSubtask)
            subtaskAdapter.submitList(subtasks.toList())
        }
        
        binding.btnTextFormat.setOnClickListener { showFormatSheet() }
        binding.btnAudio.setOnClickListener { toggleRecording() }
        binding.btnImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnFile.setOnClickListener { pickFileLauncher.launch("*/*") }

        binding.btnRemoveImage.setOnClickListener {
            imageUri = null
            binding.cvImageContainer.visibility = View.GONE
        }

        binding.chipAudio.setOnCloseIconClickListener {
            audioPath = null
            binding.chipAudio.visibility = View.GONE
        }
        
        binding.chipAudio.setOnClickListener {
            playAudio()
        }

        binding.chipFile.setOnCloseIconClickListener {
            fileUri = null
            binding.chipFile.visibility = View.GONE
        }

        binding.chipFile.setOnClickListener {
            fileUri?.let { uriStr -> openDocument(Uri.parse(uriStr)) }
        }

        binding.ivTaskImage.setOnClickListener {
            imageUri?.let { uriStr -> showFullScreenImage(Uri.parse(uriStr)) }
        }
    }

    private fun showFormatSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = LayoutFormatSheetBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        updateFormatSheetUI(sheetBinding)

        sheetBinding.btnBold.setOnClickListener {
            isBoldActive = !isBoldActive
            applyFormatToSelection(StyleSpan(Typeface.BOLD), isBoldActive)
            updateFormatSheetUI(sheetBinding)
        }
        
        sheetBinding.btnUnderline.setOnClickListener {
            isUnderlineActive = !isUnderlineActive
            applyFormatToSelection(UnderlineSpan(), isUnderlineActive)
            updateFormatSheetUI(sheetBinding)
        }
        
        sheetBinding.toggleSizeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                activeFontSize = when (checkedId) {
                    sheetBinding.btnSizeS.id -> 14
                    sheetBinding.btnSizeM.id -> 16
                    sheetBinding.btnSizeL.id -> 20
                    sheetBinding.btnSizeXL.id -> 24
                    else -> 16
                }
                val sizePx = (activeFontSize * resources.displayMetrics.scaledDensity).toInt()
                applyFormatToSelection(AbsoluteSizeSpan(sizePx), true)
            }
        }

        sheetBinding.btnClearFormat.setOnClickListener {
            val start = binding.etContent.selectionStart
            val end = binding.etContent.selectionEnd
            val editable = binding.etContent.text
            if (start != -1 && end != -1 && start != end) {
                val spans = editable.getSpans(start, end, Object::class.java)
                for (span in spans) {
                    editable.removeSpan(span)
                }
            } else {
                val text = editable.toString()
                binding.etContent.setText(text)
                isBoldActive = false
                isUnderlineActive = false
                activeFontSize = 16
                updateFormatSheetUI(sheetBinding)
            }
        }

        dialog.show()
    }

    private fun updateFormatSheetUI(sheetBinding: LayoutFormatSheetBinding) {
        val activeColor = ContextCompat.getColor(this, R.color.primary)
        val inactiveColor = ContextCompat.getColor(this, android.R.color.transparent)
        
        sheetBinding.btnBold.setBackgroundColor(if (isBoldActive) activeColor else inactiveColor)
        sheetBinding.btnBold.setTextColor(if (isBoldActive) ContextCompat.getColor(this, R.color.white) else activeColor)
        
        sheetBinding.btnUnderline.setBackgroundColor(if (isUnderlineActive) activeColor else inactiveColor)
        sheetBinding.btnUnderline.setTextColor(if (isUnderlineActive) ContextCompat.getColor(this, R.color.white) else activeColor)

        when (activeFontSize) {
            14 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeS.id)
            16 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeM.id)
            20 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeL.id)
            24 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeXL.id)
        }
    }

    private fun applyFormatToSelection(span: Any, active: Boolean) {
        val start = binding.etContent.selectionStart
        val end = binding.etContent.selectionEnd
        val editable = binding.etContent.text
        
        if (start != -1 && end != -1 && start != end) {
            if (active) {
                editable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                val spans = editable.getSpans(start, end, span.javaClass)
                for (s in spans) {
                    editable.removeSpan(s)
                }
            }
        }
    }

    private fun toggleRecording() {
        if (!isRecording) {
            checkAudioPermission()
        } else {
            stopRecording()
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val fileName = "REC_${System.currentTimeMillis()}.3gp"
        val storageDir = getExternalFilesDir(null)
        val audioFile = File(storageDir, fileName)
        audioPath = audioFile.absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioPath)
            try {
                prepare()
                start()
                isRecording = true
                binding.btnAudio.setImageResource(android.R.drawable.ic_media_pause)
                showToast("Grabando...")
            } catch (e: IOException) {
                showToast("Error al iniciar grabación")
            }
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            showToast("Grabación demasiado corta")
        } finally {
            mediaRecorder = null
            isRecording = false
            binding.btnAudio.setImageResource(R.drawable.ic_mic)
            audioPath?.let { showAudio(it) }
        }
    }

    private fun playAudio() {
        if (audioPath == null) return
        
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioPath)
                prepare()
                start()
                showToast("Reproduciendo audio...")
            } catch (e: IOException) {
                showToast("Error al reproducir audio")
            }
        }
    }

    private fun openDocument(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, contentResolver.getType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("No hay aplicaciones para abrir este archivo")
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                cursor.getLong(sizeIndex)
            } else 0L
        } ?: 0L
    }

    private fun showFullScreenImage(uri: Uri) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        Glide.with(this).load(uri).into(imageView)
        
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun showImage(uri: Uri) {
        binding.cvImageContainer.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(binding.ivTaskImage)
    }

    private fun showAudio(path: String) {
        binding.chipAudio.visibility = View.VISIBLE
        binding.chipAudio.text = "Audio grabado (Toca para oír)"
    }

    private fun showFile(uri: Uri) {
        binding.chipFile.visibility = View.VISIBLE
        val fileName = try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.path?.split("/")?.lastOrNull() ?: "Documento"
        } catch (e: Exception) {
            "Documento"
        }
        binding.chipFile.text = "Archivo: $fileName"
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de vencimiento")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            endDate = sdf.format(calendar.time)
            binding.chipEndDate.text = "Vence: $endDate"
        }
        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun saveNote() {
        val titleInput = binding.etTitle.text.toString().trim()
        val content = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.toHtml(binding.etContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            @Suppress("DEPRECATION")
            Html.toHtml(binding.etContent.text)
        }
        
        val isCompleted = binding.swCompleted.isChecked

        if (titleInput.isEmpty() && content.isEmpty() && subtasks.all { it.title.isEmpty() }) {
            Toast.makeText(this, "La tarea no puede estar vacía", Toast.LENGTH_SHORT).show()
            return
        }

        val finalTitle = if (titleInput.isEmpty()) "Sin título" else titleInput

        lifecycleScope.launch {
            val noteToSave = if (existingNote != null) {
                existingNote!!.copy(
                    title = finalTitle,
                    content = content,
                    category = category,
                    endDate = endDate,
                    isCompleted = isCompleted,
                    imagePath = imageUri,
                    audioPath = audioPath,
                    filePath = fileUri
                )
            } else {
                val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                Note(
                    title = finalTitle,
                    content = content,
                    date = sdf.format(Date()),
                    category = category,
                    endDate = endDate,
                    isCompleted = isCompleted,
                    imagePath = imageUri,
                    audioPath = audioPath,
                    filePath = fileUri
                )
            }

            val noteId = if (existingNote != null) {
                viewModel.update(noteToSave)
                existingNote!!.id
            } else {
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                db.noteDao().insert(noteToSave).toInt()
            }
            
            if (noteId != 0) {
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                db.subtaskDao().deleteSubtasksByNoteId(noteId)
                subtasks.forEach { sub ->
                    if (sub.title.isNotEmpty()) {
                        db.subtaskDao().insert(sub.copy(noteId = noteId, id = 0))
                    }
                }
            }

            Toast.makeText(this@AddNoteActivity, "Tarea guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        binding.tvDate.text = "Creada el ${sdf.format(Date())}"
    }
    
    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}
