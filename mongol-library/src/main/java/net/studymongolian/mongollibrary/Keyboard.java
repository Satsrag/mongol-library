package net.studymongolian.mongollibrary;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.util.List;


public abstract class Keyboard extends ViewGroup implements Key.KeyListener {

    static final float DEFAULT_PRIMARY_TEXT_SIZE_SP = 24;
    static final int DEFAULT_PRIMARY_TEXT_COLOR = Color.BLACK;
    static final int DEFAULT_SECONDARY_TEXT_COLOR = Color.parseColor("#61000000"); // alpha black
    static final int DEFAULT_KEY_COLOR = Color.LTGRAY;
    static final int DEFAULT_KEY_PRESSED_COLOR = Color.GRAY;
    static final int DEFAULT_KEY_BORDER_COLOR = Color.BLACK;
    static final int DEFAULT_KEY_BORDER_WIDTH = 0;
    static final int DEFAULT_KEY_BORDER_RADIUS = 5;
    static final int DEFAULT_KEY_SPACING = 2;
    static final int DEFAULT_POPUP_COLOR = Color.WHITE;
    static final int DEFAULT_POPUP_TEXT_COLOR = Color.BLACK;
    static final int DEFAULT_POPUP_HIGHLIGHT_COLOR = Color.GRAY;
    static final CandidatesLocation DEFAULT_CANDIDATES_LOCATION = CandidatesLocation.NONE;
    private static final int DEFAULT_HEIGHT_DP = 240;

    protected String mDisplayName;
    private int mPopupBackgroundColor;
    private int mPopupHighlightColor;
    private int mPopupTextColor;
    private Typeface mTypeface;
    private float mPrimaryTextSizePx;
    private float mSecondaryTextSizePx;
    private int mPrimaryTextColor;
    private int mSecondaryTextColor;
    private int mKeyColor;
    private int mKeyPressedColor;
    private int mKeyBorderColor;
    private int mKeyBorderWidth;
    private int mKeyBorderRadius;
    private int mKeySpacing;
    private CandidatesLocation mCandidatesLocation;
    private boolean mShouldShowSuffixesInPopup;

    private PopupKeyCandidatesView popupView;
    private PopupWindow popupWindow;
    private int imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED;

    public enum CandidatesLocation {
        // WARNING: these values are also defined in attrs.xml
        NONE(0),
        VERTICAL_LEFT(0x73),
        HORIZONTAL_TOP(0x31);

        int id;

        CandidatesLocation(int id) {
            this.id = id;
        }

        static CandidatesLocation fromId(int id) {
            for (CandidatesLocation location : values()) {
                if (location.id == id) return location;
            }
            throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    protected boolean mIsShowingPunctuation = false;

    // number of keys and weights are initialized by keyboard subclass
    protected int[] mNumberOfKeysInRow;

    protected float[] mInsetWeightInRow;
    protected float[] mKeyWeights;
    protected OnKeyboardListener mKeyboardListener = null;

    public Keyboard(Context context) {
        super(context);
        init(context, null);
    }

    public Keyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public Keyboard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public Keyboard(Context context, StyleBuilder builder) {
        super(context);
        init(context, builder);
    }


    private void init(Context context, StyleBuilder builder) {
        if (builder == null)
            builder = new StyleBuilder();
        mDisplayName = getDisplayName();
        mTypeface = builder.nestedTypeface;
        if (mTypeface == null)
            mTypeface = MongolFont.get(MongolFont.QAGAN, context);
        mPrimaryTextSizePx = builder.nestedPrimaryTextSizePx;
        if (mPrimaryTextSizePx <= 0)
            mPrimaryTextSizePx = getDefaultPrimaryTextSizeInPixels();
        mPrimaryTextColor = builder.nestedPrimaryTextColor;
        mSecondaryTextColor = builder.nestedSecondaryTextColor;
        mKeyColor = builder.nestedKeyColor;
        mKeyPressedColor = builder.nestedKeyPressedColor;
        mKeyBorderColor = builder.nestedKeyBorderColor;
        mKeyBorderWidth = builder.nestedKeyBorderWidth;
        mKeyBorderRadius = builder.nestedKeyBorderRadius;
        mKeySpacing = builder.nestedKeySpacing;
        mPopupBackgroundColor = builder.nestedPopupBackgroundColor;
        mPopupHighlightColor = builder.nestedPopupHighlightColor;
        mPopupTextColor = builder.nestedPopupTextColor;
        mCandidatesLocation = builder.nestedCandidatesLocation;
        setCommonDefaults();
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Keyboard, defStyleAttr, 0);
        mDisplayName = a.getString(R.styleable.Keyboard_displayName);
        if (mDisplayName == null) mDisplayName = getDisplayName();
        String fontFile = a.getString(R.styleable.Keyboard_fontAssetFile);
        mTypeface = MongolFont.get(fontFile, context);
        if (mTypeface == null) mTypeface = MongolFont.get(MongolFont.QAGAN, context);
        mPrimaryTextSizePx = a.getDimensionPixelSize(R.styleable.Keyboard_primaryTextSize,
                getDefaultPrimaryTextSizeInPixels());
        mPrimaryTextColor = a.getColor(R.styleable.Keyboard_primaryTextColor,
                DEFAULT_PRIMARY_TEXT_COLOR);
        mSecondaryTextColor = a.getColor(R.styleable.Keyboard_secondaryTextColor,
                DEFAULT_SECONDARY_TEXT_COLOR);
        mKeyColor = a.getColor(R.styleable.Keyboard_keyColor,
                DEFAULT_KEY_COLOR);
        mKeyPressedColor = a.getColor(R.styleable.Keyboard_keyPressedColor,
                DEFAULT_KEY_PRESSED_COLOR);
        mKeyBorderColor = a.getColor(R.styleable.Keyboard_keyBorderColor,
                DEFAULT_KEY_BORDER_COLOR);
        mKeyBorderWidth = a.getDimensionPixelSize(R.styleable.Keyboard_keyBorderWidth,
                DEFAULT_KEY_BORDER_WIDTH);
        mKeyBorderRadius = a.getDimensionPixelSize(R.styleable.Keyboard_keyBorderRadius,
                DEFAULT_KEY_BORDER_RADIUS);
        mKeySpacing = a.getDimensionPixelSize(R.styleable.Keyboard_keySpacing,
                DEFAULT_KEY_SPACING);
        mPopupBackgroundColor = a.getColor(R.styleable.Keyboard_popupBackgroundColor,
                DEFAULT_POPUP_COLOR);
        mPopupHighlightColor = a.getColor(R.styleable.Keyboard_popupHighlightColor,
                DEFAULT_POPUP_HIGHLIGHT_COLOR);
        mPopupTextColor = a.getColor(R.styleable.Keyboard_popupTextColor,
                DEFAULT_POPUP_TEXT_COLOR);
        mCandidatesLocation = CandidatesLocation.fromId(a.getInt(R.styleable.Keyboard_candidatesLocation,
                DEFAULT_CANDIDATES_LOCATION.id));
        a.recycle();
        setCommonDefaults();
    }

    private void setCommonDefaults() {
        mSecondaryTextSizePx = mPrimaryTextSizePx / 2;
        mShouldShowSuffixesInPopup = true;
    }

    private int getDefaultPrimaryTextSizeInPixels() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                DEFAULT_PRIMARY_TEXT_SIZE_SP, getResources().getDisplayMetrics());
    }

    public interface OnKeyboardListener {
        List<PopupKeyCandidate> getAllKeyboardNames();

        void onRequestNewKeyboard(String keyboardDisplayName);

        void onFinished(View caller);

        void onKeyboardInput(String text);

        void onKeyPopupChosen(PopupKeyCandidate popupKeyCandidate);

        void onBackspace();

        CharSequence getTextBeforeCursor(int numberOfChars);

        CharSequence getTextAfterCursor(int numberOfChars);

        String getPreviousMongolWord(boolean allowSingleSpaceBeforeCursor);
    }

    public void setOnKeyboardListener(OnKeyboardListener listener) {
        this.mKeyboardListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int desiredWidth = Integer.MAX_VALUE;
        int desiredHeight = getDefaultHeight();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);
    }

    public int getDefaultHeight() {
        return (int) (DEFAULT_HEIGHT_DP * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        // this must be set by the subclass
        int numberOfRows = mNumberOfKeysInRow.length;

        final int totalWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        final int totalHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        float x = getPaddingLeft();
        float y = getPaddingTop();
        int keyIndex = 0;
        for (int rowIndex = 0; rowIndex < numberOfRows; rowIndex++) {

            if (mInsetWeightInRow != null) {
                x += (totalWidth * mInsetWeightInRow[rowIndex]);
            }
            int end = keyIndex + mNumberOfKeysInRow[rowIndex];
            for (int i = keyIndex; i < end; i++) {
                View child = getChildAt(keyIndex);

                float keyWidth = totalWidth * mKeyWeights[keyIndex];
                float keyHeight = totalHeight / (float) numberOfRows;
                child.measure(MeasureSpec.makeMeasureSpec((int) keyWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec((int) keyHeight, MeasureSpec.EXACTLY));

                child.layout((int) x, (int) y, (int) (x + keyWidth), (int) (y + keyHeight));
                x += keyWidth;
                keyIndex++;
            }

            x = getPaddingLeft();
            y += (float) totalHeight / numberOfRows;
        }
    }

    protected void applyThemeToKeys() {
        for (int i = 0; i < getChildCount(); i++) {
            Key child = (Key) getChildAt(i);
            if (child instanceof KeyText) {
                ((KeyText) child).setTextSize(mPrimaryTextSizePx);
                ((KeyText) child).setTextColor(mPrimaryTextColor);
            } else if (child instanceof KeyImage) {
                // TODO apply theme to key image

                if (child instanceof KeyShift) {
                    ((KeyShift) child).setCapsStateIndicatorColor(mPrimaryTextColor);
                }
            }

            child.setTypeFace(mTypeface);
            child.setSubTextSize(mSecondaryTextSizePx);
            child.setSubTextColor(mSecondaryTextColor);
            child.setKeyColor(mKeyColor);
            child.setPressedColor(mKeyPressedColor);
            child.setBorderColor(mKeyBorderColor);
            child.setBorderWidth(mKeyBorderWidth);
            child.setBorderRadius(mKeyBorderRadius);
            child.setPadding(mKeySpacing, mKeySpacing, mKeySpacing, mKeySpacing);
        }
    }

    protected Bitmap getReturnImage() {
        int imageResourceId;
        if (imeOptions == EditorInfo.IME_ACTION_SEARCH) {
            imageResourceId = R.drawable.outline_search_black_24;
        } else {
            imageResourceId = R.drawable.ic_keyboard_return_32dp;
        }
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getBackspaceImage() {
        int imageResourceId = R.drawable.ic_keyboard_backspace_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getKeyboardImage() {
        int imageResourceId = R.drawable.ic_keyboard_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getUpImage() {
        int imageResourceId = R.drawable.ic_keyboard_up_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getCopyImage() {
        int imageResourceId = R.drawable.ic_keyboard_copy_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getLeftImage() {
        int imageResourceId = R.drawable.ic_keyboard_left_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getSelectAllImage() {
        int imageResourceId = R.drawable.ic_keyboard_select_all_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getRightImage() {
        int imageResourceId = R.drawable.ic_keyboard_right_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getCutImage() {
        int imageResourceId = R.drawable.ic_keyboard_cut_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getDownImage() {
        int imageResourceId = R.drawable.ic_keyboard_down_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getEndImage() {
        int imageResourceId = R.drawable.ic_keyboard_end_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getPasteImage() {
        int imageResourceId = R.drawable.ic_keyboard_paste_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getBackImage() {
        int imageResourceId = R.drawable.ic_keyboard_back_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getStartImage() {
        int imageResourceId = R.drawable.ic_keyboard_start_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getSelectBackImage() {
        int imageResourceId = R.drawable.ic_keyboard_select_back_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }

    protected Bitmap getSelectForwardImage() {
        int imageResourceId = R.drawable.ic_keyboard_select_forward_32dp;
        return BitmapFactory.decodeResource(getResources(), imageResourceId);
    }


    protected char getPreviousChar() {
        if (mKeyboardListener == null) return 0;
        CharSequence before = mKeyboardListener.getTextBeforeCursor(1);
        if (TextUtils.isEmpty(before)) return 0;
        return before.charAt(0);
    }

    protected boolean isIsolateOrInitial() {
        if (mKeyboardListener == null) return true;
        CharSequence before = mKeyboardListener.getTextBeforeCursor(2);
        CharSequence after = mKeyboardListener.getTextAfterCursor(2);
        MongolCode.Location location = MongolCode.getLocation(before, after);
        return location == MongolCode.Location.ISOLATE ||
                location == MongolCode.Location.INITIAL;
    }

    public List<PopupKeyCandidate> getCandidatesForKeyboardKey() {
        if (mKeyboardListener == null) return null;
        return mKeyboardListener.getAllKeyboardNames();
    }

    abstract public List<PopupKeyCandidate> getPopupCandidates(Key key);

    // subclasses should return the default name of the keyboard to display in the
    // keyboard chooser popup
    abstract public String getDisplayName();

    public void setImeOptions(int options) {
        imeOptions = options;
    }

    public void setCandidatesLocation(CandidatesLocation location) {
        mCandidatesLocation = location;
    }

    public boolean hasCandidatesView() {
        return mCandidatesLocation != CandidatesLocation.NONE;
    }

    @SuppressWarnings("unused")
    public void setShouldShowSuffixesInPopup(boolean whether) {
        mShouldShowSuffixesInPopup = whether;
    }

    protected boolean shouldShowSuffixesInPopup() {
        return mShouldShowSuffixesInPopup;
    }

    public CandidatesLocation getCandidatesLocation() {
        return mCandidatesLocation;
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public int getKeyColor() {
        return mKeyColor;
    }

    public int getKeyPressedColor() {
        return mKeyPressedColor;
    }

    public float getPrimaryTextSize() {
        return mPrimaryTextSizePx;
    }

    public int getPrimaryTextColor() {
        return mPrimaryTextColor;
    }

    public int getBorderColor() {
        return mKeyBorderColor;
    }

    public int getBorderWidth() {
        return mKeyBorderWidth;
    }

    public int getBorderRadius() {
        return mKeyBorderRadius;
    }

    public int getKeySpacing() {
        return mKeySpacing;
    }

    public int getPopupBackgroundColor() {
        return mPopupBackgroundColor;
    }

    public int getPopupHighlightColor() {
        return mPopupHighlightColor;
    }

    public int getPopupTextColor() {
        return mPopupTextColor;
    }

    public boolean isShowingPunctuation() {
        return mIsShowingPunctuation;
    }

    // KeyListener methods

    @Override
    public void onKeyInput(String text) {
        if (mKeyboardListener == null) return;
        mKeyboardListener.onKeyboardInput(text);
    }

    @Override
    public boolean getIsShowingPopup() {
        return popupView != null;
    }

    @Override
    public void showPopup(Key key, final int xPosition) {
        List<PopupKeyCandidate> popupCandidates = getPopupCandidates(key);
        if (popupCandidates == null || popupCandidates.size() == 0) return;
        popupView = getPopupView();
        popupView.setCandidates(popupCandidates);
        layoutAndShowPopupWindow(key, xPosition);
        highlightCurrentItemAfterPopupWindowHasLoaded(key, xPosition);
    }

    private PopupKeyCandidatesView getPopupView() {
        PopupKeyCandidatesView popupView = new PopupKeyCandidatesView(getContext());
        popupView.setBackgroundColor(mPopupBackgroundColor);
        popupView.setTextColor(mPopupTextColor);
        popupView.setHighlightColor(mPopupHighlightColor);
        popupView.setTypeface(mTypeface);
        return popupView;
    }

    private void layoutAndShowPopupWindow(Key key, int xPosition) {
        popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setClippingEnabled(false);
        int[] location = new int[2];
        key.getLocationInWindow(location);
        int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        popupView.measure(measureSpec, measureSpec);
        int popupWidth = popupView.getMeasuredWidth();
        int spaceAboveKey = key.getHeight() / 4;
        int x = xPosition - popupWidth / popupView.getChildCount() / 2;
        int screenWidth = getScreenWidth();
        if (x < 0) {
            x = 0;
        } else if (x + popupWidth > screenWidth) {
            x = screenWidth - popupWidth;
        }
        int y = location[1] - popupView.getMeasuredHeight() - spaceAboveKey;
        popupWindow.showAtLocation(key, Gravity.NO_GRAVITY, x, y);
        //popupWindow.showAsDropDown(key, 0, -500);
    }

    private static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    private void highlightCurrentItemAfterPopupWindowHasLoaded(Key key, final int xPosition) {
        key.post(new Runnable() {
            @Override
            public void run() {
                if (popupView == null) return;
                popupView.updateTouchPosition(xPosition);
            }
        });
    }

    @Override
    public void updatePopup(int xPosition) {
        if (popupView == null) return;
        popupView.updateTouchPosition(xPosition);
    }

    @Override
    public void finishPopup(int xPosition) {
        if (popupWindow == null) return;
        PopupKeyCandidate selectedItem = popupView.getCurrentItem(xPosition);
        inputPopupChoice(selectedItem);
        dismissPopup();
    }

    private void inputPopupChoice(PopupKeyCandidate choice) {
        if (mKeyboardListener == null) return;
        mKeyboardListener.onKeyPopupChosen(choice);
    }

    private void dismissPopup() {
        if (popupWindow != null)
            popupWindow.dismiss();
        popupView = null;
        popupWindow = null;
    }

    @Override
    public void onBackspace() {
        if (mKeyboardListener == null) return;
        mKeyboardListener.onBackspace();
    }


    @Override
    public void onNewKeyboardChosen(int xPosition) {
        if (mKeyboardListener == null) return;
        PopupKeyCandidate selectedKeyboard = popupView.getCurrentItem(xPosition);
        dismissPopup();
        if (selectedKeyboard == null) return;
        mKeyboardListener.onRequestNewKeyboard(selectedKeyboard.getUnicode());
    }

    protected void requestNewKeyboard() {
        if (mKeyboardListener == null) return;
        mKeyboardListener.onRequestNewKeyboard(null);
    }

    protected void finishKeyboard() {
        if (mKeyboardListener == null) return;
        mKeyboardListener.onFinished(this);
    }

    @Override
    public void onShiftChanged(boolean isShiftOn) {
        // Keyboard subclasses can override this if they have a shift key
    }

    public static class StyleBuilder {

        private Typeface nestedTypeface;
        private int nestedPopupBackgroundColor = DEFAULT_POPUP_COLOR;
        private int nestedPopupHighlightColor = DEFAULT_POPUP_HIGHLIGHT_COLOR;
        private int nestedPopupTextColor = DEFAULT_POPUP_TEXT_COLOR;
        private float nestedPrimaryTextSizePx = 0;
        private int nestedPrimaryTextColor = DEFAULT_PRIMARY_TEXT_COLOR;
        private int nestedSecondaryTextColor = DEFAULT_SECONDARY_TEXT_COLOR;
        private int nestedKeyColor = DEFAULT_KEY_COLOR;
        private int nestedKeyPressedColor = DEFAULT_KEY_PRESSED_COLOR;
        private int nestedKeyBorderColor = DEFAULT_KEY_BORDER_COLOR;
        private int nestedKeyBorderWidth = DEFAULT_KEY_BORDER_WIDTH;
        private int nestedKeyBorderRadius = DEFAULT_KEY_BORDER_RADIUS;
        private int nestedKeySpacing = DEFAULT_KEY_SPACING;
        private CandidatesLocation nestedCandidatesLocation = DEFAULT_CANDIDATES_LOCATION;

        public StyleBuilder popupBackgroundColor(int color) {
            this.nestedPopupBackgroundColor = color;
            return this;
        }

        public StyleBuilder popupHighlightColor(int color) {
            this.nestedPopupHighlightColor = color;
            return this;
        }

        public StyleBuilder popupTextColor(int color) {
            this.nestedPopupTextColor = color;
            return this;
        }

        public StyleBuilder typeface(Typeface typeface) {
            this.nestedTypeface = typeface;
            return this;
        }

        public StyleBuilder primaryTextSizePx(float sizePx) {
            this.nestedPrimaryTextSizePx = sizePx;
            return this;
        }

        public StyleBuilder primaryTextColor(int color) {
            this.nestedPrimaryTextColor = color;
            return this;
        }

        public StyleBuilder secondaryTextColor(int color) {
            this.nestedSecondaryTextColor = color;
            return this;
        }

        public StyleBuilder keyColor(int color) {
            this.nestedKeyColor = color;
            return this;
        }

        public StyleBuilder keyPressedColor(int color) {
            this.nestedKeyPressedColor = color;
            return this;
        }

        public StyleBuilder keyBorderColor(int color) {
            this.nestedKeyBorderColor = color;
            return this;
        }

        public StyleBuilder keyBorderWidth(int width) {
            this.nestedKeyBorderWidth = width;
            return this;
        }

        public StyleBuilder keyBorderRadius(int radius) {
            this.nestedKeyBorderRadius = radius;
            return this;
        }

        public StyleBuilder keySpacing(int spacing) {
            this.nestedKeySpacing = spacing;
            return this;
        }

        public StyleBuilder candidatesLocation(CandidatesLocation location) {
            this.nestedCandidatesLocation = location;
            return this;
        }

    }

}
