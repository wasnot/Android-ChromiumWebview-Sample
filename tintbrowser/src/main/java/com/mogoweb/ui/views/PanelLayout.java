/*
 * Tint Browser for Android
 * 
 * Copyright (C) 2012 - to infinity and beyond J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.mogoweb.ui.views;

import com.mogoweb.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;

public class PanelLayout extends RelativeLayout {
	
	public interface PanelEventsListener {
		void onPanelShown();
		void onPanelHidden();
	}
	
	private static final int ANIMATION_DURATION = 150;
	private static final int BEZEL_SIZE_REDUCED = 5;
	private static final int BEZEL_SIZE_STANDARD = 10;
	private static final int BEZEL_SIZE_OPENED = 100;

	private Animator mAnimator;

	private boolean mPanelShown;

	private AnimatorListener mShowListener;
	private AnimatorListener mHideListener;

	private RelativeLayout mContent;
	private RelativeLayout mPanel;
	
	private TabsScroller mTabsScroller;

	private boolean mInSlide;
	private float mBezelTopDelta;
	private float mBezelSizeReduced;
	private float mBezelSizeStandard;
	private float mBezelSizeOpened;
	private float mLastX;
	private float mTranslation;
	private float mAlpha;
	private boolean mLastMoveOpen;
	
	private PanelEventsListener mListener;

	public PanelLayout(Context context) {
		this(context, null);
	}

	public PanelLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PanelLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mListener = null;
		
		mInSlide = false;
		mLastMoveOpen = false;
		mTranslation = 0;
		mAlpha = 0;

		mPanelShown = false;
		mAnimator = null;

		if (!isInEditMode()) {
			
			TypedValue tv = new TypedValue();
			context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
			
			mBezelTopDelta = getResources().getDimension(tv.resourceId);
			
			float density = context.getResources().getDisplayMetrics().density;
			
			mBezelSizeReduced = BEZEL_SIZE_REDUCED * density + 0.5f;
			mBezelSizeStandard = BEZEL_SIZE_STANDARD * density + 0.5f;
			mBezelSizeOpened = BEZEL_SIZE_OPENED * density + 0.5f;
			
			LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = layoutInflater.inflate(R.layout.panel_layout, this);

			mContent = (RelativeLayout) v.findViewById(R.id.main_content);
			mPanel = (RelativeLayout) v.findViewById(R.id.panel);

			mTabsScroller = (TabsScroller) v.findViewById(R.id.tabs_scroller);
						
			mShowListener = new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mAnimator = null;
					mPanel.requestLayout();
					mPanelShown = true;
					mTranslation = mPanel.getWidth();
					mAlpha = 1;
					
					if (mListener != null) {
						mListener.onPanelShown();
					}
				}               
			};

			mHideListener = new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mAnimator = null;
					mPanelShown = false;
					mTranslation = 0;
					mAlpha = 0;
					
					if (mListener != null) {
						mListener.onPanelHidden();
					}
				}
			};
		}
	}
	
	public void setPanelEventsListener(PanelEventsListener listener) {
		mListener = listener;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			float y = ev.getY();
			
			if (y > mBezelTopDelta) {
				float x = ev.getX();
				
				float bezelSize;
				if (mPanelShown) {
					bezelSize = mBezelSizeOpened;
				} else {
					float height = mPanel.getHeight() - mBezelTopDelta;
					if ((y - mBezelTopDelta <= 0.1 * height) ||
							(y - mBezelTopDelta >= 0.9 * height)) {
						bezelSize = mBezelSizeReduced;
					} else {
						bezelSize = mBezelSizeStandard;
					}
				}
				
				if ((x >= mTranslation) &&
						(x <= mTranslation + bezelSize)) {
					return true;
				}
			}

			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_MOVE:
			if (mInSlide) {
				return true;
			}			

			break;
		}

		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			float y = event.getY();
			
			if (y > mBezelTopDelta) {
				float x = event.getX();
				
				float bezelSize;
				if (mPanelShown) {
					bezelSize = mBezelSizeOpened;
				} else {
					float height = mPanel.getHeight() - mBezelTopDelta;
					if ((y - mBezelTopDelta <= 0.1 * height) ||
							(y - mBezelTopDelta >= 0.9 * height)) {
						bezelSize = mBezelSizeReduced;
					} else {
						bezelSize = mBezelSizeStandard;
					}
				}
				
				if ((x >= mTranslation) &&
						(x <= mTranslation + bezelSize)) {
					
					mInSlide = true;
					mLastX = event.getX();

					return true;
				}
			}

			break;

		case MotionEvent.ACTION_UP:
			if (mInSlide) {
				mInSlide = false;
				
				if (mLastMoveOpen) {
					if (mTranslation >= 0.2 * mPanel.getWidth()) {
						showPanel();
					} else {
						hidePanel();
					}
				} else {
					if (mTranslation <= 0.9 * mPanel.getWidth()) {
						hidePanel();
					} else {
						showPanel();
					}
				}

				return true;
			}

			break;

		case MotionEvent.ACTION_MOVE:
			if (mInSlide) {
				
				float translation = event.getX() - mLastX;
				
				mLastMoveOpen = translation >= 0;
				
				mTranslation += translation;
				mAlpha = mTranslation / mPanel.getWidth();

				if (mTranslation > mPanel.getWidth()) {
					mTranslation = mPanel.getWidth();
					mAlpha = 1;
					mPanelShown = true;
				}

				if (mTranslation < 0) {
					mTranslation = 0;
					mAlpha = 0;
					mPanelShown = false;
				}

				mLastX = event.getX();

				mContent.setTranslationX(mTranslation);
				mPanel.setAlpha(mAlpha);

				return true;
			}

			break;
		}

		return super.onTouchEvent(event);
	}

	public TabsScroller getTabsScroller() {
		return mTabsScroller;
	}
		
	public void togglePanel() {
		if (mPanelShown) {
			hidePanel();
		} else {
			showPanel();
		}
	}

	public void showPanel() {
		if (mAnimator != null) {
			mAnimator.end();
		}

		mPanel.setAlpha(mAlpha);

		AnimatorSet animator = new AnimatorSet();		

		AnimatorSet.Builder b = animator.play(ObjectAnimator.ofFloat(mPanel, "alpha", 1));

		b.with(ObjectAnimator.ofFloat(mContent, "translationX", mPanel.getWidth()));

		animator.addListener(mShowListener);

		mAnimator = animator;
				
		mAnimator.setDuration((long) (ANIMATION_DURATION * ((mPanel.getWidth() - mTranslation) / mPanel.getWidth())));
		mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

		mAnimator.start();
	}

	public void hidePanel() {
		if (mAnimator != null) {
			mAnimator.end();
		}

		mPanel.setAlpha(mAlpha);

		AnimatorSet animator = new AnimatorSet();
		AnimatorSet.Builder b = animator.play(ObjectAnimator.ofFloat(mPanel, "alpha", 0));

		b.with(ObjectAnimator.ofFloat(mContent, "translationX", 0));

		animator.addListener(mHideListener);

		mAnimator = animator;

		mAnimator.setDuration((long) (ANIMATION_DURATION * (mTranslation / mPanel.getWidth())));
		mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

		mAnimator.start();
	}
	
	public boolean isPanelShown() {
		return mPanelShown;
	}

}
