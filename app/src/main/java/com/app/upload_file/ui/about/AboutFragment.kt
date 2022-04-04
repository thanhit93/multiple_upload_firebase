package com.app.upload_file.ui.about

import android.os.Bundle
import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.app.upload_file.R
import com.app.upload_file.di.component.DaggerFragmentComponent
import com.app.upload_file.di.module.FragmentModule
import kotlinx.android.synthetic.main.fragment_about.*
import javax.inject.Inject

/**
 * Created by tt on 20/02/2021.
 */
class AboutFragment: Fragment(), AboutContract.View {

    @Inject lateinit var presenter: AboutContract.Presenter

    private lateinit var rootView: View

    fun newInstance(): AboutFragment {
        return AboutFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectDependency()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater!!.inflate(R.layout.fragment_about, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attach(this)
        presenter.subscribe()
        initView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.unsubscribe()
    }

    override fun showProgress(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }
    }

    override fun loadMessageSuccess(message: String) {
        aboutText.text = getString(R.string.about_text)
        aboutText.visibility = View.VISIBLE
    }

    private fun injectDependency() {
        val aboutComponent = DaggerFragmentComponent.builder()
                .fragmentModule(FragmentModule())
                .build()

        aboutComponent.inject(this)
    }

    private fun initView() {
        presenter.loadMessage()

        button.setOnClickListener {
            //startActivity(Intent(context, DetailBookActivity::class.java))
        }
    }

    companion object {
        val TAG: String = "AboutFragment"
    }
}