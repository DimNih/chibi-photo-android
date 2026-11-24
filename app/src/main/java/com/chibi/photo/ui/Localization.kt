package com.chibi.photo.ui

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    ID("id", "Indonesia")
}

data class StringsDict(
    val appTitle: String,
    val tagline: String,
    val subtitle: String,
    val openStudio: String,
    val importCustomPhoto: String,
    val choosePreset: String,
    val previewHero: String,
    val tapToSquishHint: String,

    // Studio Header & Tabs
    val back: String,
    val reset: String,
    val exportSnapshot: String,
    val exportGif: String,
    val recordingGif: String,
    val stopRecording: String,
    val tabTouch: String,
    val tabMask: String,
    val tabBounce: String,
    val tabSound: String,

    // Tools
    val toolPinch: String,
    val toolPinchHint: String,
    val toolPoke: String,
    val toolPokeHint: String,
    val toolSwirl: String,
    val toolSwirlHint: String,
    val toolPulse: String,
    val toolPulseHint: String,
    val toolGravity: String,
    val toolGravityHint: String,

    // Mask / Sculpt
    val sculptBrushSize: String,
    val sculptDraw: String,
    val sculptErase: String,
    val sculptSymmetry: String,
    val sculptQuickTemplates: String,
    val zoneDualCheeks: String,
    val zoneCenterCore: String,
    val zoneUpperCrest: String,
    val zoneFullCanvas: String,
    val zoneClear: String,

    // Bounce / Physics
    val physicsPresets: String,
    val materialSoftGel: String,
    val materialSoftGelDesc: String,
    val materialWaterDrop: String,
    val materialWaterDropDesc: String,
    val materialLatexSnap: String,
    val materialLatexSnapDesc: String,
    val materialWarmDough: String,
    val materialWarmDoughDesc: String,
    val materialMemoryFoam: String,
    val materialMemoryFoamDesc: String,
    val paramFrequency: String,
    val paramDamping: String,
    val paramSoftness: String,
    val paramPulseBpm: String,
    val toggleBlush: String,
    val toggleLattice: String,

    // Sound & Haptics
    val soundFxToggle: String,
    val hapticToggle: String,
    val soundProfileTitle: String,
    val soundProfileTactile: String,
    val soundProfileHydro: String,
    val soundProfileChime: String,

    // Dialogs & Export
    val exportSuccessTitle: String,
    val exportSuccessMessage: String,
    val actionShare: String,
    val actionDone: String,
    val encodingMessage: String,

    // Telemetry & Info
    val peakDisplacement: String,
    val averageStrain: String,
    val liveWaveform: String,
    val tiltHint: String,

    // Settings
    val lightMode: String,
    val darkMode: String,
    val switchLanguage: String
)

object AppStrings {
    val english = StringsDict(
        appTitle = "Chibi Photo",
        tagline = "Squishy chibi & portrait physics studio",
        subtitle = "Pick a character or import a photo to squish & pinch",
        openStudio = "Open Studio",
        importCustomPhoto = "Import Custom Photo",
        choosePreset = "Preset Characters",
        previewHero = "Interactive Preview",
        tapToSquishHint = "Tap or drag face to test elasticity",

        back = "Back",
        reset = "Reset",
        exportSnapshot = "Photo",
        exportGif = "GIF",
        recordingGif = "Recording...",
        stopRecording = "Stop",
        tabTouch = "Touch",
        tabMask = "Mask",
        tabBounce = "Bounce",
        tabSound = "Sound",

        toolPinch = "Pinch",
        toolPinchHint = "Grab and stretch cheeks with multi-touch recoil",
        toolPoke = "Poke",
        toolPokeHint = "Press to indent and ripple surfaces outward",
        toolSwirl = "Swirl",
        toolSwirlHint = "Twist vertices around touch anchor",
        toolPulse = "Pulse",
        toolPulseHint = "Continuous breathing oscillation to tempo",
        toolGravity = "Gravity",
        toolGravityHint = "Tilt device to sway elastic regions",

        sculptBrushSize = "Brush Radius",
        sculptDraw = "Paint",
        sculptErase = "Erase",
        sculptSymmetry = "Mirror X",
        sculptQuickTemplates = "Quick Mask Templates",
        zoneDualCheeks = "Dual Cheeks",
        zoneCenterCore = "Center Core",
        zoneUpperCrest = "Upper Crests",
        zoneFullCanvas = "Full Canvas",
        zoneClear = "Clear Mask",

        physicsPresets = "Viscoelastic Materials",
        materialSoftGel = "Soft Gel",
        materialSoftGelDesc = "6.8 Hz · Low damping wobble",
        materialWaterDrop = "Water Drop",
        materialWaterDropDesc = "5.2 Hz · Ripple wave motion",
        materialLatexSnap = "Latex Snap",
        materialLatexSnapDesc = "11.2 Hz · High tension snap",
        materialWarmDough = "Warm Dough",
        materialWarmDoughDesc = "4.6 Hz · Heavy soft rebound",
        materialMemoryFoam = "Memory Foam",
        materialMemoryFoamDesc = "3.2 Hz · Slow viscoelastic recovery",
        paramFrequency = "Oscillation Frequency",
        paramDamping = "Damping Ratio (Zeta)",
        paramSoftness = "Elastic Softness",
        paramPulseBpm = "Pulse Tempo (BPM)",
        toggleBlush = "Strain Cheek Blush",
        toggleLattice = "Wireframe Lattice Grid",

        soundFxToggle = "Squish Sound FX",
        hapticToggle = "Haptic Vibration",
        soundProfileTitle = "Acoustic Synthesizer Profile",
        soundProfileTactile = "Studio Pluck",
        soundProfileHydro = "Hydro Drop",
        soundProfileChime = "FM Harmonic",

        exportSuccessTitle = "Export Ready",
        exportSuccessMessage = "Saved to Gallery / Pictures / ChibiPhoto",
        actionShare = "Share",
        actionDone = "Done",
        encodingMessage = "Processing animated GIF...",

        peakDisplacement = "Peak Displacement",
        averageStrain = "Average Strain",
        liveWaveform = "Kinetic Telemetry Waveform",
        tiltHint = "Tilt your phone to test gravity pull",

        lightMode = "Light",
        darkMode = "Dark",
        switchLanguage = "Language"
    )

    val indonesian = StringsDict(
        appTitle = "Chibi Photo",
        tagline = "Studio foto chibi kenyal & elastis",
        subtitle = "Pilih karakter atau unggah foto untuk mulai dicubit",
        openStudio = "Buka Studio",
        importCustomPhoto = "Pilih Foto Sendiri",
        choosePreset = "Karakter Pilihan",
        previewHero = "Pratinjau Interaktif",
        tapToSquishHint = "Sentuh atau tarik pipi untuk mencoba kelenturan",

        back = "Kembali",
        reset = "Reset",
        exportSnapshot = "Foto",
        exportGif = "GIF",
        recordingGif = "Merekam...",
        stopRecording = "Selesai",
        tabTouch = "Sentuh",
        tabMask = "Topeng",
        tabBounce = "Pantulan",
        tabSound = "Suara",

        toolPinch = "Cubit",
        toolPinchHint = "Tarik dan cubit pipi elastis dengan efek mantul",
        toolPoke = "Tusuk",
        toolPokeHint = "Tekan untuk membuat lekukan ke dalam dan riak",
        toolSwirl = "Putar",
        toolSwirlHint = "Puntir dan putar bagian sekitar sentuhan",
        toolPulse = "Denyut",
        toolPulseHint = "Berdenyut teratur mengikuti tempo ketukan",
        toolGravity = "Gravitasi",
        toolGravityHint = "Miringkan HP untuk menggoyangkan pipi elastis",

        sculptBrushSize = "Ukuran Kuas",
        sculptDraw = "Oles Topeng",
        sculptErase = "Hapus",
        sculptSymmetry = "Simetri Kanan-Kiri",
        sculptQuickTemplates = "Pola Topeng Instan",
        zoneDualCheeks = "Kedua Pipi",
        zoneCenterCore = "Pusat Inti",
        zoneUpperCrest = "Puncak Kepala",
        zoneFullCanvas = "Seluruh Layar",
        zoneClear = "Hapus Semua",

        physicsPresets = "Bahan Kekenyalan",
        materialSoftGel = "Gel Lembut",
        materialSoftGelDesc = "6.8 Hz · Goyangan lembut & lentur",
        materialWaterDrop = "Tetes Air",
        materialWaterDropDesc = "5.2 Hz · Riak gelombang getar",
        materialLatexSnap = "Karet Elastis",
        materialLatexSnapDesc = "11.2 Hz · Tegangan tinggi & cepat balik",
        materialWarmDough = "Adonan Hangat",
        materialWarmDoughDesc = "4.6 Hz · Empuk mantap & rebound kenyal",
        materialMemoryFoam = "Busa Memori",
        materialMemoryFoamDesc = "3.2 Hz · Kembali perlahan & lembut",
        paramFrequency = "Frekuensi Getaran",
        paramDamping = "Tingkat Redaman (Zeta)",
        paramSoftness = "Tingkat Kelenturan",
        paramPulseBpm = "Tempo Denyut (BPM)",
        toggleBlush = "Rona Merah Saat Ditarik",
        toggleLattice = "Garis Kisi Ketegangan",

        soundFxToggle = "Efek Suara Kenyal",
        hapticToggle = "Getaran Haptik",
        soundProfileTitle = "Pilihan Suara Sintesis",
        soundProfileTactile = "Petikan Studio",
        soundProfileHydro = "Tetes Air",
        soundProfileChime = "Harmoni FM",

        exportSuccessTitle = "Berhasil Disimpan",
        exportSuccessMessage = "Tersimpan di Galeri / Foto / ChibiPhoto",
        actionShare = "Bagikan",
        actionDone = "Selesai",
        encodingMessage = "Sedang memproses GIF animasi...",

        peakDisplacement = "Puncak Pergeseran",
        averageStrain = "Rata-rata Regangan",
        liveWaveform = "Gelombang Telemetri Kinetik",
        tiltHint = "Miringkan HP untuk menguji tarikan gravitasi",

        lightMode = "Terang",
        darkMode = "Gelap",
        switchLanguage = "Bahasa"
    )

    fun forLang(lang: AppLanguage): StringsDict = when (lang) {
        AppLanguage.EN -> english
        AppLanguage.ID -> indonesian
    }
}

