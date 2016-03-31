package com.datonicgroup.narrate.app.ui.entries;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.StringSignature;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.views.BookmarkView;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.PaletteLoader;
import com.datonicgroup.narrate.app.util.PaletteRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Adapter that handles images coming from backend.
 * <p/>
 * Lots of code borrowed from Wally.
 * <p/>
 * https://github.com/Musenkishi/wally/blob/master/wally/src/main/java/com/musenkishi/wally/adapters/RecyclerImagesAdapter.java
 */
public class RecyclerEntriesGridAdapter extends RecyclerView.Adapter<RecyclerEntriesGridAdapter.ViewHolder> {

    protected int itemSize;

    public void setItemSize(int itemSize) {
        this.itemSize = itemSize;
    }

    private OnFavoriteClickedListener onFavoriteClickedListener;
    private OnItemClickListener onItemClickListener;
    private OnGetViewListener onGetViewListener;

    private ArrayList<Entry> entries;
    private int barHeight;
    private int textHeight;

    private final int[] mColors;

    private HashMap<String, Boolean> existingFiles = new HashMap<String, Boolean>();
    private static HashMap<String, Integer> mColorMap = new HashMap<>();

    /**
     * Don't use this constructor.
     */
    public RecyclerEntriesGridAdapter() {
        throw new NoDataException("No data set. Did you use the correct constructor?");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = (LayoutInflater) viewGroup.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.view_cell_thumb_tile, viewGroup, false);
        if (barHeight == 0) {
            barHeight = view.getResources().getDimensionPixelSize(R.dimen.default_height);
            textHeight = view.getResources().getDimensionPixelSize(R.dimen.thumb_cell_text_height);
        }
        view.getLayoutParams().height = itemSize + barHeight;
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        if (onGetViewListener != null) {
            onGetViewListener.onBindView(position);
        }

        final Entry entry = getItem(position);

        if (entry != null) {

            viewHolder.bottomBar.setBackgroundColor(viewHolder.bottomBar.getContext().getResources().getColor(R.color.Transparent));
            viewHolder.textViewTitle.setTextColor(viewHolder.bottomBar.getContext().getResources().getColor(R.color.Thumb_Text));


            /*if (entry.photos.isEmpty()) {

                viewHolder.textViewBody.setVisibility(View.VISIBLE);
                viewHolder.textViewBody.setText(entry.text.substring(0, Math.min(1000, entry.text.length())));
                viewHolder.imageView.setVisibility(View.GONE);

                viewHolder.bottomBar.setBackgroundResource(R.color.Thumb_Background);

                View mBackgroundView = ((ViewGroup) ((ViewGroup) viewHolder.itemView).getChildAt(0)).getChildAt(0);
                if (mColorMap.containsKey(entry.uuid)) {
                    mBackgroundView.setBackgroundColor(mColorMap.get(entry.uuid));
                } else {
                    int color = mColors[MathUtil.randomNumberInRange(0, mColors.length - 1)];
                    mColorMap.put(entry.uuid, color);
                    mBackgroundView.setBackgroundColor(color);
                }

            } else {*/

                final RequestListener<String, GlideDrawable> glideDrawableRequestListener = new RequestListener<String, GlideDrawable>() {

                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        LogUtil.log("", "onException()");
                        if ( e != null )
                            e.printStackTrace();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        LogUtil.log("", "onResourceReady()");
                        Bitmap bitmap = ((GlideBitmapDrawable) resource).getBitmap();
                        if (bitmap != null) {
                            Context context = viewHolder.bottomBar.getContext();
                            PaletteLoader.with(context, model)
                                    .load(bitmap)
                                    .setPaletteRequest(new PaletteRequest(PaletteRequest.SwatchType.REGULAR_VIBRANT, PaletteRequest.SwatchColor.BACKGROUND))
                                    .into(viewHolder.bottomBar);
                            PaletteLoader.with(context, model)
                                    .load(bitmap)
                                    .setPaletteRequest(new PaletteRequest(PaletteRequest.SwatchType.REGULAR_VIBRANT, PaletteRequest.SwatchColor.TEXT_TITLE))
                                    .into(viewHolder.textViewTitle);
//                            PaletteLoader.with(context, model)
//                                    .load(bitmap)
//                                    .fallbackColor(viewHolder.textViewTitle.getCurrentTextColor())
//                                    .setPaletteRequest(new PaletteRequest(PaletteRequest.SwatchType.REGULAR_VIBRANT, PaletteRequest.SwatchColor.TEXT_TITLE))
//                                    .mask()
//                                    .into(viewHolder.imageButton);
                        }
                        return false;
                    }
                };

            String path = entry.photos.get(0).path;
            final File image = new File(path);
                Glide.with(GlobalApplication.getAppContext())
                        .load(path)
                        .fitCenter()
                        .placeholder(R.color.transparent)
                        .listener(glideDrawableRequestListener)
                        .signature(new StringSignature(String.valueOf(image.lastModified())))
                        .into(viewHolder.imageView);

//            }

            viewHolder.textViewTitle.setText(entry.title);
            viewHolder.bookmark.clearAnimation();

            if (entry.starred) {
                viewHolder.bookmark.setTranslationY(0);
                viewHolder.bookmark.setTranslationX(0);
                viewHolder.bookmark.setAlpha(1f);
                viewHolder.bookmark.setScaleX(1f);
                viewHolder.bookmark.setScaleY(1f);
                viewHolder.bookmark.setVisibility(View.VISIBLE);
            } else {
                viewHolder.bookmark.setVisibility(View.INVISIBLE);
            }

            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (entry.starred) {
                        ObjectAnimator alpha = ObjectAnimator.ofFloat(viewHolder.bookmark, "alpha", 1f, 0f);
                        alpha.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (onFavoriteClickedListener != null)
                                    onFavoriteClickedListener.onFavoriteButtonClicked(entry, position);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                if (onFavoriteClickedListener != null)
                                    onFavoriteClickedListener.onFavoriteButtonClicked(entry, position);
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        alpha.start();
                    } else {
                        int scaleX = 2;
                        int scaleY = 2;

                        int transX = (viewHolder.itemView.getWidth() - (scaleX * viewHolder.bookmark.getWidth())) / 2;
                        int transY = (viewHolder.itemView.getHeight() - (scaleY * viewHolder.bookmark.getHeight())) / 2;

                        viewHolder.bookmark.setAlpha(0f);
                        viewHolder.bookmark.setVisibility(View.VISIBLE);
                        viewHolder.bookmark.setScaleX(scaleX);
                        viewHolder.bookmark.setScaleY(scaleY);
                        viewHolder.bookmark.setTranslationX(-transX);
                        viewHolder.bookmark.setTranslationY(transY);

                        ObjectAnimator alpha = ObjectAnimator.ofFloat(viewHolder.bookmark, "alpha", 0f, 1f);

                        ObjectAnimator translationX = ObjectAnimator.ofFloat(viewHolder.bookmark, "translationX", -transX, 0f);
                        ObjectAnimator translationY = ObjectAnimator.ofFloat(viewHolder.bookmark, "translationY", transY, 0f);
                        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(viewHolder.bookmark, "scaleX", scaleX, 1f);
                        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(viewHolder.bookmark, "scaleY", scaleY, 1f);

                        AnimatorSet transAnim = new AnimatorSet();
                        transAnim.playTogether(translationX, translationY, scaleXAnim, scaleYAnim);
                        transAnim.setInterpolator(new DecelerateInterpolator());
                        transAnim.setDuration(300);
                        transAnim.setStartDelay(150);

                        AnimatorSet anim = new AnimatorSet();
                        anim.playSequentially(alpha, transAnim);
                        anim.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (onFavoriteClickedListener != null)
                                    onFavoriteClickedListener.onFavoriteButtonClicked(entry, position);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                if (onFavoriteClickedListener != null)
                                    onFavoriteClickedListener.onFavoriteButtonClicked(entry, position);
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        anim.start();

//                        viewHolder.bookmark.animate()
//                                .setDuration(1000)
//                                .alpha(1f)
//                                .scaleX(1f)
//                                .scaleY(1f)
//                                .translationX(0)
//                                .translationY(0)
//                                .start();
                    }

                    return true;
                }
            });
        }

        viewHolder.itemView.getLayoutParams().height = itemSize + barHeight;
        viewHolder.itemView.getLayoutParams().width = itemSize;
    }

    public RecyclerEntriesGridAdapter(ArrayList<Entry> entries, int itemSize) {
        this.entries = entries;
        this.itemSize = itemSize;
        this.mColors = GlobalApplication.getAppContext().getResources().getIntArray(R.array.grid_bg_colors);
    }

    public Entry getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    public ArrayList<Entry> getEntries() {
        return entries;
    }

    public void setOnFavoriteClickedListener(OnFavoriteClickedListener onFavoriteClickedListener) {
        this.onFavoriteClickedListener = onFavoriteClickedListener;
    }

    public void updateSavedFilesList(HashMap<String, Boolean> savedFilesList) {
        existingFiles = savedFilesList;
    }

    /**
     * Will loop through all items in the adapter and check if any are included in the existing files map.
     * For each match, a {@code notifyItemChanged()} will be called.
     */
    public void notifySavedItemsChanged() {
        for (int i = 0; i < getItemCount(); i++) {
            Entry entry = getItem(i);
            if (existingFiles.containsKey(entry.uuid)) {
                notifyItemChanged(i);
            }
        }
    }

    public interface OnFavoriteClickedListener {
        abstract void onFavoriteButtonClicked(Entry entry, int position);
    }

    private class NoDataException extends NullPointerException {
        private NoDataException(String detailMessage) {
            super(detailMessage);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        ImageView imageView;
        ViewGroup bottomBar;
        BookmarkView bookmark;
        TextView textViewTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            imageView = (ImageView) itemView.findViewById(R.id.thumb_image_view);
            bookmark = (BookmarkView) itemView.findViewById(R.id.bookmark);
            textViewTitle = (TextView) itemView.findViewById(R.id.thumb_text_title);
            bottomBar = (ViewGroup) itemView.findViewById(R.id.thumb_bottom_view);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(v, getPosition());
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        abstract void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnGetViewListener {
        abstract void onBindView(int position);
    }

    public void setOnGetViewListener(OnGetViewListener onGetViewListener) {
        this.onGetViewListener = onGetViewListener;
    }
}