class EditMedicationViewModelFactory(
    private val medicationId: Long,
    private val medicationDao: MedicationDao,
    private val notificationManager: MedicationNotificationManager
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditMedicationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditMedicationViewModel(medicationId, medicationDao, notificationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 