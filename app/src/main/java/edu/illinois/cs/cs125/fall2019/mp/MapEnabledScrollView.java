package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * A ScrollView that allows map fragments inside it to be panned vertically.
 * <p>
 * The normal ScrollView intercepts vertical scroll events, so the overall view is scrolled
 * rather than the map inside.
 * <p>
 * Based on https://stackoverflow.com/a/37493838/
 * <p>
 * STOP! Do not modify this file. Changes will be overwritten during official grading.
 */
public final class MapEnabledScrollView extends ScrollView {

    /**
     * Creates a MapEnabledScrollView UI control.
     * @param context a UI context
     */
    public MapEnabledScrollView(final Context context) {
        super(context);
    }

    /**
     * Creates a MapEnabledScrollView UI control.
     * @param context a UI context
     * @param attrs the AttributeSet passed to the ScrollView constructor
     */
    public MapEnabledScrollView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Creates a MapEnabledScrollView UI control.
     * @param context a UI context
     * @param attrs the AttributeSet passed to the ScrollView constructor
     * @param defStyle an @attr/ resource ID
     */
    public MapEnabledScrollView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Creates a MapEnabledScrollView UI control.
     * @param context a UI context
     * @param attrs the AttributeSet passed to the ScrollView constructor
     * @param defStyle an @attr/ resource ID
     * @param defStyleRes a @style/ resource ID
     */
    public MapEnabledScrollView(final Context context, final AttributeSet attrs, final int defStyle,
                                final int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);
    }

    /**
     * Called when a touch event is being sent down the view hierarchy.
     * <p>
     * Usually ScrollView intercepts some events, in which case the views contained in it
     * do not receive them. This implementation never steals the events.
     * @param ev the event
     * @return always false
     */
    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_DOWN:
                super.onTouchEvent(ev);
                // Fall through
            default:
                return false;
        }
    }

    /**
     * Called when this view needs to handle a touch event.
     * <p>
     * This implementation does the same thing as a normal ScrollView except that it always
     * says the event was handled.
     * @param ev the event
     * @return always true
     */
    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        super.onTouchEvent(ev);
        return true;
    }

}
