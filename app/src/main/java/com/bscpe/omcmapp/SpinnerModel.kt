import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SpinnerModel : ViewModel() {
    val selectedPosition = MutableLiveData<Int>()
}