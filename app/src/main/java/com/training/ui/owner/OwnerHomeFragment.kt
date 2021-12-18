package com.training.ui.owner

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.training.R
import com.training.application.MainApplication
import com.training.model.ReservationModel
import com.training.model.StadiumModel
import com.training.model.UserModel
import com.training.states.AppDataState
import com.training.ui.adapters.ReservationAdapter
import com.training.ui.adapters.ReservationAdapterOwner
import com.training.util.constants.DataError
import com.training.viewmodels.DataRetrieveViewModel
import com.training.viewmodels.EditViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_customer_home.*
import kotlinx.android.synthetic.main.fragment_customer_home.progress_bar_CustomerHome
import kotlinx.android.synthetic.main.fragment_owner_home.*
import java.util.*

@AndroidEntryPoint
class OwnerHomeFragment : Fragment() {
    private val viewModelGet: DataRetrieveViewModel by viewModels()
    private val viewModelEdit: EditViewModel by viewModels()
    val recyclerAdapter = ReservationAdapterOwner(::setReservationStatus)
    private lateinit var user: UserModel
    val cal = Calendar.getInstance()
    @RequiresApi(Build.VERSION_CODES.N)
    val today = SimpleDateFormat("MM/dd/yyyy").format(Date(cal.timeInMillis)).toString()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        user = getUserData()
        return inflater.inflate(R.layout.fragment_owner_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OwnerReservationTodayRecycler.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            this.adapter = recyclerAdapter
        }

        observeLiveData()

        if(user.linked) {
            user.stadium_key?.let { viewModelGet.getStadiumDailyReservations(it, today) }
        }else{
            displayErrorView(true)
        }

    }

    private fun setReservationStatus(reservation: ReservationModel) {
        viewModelEdit.updateReservation(reservation)
    }

    private fun displayProgressbar(isDisplayed: Boolean) {
        progress_bar_OwnerHome.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    private fun displayErrorView(isVisible: Boolean) {
        if (isVisible) {
            OwnerHasReservationToday.visibility = View.GONE
            OwnerHasNoReservationToday.visibility = View.VISIBLE
        } else {
            OwnerHasNoReservationToday.visibility = View.GONE
            OwnerHasReservationToday.visibility = View.VISIBLE
        }
    }

    private fun getUserData(): UserModel {
        var sp = requireActivity().getSharedPreferences("onLogged", Context.MODE_PRIVATE)
        val user = UserModel(
            sp.getString("email", "email").toString(),
            sp.getString("password", "pass").toString(),
            sp.getString("fname", "").toString(),
            sp.getString("lname", "").toString(),
            sp.getString("phone", "").toString(),
            sp.getString("user type", "customer").toString(),
            sp.getBoolean("first usage", false),
            sp.getBoolean("linked", false),
            sp.getString("stadium_key", "").toString()
        )
        return user
    }

    private fun observeLiveData() {
        viewModelGet.reservations_retrieveState.observe(this, { data ->
            when (data::class) {
                AppDataState.Loading::class -> {
                    displayProgressbar(true)
                    displayErrorView(false)
                }

                AppDataState.Success::class -> {
                    displayProgressbar(false)
                    displayErrorView(false)
                    val state = data as AppDataState.Success
                    recyclerAdapter.setItem_List(state.data)
                    OwnerReservationTodayRecycler.adapter = recyclerAdapter
                }
                AppDataState.Error::class -> {
                    displayProgressbar(false)
                    val state = data as AppDataState.Error
                    if (state.type == DataError.NO_DATA) {
                        displayErrorView(true)
                    } else
                        Toast.makeText(
                            requireContext(),
                            MainApplication.getApplication().getString(R.string.unexpectedError),
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }
        })

        viewModelEdit.updateState.observe(this, { data ->
            when (data::class) {
                AppDataState.Loading::class -> {
                    displayProgressbar(true)
                }

                AppDataState.OperationSuccess::class -> {
                    displayProgressbar(false)
                    OwnerReservationTodayRecycler.adapter = recyclerAdapter
                    Toast.makeText(
                        requireContext(),
                        MainApplication.getApplication().getString(R.string.statusChanged),
                        Toast.LENGTH_SHORT
                    ).show()

                }

                AppDataState.Error::class -> {
                    displayProgressbar(false)
                    Toast.makeText(
                        requireContext(),
                        MainApplication.getApplication().getString(R.string.unexpectedError),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

}