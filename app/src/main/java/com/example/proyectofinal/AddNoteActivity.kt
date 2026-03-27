package com.example.proyectofinal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
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
import android.util.Log
import android.util.TypedValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    
    private var isBoldActive = false
    private var isUnderlineActive = false
    private var activeFontSize: Int = 16 

    private lateinit var subtaskAdapter: SubtaskAdapter
    private val subtasks = mutableListOf<Subtask>()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.e("AddNoteActivity", "Error permission", e)
            }
            imageUri = it.toString()
            showImage(it)
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val size = getFileSize(it)
            if (size > 10 * 1024 * 1024) {
                showToast("El archivo es demasiado grande (máx. 10MB)")
                return@let
            }
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.e("AddNoteActivity", "Error permission", e)
            }
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
                if (count > before) {
                    val startIdx = start
                    val endIdx = start + count
                    val editable = binding.etContent.text
                    
                    if (isBoldActive) {
                        editable.setSpan(StyleSpan(Typeface.BOLD), startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    if (isUnderlineActive) {
                        editable.setSpan(UnderlineSpan(), startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    // Aplicar tamaño activo solo al texto nuevo
                    editable.setSpan(AbsoluteSizeSpan(activeFontSize, true), startIdx, endIdx, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSubtasks() {
        subtaskAdapter = SubtaskAdapter(
            onTitleChanged = { position, newTitle ->
                if (position >= 0 && position < subtasks.size) {
                    subtasks[position] = subtasks[position].copy(title = newTitle)
                }
            },
            onCheckedStateChanged = { position, isChecked ->
                if (position >= 0 && position < subtasks.size) {
                    subtasks[position] = subtasks[position].copy(isCompleted = isChecked)
                }
            },
            onDeleteClicked = { position ->
                if (position >= 0 && position < subtasks.size) {
                    subtasks.removeAt(position)
                    subtaskAdapter.submitList(subtasks.toList())
                }
            }
        )
        binding.rvSubtasks.layoutManager = LinearLayoutManager(this)
        binding.rvSubtasks.adapter = subtaskAdapter
    }

    @SuppressLint("SetTextI18n")
    private fun loadExistingNote(id: Int) {
        lifecycleScope.launch {
            val note = withContext(Dispatchers.IO) { viewModel.getNoteById(id) }
            existingNote = note
            note?.let { n ->
                binding.etTitle.setText(n.title)
                
                // Restablecer el tamaño base a 16sp para evitar herencias raras
                binding.etContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

                val spannedContent =
                    Html.fromHtml(n.content, Html.FROM_HTML_MODE_LEGACY)

                binding.etContent.setText(spannedContent)
                
                binding.tvDate.text = "Creada el ${n.dateText}"
                binding.swCompleted.isChecked = n.isCompleted
                category = n.category
                endDate = n.endDate
                imageUri = n.imagePath
                audioPath = n.audioPath
                fileUri = n.filePath

                if (endDate != null) {
                    binding.chipEndDate.text = "Vence: $endDate"
                }
                
                imageUri?.let { showImage(Uri.parse(it)) }
                audioPath?.let { showAudio(it) }
                fileUri?.let { showFile(Uri.parse(it)) }

                setupCategoryDropdown()
                
                val db = AppDatabase.getDatabase(this@AddNoteActivity)
                val loadedSubtasks = withContext(Dispatchers.IO) {
                    db.subtaskDao().getSubtasksByNoteId(n.id).first()
                }
                subtasks.clear()
                subtasks.addAll(loadedSubtasks)
                subtaskAdapter.submitList(subtasks.toList())
            }
        }
    }

    private fun setupCategoryDropdown() {
        val sharedPref = getSharedPreferences("RapiTaskPrefs", MODE_PRIVATE)
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
            binding.bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val baseMargin = (24 * resources.displayMetrics.density).toInt()
                bottomMargin = systemBars.bottom + baseMargin
            }
            insets
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveNote() }
        binding.chipEndDate.setOnClickListener { showDatePicker() }

        binding.btnAddSubtask.setOnClickListener {
            val newSubtask = Subtask(noteId = existingNote?.id ?: 0, title = "")
            subtasks.add(newSubtask)
            subtaskAdapter.submitList(subtasks.toList())
        }
        
        binding.btnTextFormat.setOnClickListener { showFormatSheet() }
        binding.btnAudio.setOnClickListener { toggleRecording() }
        binding.btnImage.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        binding.btnFile.setOnClickListener { pickFileLauncher.launch(arrayOf("*/*")) }

        binding.btnRemoveImage.setOnClickListener {
            imageUri = null
            binding.cvImageContainer.visibility = View.GONE
        }

        binding.chipAudio.setOnCloseIconClickListener {
            audioPath = null
            binding.chipAudio.visibility = View.GONE
        }
        
        binding.chipAudio.setOnClickListener { playAudio() }

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
                    sheetBinding.btnSizeS.id -> 12
                    sheetBinding.btnSizeM.id -> 16
                    sheetBinding.btnSizeL.id -> 24
                    sheetBinding.btnSizeXL.id -> 32
                    else -> 16
                }
                applyFormatToSelection(AbsoluteSizeSpan(activeFontSize, true), true)
            }
        }

        sheetBinding.btnClearFormat.setOnClickListener {
            val start = binding.etContent.selectionStart
            val end = binding.etContent.selectionEnd
            if (start != -1 && end != -1 && start != end) {
                val editable = binding.etContent.text
                val spans = editable.getSpans(start, end, Object::class.java)
                for (span in spans) { editable.removeSpan(span) }
            } else {
                binding.etContent.setText(binding.etContent.text.toString())
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
            12 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeS.id)
            16 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeM.id)
            24 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeL.id)
            32 -> sheetBinding.toggleSizeGroup.check(sheetBinding.btnSizeXL.id)
        }
    }

    private fun applyFormatToSelection(span: Any, active: Boolean) {
        val start = binding.etContent.selectionStart
        val end = binding.etContent.selectionEnd
        val editable = binding.etContent.text
        if (start != -1 && end != -1 && start != end) {
            // Si es un cambio de tamaño, primero eliminamos cualquier AbsoluteSizeSpan existente en el rango
            if (span is AbsoluteSizeSpan) {
                val existingSpans = editable.getSpans(start, end, AbsoluteSizeSpan::class.java)
                for (s in existingSpans) { editable.removeSpan(s) }
            }
            
            if (active) {
                editable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (span !is AbsoluteSizeSpan) {
                val spans = editable.getSpans(start, end, span.javaClass)
                for (s in spans) { editable.removeSpan(s) }
            }
        }
    }

    private fun toggleRecording() {
        if (!isRecording) checkAudioPermission() else stopRecording()
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        } else startRecording()
    }

    private fun startRecording() {
        val fileName = "REC_${System.currentTimeMillis()}.3gp"
        val storageDir = getExternalFilesDir(null)
        val audioFile = File(storageDir, fileName)
        audioPath = audioFile.absolutePath
        @Suppress("DEPRECATION")
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()
        mediaRecorder?.apply {
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
            } catch (e: IOException) { showToast("Error al grabar") }
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
        } catch (e: Exception) {} finally {
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
                setDataSource(audioPath); prepare(); start()
                showToast("Reproduciendo...")
            } catch (e: IOException) { showToast("Error al reproducir") }
        }
    }

    private fun openDocument(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, contentResolver.getType(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) { showToast("Error al abrir") }
    }

    private fun getFileSize(uri: Uri): Long {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) cursor.getLong(sizeIndex) else 0L
        } ?: 0L
    }

    private fun showFullScreenImage(uri: Uri) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
        var durationText = "00:00"
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(path)
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            durationText = String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            Log.e("AddNoteActivity", "Error getting audio duration", e)
        } finally {
            try { mmr.release() } catch (e: Exception) {}
        }
        binding.chipAudio.text = "Audio ($durationText)"
    }

    private fun showFile(uri: Uri) {
        binding.chipFile.visibility = View.VISIBLE
        var fileName = "Archivo"
        try {
            if (uri.scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            } else {
                fileName = uri.lastPathSegment ?: "Archivo"
            }
        } catch (e: Exception) {
            fileName = "Archivo"
        }
        binding.chipFile.text = fileName
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker().build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            endDate = sdf.format(calendar.time)
            binding.chipEndDate.text = "Vence: $endDate"
        }
        datePicker.show(supportFragmentManager, "DP")
    }

    private fun saveNote() {
        val titleInput = binding.etTitle.text.toString().trim()
        val contentHtml = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.toHtml(binding.etContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            @Suppress("DEPRECATION")
            Html.toHtml(binding.etContent.text)
        }
        
        if (titleInput.isEmpty() && binding.etContent.text.isEmpty() && subtasks.all { it.title.isEmpty() }) {
            showToast("Tarea vacía"); return
        }

        val finalTitle = titleInput.ifEmpty { "Sin título" }
        val currentSubtasks = subtasks.toList() 

        lifecycleScope.launch {
            val noteToSave = if (existingNote != null) {
                existingNote!!.copy(
                    title = finalTitle, content = contentHtml, category = category,
                    endDate = endDate, isCompleted = binding.swCompleted.isChecked,
                    imagePath = imageUri, audioPath = audioPath, filePath = fileUri,
                    fontSize = activeFontSize.toFloat()
                )
            } else {
                val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                Note(
                    title = finalTitle, content = contentHtml, dateText = sdf.format(Date()),
                    category = category, endDate = endDate, isCompleted = binding.swCompleted.isChecked,
                    imagePath = imageUri, audioPath = audioPath, filePath = fileUri,
                    fontSize = activeFontSize.toFloat()
                )
            }

            val noteId = withContext(Dispatchers.IO) {
                if (existingNote != null) { viewModel.update(noteToSave); existingNote!!.id }
                else { viewModel.insert(noteToSave).toInt() }
            }
            
            if (noteId > 0) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(this@AddNoteActivity)
                    db.subtaskDao().deleteSubtasksByNoteId(noteId)
                    currentSubtasks.forEach { sub ->
                        if (sub.title.isNotEmpty()) {
                            db.subtaskDao().insert(sub.copy(noteId = noteId, id = 0))
                        }
                    }
                }
            }
            showToast("Guardado"); finish()
        }
    }

    private fun setCurrentDate() {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        binding.tvDate.text = "Creada el ${sdf.format(Date())}"
    }
    
    private fun showToast(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release(); mediaPlayer?.release()
    }
}
