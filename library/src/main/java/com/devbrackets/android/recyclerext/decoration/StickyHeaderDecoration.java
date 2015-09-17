/*
 * Copyright (C) 2015 Brian Wernick
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.devbrackets.android.recyclerext.decoration;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.devbrackets.android.recyclerext.R;
import com.devbrackets.android.recyclerext.adapter.RecyclerHeaderAdapter;

/**
 * A RecyclerView Decoration that allows for Header views from
 * the {@link RecyclerHeaderAdapter} to be persisted when they
 * reach the start of the RecyclerView's frame.
 */
public class StickyHeaderDecoration extends RecyclerView.ItemDecoration {

    public enum LayoutOrientation {
        VERTICAL,
        HORIZONTAL
    }

    @Nullable
    private Bitmap stickyHeader;

    private int stickyStart = 0;
    private LayoutOrientation orientation = LayoutOrientation.VERTICAL;

    public StickyHeaderDecoration(RecyclerView parent) {
        parent.addOnScrollListener(new StickyViewScrollListener());
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (stickyHeader != null) {
            int x = orientation == LayoutOrientation.HORIZONTAL ? stickyStart : 0;
            int y = orientation == LayoutOrientation.HORIZONTAL ? 0 : stickyStart;
            c.drawBitmap(stickyHeader, x, y, null);
        }
    }

    /**
     * Clears the current sticky header from the view.
     */
    public void clearStickyHeader() {
        if (stickyHeader != null) {
            stickyHeader.recycle();
            stickyHeader = null;
        }
    }

    /**
     * Sets the orientation of the current layout
     *
     * @param orientation The layouts orientation
     */
    public void setOrientation(LayoutOrientation orientation) {
        this.orientation = orientation;
    }

    /**
     * Retrieves the current orientation to use for edgeScrolling and position calculations.
     *
     * @return The current orientation [default: {@link LayoutOrientation#VERTICAL}]
     */
    public LayoutOrientation getOrientation() {
        return orientation;
    }

    /**
     * Generates the Bitmap that will be used to represent the view stuck at the top of the
     * parent RecyclerView.
     *
     * @param view The view to create the drag bitmap from
     * @return The bitmap representing the drag view
     */
    private Bitmap createStickyViewBitmap(View view) {
        Rect stickyViewBounds = new Rect(0, 0, view.getRight() - view.getLeft(), view.getBottom() - view.getTop());

        Bitmap bitmap = Bitmap.createBitmap(stickyViewBounds.width(), stickyViewBounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }


    /**
     * Listens to the scroll events for the RecyclerView that will have
     * sticky headers.  When a new header reaches the start it will be
     * transformed in to a sticky view and attached to the start of the
     * RecyclerView.  Additionally, when a new header is reaching the
     * start, the headers will be transitioned smoothly
     * <p>
     * TODO: this doesn't work correctly when scrolling towards the start of the list (header doesn't appear until hitting the view location)
     * NOTE: dx and dy are + scrolling right/down, - left/up (and 0 when no change)
     */
    private class StickyViewScrollListener extends RecyclerView.OnScrollListener {
        private long currentStickyId = Long.MIN_VALUE;
        private int[] windowLocation = new int[2];
        private int parentStart = Integer.MIN_VALUE;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            View nextHeader = findNextHeader(recyclerView);
            if (nextHeader == null) {
                return;
            }

            //If the next header is different than the current one, perform the swap
            Long headerId = (Long) nextHeader.getTag(R.id.sticky_view_header_id);
            if (headerId != null && headerId != currentStickyId) {
                performStickyHeaderSwap(nextHeader, headerId);
            }
        }

        /**
         * If the next header is at the start of the RecyclerView then the
         * {@link #stickyHeader} will be updated.  Otherwise the position of the
         * {@link #stickyHeader} will be updated so that it will smoothly move off
         * the screen as the <code>nextHeader</code> view reaches the top.
         *
         * @param nextHeader The header view to replace the current one
         * @param headerId   The id for the header view
         */
        private void performStickyHeaderSwap(View nextHeader, long headerId) {
            int nextHeaderStart = orientation == LayoutOrientation.HORIZONTAL ? windowLocation[0] : windowLocation[1];
            int trueStart = nextHeaderStart - parentStart;

            if (stickyHeader != null && trueStart > 0) {
                stickyStart = trueStart - (orientation == LayoutOrientation.HORIZONTAL ? stickyHeader.getWidth() : stickyHeader.getHeight());
            } else {
                stickyStart = 0;
                currentStickyId = headerId;
                stickyHeader = createStickyViewBitmap(nextHeader);
            }
        }

        /**
         * Finds the next visible header view.
         *
         * @param recyclerView The RecyclerView to find the next header for
         * @return The next header or null
         */
        @Nullable
        private View findNextHeader(RecyclerView recyclerView) {
            int attachedViewCount = recyclerView.getLayoutManager().getChildCount();
            if (attachedViewCount <= 0) {
                return null;
            }

            //Make sure we have the start of the RecyclerView stored
            if (parentStart == Integer.MIN_VALUE) {
                recyclerView.getLocationInWindow(windowLocation);
                parentStart = orientation == LayoutOrientation.HORIZONTAL ? windowLocation[0] : windowLocation[1];
            }

            //Determines the max start position to look for the next sticky header
            int maxStartPosition = parentStart;
            if (stickyHeader != null) {
                if (orientation == LayoutOrientation.HORIZONTAL) {
                    maxStartPosition += stickyHeader.getWidth() + 1;
                } else {
                    maxStartPosition += stickyHeader.getHeight() + 1;
                }
            }

            //Attempts to find the first header
            for (int viewIndex = 0; viewIndex < attachedViewCount; viewIndex++) {
                View view = recyclerView.getLayoutManager().getChildAt(viewIndex);
                view.getLocationInWindow(windowLocation);

                //If the start location is greater than the max, we don't have a header to worry about
                int startLoc = orientation == LayoutOrientation.HORIZONTAL ? windowLocation[0] : windowLocation[1];
                if (startLoc > maxStartPosition) {
                    return null;
                }

                //Determine if the view is a header to return
                Integer type = (Integer) view.getTag(R.id.sticky_view_type_tag);
                if (type != null && type == RecyclerHeaderAdapter.VIEW_TYPE_HEADER) {
                    return view;
                }
            }

            //We shouldn't reach this under normal circumstances
            return null;
        }
    }
}
