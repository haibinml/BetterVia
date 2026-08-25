package com.jiguro.bettervia;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import fastscroll.FastScroll;
import fastscroll.FastScrollRecyclerView;
import fastscroll.StretchEdgeEffectFactory;
import java.util.ArrayList;
import java.util.List;

public class SettingsList extends FrameLayout {

  private static final int TYPE_HEADER = 0;
  private static final int TYPE_ITEM = 1;
  private static final int TYPE_SWITCH = 2;
  private static final int TYPE_RADIO = 3;
  private static final int TYPE_CUSTOM = 4;

  private static final int THUMB_DEFAULT_COLOR = 0xFF808080;

  private final Context context;
  private final List<Item> items = new ArrayList<Item>();
  private final SettingsAdapter adapter;
  private final FastScrollRecyclerView recyclerView;

  private Runnable customCardThemeReapply;

  public void setCustomCardThemeReapply(Runnable callback) {
    this.customCardThemeReapply = callback;
  }

  public SettingsList(Context context) {
    super(context);
    this.context = context;

    this.recyclerView = new FastScrollRecyclerView(context);
    recyclerView.setLayoutManager(new LinearLayoutManager(context));
    recyclerView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
    recyclerView.setEdgeEffectFactory(new StretchEdgeEffectFactory());
    addView(
        recyclerView,
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    adapter = new SettingsAdapter();
    recyclerView.setAdapter(adapter);
    recyclerView.setItemAnimator(null);

    FastScroll fastScroll = recyclerView.getFastScrollDelegate();
    fastScroll.setEnabled(true);
    fastScroll.setThumbSize(24, 64);
    boolean rtl = isRtl(context);
    fastScroll.setRtl(rtl);
    fastScroll.setTouchSlop(dp(context, 4));
    fastScroll.setThumbDrawable(createThumbDrawable(context, rtl, 24, THUMB_DEFAULT_COLOR));
  }

  public void addItem(String titleKey, Runnable onOpen) {
    addItem(titleKey, null, onOpen);
  }

  public void addItem(String titleKey, String subtitleKey, Runnable onOpen) {
    Item item = new Item(TYPE_ITEM);
    item.titleKey = titleKey;
    item.subtitleKey = subtitleKey;
    item.onOpen = onOpen;
    items.add(item);
    adapter.notifyItemInserted(items.size() - 1);
  }

  public void addSectionHeader(String titleKey) {
    Item item = new Item(TYPE_HEADER);
    item.titleKey = titleKey;
    items.add(item);
    adapter.notifyItemInserted(items.size() - 1);
  }

  public void addSwitchItem(
      String titleKey,
      String subtitleKey,
      boolean checked,
      CompoundButton.OnCheckedChangeListener listener) {
    Item item = new Item(TYPE_SWITCH);
    item.titleKey = titleKey;
    item.subtitleKey = subtitleKey;
    item.checked = checked;
    item.switchListener = listener;
    items.add(item);
    adapter.notifyItemInserted(items.size() - 1);
  }

  public void addRadioItem(String title, boolean checked, Runnable onSelect) {
    addRadioItem(title, checked, false, onSelect);
  }

  public void addRadioItem(String title, boolean checked, boolean recommend, Runnable onSelect) {
    Item item = new Item(TYPE_RADIO);
    item.radioTitle = title;
    item.checked = checked;
    item.recommend = recommend;
    item.onSelect = onSelect;
    items.add(item);
    adapter.notifyItemInserted(items.size() - 1);
  }

  public void addCustom(
      View view, int marginLeftDp, int marginTopDp, int marginRightDp, int marginBottomDp) {
    Item item = new Item(TYPE_CUSTOM);
    item.customView = view;
    item.customMarginLeft = marginLeftDp;
    item.customMarginTop = marginTopDp;
    item.customMarginRight = marginRightDp;
    item.customMarginBottom = marginBottomDp;
    items.add(item);
    adapter.notifyItemInserted(items.size() - 1);
  }

  public int getItemCount() {
    return items.size();
  }

  public void replaceCustomView(int index, View newView) {
    if (index < 0 || index >= items.size()) return;
    Item item = items.get(index);
    if (item.type != TYPE_CUSTOM) return;
    item.customView = newView;
    adapter.notifyItemChanged(index);
  }

  public void updateItem(int index, String subtitleKey, boolean enabled) {
    if (index < 0 || index >= items.size()) {
      return;
    }
    Item it = items.get(index);
    it.subtitleKey = subtitleKey;
    it.subtitleText = null;
    it.enabled = enabled;
    adapter.notifyItemChanged(index);
  }

  public void updateItemText(int index, CharSequence subtitleText, boolean enabled) {
    if (index < 0 || index >= items.size()) {
      return;
    }
    Item it = items.get(index);
    it.subtitleText = subtitleText;
    it.subtitleKey = null;
    it.enabled = enabled;
    adapter.notifyItemChanged(index);
  }

  public void updateSwitch(int index, boolean checked) {
    if (index < 0 || index >= items.size()) {
      return;
    }
    Item it = items.get(index);
    it.checked = checked;
    adapter.notifyItemChanged(index);
  }

  public void reapplyTheme() {
    try {
      adapter.notifyDataSetChanged();
      FastScroll fastScroll = recyclerView.getFastScrollDelegate();
      fastScroll.setThumbDrawable(
          createThumbDrawable(context, isRtl(context), 24, THUMB_DEFAULT_COLOR));
      if (customCardThemeReapply != null) customCardThemeReapply.run();
    } catch (Throwable t) {
    }
  }

  private static boolean isRtl(Context ctx) {
    return TextUtils.getLayoutDirectionFromLocale(ctx.getResources().getConfiguration().locale)
        == View.LAYOUT_DIRECTION_RTL;
  }

  private static int dp(Context ctx, int value) {
    return (int) (ctx.getResources().getDisplayMetrics().density * value + 0.5f);
  }

  private static Drawable createThumbDrawable(
      Context ctx, boolean rtl, int widthDp, int defaultColor) {
    int pressedColor =
        Hook.isDarkTheme(ctx) ? ThemeColors.DARK_SWITCH_ON_COLOR : ThemeColors.SWITCH_FILL_COLOR;
    int insetA = dp(ctx, widthDp - 6);
    int insetB = dp(ctx, widthDp - 5);
    float corner = dp(ctx, 999);

    float[] radii = new float[8];
    if (!rtl) {
      radii[0] = corner;
      radii[1] = corner;
      radii[2] = 0;
      radii[3] = 0;
      radii[4] = 0;
      radii[5] = 0;
      radii[6] = corner;
      radii[7] = corner;
    } else {
      radii[0] = 0;
      radii[1] = 0;
      radii[2] = corner;
      radii[3] = corner;
      radii[4] = corner;
      radii[5] = corner;
      radii[6] = 0;
      radii[7] = 0;
    }

    GradientDrawable pressed = new GradientDrawable();
    pressed.setShape(GradientDrawable.RECTANGLE);
    pressed.setCornerRadii(radii);
    pressed.setColor(pressedColor);

    GradientDrawable normal = new GradientDrawable();
    normal.setShape(GradientDrawable.RECTANGLE);
    normal.setCornerRadii(radii);
    normal.setColor(defaultColor);

    LayerDrawable pressedLayer = new LayerDrawable(new Drawable[] {pressed});
    LayerDrawable normalLayer = new LayerDrawable(new Drawable[] {normal});
    if (rtl) {
      pressedLayer.setLayerInset(0, 0, 0, insetA, 0);
      normalLayer.setLayerInset(0, 0, 0, insetB, 0);
    } else {
      pressedLayer.setLayerInset(0, insetA, 0, 0, 0);
      normalLayer.setLayerInset(0, insetB, 0, 0, 0);
    }

    StateListDrawable sld = new StateListDrawable();
    sld.addState(new int[] {android.R.attr.state_pressed}, pressedLayer);
    sld.addState(new int[0], normalLayer);
    return sld;
  }

  private static class Item {
    final int type;

    String titleKey;
    String subtitleKey;
    CharSequence subtitleText;
    Runnable onOpen;
    boolean checked;
    boolean enabled = true;
    CompoundButton.OnCheckedChangeListener switchListener;
    String radioTitle;
    Runnable onSelect;
    boolean recommend;
    View customView;
    int customMarginLeft;
    int customMarginTop;
    int customMarginRight;
    int customMarginBottom;

    Item(int type) {
      this.type = type;
    }
  }

  private class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      switch (viewType) {
        case TYPE_HEADER:
          ViewHolder headerHolder = wrap(createHeaderRow());
          headerHolder.header = (TextView) headerHolder.itemView;
          return headerHolder;
        case TYPE_ITEM:
          return wrap(createItemRow());
        case TYPE_SWITCH:
          return wrap(createSwitchRow());
        case TYPE_RADIO:
          return wrap(createRadioRow());
        case TYPE_CUSTOM:
        default:
          return wrap(createCustomRow());
      }
    }

    private ViewHolder wrap(View itemView) {
      itemView.setLayoutParams(
          new RecyclerView.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      Item item = items.get(position);
      switch (item.type) {
        case TYPE_HEADER:
          holder.header.setText(LocalizedStringProvider.getInstance().get(context, item.titleKey));
          break;
        case TYPE_ITEM:
          bindItemRow(holder, item);
          break;
        case TYPE_SWITCH:
          bindSwitchRow(holder, item);
          break;
        case TYPE_RADIO:
          bindRadioRow(holder, item);
          break;
        case TYPE_CUSTOM:
          bindCustomRow(holder, item);
          break;
      }
    }

    @Override
    public int getItemCount() {
      return items.size();
    }

    @Override
    public int getItemViewType(int position) {
      return items.get(position).type;
    }

    private View createHeaderRow() {
      TextView header = new TextView(context);
      header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      header.setTextColor(ThemeColors.SECTION_HEADER_TEXT_COLOR);
      header.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 4));
      return header;
    }

    private View createItemRow() {
      LinearLayout item = baseRow();
      LinearLayout textCol = new LinearLayout(context);
      textCol.setOrientation(LinearLayout.VERTICAL);
      TextView title = createTitle();
      textCol.addView(title);
      item.addView(
          textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
      return item;
    }

    private View createSwitchRow() {
      LinearLayout item = baseRow();
      LinearLayout textCol = new LinearLayout(context);
      textCol.setOrientation(LinearLayout.VERTICAL);
      TextView title = createTitle();
      textCol.addView(title);
      item.addView(
          textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
      CheckBox sw = new CheckBox(context);
      sw.setButtonDrawable(SettingsUI.createCircleToggleDrawable(context));
      sw.setClickable(false);
      sw.setFocusable(false);
      LinearLayout.LayoutParams swParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      swParams.setMarginStart(dp(context, 16));
      item.addView(sw, swParams);
      return item;
    }

    private View createRadioRow() {
      LinearLayout item = baseRow();
      TextView title = createTitle();
      item.addView(
          title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
      CheckBox radio = new CheckBox(context);
      radio.setButtonDrawable(SettingsUI.createCircleToggleDrawable(context));
      radio.setClickable(false);
      radio.setFocusable(false);
      LinearLayout.LayoutParams radioParams =
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      radioParams.setMarginStart(dp(context, 16));
      item.addView(radio, radioParams);
      return item;
    }

    private View createCustomRow() {
      return new FrameLayout(context);
    }

    private LinearLayout baseRow() {
      LinearLayout item = new LinearLayout(context);
      item.setOrientation(LinearLayout.HORIZONTAL);
      item.setGravity(Gravity.CENTER_VERTICAL);
      item.setPadding(dp(context, 16), dp(context, 20), dp(context, 16), dp(context, 20));
      item.setBackground(SettingsUI.createRippleDrawable(context));
      return item;
    }

    private TextView createTitle() {
      TextView title = new TextView(context);
      title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      title.setTextColor(Hook.getTextColorStatic(context));
      title.setSingleLine(true);
      title.setEllipsize(TextUtils.TruncateAt.END);
      return title;
    }

    private TextView createSubtitle() {
      TextView subtitle = new TextView(context);
      subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      subtitle.setTextColor(Hook.getHintColorStatic(context));
      subtitle.setMaxLines(3);
      subtitle.setEllipsize(TextUtils.TruncateAt.END);
      return subtitle;
    }

    private void bindItemRow(final ViewHolder holder, final Item item) {
      LinearLayout textCol = (LinearLayout) ((LinearLayout) holder.itemView).getChildAt(0);
      TextView title = (TextView) textCol.getChildAt(0);
      title.setText(LocalizedStringProvider.getInstance().get(context, item.titleKey));
      title.setTextColor(Hook.getTextColorStatic(context));
      TextView subtitle;
      if (textCol.getChildCount() > 1) {
        subtitle = (TextView) textCol.getChildAt(1);
      } else {
        subtitle = createSubtitle();
        textCol.addView(subtitle);
      }
      if (item.subtitleText != null) {
        subtitle.setText(item.subtitleText);
        subtitle.setVisibility(View.VISIBLE);
      } else if (item.subtitleKey != null) {
        subtitle.setText(LocalizedStringProvider.getInstance().get(context, item.subtitleKey));
        subtitle.setVisibility(View.VISIBLE);
      } else {
        subtitle.setVisibility(View.GONE);
      }
      subtitle.setTextColor(Hook.getHintColorStatic(context));
      setRowClick(holder.itemView, item.onOpen);
      holder.itemView.setClickable(item.enabled);
      holder.itemView.setAlpha(item.enabled ? 1.0f : 0.4f);
    }

    private void bindSwitchRow(final ViewHolder holder, final Item item) {
      LinearLayout row = (LinearLayout) holder.itemView;
      LinearLayout textCol = (LinearLayout) row.getChildAt(0);
      TextView title = (TextView) textCol.getChildAt(0);
      title.setText(LocalizedStringProvider.getInstance().get(context, item.titleKey));
      title.setTextColor(Hook.getTextColorStatic(context));
      TextView subtitle;
      if (textCol.getChildCount() > 1) {
        subtitle = (TextView) textCol.getChildAt(1);
      } else {
        subtitle = createSubtitle();
        textCol.addView(subtitle);
      }
      if (item.subtitleText != null) {
        subtitle.setText(item.subtitleText);
        subtitle.setVisibility(View.VISIBLE);
      } else if (item.subtitleKey != null) {
        subtitle.setText(LocalizedStringProvider.getInstance().get(context, item.subtitleKey));
        subtitle.setVisibility(View.VISIBLE);
      } else {
        subtitle.setVisibility(View.GONE);
      }
      subtitle.setTextColor(Hook.getHintColorStatic(context));
      final CheckBox sw = (CheckBox) row.getChildAt(1);
      sw.setOnCheckedChangeListener(null);
      sw.setChecked(item.checked);
      sw.setOnCheckedChangeListener(item.switchListener);
      row.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              sw.setChecked(!sw.isChecked());
            }
          });
    }

    private void bindRadioRow(final ViewHolder holder, final Item item) {
      LinearLayout row = (LinearLayout) holder.itemView;
      TextView title = (TextView) row.getChildAt(0);
      title.setText(item.radioTitle);
      title.setTextColor(Hook.getTextColorStatic(context));
      final CheckBox radio = (CheckBox) row.getChildAt(1);
      radio.setChecked(item.checked);
      Object oldTag = row.getTag();
      if (oldTag instanceof android.animation.ValueAnimator) {
        ((android.animation.ValueAnimator) oldTag).cancel();
      }
      row.getOverlay().clear();
      if (item.recommend) {
        startRecommendBlink(row);
      }
      setRowClick(row, item.onSelect);
    }

    private void startRecommendBlink(final View row) {
      try {
        final GradientDrawable highlight = new GradientDrawable();
        highlight.setShape(GradientDrawable.RECTANGLE);
        int accent = Hook.isDarkTheme(context) ? 0x40617ac1 : 0x406f8de1;
        highlight.setColor(accent);
        final android.view.ViewOverlay overlay = row.getOverlay();
        overlay.add(highlight);
        row.post(
            new Runnable() {
              @Override
              public void run() {
                try {
                  highlight.setBounds(0, 0, row.getWidth(), row.getHeight());
                } catch (Throwable ignored) {
                }
              }
            });
        final android.animation.ValueAnimator anim =
            android.animation.ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(700);
        anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        anim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        anim.addUpdateListener(
            new android.animation.ValueAnimator.AnimatorUpdateListener() {
              @Override
              public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                try {
                  float t = (Float) animation.getAnimatedValue();
                  highlight.setAlpha((int) (t * 128));
                } catch (Throwable ignored) {
                }
              }
            });
        row.setTag(anim);
        anim.start();
      } catch (Throwable t) {
      }
    }

    private void bindCustomRow(ViewHolder holder, Item item) {
      FrameLayout wrapper = (FrameLayout) holder.itemView;
      wrapper.removeAllViews();
      if (item.customView != null) {
        FrameLayout.LayoutParams lp =
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(
            dp(context, item.customMarginLeft),
            dp(context, item.customMarginTop),
            dp(context, item.customMarginRight),
            dp(context, item.customMarginBottom));
        wrapper.addView(item.customView, lp);
      }
    }

    private void setRowClick(final View row, final Runnable action) {
      if (action == null) {
        row.setOnClickListener(null);
        return;
      }
      row.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (!row.isClickable()) {
                return;
              }
              row.setClickable(false);
              v.postDelayed(
                  new Runnable() {
                    @Override
                    public void run() {
                      action.run();
                      row.setClickable(true);
                    }
                  },
                  150);
            }
          });
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      TextView header;

      ViewHolder(View itemView) {
        super(itemView);
      }
    }
  }
}
