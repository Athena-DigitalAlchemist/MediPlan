package com.example.mediplan.utils

import android.view.View

fun View.fadeIn() {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate()
        .alpha(1f)
        .setDuration(300)
        .start()
}

fun View.fadeOut(onComplete: () -> Unit = {}) {
    this.animate()
        .alpha(0f)
        .setDuration(300)
        .withEndAction {
            this.visibility = View.GONE
            onComplete()
        }
        .start()
} 