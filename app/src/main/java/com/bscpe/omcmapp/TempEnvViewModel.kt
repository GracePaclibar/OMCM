import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TempEnvViewModel : ViewModel() {

    private val _selectedFilterPosition = MutableLiveData<Int>()
    val selectedFilterPosition: LiveData<Int> = _selectedFilterPosition

    fun setSelectedFilterPosition(position: Int) {
        _selectedFilterPosition.value = position
    }
}