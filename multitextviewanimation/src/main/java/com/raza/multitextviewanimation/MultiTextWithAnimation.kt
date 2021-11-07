package com.raza.multitextviewanimation

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatTextView
import java.util.concurrent.TimeUnit

open class MultiTextWithAnimation : AppCompatTextView {

    private var timeout: Int = DEFAULT_TIME_OUT
    private var position = 0
    private var isViewShown = false
    private var stopped = false

    private var inAnimation: Animation? = null
    private var outAnimation: Animation? = null
    private var texts: Array<CharSequence>? = null
    private var animationHandler: Handler? = null

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context,
        attrs, defStyle) {
        init()
        handleAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
        init()
        handleAttrs(attrs)
    }

    constructor(context: Context) : super(context) {}

    /**
     * Initialize the view and the animations
     */
    private fun init() {
        inAnimation = AnimationUtils.loadAnimation(context, R.anim.fadein)
        outAnimation = AnimationUtils.loadAnimation(context, R.anim.fadeout)
        animationHandler = Handler()
        this.isViewShown = true
    }


    /**
     * Resumes the animation
     * Should only be used if you notice [.onAttachedToWindow] ()} is not being executed as expected
     */
    fun resume() {
        isViewShown = true
        startAnimation()
    }

    /**
     * Pauses the animation
     * Should only be used if you notice [.onDetachedFromWindow] is not being executed as expected
     */
    fun pause() {
        isViewShown = false
        stopAnimation()
    }


    /**
     * Stops the animation
     * Unlike the pause function, the stop method will permanently stop the animation until the view is restarted
     */
    fun stop() {
        isViewShown = false
        stopped = true
        stopAnimation()
    }

    /**
     * Restarts the animation
     * Only use this to restart the animation after stopping it using [.stop]
     */
    fun restart() {
        isViewShown = true
        stopped = false
        startAnimation()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pause()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resume()
    }


    private fun handleAttrs(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MultiTextWithAnimation)
        this.texts = a.getTextArray(R.styleable.MultiTextWithAnimation_texts)
        val inAnimationRes =
            a.getResourceId(R.styleable.MultiTextWithAnimation_inAnimation, R.anim.fadein)
        val outAnimationRes =
            a.getResourceId(R.styleable.MultiTextWithAnimation_outAnimation, R.anim.fadeout)


        inAnimation = AnimationUtils.loadAnimation(context, inAnimationRes)
        outAnimation = AnimationUtils.loadAnimation(context, outAnimationRes)
        this.timeout = Math.abs(a.getInteger(R.styleable.MultiTextWithAnimation_timeout, 14500)) +
                resources.getInteger(android.R.integer.config_longAnimTime)

        val shuffle = a.getBoolean(R.styleable.MultiTextWithAnimation_shuffle, false)
        if (shuffle) {
            shuffleLabels()
        }

        a.recycle()
    }


    /**
     * Shuffle the strings
     * Each time this method is ran the order of the strings will be randomized
     * After you set texts dynamically you will have to call shuffle again
     */
    private fun shuffleLabels() {
        arrayListOf(texts).shuffle()
        this.texts = texts
    }


    /**
     * Get a list of the texts
     *
     * @return the texts array
     */
    fun getTexts(): Array<CharSequence>? {
        return texts
    }

    /**
     * Sets the texts to be shuffled using a string array
     *
     * @param texts The string array to use for the texts
     */
    fun setTexts(texts: Array<CharSequence>) {
        require(texts.isNotEmpty()) { "There must be at least one text" }
        this.texts = texts
        stopAnimation()
        position = 0
        startAnimation()
    }

    /**
     * Sets the texts to be shuffled using a string array resource
     *
     * @param texts The string array resource to use for the texts
     */
    fun setTexts(texts: Int) {
        require(resources.getStringArray(texts).isNotEmpty()) { "There must be at least one text" }
        this.texts = resources.getTextArray(texts)
        stopAnimation()
        position = 0
        startAnimation()
    }


    /**
     * This method should only be used to forcefully apply timeout changes
     * It will dismiss the currently queued animation change and start a new animation
     */
    fun forceRefresh() {
        stopAnimation()
        startAnimation()
    }

    /**
     * Sets the length of time to wait between text changes in milliseconds
     *
     * @param timeout The length of time to wait between text change in milliseconds
     */
    fun setTimeout(timeout: Int) {
        require(timeout >= 1) { "Timeout must be longer than 0" }
        this.timeout = timeout
    }

    /**
     * Sets the length of time to wait between text changes in specific time units
     *
     * @param timeout  The length of time to wait between text change
     * @param timeUnit The time unit to use for the timeout parameter
     * Must be of [TimeUnit] type.    Either [.MILLISECONDS] or
     * [.SECONDS] or
     * [.MINUTES]
     */
    fun setTimeout(timeout: Double,timeUnit: TimeUnit?) {
        require(timeout > 0) { "Timeout must be longer than 0" }
        val multiplier: Int = when (timeUnit) {
            TimeUnit.MILLISECONDS -> 1
            TimeUnit.SECONDS -> 1000
            TimeUnit.MINUTES -> 60000
            else -> 1
        }
        this.timeout = (timeout * multiplier).toInt()
    }

    fun setTimeout(timeout: Long, timeUnit: TimeUnit?) {
        if (timeout <= 0) {
            throw IllegalArgumentException("Timeout must be longer than 0")
        } else {
            this.timeout = TimeUnit.MILLISECONDS
                .convert(timeout, timeUnit).toInt()
        }
    }

    /**
     * Start the specified animation now if should
     *
     * @param animation the animation to start now
     */
    override fun startAnimation(animation: Animation?) {
        if (isViewShown && !stopped) {
            super.startAnimation(animation)
        }
    }

    /**
     * Stop the currently active animation
     */
    private fun stopAnimation() {
        animationHandler!!.removeCallbacksAndMessages(null)
        if (animation != null) animation.cancel()
    }


    /**
     * Start the animation
     */
    protected fun startAnimation() {
        if (!isInEditMode) {
            text = texts!![position]
            startAnimation(inAnimation)
            animationHandler!!.postDelayed({
                startAnimation(outAnimation)
                if (animation != null) {
                    animation.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}
                        override fun onAnimationEnd(animation: Animation) {
                            if (isViewShown) {
                                position = if (position == texts!!.size - 1) 0 else position + 1
                                startAnimation()
                            }
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })
                }
            }, timeout.toLong())
        }
    }

    companion object {
        const val DEFAULT_TIME_OUT = 15000
        const val MILLISECONDS = 1
        const val SECONDS = 2
        const val MINUTES = 3
    }
}