package tabstats.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/**
 * GuiTextField that keeps the real text value but masks what is rendered while still
 * delegating all keyboard handling to the base implementation.
 */
public class MaskedGuiTextField extends GuiTextField {
    private static final String[] LINE_SCROLL_FIELD = {"lineScrollOffset", "field_146225_q"};

    private final int trailingVisibleChars;
    private boolean revealing;

    public MaskedGuiTextField(int id, FontRenderer fontRenderer, int x, int y, int width, int height) {
        this(id, fontRenderer, x, y, width, height, 4);
    }

    public MaskedGuiTextField(int id, FontRenderer fontRenderer, int x, int y, int width, int height, int trailingVisibleChars) {
        super(id, fontRenderer, x, y, width, height);
        this.trailingVisibleChars = Math.max(0, trailingVisibleChars);
    }

    public void setRevealing(boolean revealing) {
        this.revealing = revealing;
    }

    public boolean isRevealing() {
        return revealing;
    }

    @Override
    public void drawTextBox() {
        if (revealing) {
            super.drawTextBox();
            return;
        }

        String original = getText();
        if (original.isEmpty()) {
            super.drawTextBox();
            return;
        }

        String masked = mask(original);
        if (masked.equals(original)) {
            super.drawTextBox();
            return;
        }

        int cursorPos = getCursorPosition();
        int selectionPos = getSelectionEnd();
        Integer lineOffset = getLineScrollOffset();

        try {
            super.setText(masked);
            setCursorPosition(cursorPos);
            setSelectionPos(selectionPos);
            if (lineOffset != null) {
                setLineScrollOffset(lineOffset);
            }
            super.drawTextBox();
        } finally {
            super.setText(original);
            setCursorPosition(cursorPos);
            setSelectionPos(selectionPos);
            if (lineOffset != null) {
                setLineScrollOffset(lineOffset);
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        String original = getText();
        if (revealing || original.isEmpty()) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        String masked = mask(original);
        if (masked.equals(original)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        int cursorPos = getCursorPosition();
        int selectionPos = getSelectionEnd();
        Integer lineOffset = getLineScrollOffset();

        try {
            // Use the masked value while processing the click so hit-testing matches what we draw.
            super.setText(masked);
            setCursorPosition(cursorPos);
            setSelectionPos(selectionPos);
            if (lineOffset != null) {
                setLineScrollOffset(lineOffset);
            }
            super.mouseClicked(mouseX, mouseY, mouseButton);

            cursorPos = getCursorPosition();
            selectionPos = getSelectionEnd();
            lineOffset = getLineScrollOffset();
        } finally {
            super.setText(original);
            setCursorPosition(cursorPos);
            setSelectionPos(selectionPos);
            if (lineOffset != null) {
                setLineScrollOffset(lineOffset);
            }
        }
    }

    private String mask(String value) {
        int length = value.length();
        if (length <= trailingVisibleChars) {
            return value;
        }
        int maskedCount = length - trailingVisibleChars;
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < maskedCount; i++) {
            builder.append('*');
        }
        builder.append(value.substring(maskedCount));
        return builder.toString();
    }

    private Integer getLineScrollOffset() {
        try {
            return ReflectionHelper.getPrivateValue(GuiTextField.class, this, LINE_SCROLL_FIELD);
        } catch (ReflectionHelper.UnableToAccessFieldException ignored) {
            return null;
        }
    }

    private void setLineScrollOffset(int offset) {
        try {
            ReflectionHelper.setPrivateValue(GuiTextField.class, this, offset, LINE_SCROLL_FIELD);
        } catch (ReflectionHelper.UnableToAccessFieldException ignored) {
            // If we cannot set the value we simply accept the slight visual mismatch.
        }
    }
}
