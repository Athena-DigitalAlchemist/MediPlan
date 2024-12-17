package com.example.mediplan.ui

import android.content.Context
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AnimatedLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun addView(child: View) {
        super.addView(child)
        animateViewIn(child)
    }

    override fun addView(child: View, index: Int) {
        super.addView(child, index)
        animateViewIn(child)
    }

    private fun animateViewIn(view: View) {
        view.alpha = 0f
        view.translationY = 100f

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
} 