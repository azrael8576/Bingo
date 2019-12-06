package com.alex.bingo.Bean

import android.content.Context
import android.util.AttributeSet

class NumberButton : androidx.appcompat.widget.AppCompatButton {
    var number: Int = 0
    var isPicked: Boolean = false
    var position: Int = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
}